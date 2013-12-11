package controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.mongodb.DBObject;
import com.mongodb.DBRef;

import play.Logger;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

import logic.FeedsLogic;
import logic.TalkerLogic;
import logic.TopicLogic;
import models.CommentBean;
import models.ConversationBean;
import models.DiseaseBean;
import models.GuidanceNewsLetterBean;
import models.NewsLetterBean;
import models.ServiceAccountBean;
import models.VideoBean;
import models.PrivacySetting.PrivacyType;
import models.ServiceAccountBean.ServiceType;
import models.TalkerBean;
import models.TopicBean;
import models.actions.Action;
import play.Logger;
import play.data.validation.Error;
import util.EmailUtil;
import util.BitlyUtil;
import util.CommonUtil;
import util.DBUtil;
import util.FacebookUtil;
import util.SearchUtil;
import util.TwitterUtil;
import util.jobs.ConvoFromTwitterJob;
import util.jobs.EmailReminderJob;
import util.jobs.ThoughtsFromServicesJob;
import dao.ActionDAO;
import dao.ActivityLogDAO;
import dao.ApplicationDAO;
import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.DiseaseDAO;
import dao.NewsLetterDAO;
import dao.TalkerDAO;
import dao.TopicDAO;
import dao.VideoDAO;

@With( LoggerController.class )
public class Explore extends Controller {
	
	@Before
	static void prepareParams() {
		//used for Tw/Fb sharing and SEO
        String currentURL = "http://"+request.host+request.path;
        renderArgs.put("currentURL", currentURL);
	}
	
	public static void openQuestions() {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	//boolean loggedIn = (talker != null);
    	boolean newsLetterFlag = false;
    	boolean rewardLetterFlag = false;
    	if (talker != null) {
    		TalkerLogic.preloadTalkerInfo(talker);
    		newsLetterFlag = ApplicationDAO.isEmailExists(talker.getEmail());
    		rewardLetterFlag=ApplicationDAO.isnewsLetterSubscribe(talker.getEmail(),"TalkAboutHealth Rewards");
    	}
    	int limit = session.get("topicCount")==null?TopicLogic.TOPICS_PER_PAGE:Integer.parseInt(session.get("topicCount"));
    	List<TopicBean> popularTopics = TopicLogic.loadPopularTopics(limit);
    	String cancerType = session.get("cancerType"); 
		List<ConversationBean> openQuestions = ConversationDAO.getOpenQuestions(null,cancerType);
		
		render(talker, openQuestions, popularTopics,newsLetterFlag,rewardLetterFlag);
    }
    
    public static void liveTalks() {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	List<TopicBean> popularTopics = null;
    	if (talker != null) {
    		TalkerLogic.preloadTalkerInfo(talker);
    	} else {
    		int limit = session.get("topicCount")==null?TopicLogic.TOPICS_PER_PAGE:Integer.parseInt(session.get("topicCount"));
    		popularTopics = TopicLogic.loadPopularTopics(limit);
    	}
    	List<ConversationBean> liveTalks = ConversationDAO.getLiveConversations();
		render(talker, liveTalks, popularTopics);
    }
    
    public static void browseTopics() {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	int limit = FeedsLogic.FEEDS_PER_PAGE;
    	//In case you want to save the populated list
    	//session.get("topicCount")==null?20:Integer.parseInt(session.get("topicCount"));
    	session.put("topicCount", TopicLogic.TOPICS_PER_PAGE);
    	boolean newsLetterFlag = false;
    	boolean rewardLetterFlag = false;
    	if (talker != null) {
    		TalkerLogic.preloadTalkerInfo(talker);
    		newsLetterFlag = ApplicationDAO.isEmailExists(talker.getEmail());
    		rewardLetterFlag=ApplicationDAO.isnewsLetterSubscribe(talker.getEmail(),"TalkAboutHealth Rewards");
    	}

    	List<TopicBean> popularTopics = TopicLogic.loadPopularTopics(TopicLogic.TOPICS_PER_PAGE);

    	Set<TopicBean> topicsTree = TopicLogic.getAllTopicsTree();

    	render(topicsTree, talker, popularTopics, newsLetterFlag,rewardLetterFlag, limit);
    }
    
    /**
     * Load topic aJax list for more link on the topic page
     * @param topicCount
     */
    public static void topicAjaxLoad(){
    	List<TopicBean> _popularTopics = new ArrayList<TopicBean>();
		for (TopicBean topic : TalkerLogic.loadAllTopicsFromCache()) {
			if (topic.getConversations() == null) {
				topic.setConversations(ConversationDAO.loadConversationsByTopic(topic.getId()));
			}
			_popularTopics.add(topic);
		}
		
		//sort by number of questions
		Collections.sort(_popularTopics, new Comparator<TopicBean>() {
			@Override
			public int compare(TopicBean o1, TopicBean o2) {
				return o2.getConversations().size() - o1.getConversations().size();
			}		
		});
		
    	int limit = 5;
    	int topicCount = session.get("topicCount")==null?FeedsLogic.FEEDS_PER_PAGE:Integer.parseInt(session.get("topicCount"));
    	limit = topicCount + limit;
        if (_popularTopics.size() > limit) {
        	_popularTopics = _popularTopics.subList(topicCount, limit);
        } else {
        	_popularTopics = null;
        }

    	boolean more = true;
    	session.put("topicCount", limit);
    	render("tags/common/popularTopics.html", _popularTopics,more,limit);
    }
    
    public static void browseMembers(String action) throws Throwable {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);

		//List<String> cat = FeedsLogic.getCancerType(currentTalker);
		
		//Active talkers on this day
		// Displaying members active since last 2 week's rather than 1 week
		Calendar oneWeekBeforeNow = Calendar.getInstance();
		oneWeekBeforeNow.add(Calendar.WEEK_OF_YEAR, -2);
		
		String cancerType = session.get("cancerType");
		List<TalkerBean> activeTalkers =  ApplicationDAO.getActiveTalkers(oneWeekBeforeNow.getTime(),cancerType);
		
		//List<TalkerBean> activeTalkers = new ArrayList<TalkerBean>();
		//for(TalkerBean talkerBean : activeTalkers1) {
		//		activeTalkers.add(talkerBean);
		//}
		
		if(activeTalkers != null && activeTalkers.size() > TalkerLogic.TALKERS_PER_PAGE)
			activeTalkers = activeTalkers.subList(0, TalkerLogic.TALKERS_PER_PAGE);
		
		List<TalkerBean> newTalkers = ApplicationDAO.getNewTalkers(cancerType);

		//List<TalkerBean> newTalkers = new ArrayList<TalkerBean>();
		//for(TalkerBean talkerBean : newTalkers1){
		//		newTalkers.add(talkerBean);
		//}
		
		if(newTalkers != null && newTalkers.size() > TalkerLogic.TALKERS_PER_PAGE)
			newTalkers = newTalkers.subList(0,TalkerLogic.TALKERS_PER_PAGE); 

		//check if search is performed now
		String query = params.get("query");
		List<TalkerBean> results = null;
		if (query != null) {
			params.flash("query");
			try {
				results = SearchUtil.searchTalker(query,currentTalker,cancerType);
				if(results != null && results.size() > TalkerLogic.TALKERS_PER_PAGE)
					results = results.subList(0, TalkerLogic.TALKERS_PER_PAGE);
			} catch (Exception e) {
				Logger.error(e,"Explore.java : browseMembers");
			}
		}
		
		
		
		//Move members to particular tabs based on member's connection
		Map<String, Set<TalkerBean>> members = new LinkedHashMap<String, Set<TalkerBean>>();
		members.put("Experts", new LinkedHashSet<TalkerBean>());
		members.put("Patients", new LinkedHashSet<TalkerBean>());
		members.put("Former Patients", new LinkedHashSet<TalkerBean>());
		//members.put("Parents", new LinkedHashSet<TalkerBean>());
		//members.put("Caregivers", new LinkedHashSet<TalkerBean>());
		members.put("Family and Friends", new LinkedHashSet<TalkerBean>());
		if(StringUtils.isBlank(cancerType)){
			List<DiseaseBean> diseaseList = DiseaseDAO.getCatchedDiseasesList(session);
			for (DiseaseBean diseaseBean : diseaseList) {
				members.put(diseaseBean.getName() , new LinkedHashSet<TalkerBean>());	
			}
		}
		//members.put("Breast Cancer", new LinkedHashSet<TalkerBean>());
		//members.put("Ovarian Cancer", new LinkedHashSet<TalkerBean>());

		
		/*
		//match tabs with possible connections
		Map<String, List<String>> memberTypes = new LinkedHashMap<String, List<String>>();
		memberTypes.put("Experts", TalkerBean.PROFESSIONAL_CONNECTIONS_LIST);
		memberTypes.put("Patients", Arrays.asList("Just Diagnosed","Current Patient"));
		memberTypes.put("Former Patients", Arrays.asList("Survivor (1 year)","Survivor (2 - 5 years)","Survivor (5 - 10 years)","Survivor (10 - 20 years)","Survivor (Greater than 20 years)"));
		memberTypes.put("Parents", Arrays.asList("Parent"));
		memberTypes.put("Caregivers", Arrays.asList("Caregiver"));
		memberTypes.put("Family & Friends", Arrays.asList("Family member", "Friend"));
		
		//Set<TalkerBean> allActiveTalkers = ApplicationDAO.getActiveTalkers(null);
		List<TalkerBean> allActiveTalkers = TalkerDAO.loadAllTalkers(true,currentTalker);
		//re-structure members by connection type
		
		for (TalkerBean talker : allActiveTalkers) {
			for (Entry<String, List<String>> memberTypeEntry : memberTypes.entrySet()) {
				if (memberTypeEntry.getValue().contains(talker.getConnection()) && talker.getName() != null) {
					if(members.get(memberTypeEntry.getKey()).size() < TalkerLogic.TALKERS_PER_PAGE){
						members.get(memberTypeEntry.getKey()).add(talker);
					}
				}
			}
		}
*/

		//default tab is 'active'
		if (action == null || action.equals("browsemembers")) {
			action = "active";
		}
		boolean loggedIn = (currentTalker != null);
		activeTalkers = TalkerDAO.getSortedTalkerList(activeTalkers,loggedIn);
		render(currentTalker, action, activeTalkers, newTalkers, results, members);
	}
    

	public static void searchConversations() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		String query = params.get("query");
		List<ConversationBean> results = null;
		if (query != null) {
			params.flash("query");
			try {
				String cancerType = session.get("cancerType");
				results = SearchUtil.searchConvo(query, 10, talker,cancerType);
			}
			catch (Exception e) {
				Logger.error(e, "Explore.java : searchConversations");
			}
		}
		render(talker, results);
	}

	/**
	 * Page with conversations feed and popular conversations
	 * @param action default tab ('feed' or 'popular')
	 */
	public static void conversations(String action) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		Set<Action> recentConvo = null;

		List<TopicBean> popularTopics = null;
		if (action == null) {
			action = "feed";
		}
		render(action, recentConvo, popularTopics,talker);
	}
	public static void ajaxFeedUpdate(String type){
		String cancerType = session.get("cancerType");
		if(type.equals("popularConvo")) {
			List<ConversationBean> popularConvo = ConversationDAO.loadPopularAnswers("popular",null);
			render("Explore/feedList.html",popularConvo, type);
		}
		if(type.equals("expertConvo")) {
			List<ConversationBean> expertConvo = ConversationDAO.loadExpertsAnswer(null,cancerType);
			render("Explore/feedList.html",expertConvo, type);
		}
		if(type.equals("openConvo")) {
			List<ConversationBean> openConvo = ConversationDAO.getOpenQuestions(null,cancerType);
			render("Explore/feedList.html",openConvo, type);
		}
		if(type.equals("recentConvo")) {
			List<ConversationBean> recentConvo = ConversationDAO.loadSharedExperiences(null,cancerType);
			render("Explore/feedList.html",recentConvo, type);
		}
	}

	public static void homePageFeed(String type, String lastActionId){
		if(lastActionId != null && "".equals(lastActionId))
			lastActionId = null;
		if(type.equals("expert")) {
			String cancerType = session.get("cancerType"); 
			List<ConversationBean> convoFeed = ConversationDAO.loadExpertsAnswer(lastActionId,cancerType);
			render("Explore/homeFeedList.html",convoFeed, type);
		}
		if(type.equals("open")) {
			String cancerType = session.get("cancerType"); 
			List<ConversationBean> convoFeed = ConversationDAO.getOpenQuestions(lastActionId,cancerType);
			render("Explore/homeFeedList.html",convoFeed, type);
		}
		if(type.equals("recent")){
			
			String cancerType = session.get("cancerType"); 
			List<ConversationBean> convoFeed = ConversationDAO.loadSharedExperiences(lastActionId,cancerType);
			render("Explore/homeFeedList.html",convoFeed, type);

			/* Removed Old Listing for new tab changes
			TalkerBean talker = CommonUtil.loadCachedTalker(session);
			Set<Action> convoFeed = FeedsLogic.getAllCancerFeed(lastActionId, Security.isConnected(), talker);	
			render("Explore/homeFeedList.html",convoFeed, type);*/
		}
	}

	/**
	 * Used by "More" button in different feeds.
	 * @param afterActionId load actions after given action
	 */
    public static void feedAjaxLoad(String feedType, String afterActionId, String talkerName) {
    	TalkerBean _talker = CommonUtil.loadCachedTalker(session);
    	boolean loggedIn = (_talker != null);
    	Set<Action> _feedItems = null;
    	List<TalkerBean> _similarMembers = null;
    	List<ConversationBean> popularConvos = null;
    	String cancerType = session.get("cancerType"); 
    	if ("convoFeed".equalsIgnoreCase(feedType)) {
    		_feedItems = FeedsLogic.getConvoFeed(_talker, afterActionId);
    		render("tags/feed/feedList.html", _feedItems, _talker);
    	} else if ("communityFeed".equalsIgnoreCase(feedType)) {
    		_feedItems = FeedsLogic.getCommunityFeed(afterActionId, loggedIn, _talker);
    		render("tags/feed/feedList.html", _feedItems, _talker);
    	} else if ("popularConvo".equalsIgnoreCase(feedType)){
    	     popularConvos = ConversationDAO.loadPopularAnswers("popular",afterActionId);
    	     render("tags/convo/convoList.html", popularConvos);
        }else if ("recentConvo".equalsIgnoreCase(feedType)){
        	List<ConversationBean> recentConvo = ConversationDAO.loadSharedExperiences(afterActionId,cancerType);
        	String type = feedType;
    		render("tags/feed/recentFeedList.html", recentConvo, _talker,type);
	    }else if ("expertConvo".equalsIgnoreCase(feedType)) {
	    	String type = feedType;
	   	     List<ConversationBean> expertConvos = null;
	    	 expertConvos = ConversationDAO.loadExpertsAnswer(afterActionId,cancerType);
		     render("tags/convo/convoList.html", expertConvos ,type);
	    } else if("openConvo".equalsIgnoreCase(feedType)) {
			List<ConversationBean> openConvo = ConversationDAO.getOpenQuestions(afterActionId,cancerType);
			render("tags/convo/convoList.html",openConvo, feedType);
		} else if("USR".equalsIgnoreCase(feedType) || "EXP".equalsIgnoreCase(feedType)){
    		_similarMembers = TalkerLogic.getRecommendedTalkers(_talker,feedType,afterActionId,cancerType);
    		render("tags/profile/similarMemberList.html", _similarMembers);
    	} else if("TOPIC".equals(feedType)) {
    		List<TopicBean> _recommendedTopics = TalkerLogic.getRecommendedTopics(_talker,afterActionId);
    		render("tags/topicList.html", _recommendedTopics);
    	} else {
    		TalkerBean profileTalker = TalkerDAO.getByUserName(talkerName);
    		if (profileTalker != null) {
    			_feedItems = FeedsLogic.getTalkerFeed(profileTalker, afterActionId);
    		}
    		render("tags/feed/feedList_new.html", _feedItems, _talker);
    	}
    }
    
	public static void ajaxLoadMoreUser(String feedType, String afterActionId,String searchTerm){
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		List<TalkerBean> activeTalkers = null;
		boolean loadFlag = false;
		String cancerType = session.get("cancerType");
		boolean loggedIn = (currentTalker != null);
		if ("active".equals(feedType)) {
			Calendar twoWeekBeforeNow = Calendar.getInstance();
			twoWeekBeforeNow.add(Calendar.WEEK_OF_YEAR, -2);
			activeTalkers =  ApplicationDAO.getActiveTalkers(twoWeekBeforeNow.getTime(),cancerType);
			//activeTalkers = TalkerDAO.getSortedTalkerList(activeTalkers,loggedIn);
		} else if("new".equals(feedType)) {
			activeTalkers = ApplicationDAO.getNewTalkers(cancerType);
		} else if("search".equals(feedType)) {
			try {
				activeTalkers = SearchUtil.searchTalker(searchTerm,currentTalker,cancerType);
			} catch (Exception e) {
					Logger.error(e, "Explore.java : ajaxLoadMoreUser");
			}
		} else {
			List<String> memberTypeEntry = null;
			if("Experts".equals(feedType))
				memberTypeEntry = TalkerBean.PROFESSIONAL_CONNECTIONS_LIST;
			else if("Patients".equals(feedType))
				memberTypeEntry = Arrays.asList("Just Diagnosed","Current Patient");
			else if("Former Patients".equals(feedType))
				memberTypeEntry = Arrays.asList("Survivor (1 year)","Survivor (2 - 5 years)","Survivor (5 - 10 years)","Survivor (10 - 20 years)","Survivor (Greater than 20 years)");
			/*else if("Parents".equals(feedType))
				memberTypeEntry = Arrays.asList("Parent");
			else if("Caregivers".equals(feedType))
				memberTypeEntry = Arrays.asList("Caregiver");*/
			else if("Family & Friends".equals(feedType))
				memberTypeEntry = Arrays.asList("Family member", "Friend","Parent","Caregiver","other");
			/*else if("Breast Cancer".equals(feedType)){
				memberTypeEntry = Arrays.asList(null,"Breast Cancer");
				loadFlag = true;
			}*/ else {  //if("Ovarian Cancer".equals(feedType)){ //Ovarian Cancer  
				feedType = feedType.replaceAll("&", "and");
				if("Non Hodgkin Lymphoma".equals(feedType))
					feedType = "Non-Hodgkin Lymphoma";
				memberTypeEntry = Arrays.asList(feedType);
				loadFlag = true;
			}
			List<TalkerBean> allActiveTalkers = null;
			activeTalkers = new ArrayList<TalkerBean>();
			
			if(loadFlag) {
				allActiveTalkers = TalkerDAO.loadAllTalkersByCategory(true,memberTypeEntry);
				allActiveTalkers = TalkerDAO.getSortedTalkerList(allActiveTalkers,loggedIn);
				for (TalkerBean talker : allActiveTalkers) {
					if (talker.getName() != null)
						activeTalkers.add(talker);
				}
			} else {
				allActiveTalkers = TalkerDAO.loadAllTalkers(true,currentTalker,cancerType);
				allActiveTalkers = TalkerDAO.getSortedTalkerList(allActiveTalkers,loggedIn);
				for (TalkerBean talker : allActiveTalkers) {
					if (memberTypeEntry.contains(talker.getConnection()) && talker.getName() != null) 
						activeTalkers.add(talker);
				}
			}
		}

		int talkerCounter = 1;
		if(activeTalkers != null) {
			if(afterActionId != null && !afterActionId.equals("")) {
				for (TalkerBean talkerBean : activeTalkers) {
					if(afterActionId.equals(talkerBean.getId()))
						break;
					else
						talkerCounter++;
				}
			}else
				talkerCounter = 0;
			int limit = talkerCounter + TalkerLogic.TALKERS_PER_PAGE;
			if(talkerCounter < activeTalkers.size() && limit >  activeTalkers.size()) {
				limit = activeTalkers.size();
				activeTalkers = activeTalkers.subList(talkerCounter, limit);
			} else if(limit >  activeTalkers.size()) {
				activeTalkers = null;
			} else {
				activeTalkers = activeTalkers.subList(talkerCounter, limit);
			}
			
			if(afterActionId != null && !afterActionId.equals("")) 
				render("tags/talker/talkerList.html", activeTalkers,currentTalker,feedType);
			else
				render("tags/talker/actionTalkerList.html", activeTalkers,currentTalker,feedType);
		} else {
			renderText("Error");
		}
	}
	/**
	 * Page with conversations feed of particular disease
	 * @param cancerType
	 */
	public static void community(String csrType) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		List <ConversationBean> communityFeed = null;
		boolean newsLetterFlag=false;
		boolean rewardLetterFlag=false;
		if(talker != null) {
			talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
			newsLetterFlag = ApplicationDAO.isEmailExists(talker.getEmail());
			rewardLetterFlag=ApplicationDAO.isnewsLetterSubscribe(talker.getEmail(),"TalkAboutHealth Rewards");
			TalkerLogic.preloadTalkerInfo(talker);
		}
		if(csrType == null || (csrType != null && csrType.equals("")))
			csrType = "All Cancers";
		else
			csrType = csrType.replaceAll("_", " ");
		
			boolean isValid = false;
			List<DiseaseBean> diseaseList = DiseaseDAO.getCatchedDiseasesList(session);
			for(int index = 0; index < diseaseList.size(); index++) {
				if(diseaseList.get(index).getName().equalsIgnoreCase(csrType)) {
					isValid = true;
					csrType = diseaseList.get(index).getName();
					break;
				}
			}
			if(isValid) {
					communityFeed = ConversationDAO.loadExpertsAnswer(null,csrType);
			} else {
				notFound("The page you requested was not found.");
			}
		String type = "expertConvo";
		//Logging disease
		DiseaseBean diseaseBean = DiseaseDAO.getByName(csrType);
		ActivityLogDAO.logSingleDisease(diseaseBean);
		render(communityFeed, csrType,talker,rewardLetterFlag,newsLetterFlag,type);
	}
	
	/**
	 * Used by "More" button in community feeds.
	 * @param afterActionId load actions after given action
	 */
    public static void communityFeedAjaxLoad( String afterActionId, String type, String csrType) {
    	if(type.equals("expertConvo")) {
    		type = "expert"; 
    		List<ConversationBean> convoFeed = ConversationDAO.loadExpertsAnswer(afterActionId,csrType);
   			render("Explore/homeFeedList.html",convoFeed, type,csrType);
		}
		if(type.equals("openConvo")) {
			List<ConversationBean> convoFeed = ConversationDAO.getCommunityOpenQuestions(afterActionId,csrType);
			type = "open";
			render("Explore/homeFeedList.html",convoFeed, type,csrType);
		}
		if(type.equals("recentConvo")) {
			List<ConversationBean> convoFeed = ConversationDAO.loadSharedExperiences(afterActionId,csrType);
			type = "recent";
			render("Explore/homeFeedList.html",convoFeed, type,csrType);
		} 
		if(type.equals("videoConvo")) {
			List<VideoBean> videoBeanList = VideoDAO.loadVideoConvo(afterActionId,csrType,10);
			type = "video";
			String feedType =type ; 
			TalkerBean _talker = CommonUtil.loadCachedTalker(session);
			render("tags/convo/convoList_topic.html", videoBeanList, feedType, _talker);
		}
    }
    /**
	 * Page to displaying topics information
	 * 
	 */
	public static void topics(String topic) {
		String cancerType = session.get("cancerType");
		if(topic != null) {
			topic = topic.replace("_", " ");
			topic = topic.replace("+", " ");
			topic = topic.toLowerCase();
		}
		
		if(StringUtils.isNotBlank(cancerType)){
			render("@topics_micro",cancerType,topic);
		}
		
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		List<DiseaseBean> homediseaseList = DiseaseDAO.getCatchedDiseasesList(session);
		List<DiseaseBean> diseaseList = new ArrayList<DiseaseBean>();

		/*	Code to remove new cancer categories from home page */
		Set<String> newCancerList = DiseaseDAO.newCancerTypes();

		for (DiseaseBean diseaseBean : homediseaseList) {
			if(!newCancerList.contains(diseaseBean.getName()))
				diseaseList.add(diseaseBean);
		}
		
		List<DiseaseBean> diseaseList1 = null;
		List<DiseaseBean> diseaseList2 = null;
		List<DiseaseBean> diseaseList3 = null;
		int size = 0;
  		if(diseaseList != null && diseaseList.size() > 0){
  			int mod = (diseaseList.size()-1)%3;
      		size = (diseaseList.size()-1)/3;
      		if(mod > 0)
      			size = size + 1;
      		diseaseList1 = diseaseList.subList(0, size);
      		diseaseList2 = diseaseList.subList(size, size + size);
      		diseaseList3 = diseaseList.subList(size + size, diseaseList.size()-1);
  		}
		render(talker,diseaseList1,diseaseList2,diseaseList3,topic);
	}

	/**
	 * Page to displaying newsletter information
	 * 
	 */
	public static void newsletter(String name) {
		String cancerType = session.get("cancerType"); 
		if(StringUtils.isNotBlank(name)) {
			if(name.equals("cancer")) {
			} else if(name.equals("breast-cancer") && "Breast Cancer".equals(cancerType)) {
			} else {
				notFound();
			}
			System.out.println("Need to add a page");
		} else {
			System.out.println("Its fine here");
		}

		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		NewsLetterBean newsletter = null;
		GuidanceNewsLetterBean letterBean = null;
		if(talker != null) {
			newsletter = NewsLetterDAO.getNewsLetterInfo(talker.getEmail());
			letterBean = NewsLetterDAO.getGuidanceNewsLetterInfo(talker.getEmail(),"Breast Cancer Guidance");
		}
		List<DiseaseBean> diseaseList = DiseaseDAO.getCatchedDiseasesList(session);
		render(newsletter,diseaseList,talker,cancerType,letterBean);
	}

	/**
	 * Page to displaying newsletter information
	 * 
	 */
	public static void newsletterOld() {
		redirect("/cancer-newsletters",true);
	}

	public static void subscribeNewsLetter(NewsLetterBean newsletter) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		String email = newsletter.getEmail();
		validation.required(email).message("Email is required");
		validation.email(email.trim());
		if (validation.hasErrors()) {
			renderText("Error:" + validation.errors().get(0));
		}

		if(newsletter != null && newsletter.getNewsLetterType() != null && newsletter.getNewsLetterType().length > 0){
			NewsLetterDAO.saveOrUpdateNewsletter(newsletter,talker);
			renderText("Ok");
		}else{
			renderText("Please select on of the option");
		}
	}

	public static void subscribeNewsLetterAll(NewsLetterBean newsletter) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		String email = newsletter.getEmail();
		validation.required(email).message("Email is required");
		validation.email(email.trim());
		if (validation.hasErrors()) {
			renderText("Error:" + validation.errors().get(0));
		}

		if(newsletter != null && newsletter.getNewsLetterType() != null && newsletter.getNewsLetterType().length > 0) {
			NewsLetterDAO.saveOrUpdateNewsletterAll(newsletter,talker);
			renderText("Ok");
		}else{
			renderText("Please select on of the option");
		}
	}
	
	public static void getDiseaseListMenu(String oldPage) {
	
		List<DiseaseBean> homediseaseList = DiseaseDAO.getCatchedDiseasesList(session);
		List<DiseaseBean> diseaseList = new ArrayList<DiseaseBean>();

		/*	Code to remove new cancer categories from home page */
		Set<String> newCancerList = DiseaseDAO.newCancerTypes();

		for (DiseaseBean diseaseBean : homediseaseList) {
			if(!newCancerList.contains(diseaseBean.getName()))
				diseaseList.add(diseaseBean);
		}
		
		List<DiseaseBean> diseaseList1 = null;
		List<DiseaseBean> diseaseList2 = null;
		int size = 0;
  		if(diseaseList != null && diseaseList.size() > 0){
  			int mod = (diseaseList.size()-1)%2;
      		size = (diseaseList.size()-1)/2;
      		if(mod > 0)
      			size = size + 1;
      		diseaseList1 = diseaseList.subList(0, size);
      		diseaseList2 = diseaseList.subList(size, diseaseList.size()-1);
  		}
  		if(StringUtils.isNotBlank(oldPage)) {
  			render("Explore/diseaselistmenuOld.html", diseaseList1,diseaseList2);
  		} else {
  			render("Explore/diseaselistmenu.html", diseaseList1,diseaseList2);
  		}
	}
}

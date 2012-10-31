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
import models.NewsLetterBean;
import models.ServiceAccountBean;
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
import dao.ApplicationDAO;
import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.DiseaseDAO;
import dao.NewsLetterDAO;
import dao.TalkerDAO;
import dao.TopicDAO;

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
		
		List<TalkerBean> newTalkers1 = ApplicationDAO.getNewTalkers(cancerType);

		List<TalkerBean> newTalkers = new ArrayList<TalkerBean>();
		for(TalkerBean talkerBean : newTalkers1){
				newTalkers.add(talkerBean);
		}
		
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
		//boolean loggedIn = (talker != null);
		Set<Action> recentConvo = null;//FeedsLogic.getAllCancerFeed(null, loggedIn, talker);
		
		List<TopicBean> popularTopics = null;
    	//if (talker == null) {
    	//	int limit = session.get("topicCount")==null?TopicLogic.TOPICS_PER_PAGE:Integer.parseInt(session.get("topicCount"));
    	//	popularTopics = TopicLogic.loadPopularTopics(limit);
		//}
    	
    	//For removing answer from feed list which have moderate no moderate value or value as "Delete Answer"
		//Iterator<Action> recentConvosIter = recentConvos.iterator();
		/* 
		while (communityFeedIter.hasNext()) {
			 Action actionIterator = communityFeedIter.next();
			 if(actionIterator != null && actionIterator.getConvo() != null){
				 List<CommentBean> commentBeanList = actionIterator.getConvo().getComments();
				 for(int index = 0; index < commentBeanList.size(); index++){
					 CommentBean commentBean = commentBeanList.get(index);
					 CommentBean comment =  CommentsDAO.getConvoCommentById(commentBean.getId());
					 if(comment != null && comment.getModerate() != null  && !comment.getFromTalker().equals(talker)){
						 if(comment.getModerate().equalsIgnoreCase(AnswerNotification.DELETE_ANSWER)){
							 commentBeanList.remove(index);
							 actionIterator.getConvo().setComments(commentBeanList);
						 }else if(comment.getModerate().equalsIgnoreCase("null")){
							 commentBeanList.remove(index);
							 actionIterator.getConvo().setComments(commentBeanList);
						 }
					 }else{
						 commentBeanList.remove(index);
						 actionIterator.getConvo().setComments(commentBeanList);
					 }
				 }
			 }
		 }
		 */
		//"Popular Conversations" - ordered by page views
		//List<ConversationBean> popularConvo = ConversationDAO.loadPopularAnswers("popular",null);
		//For removing answer from feed list which have moderate value as "Delete Answer"
		/*for(int index = 0; index < popularConvo.size(); index++){
			 ConversationBean conversationBean = popularConvo.get(index);
			 List<CommentBean> answerList = CommentsDAO.loadConvoAnswersTree(conversationBean.getId());
			 for(int index1 = 0; index1 < answerList.size(); index1++){
				 CommentBean commentBean = answerList.get(index1);
				 if(commentBean != null && commentBean.getModerate() != null  && !commentBean.getFromTalker().equals(talker)){
					 if(commentBean.getModerate().equalsIgnoreCase(AnswerNotification.DELETE_ANSWER)){
						 answerList.remove(index1);
					 }else if(commentBean.getModerate().equalsIgnoreCase("null")){
						 answerList.remove(index1);
					 }
				 }else{
					 answerList.remove(index1);
				 }
				 conversationBean.setComments(answerList);
			 }
		 }*/
		//Set<Action> popularConvos = FeedsLogic.getPopularConvoFeed(null);

		if (action == null) {
			action = "feed";
		}
		render(action, recentConvo, popularTopics,talker);
	}
	public static void ajaxFeedUpdate(String type){
		if(type.equals("popularConvo")) {
			List<ConversationBean> popularConvo = ConversationDAO.loadPopularAnswers("popular",null);
			render("Explore/feedList.html",popularConvo, type);
		}
		if(type.equals("expertConvo")) {
			String cancerType = session.get("cancerType"); 
			List<ConversationBean> expertConvo = ConversationDAO.loadExpertsAnswer(null,cancerType);
			render("Explore/feedList.html",expertConvo, type);
		}
		if(type.equals("openConvo")) {
			String cancerType = session.get("cancerType");
			List<ConversationBean> openConvo = ConversationDAO.getOpenQuestions(null,cancerType);
			render("Explore/feedList.html",openConvo, type);
		}
		if(type.equals("recentConvo")) {
			TalkerBean talker = CommonUtil.loadCachedTalker(session);
			boolean loggedIn = (talker != null);
			String cancerType = session.get("cancerType"); 
			talker = new TalkerBean();
			talker.setCategory(cancerType);
			Set<Action> recentConvo = FeedsLogic.getAllCancerFeed(null, loggedIn, talker);	
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
			TalkerBean talker = CommonUtil.loadCachedTalker(session);
			Set<Action> convoFeed = FeedsLogic.getAllCancerFeed(lastActionId, Security.isConnected(), talker);	
			render("Explore/homeFeedList.html",convoFeed, type);
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
        	_feedItems = FeedsLogic.getAllCancerFeed(afterActionId, loggedIn, _talker);
    		render("tags/feed/feedList.html", _feedItems, _talker);
	    }else if ("expertConvo".equalsIgnoreCase(feedType)){
	   	     List<ConversationBean> expertConvos = null;
	    	 expertConvos = ConversationDAO.loadExpertsAnswer(afterActionId,cancerType);
		     render("tags/convo/convoList.html", expertConvos ,feedType);
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
    		render("tags/feed/feedList.html", _feedItems, _talker);
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
			activeTalkers = TalkerDAO.getSortedTalkerList(activeTalkers,loggedIn);
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
	public static void community(String cancerType) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		boolean loggedIn = (talker != null);
		Set<Action> communityFeed = null;
		boolean newsLetterFlag=false;
		boolean rewardLetterFlag=false;
		String sessionCancer = session.get("cancerType");
		if(talker != null){
			talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
			 newsLetterFlag = ApplicationDAO.isEmailExists(talker.getEmail());
			rewardLetterFlag=ApplicationDAO.isnewsLetterSubscribe(talker.getEmail(),"TalkAboutHealth Rewards");
		}
		if(cancerType == null || (cancerType != null && cancerType.equals("")))
			cancerType = "All Cancers";
		else
			cancerType = cancerType.replaceAll("_", " ");
		
		if(cancerType.equalsIgnoreCase("All Cancers")){
			communityFeed = FeedsLogic.getAllCancerFeed(null, loggedIn, talker);
		}else{
			boolean isValid = false;
			List<DiseaseBean> diseaseList = DiseaseDAO.getCatchedDiseasesList(session);
			for(int index = 0; index < diseaseList.size(); index++){
				if(diseaseList.get(index).getName().equalsIgnoreCase(cancerType)){
					isValid = true;
					cancerType = diseaseList.get(index).getName();
					break;
				}
			}
			if(isValid){
				if(talker != null){
					String category = talker.getCategory();
					talker.setCategory(cancerType);
					String[] otherCategories = talker.getOtherCategories();
					String[] otherCat = new String[1];
					otherCat[0] = cancerType;
					talker.setOtherCategories(otherCat);
					communityFeed = FeedsLogic.getCommunityFeed(null, loggedIn, talker);
					talker.setCategory(category);
					talker.setOtherCategories(otherCategories);
				}else{
					talker = new TalkerBean();
					talker.setCategory(cancerType);
					String[] otherCat = new String[1];
					otherCat[0] = cancerType;
					talker.setOtherCategories(otherCat);
					communityFeed = FeedsLogic.getCommunityFeed(null, loggedIn, talker);
					talker = null;
				}
			}else{
				notFound("The page you requested was not found.");
			}
		}
				render(communityFeed, cancerType,talker,rewardLetterFlag,newsLetterFlag);
	}
	
	/**
	 * Used by "More" button in community feeds.
	 * @param afterActionId load actions after given action
	 */
    public static void communityFeedAjaxLoad( String afterActionId, String feedType, String cancerType) {
    	TalkerBean _talker = CommonUtil.loadCachedTalker(session);
    	boolean loggedIn = (_talker != null);
    	Set<Action> _feedItems = null;
    	if (feedType != null && feedType.equals("communityFeed")) {
    		
    		if(_talker != null){
				String category = _talker.getCategory();
				_talker.setCategory(cancerType);
				_feedItems = FeedsLogic.getCommunityFeed(afterActionId, loggedIn, _talker);
				_talker.setCategory(category);
			}else{
				_talker = new TalkerBean();
				_talker.setCategory(cancerType);
				_talker.setOtherCategories(new String[0]);
				_feedItems = FeedsLogic.getCommunityFeed(afterActionId, loggedIn, _talker);
				_talker = null;
			}
     		render("tags/feed/feedList.html", _feedItems, _talker);
     	} else if(feedType != null && feedType.equals("allFeed")) {
    		_feedItems = FeedsLogic.getAllCancerFeed(afterActionId, loggedIn, _talker);
    		render("tags/feed/feedList.html", _feedItems, _talker);
    	}
    }
    /**
	 * Page to displaying topics information
	 * 
	 */
	public static void topics(String topic) {
		String cancerType = session.get("cancerType");
		if(topic != null){
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
	public static void newsletter(){
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		NewsLetterBean newsletter = new NewsLetterBean();
		if(talker != null){
			newsletter = NewsLetterDAO.getNewsLetterInfo(talker.getEmail());
		}
		List<DiseaseBean> diseaseList = DiseaseDAO.getCatchedDiseasesList(session);
		render(newsletter,diseaseList,talker);
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
}

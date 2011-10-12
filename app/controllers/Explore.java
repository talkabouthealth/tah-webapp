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
import models.ServiceAccountBean;
import models.PrivacySetting.PrivacyType;
import models.ServiceAccountBean.ServiceType;
import models.TalkerBean;
import models.TopicBean;
import models.actions.Action;
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
    	boolean newsLetterFlag = false;
    	if (talker != null) {
    		TalkerLogic.preloadTalkerInfo(talker);
    		newsLetterFlag = ApplicationDAO.isEmailExists(talker.getEmail());
    	}
    	int limit = session.get("topicCount")==null?20:Integer.parseInt(session.get("topicCount"));
    	List<TopicBean> popularTopics = TopicLogic.loadPopularTopics(limit);
		List<ConversationBean> openQuestions = ConversationDAO.getOpenQuestions();
		
		render(talker, openQuestions, popularTopics,newsLetterFlag);
    }
    
    public static void liveTalks() {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	List<TopicBean> popularTopics = null;
    	if (talker != null) {
    		TalkerLogic.preloadTalkerInfo(talker);
    	}
    	else {
    		int limit = session.get("topicCount")==null?20:Integer.parseInt(session.get("topicCount"));
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
    	session.put("topicCount", limit);
    	boolean newsLetterFlag = false;
    	
    	if (talker != null) {
    		TalkerLogic.preloadTalkerInfo(talker);
    		newsLetterFlag = ApplicationDAO.isEmailExists(talker.getEmail());
    	}

    	List<TopicBean> popularTopics = TopicLogic.loadPopularTopics(limit);

    	Set<TopicBean> topicsTree = TopicLogic.getAllTopicsTree();

    	render(topicsTree, talker, popularTopics, newsLetterFlag, limit);
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
        }else{
        	_popularTopics = null;
        }

    	boolean more = true;
    	session.put("topicCount", limit);
    	render("tags/common/popularTopics.html", _popularTopics,more,limit);
    }
    
    public static void browseMembers(String action) throws Throwable {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);

		//Active talkers on this day
		// Displaying members active since last 2 week's rather than 1 week
		Calendar oneWeekBeforeNow = Calendar.getInstance();
		oneWeekBeforeNow.add(Calendar.WEEK_OF_YEAR, -2);
		
		
		List<TalkerBean> activeTalkers =  ApplicationDAO.getActiveTalkers(oneWeekBeforeNow.getTime());
		if(activeTalkers != null && activeTalkers.size() > TalkerLogic.TALKERS_PER_PAGE)
			activeTalkers = activeTalkers.subList(0, TalkerLogic.TALKERS_PER_PAGE);
		
		List<TalkerBean> newTalkers = ApplicationDAO.getNewTalkers();

		if(newTalkers != null && newTalkers.size() > TalkerLogic.TALKERS_PER_PAGE)
			newTalkers = newTalkers.subList(0,TalkerLogic.TALKERS_PER_PAGE); 

		//check if search is performed now
		String query = params.get("query");
		List<TalkerBean> results = null;
		if (query != null) {
			params.flash("query");
			try {
				results = SearchUtil.searchTalker(query);
				if(results != null && results.size() > TalkerLogic.TALKERS_PER_PAGE)
					results = results.subList(0, TalkerLogic.TALKERS_PER_PAGE);
			}
			catch (Exception e) {
				Logger.error(e, "Talker search on Browser Members page.");
			}
		}

		//Move members to particular tabs based on member's connection
		Map<String, Set<TalkerBean>> members = new LinkedHashMap<String, Set<TalkerBean>>();
		members.put("Experts", new LinkedHashSet<TalkerBean>());
		members.put("Patients", new LinkedHashSet<TalkerBean>());
		members.put("Former Patients", new LinkedHashSet<TalkerBean>());
		members.put("Parents", new LinkedHashSet<TalkerBean>());
		members.put("Caregivers", new LinkedHashSet<TalkerBean>());
		members.put("Family & Friends", new LinkedHashSet<TalkerBean>());
		
		//match tabs with possible connections
		Map<String, List<String>> memberTypes = new LinkedHashMap<String, List<String>>();
		memberTypes.put("Experts", TalkerBean.PROFESSIONAL_CONNECTIONS_LIST);
		memberTypes.put("Patients", Arrays.asList("Just Diagnosed","Current Patient"));
		memberTypes.put("Former Patients", Arrays.asList("Survivor (1 year)","Survivor (2 - 5 years)","Survivor (5 - 10 years)","Survivor (10 - 20 years)","Survivor (Greater than 20 years)"));
		memberTypes.put("Parents", Arrays.asList("Parent"));
		memberTypes.put("Caregivers", Arrays.asList("Caregiver"));
		memberTypes.put("Family & Friends", Arrays.asList("Family member", "Friend"));
		
		//Set<TalkerBean> allActiveTalkers = ApplicationDAO.getActiveTalkers(null);
		//List<TalkerBean> allActiveTalkers = TalkerDAO.loadAllTalkers(true);
		List<TalkerBean> allActiveTalkers = TalkerDAO.loadAllTalker(true);
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

		//default tab is 'active'
		if (action == null || action.equals("browsemembers")) {
			action = "active";
		}
		render(currentTalker, action, activeTalkers, newTalkers, results, members);
	}
    

	public static void searchConversations() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		String query = params.get("query");
		List<ConversationBean> results = null;
		if (query != null) {
			params.flash("query");
			try {
				results = SearchUtil.searchConvo(query, 10);
			}
			catch (Exception e) {
				Logger.error(e, "Search Conversations error");
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
		boolean loggedIn = (talker != null);
		Set<Action> communityFeed = FeedsLogic.getCommunityFeed(null, loggedIn);
		
		List<TopicBean> popularTopics = null;
    	if (talker == null) {
    		int limit = session.get("topicCount")==null?20:Integer.parseInt(session.get("topicCount"));
    		popularTopics = TopicLogic.loadPopularTopics(limit);
    	}
    	
    	//For removing answer from feed list which have moderate no moderate value or value as "Delete Answer"
		Iterator<Action> communityFeedIter = communityFeed.iterator();
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
		 
		//"Popular Conversations" - ordered by page views
		List<ConversationBean> popularConvos = ConversationDAO.loadPopularConversations(null);
		
		//For removing answer from feed list which have moderate value as "Delete Answer"
		for(int index = 0; index < popularConvos.size(); index++){
			 ConversationBean conversationBean = popularConvos.get(index);
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
		 }
		//Set<Action> popularConvos = FeedsLogic.getPopularConvoFeed(null);
		
		if (action == null) {
			action = "feed";
		}
		render(action, communityFeed, popularConvos, popularTopics);
	}
	
	public static void ajaxLoadMoreUser(String feedType, String afterActionId,String searchTerm){
		System.out.println(feedType);
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		List<TalkerBean> activeTalkers = null;
		
		if ("active".equals(feedType)) {
			Calendar twoWeekBeforeNow = Calendar.getInstance();
			twoWeekBeforeNow.add(Calendar.WEEK_OF_YEAR, -2);
			activeTalkers =  ApplicationDAO.getActiveTalkers(twoWeekBeforeNow.getTime());

		} else if("new".equals(feedType)) {
			activeTalkers = ApplicationDAO.getNewTalkers();
		} else if("search".equals(feedType)) {
			System.out.println(searchTerm);
			if (searchTerm != null) {
				try {
					activeTalkers = SearchUtil.searchTalker(searchTerm);
				}
				catch (Exception e) {
					Logger.error(e, "Talker search on Browser Members page.");
				}
			}
		} else {
			List<String> memberTypeEntry = null;
			if("Experts".equals(feedType))
				memberTypeEntry = TalkerBean.PROFESSIONAL_CONNECTIONS_LIST;
			else if("Patients".equals(feedType))
				memberTypeEntry = Arrays.asList("Just Diagnosed","Current Patient");
			else if("Former Patients".equals(feedType))
				memberTypeEntry = Arrays.asList("Survivor (1 year)","Survivor (2 - 5 years)","Survivor (5 - 10 years)","Survivor (10 - 20 years)","Survivor (Greater than 20 years)");
			else if("Parents".equals(feedType))
				memberTypeEntry = Arrays.asList("Parent");
			else if("Caregivers".equals(feedType))
				memberTypeEntry = Arrays.asList("Caregiver");
			else if("Family & Friends".equals(feedType))
				memberTypeEntry = Arrays.asList("Family member", "Friend");
				
			activeTalkers = new ArrayList<TalkerBean>();
			//List<TalkerBean> allActiveTalkers = TalkerDAO.loadAllTalkers(true);
			List<TalkerBean> allActiveTalkers = TalkerDAO.loadAllTalker(true);
			for (TalkerBean talker : allActiveTalkers) {
				if (memberTypeEntry.contains(talker.getConnection()) && talker.getName() != null) {
					activeTalkers.add(talker);
				}
			}
		}

		int talkerCounter = 1;
		if(activeTalkers != null){
			for (TalkerBean talkerBean : activeTalkers) {
				if(afterActionId.equals(talkerBean.getId()))
					break;
				else
					talkerCounter++;
			}
			int limit = talkerCounter + TalkerLogic.TALKERS_PER_PAGE;
			if(talkerCounter < activeTalkers.size() && limit >  activeTalkers.size()){
				limit = activeTalkers.size();
				activeTalkers = activeTalkers.subList(talkerCounter, limit);
			}else if(limit >  activeTalkers.size()){
				activeTalkers = null;
			} else{
				activeTalkers = activeTalkers.subList(talkerCounter, limit);
			}
			render("tags/talker/talkerList.html", activeTalkers,currentTalker);
		} else {
			renderText("Error");
		}
	}
}

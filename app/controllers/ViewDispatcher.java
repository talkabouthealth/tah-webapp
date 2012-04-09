package controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import logic.FeedsLogic;
import logic.TalkerLogic;
import logic.TopicLogic;
import logic.FeedsLogic.FeedType;
import models.CommentBean;
import models.DiseaseBean;
import models.HealthItemBean;
import models.PrivacySetting.PrivacyType;
import models.PrivacySetting.PrivacyValue;
import models.TalkerBean;
import models.TalkerDiseaseBean;
import models.ConversationBean;
import models.TopicBean;
import models.actions.Action;
import models.actions.Action.ActionType;
import dao.ActionDAO;
import dao.ApplicationDAO;
import dao.CommentsDAO;
import dao.DiseaseDAO;
import dao.HealthItemDAO;
import dao.MessagingDAO;
import dao.TalkerDAO;
import dao.TalkerDiseaseDAO;
import dao.ConversationDAO;
import dao.TopicDAO;
import play.Logger;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;
import play.templates.JavaExtensions;
import util.CommonUtil;
import util.SearchUtil;

/**
 * Used for dispatch/show Users, Convos/Questions and Topics,
 * as they all share one url: http://talkabouthealth.com/{name} 
 *
 */
@With( LoggerController.class )
public class ViewDispatcher extends Controller {
	
	@Before
	static void prepareParams() {
        String currentURL = "http://"+request.host+request.path;
        renderArgs.put("currentURL", currentURL);
	}
	
	public static void view(String name) throws Throwable {
		TalkerBean talker = TalkerDAO.getByURLName(name);
		boolean showTalker = false;
		boolean showAnom = false;
		if (talker != null) {
			if(Security.isConnected()){
				showTalker = true;
			} else if(PrivacyValue.PUBLIC.equals(talker.getPrivacyValue(PrivacyType.PROFILE_INFO))){
				if(!name.equals(talker.getAnonymousName()) && !PrivacyValue.PUBLIC.equals(talker.getPrivacyValue(PrivacyType.USERNAME))){
					showTalker = false;
					showAnom = true;
				}else{
					showTalker = true;
				}
			}
			if ( showTalker ){
				showTalker(talker);
				return;
			} else {
				if(showAnom){
					redirect("/" + talker.getAnonymousName());
				} else {
					redirect("/login");
				}
			}
		}
		
		
		//next - question or conversation
		ConversationBean convo = ConversationDAO.getByURL(name);
		if (convo != null) {
			if (!convo.getMainURL().equals(name)) {
				//we come here by old url - redirect to main
				redirect("/"+convo.getMainURL());
			}
			if (convo.getMergedWith() != null) {
				ConversationBean mainConvo = ConversationDAO.getById(convo.getMergedWith());
				redirect("/"+mainConvo.getMainURL(), true);
			}
			if (convo.isDeleted()) {
				notFound();
			}
			
			showConvo(convo);
			return;
		}
		
		//last - topic
		TopicBean topic = TopicDAO.getByURL(name);
		if (topic != null) {
			if (topic.isDeleted()) {
				if (Security.isConnected()) {
					talker = CommonUtil.loadCachedTalker(session);
					talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
				} else {
					talker = null;
				}
				render("Topics/notExists.html", talker);
				return;
			}
			if (!topic.getMainURL().equals(name)) {
				//we come here by old url - redirect to main
				redirect("/"+topic.getMainURL());
			}
			showTopic(topic);
			return;
		}
		
		//nothing was found
		notFound("The page you requested was not found.");
		//todo();
	}

	/**
	 * Shows public profile page of given talker
	 * @param talker
	 */
	private static void showTalker(TalkerBean talker) throws Throwable {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		boolean newsLetterFlag = false;
		if (talker.isSuspended()) {
			if (currentTalker != null) {
				//we need followers for displaying user's info
				currentTalker.setFollowerList(TalkerDAO.loadFollowers(currentTalker.getId()));
				newsLetterFlag = ApplicationDAO.isEmailExists(currentTalker.getEmail());
			}
			render("PublicProfile/suspended.html", currentTalker);
			return;
		}
		
		//Health info
		//For now we have only one disease - Breast Cancer
		
		final String diseaseName = talker.getCategory();
		DiseaseBean disease = DiseaseDAO.getByName(diseaseName);
		List<TalkerDiseaseBean> talkerDiseaseList = TalkerDiseaseDAO.getListByTalkerId(talker.getId());
		TalkerDiseaseBean talkerDisease = null;
		if(talkerDiseaseList != null){
			for(TalkerDiseaseBean diseaseBean : talkerDiseaseList){
				if(diseaseBean != null && diseaseBean.getDiseaseName().equalsIgnoreCase(talker.getCategory())){
					talkerDisease = diseaseBean;
				}
			}
		}
		if (talkerDisease != null) {
			talkerDisease.setName(diseaseName);
		}

		//Load all healthItems for this disease
		Map<String, HealthItemBean> healthItemsMap = TalkerLogic.loadHealthItemsFromCache(diseaseName , false);
		
		int numOfStartedConvos = ConversationDAO.getNumOfStartedConvos(talker.getId());
		
		TalkerLogic.preloadTalkerInfo(talker);
		
		boolean notProvidedInfo = false;
		boolean notViewableInfo = false;
		if (talker.equals(currentTalker)) {
			//if user has not provided Personal Info or Health Info
			EnumSet<ActionType> userActionTypes = 
				ActionDAO.loadTalkerActionTypes(talker.getId());
			
			if (!userActionTypes.contains(ActionType.UPDATE_PERSONAL)) {
				notProvidedInfo = true;
			}
			if (!talker.isProf() && !userActionTypes.contains(ActionType.UPDATE_HEALTH)) {
				notProvidedInfo = true;
			}
			
			//if user has not made the information viewable to the Community
			if (talker.isPrivate(PrivacyType.PROFILE_INFO) 
					|| talker.isPrivate(PrivacyType.HEALTH_INFO)) {
				notViewableInfo = true;
			}
			if (talker.isProf() && talker.isPrivate(PrivacyType.PROFESSIONAL_INFO)) {
				notViewableInfo = true;
			}
			newsLetterFlag = ApplicationDAO.isEmailExists(currentTalker.getEmail());
		}
		
		Set<Action> talkerFeed = FeedsLogic.getTalkerFeed(talker, null);
		
		//For removing answer from feed list which have moderate no moderate value or value as "Delete Answer"
		Iterator<Action> talkerFeedIter = talkerFeed.iterator();
		/* while (talkerFeedIter.hasNext()) {
			 Action actionIterator = talkerFeedIter.next();
			 if(actionIterator != null && actionIterator.getConvo() != null){
				 List<CommentBean> commentBeanList = actionIterator.getConvo().getComments();
				 for(int index = 0; index < commentBeanList.size(); index++){
					 CommentBean commentBean = commentBeanList.get(index);
					 CommentBean comment =  CommentsDAO.getConvoCommentById(commentBean.getId());
					 if(comment != null && comment.getModerate() != null && comment.getFromTalker().equals(talker)){
						 if(comment.getModerate().equalsIgnoreCase(AnswerNotification.DELETE_ANSWER)){
							 commentBeanList.remove(index);
							 actionIterator.getConvo().setComments(commentBeanList);
						 }else if(comment.getModerate().equalsIgnoreCase("null")){
							 commentBeanList.remove(index);
							 actionIterator.getConvo().setComments(commentBeanList);
						 }
					 }else {
						 if(actionIterator.getTalker().getActivityList()!=null){
							 int count = actionIterator.getTalker().getActivityList().size();
							 actionIterator.getTalker().getActivityList().remove(count);
						 }
						 commentBeanList.remove(index);
						 actionIterator.getConvo().setComments(commentBeanList);
					 }
				 }
			 }
		 }
		*/
		
		List<Action> answersFeed = new ArrayList<Action>();
		int numOfTopAnswers = TalkerLogic.prepareTalkerAnswers(talker.getId(), answersFeed, false);
		int numOfAnswers = answersFeed.size();
		
		if(talkerDisease != null) {
			talkerDisease.setHealthItemsMap(healthItemsMap);
			talkerDisease.setDiseaseQuestions(disease);
		}
		
		render("PublicProfile/newview.html", talker, disease, talkerDisease, healthItemsMap, 
				currentTalker, talkerFeed,
				notProvidedInfo, notViewableInfo,
				numOfAnswers, numOfTopAnswers, numOfStartedConvos,newsLetterFlag);
		
	}
	
	private static void showConvo(ConversationBean convo) {
		TalkerBean talker = null;
		boolean newsLetterFlag = false;
		if (Security.isConnected()) {
			talker = CommonUtil.loadCachedTalker(session);
			newsLetterFlag = ApplicationDAO.isEmailExists(talker.getEmail());
		}
		ConversationDAO.incrementConvoViews(convo.getId());
		Date latestActivityTime = ActionDAO.getConvoLatestActivity(convo);
		
		//For displaying answers sequence wise
		List<CommentBean> answerList = CommentsDAO.loadConvoAnswersTree(convo.getId());
		
		if (Security.isConnected()) {
			if(!talker.isAdmin()){
				for(int index = 0; index < answerList.size(); index++){
					CommentBean commentBean= answerList.get(index);
					if(commentBean.isDeleted())
						answerList.remove(index);
				}
			}
		}else{
			for(int index = 0; index < answerList.size(); index++){
				CommentBean commentBean= answerList.get(index);
				if(commentBean.isDeleted())
					answerList.remove(index);
			}
		}
		List<CommentBean> commentList = answerList;
		//For getting answers in top position which have question text
		//For removing answer from question page which have moderate no moderate value or value as "Delete Answer" .
		/*for(int index = 0; index < answerList.size(); index++){
			CommentBean commentBean= answerList.get(index);
			if(commentBean.getModerate() == null && !commentBean.getFromTalker().equals(talker)){
				commentList.remove(index);
			}else if(commentBean.getModerate() != null){
				 if(commentBean.getModerate().equalsIgnoreCase(AnswerNotification.DELETE_ANSWER)){
					 commentList.remove(index);
				 }else if(commentBean.getModerate().equalsIgnoreCase("null")){
					 commentList.remove(index);
				 }
			}
		}*/
		
		convo.setComments(commentList);
		convo.setReplies(CommentsDAO.loadConvoReplies(convo.getId()));
		
		boolean userHasAnswer = convo.hasUserAnswer(talker);
		
		List<ConversationBean> relatedConvos = null;
		try {
			relatedConvos = SearchUtil.getRelatedConvos(talker,convo);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		render("Conversations/viewConvo.html", talker, convo, latestActivityTime, 
				relatedConvos, userHasAnswer,newsLetterFlag);
    }
	
	private static void showTopic(TopicBean topic) {
		TalkerBean talker = null;
		boolean newsLetterFlag = false;
		if (Security.isConnected()) {
			talker = CommonUtil.loadCachedTalker(session);
			newsLetterFlag = ApplicationDAO.isEmailExists(talker.getEmail());
		}
		
		TopicDAO.incrementTopicViews(topic.getId());
		
		//List<String> cat = FeedsLogic.getCancerType(talker);
		
		//Need to get specific count of feeds first
		//FeedsLogic.FEEDS_PER_PAGE
		
		//load latest activities for convos with this topic
		Set<Action> activities = FeedsLogic.getTopicFeed(talker,topic, null);
		
		List<ConversationBean> openConvos  = new ArrayList<ConversationBean>(topic.getConversations());
		List<ConversationBean> openConvosSaved = new ArrayList<ConversationBean>();
		for (ConversationBean conversationBean : openConvos) {
			//added for check cancer type
			if(conversationBean.isOpened()){ //&& cat.contains(conversationBean.getCategory())
				openConvosSaved.add(conversationBean);
			}
			if(openConvosSaved.size() >= FeedsLogic.FEEDS_PER_PAGE)
				break;
		}
		openConvos.clear();
		
		//- "Popular Conversations" - topic conversations ordered by page views
		List<ConversationBean> popularConvos1 = new ArrayList<ConversationBean>(topic.getConversations());
		List<ConversationBean> popularConvos = new ArrayList<ConversationBean>();
		//added for check cancer type
		for (ConversationBean conversationBean : popularConvos1) {
			//if(cat.contains(conversationBean.getCategory())){
				popularConvos.add(conversationBean);
			//}
		}
		popularConvos1.clear();
		Collections.sort(popularConvos);
		//Added code for adding pagination to the popular topic section on topic page
		if(popularConvos.size() >= FeedsLogic.FEEDS_PER_PAGE){
			popularConvos = popularConvos.subList(0, FeedsLogic.FEEDS_PER_PAGE);	
		}

		
		//- "Trending Conversations" - ordered by page views in the last two weeks, 
		//cannot contain conversations in the top 10 of "Popular Conversations" tab
		List<ConversationBean> trendingConvos = new ArrayList<ConversationBean>();
		
		List<Action> topicMentions1 = CommentsDAO.getTopicMentions(topic);
		List<Action> topicMentions = new ArrayList<Action>();
		//added for check cancer type
		for (Action action : topicMentions1) {
			//if(cat.contains(action.getTalker().getCategory())){
				topicMentions.add(action);
			//}
		}
		topicMentions1.clear();
		if(topicMentions.size() >= FeedsLogic.FEEDS_PER_PAGE){
			topicMentions = topicMentions.subList(0, FeedsLogic.FEEDS_PER_PAGE);	
		}
		
		if(talker != null)
			session.put("inboxUnreadCount", MessagingDAO.getUnreadMessageCount(talker.getId()));
		
		render("Topics/viewTopic.html", talker, topic, activities, openConvosSaved,
				popularConvos, trendingConvos, topicMentions,newsLetterFlag);
	}

	/**
	 * Used by "More" button in different feeds.
	 * @param afterActionId load actions after given action
	 */
    public static void topicAjaxLoad(String title, String afterActionId, String feedType) {
    	TalkerBean _talker = CommonUtil.loadCachedTalker(session);
    	TopicBean topic = TopicDAO.getByURL(title);
    	
    	int countArray = 1;
    	if(feedType.equals("popularConvos")){
    		List<ConversationBean> popularConvos = new ArrayList<ConversationBean>(topic.getConversations());
    		Collections.sort(popularConvos);
    		for (ConversationBean conversationBean : popularConvos) {
				if(!conversationBean.getId().equals(afterActionId))
					countArray++;
				else
					break;
			}
    		int countArrayLimit = countArray + FeedsLogic.FEEDS_PER_PAGE;
    		//Added code for adding pagination to the popular topic section on topic page
    		if(popularConvos.size() > countArrayLimit){
    			popularConvos = popularConvos.subList(countArray, countArrayLimit);	
    		}else{
    			popularConvos = popularConvos.subList(countArray, popularConvos.size());
    		}
    		render("tags/convo/convoList.html", popularConvos, _talker);
    	}else if(feedType.equals("openQuestions")){
    		List<ConversationBean> openConvos  = new ArrayList<ConversationBean>(topic.getConversations());
    		List<ConversationBean> popularConvos = new ArrayList<ConversationBean>();
    		for (ConversationBean conversationBean : openConvos) {
    			if(conversationBean.isOpened()){
    				popularConvos.add(conversationBean);
    			}
    		}
    		for (ConversationBean conversationBean : popularConvos) {
				if(!conversationBean.getId().equals(afterActionId))
					countArray++;
				else
					break;
			}
    		int countArrayLimit = countArray + FeedsLogic.FEEDS_PER_PAGE;
    		//Added code for adding pagination to the popular topic section on topic page
    		if(popularConvos.size() > countArrayLimit){
    			popularConvos = popularConvos.subList(countArray, countArrayLimit);	
    		}else{
    			popularConvos = popularConvos.subList(countArray, popularConvos.size());
    		}
    		render("tags/convo/convoList.html", popularConvos, _talker);
    	}else if(feedType.equals("thoughts")){
    		List<Action> _feedItems = CommentsDAO.getTopicMentions(topic);
    		for (Action conversationBean : _feedItems) {
				if(!conversationBean.getId().equals(afterActionId))
					countArray++;
				else
					break;
			}
    		int countArrayLimit = countArray + FeedsLogic.FEEDS_PER_PAGE;
    		if(_feedItems.size() > countArrayLimit){
    			_feedItems = _feedItems.subList(countArray, countArrayLimit);
    		}else{
    			_feedItems = _feedItems.subList(countArray, _feedItems.size());
    		}
    		render("tags/feed/feedList.html", _feedItems, _talker);
    	}else{
    		Set<Action> _feedItems = null;
    		_feedItems = FeedsLogic.getTopicFeed(_talker,topic, afterActionId);
    		render("tags/feed/feedList.html", _feedItems, _talker);
    	}
    }
}

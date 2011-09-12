package controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
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
		final String diseaseName = "Breast Cancer";
		DiseaseBean disease = DiseaseDAO.getByName(diseaseName);
		TalkerDiseaseBean talkerDisease = TalkerDiseaseDAO.getByTalkerId(talker.getId());
		if (talkerDisease != null) {
			talkerDisease.setName(diseaseName);
		}

		//Load all healthItems for this disease
		Map<String, HealthItemBean> healthItemsMap = TalkerLogic.loadHealthItemsFromCache(diseaseName);
		
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
		
		convo.setComments(CommentsDAO.loadConvoAnswersTree(convo.getId()));
		boolean userHasAnswer = convo.hasUserAnswer(talker);
		
		convo.setReplies(CommentsDAO.loadConvoReplies(convo.getId()));
		
		List<ConversationBean> relatedConvos = null;
		try {
			relatedConvos = SearchUtil.getRelatedConvos(convo);
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
		
		//Need to get specific count of feeds first
		//FeedsLogic.FEEDS_PER_PAGE
		
		//load latest activities for convos with this topic
		Set<Action> activities = FeedsLogic.getTopicFeed(topic, null);
		
		List<ConversationBean> openConvos  = new ArrayList<ConversationBean>(topic.getConversations());
		List<ConversationBean> openConvosSaved = new ArrayList<ConversationBean>();
		for (ConversationBean conversationBean : openConvos) {
			if(conversationBean.isOpened()){
				openConvosSaved.add(conversationBean);
			}
			if(openConvosSaved.size() >= FeedsLogic.FEEDS_PER_PAGE)
				break;
		}
		
		//- "Popular Conversations" - topic conversations ordered by page views
		List<ConversationBean> popularConvos = new ArrayList<ConversationBean>(topic.getConversations());
		Collections.sort(popularConvos);
		//Added code for adding pagination to the popular topic section on topic page
		if(popularConvos.size() >= FeedsLogic.FEEDS_PER_PAGE){
			popularConvos = popularConvos.subList(0, FeedsLogic.FEEDS_PER_PAGE);	
		}

		
		//- "Trending Conversations" - ordered by page views in the last two weeks, 
		//cannot contain conversations in the top 10 of "Popular Conversations" tab
		List<ConversationBean> trendingConvos = new ArrayList<ConversationBean>();
		
		List<Action> topicMentions = CommentsDAO.getTopicMentions(topic);
		if(topicMentions.size() >= FeedsLogic.FEEDS_PER_PAGE){
			topicMentions = topicMentions.subList(0, FeedsLogic.FEEDS_PER_PAGE);	
		}
		
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
    		_feedItems = FeedsLogic.getTopicFeed(topic, afterActionId);
    		render("tags/feed/feedList.html", _feedItems, _talker);
    	}
    }
}

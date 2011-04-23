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
import models.CommentBean;
import models.DiseaseBean;
import models.HealthItemBean;
import models.PrivacySetting.PrivacyType;
import models.TalkerBean;
import models.TalkerDiseaseBean;
import models.ConversationBean;
import models.TopicBean;
import models.actions.Action;
import models.actions.Action.ActionType;
import dao.ActionDAO;
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
		//first try user
		TalkerBean talker = TalkerDAO.getByURLName(name);
		if (talker != null) {
			showTalker(talker);
			return;
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
				}
				else {
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
		notFound();
	}

	/**
	 * Shows public profile page of given talker
	 * @param talker
	 */
	private static void showTalker(TalkerBean talker) throws Throwable {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		
		if (talker.isSuspended()) {
			if (currentTalker != null) {
				//we need followers for displaying user's info
				currentTalker.setFollowerList(TalkerDAO.loadFollowers(currentTalker.getId()));
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
				numOfAnswers, numOfTopAnswers, numOfStartedConvos);
	}
	
	private static void showConvo(ConversationBean convo) {
		TalkerBean talker = null;
		if (Security.isConnected()) {
			talker = CommonUtil.loadCachedTalker(session);
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
				relatedConvos, userHasAnswer);
    }
	
	private static void showTopic(TopicBean topic) {
		TalkerBean talker = null;
		if (Security.isConnected()) {
			talker = CommonUtil.loadCachedTalker(session);
		}
		
		TopicDAO.incrementTopicViews(topic.getId());
		
		//load latest activities for convos with this topic
		Set<Action> activities = FeedsLogic.getTopicFeed(topic, null);
		
		//- "Popular Conversations" - topic conversations ordered by page views
		List<ConversationBean> popularConvos = 
			new ArrayList<ConversationBean>(topic.getConversations());
		Collections.sort(popularConvos);
		
		//- "Trending Conversations" - ordered by page views in the last two weeks, 
		//cannot contain conversations in the top 10 of "Popular Conversations" tab
		List<ConversationBean> trendingConvos = new ArrayList<ConversationBean>();
		
		List<Action> topicMentions = CommentsDAO.getTopicMentions(topic);
		
		render("Topics/viewTopic.html", talker, topic, activities, 
				popularConvos, trendingConvos, topicMentions);
	}

}

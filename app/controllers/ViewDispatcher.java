package controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logic.TalkerLogic;
import models.DiseaseBean;
import models.HealthItemBean;
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
import play.mvc.Controller;
import play.templates.JavaExtensions;
import util.CommonUtil;
import util.SearchUtil;

/**
 * Used for dispatch/show Users, Convos/Questions and Topics,
 * as they all share one url: http://talkabouthealth.com/{name} 
 *
 */
public class ViewDispatcher extends Controller {
	
	public static void view(String name) throws Throwable {
		//first try user
		TalkerBean talker = TalkerDAO.getByUserName(name);
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
			
			showConvo(convo);
			return;
		}
		
		//last - topic
		TopicBean topic = TopicDAO.getByURL(name);
		if (topic != null) {
			if (!topic.getMainURL().equals(name)) {
				//we come here by old url - redirect to main
				redirect("/"+topic.getMainURL());
			}
			
			showTopic(topic);
			return;
		}
		
		notFound();
	}

	private static void showTalker(TalkerBean talker) throws Throwable {
		//user should be logged to view Public Profile
		Secure.checkAccess();
		
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		
		//Health info
		//For now we have only one disease - Breast Cancer
		final String diseaseName = "Breast Cancer";
		DiseaseBean disease = DiseaseDAO.getByName(diseaseName);
		TalkerDiseaseBean talkerDisease = TalkerDiseaseDAO.getByTalkerId(talker.getId());
		if (talkerDisease != null) {
			talkerDisease.setName(diseaseName);
		}
		
		//Load all healthItems for this disease
		Map<String, HealthItemBean> healthItemsMap = new HashMap<String, HealthItemBean>();
		for (String itemName : new String[] {"symptoms", "tests", 
				"procedures", "treatments", "sideeffects"}) {
			HealthItemBean healthItem = HealthItemDAO.getHealthItemByName(itemName, diseaseName);
			healthItemsMap.put(itemName, healthItem);
		}
		
		talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
		talker.setActivityList(ActionDAO.load(talker.getId()));
		talker.setProfileCommentsList(CommentsDAO.loadProfileComments(talker.getId()));
		talker.setFollowingConvosFullList(TalkerDAO.loadFollowingConversations(talker.getId()));
		
		talker.setStartedTopicsList(ConversationDAO.loadConversations(talker.getId(), ActionType.START_CONVO));
		talker.setJoinedTopicsList(ConversationDAO.loadConversations(talker.getId(), ActionType.JOIN_CONVO));
		
		TalkerLogic.calculateProfileCompletion(talker);
		
//		render("PublicProfile/view.html", talker, disease, talkerDisease, healthItemsMap, currentTalker);
		render("PublicProfile/newview.html", talker, disease, talkerDisease, healthItemsMap, currentTalker);
	}
	
	private static void showConvo(ConversationBean convo) {
		TalkerBean talker = null;
		if (Security.isConnected()) {
			talker = CommonUtil.loadCachedTalker(session);
		}
		
		ConversationDAO.incrementConvoViews(convo.getId());
		
		List<Action> activities = ActionDAO.loadLatestByConversation(convo);
		Date latestActivityTime = null;
		if (activities.size() > 0) {
			latestActivityTime = activities.get(0).getTime();
		}
		
//		List<TopicBean> topicsList = TopicDAO.getTopics();
		
		convo.setComments(CommentsDAO.loadConvoAnswers(convo.getId()));
		List<ConversationBean> relatedConvos = null;
		try {
			relatedConvos = SearchUtil.getRelatedConvos(convo);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		render("Conversations/viewConvo.html", talker, convo, latestActivityTime, relatedConvos);
    }
	
	private static void showTopic(TopicBean topic) {
		TalkerBean talker = null;
		if (Security.isConnected()) {
			talker = CommonUtil.loadCachedTalker(session);
		}
		
		//load latest activities for convos with this topic
		List<Action> activities = ActionDAO.loadLatestByTopic(topic);
		if (activities.size() > 20) {
			activities = activities.subList(0, 20);
		}
		
		TopicDAO.incrementTopicViews(topic.getId());
		
		//- "Popular Conversations" - ordered by page views
		List<ConversationBean> popularConvos = 
			new ArrayList<ConversationBean>(topic.getConversations());
		Collections.sort(popularConvos, new Comparator<ConversationBean>() {
			@Override
			public int compare(ConversationBean o1, ConversationBean o2) {
				return o2.getViews()-o1.getViews();
			}
		});
		
		//- "Trending Conversations" - ordered by page views in the last two weeks, 
		//cannot contain conversations in the top 10 of "Popular Conversations" tab
		List<ConversationBean> trendingConvos = new ArrayList<ConversationBean>();
		
		render("Topics/viewTopic.html", talker, topic, activities, popularConvos, trendingConvos);
	}

}

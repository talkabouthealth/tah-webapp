package controllers;

import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import controllers.PublicProfile.ProfileCompletion;

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
		
		calculateProfileCompletion(talker);
		
		render("PublicProfile/view.html", talker, disease, talkerDisease, healthItemsMap, currentTalker);
	}
	
	private static void calculateProfileCompletion(TalkerBean talker) {
		//check what items are completed
		EnumSet<ProfileCompletion> profileActions = EnumSet.of(ProfileCompletion.BASIC);
		
		//TODO: finish this items
//		COMMENT_CONVO(10, "Comment on a Conversation to get to "),
//		WRITE_SUMMARY(5, "Write or edit a summary of a Conversation to get to ");
		
		//TODO: if user deletes info after entering?
//		COMPLETE_PERSONAL(15, "Complete your Personal Info to get to "),
//		COMPLETE_HEALTH(10, "Complete your Health Details to get to "),
		
		for (Action action : talker.getActivityList()) {
			ActionType type = action.getType();
			switch (type) {
			case START_CONVO:
				profileActions.add(ProfileCompletion.START_CONVO);
			case JOIN_CONVO:
				profileActions.add(ProfileCompletion.JOIN_CONVO);
			case GIVE_THANKS:
				profileActions.add(ProfileCompletion.GIVE_THANKYOU);
			case UPDATE_PERSONAL:
				profileActions.add(ProfileCompletion.COMPLETE_PERSONAL);
			case UPDATE_HEALTH:
				profileActions.add(ProfileCompletion.COMPLETE_HEALTH);
			}
		}
		
		if (!talker.getFollowingList().isEmpty()) {
			profileActions.add(ProfileCompletion.FOLLOW);
		}
		
		//calculate current sum and next item to complete
		ProfileCompletion nextItem = null;
		int sum = 0;
		for (ProfileCompletion profileCompletion : ProfileCompletion.values()) {
			if (profileActions.contains(profileCompletion)) {
				sum += profileCompletion.getValue();
			}
			else {
				if (nextItem == null) {
					nextItem = profileCompletion;
				}
			}
		}
		String message = null;
		if (sum != 100) {
			int nextSum = sum + nextItem.getValue();
			message = nextItem.getDescription()+" "+nextSum+"%.";
		}
		
		talker.setProfileCompletionMessage(message);
		talker.setProfileCompletionValue(sum);
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
		
		TopicDAO.incrementTopicViews(topic.getId());
		
		render("Topics/viewTopic.html", talker, topic, activities);
	}

}

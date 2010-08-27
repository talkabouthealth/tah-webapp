package controllers;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import controllers.PublicProfile.ProfileCompletion;

import models.DiseaseBean;
import models.HealthItemBean;
import models.TalkerBean;
import models.TalkerDiseaseBean;
import models.TopicBean;
import models.actions.Action;
import dao.ActivityDAO;
import dao.CommentsDAO;
import dao.DiseaseDAO;
import dao.HealthItemDAO;
import dao.TalkerDAO;
import dao.TalkerDiseaseDAO;
import dao.TopicDAO;
import play.mvc.Controller;
import play.templates.JavaExtensions;
import util.CommonUtil;

/**
 * Is used for dispatch/show Users, Convos/Questions and Topics,
 * as they all share one url: http://talkabouthealth.com/{name} 
 *
 */
public class ViewDispatcher extends Controller {
	
	public static void view(String name) throws Throwable {
		//first try userName
		TalkerBean talker = TalkerDAO.getByUserName(name);
		if (talker != null) {
			showTalker(talker);
			return;
		}
		
		//next - question or conversation
		TopicBean topic = TopicDAO.getByURL(name);
		if (topic != null) {
			showConvo(topic);
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
		talker.setActivityList(ActivityDAO.load(talker.getId()));
		talker.setProfileCommentsList(CommentsDAO.loadProfileComments(talker.getId()));
		talker.setFollowingTopicsFullList(TalkerDAO.loadFollowingTopics(talker.getId()));
		
		talker.setStartedTopicsList(TopicDAO.loadTopics(talker.getId(), "START_CONVO"));
		talker.setJoinedTopicsList(TopicDAO.loadTopics(talker.getId(), "JOIN_CONVO"));
		
		//TODO: Temporarily - later we'll load all list of topics
		talker.setNumberOfTopics(TopicDAO.getNumberOfTopics(talker.getId()));
		
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
			String type = action.getType();
			if (type.equals("START_CONVO")) {
				profileActions.add(ProfileCompletion.START_CONVO);
			}
			else if (type.equals("JOIN_CONVO")) {
				profileActions.add(ProfileCompletion.JOIN_CONVO);
			}
			else if (type.equals("GIVE_THANKS")) {
				profileActions.add(ProfileCompletion.GIVE_THANKYOU);
			}
			else if (type.equals("UPDATE_PERSONAL")) {
				profileActions.add(ProfileCompletion.COMPLETE_PERSONAL);
			}
			else if (type.equals("UPDATE_HEALTH")) {
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
	
	private static void showConvo(TopicBean topic) {
		TalkerBean talker = null;
		if (Security.isConnected()) {
			talker = CommonUtil.loadCachedTalker(session);
		}
		
		TopicDAO.incrementTopicViews(topic.getId());
		
		topic.setComments(CommentsDAO.loadTopicComments(topic.getId()));
		
		//temporary test data
		topic.setDetails("Suggestions for friends and family...");
		topic.setTags(Arrays.asList("support", "help", "testtag"));
		topic.setSummary("Summary.........");
		topic.setSumContributors(Arrays.asList("murray", "situ"));
		
		render("Topics/viewTopic.html", talker, topic);
    }

}

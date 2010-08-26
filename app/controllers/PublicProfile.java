package controllers;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.DiseaseBean;
import models.HealthItemBean;
import models.TalkerBean;
import models.TalkerDiseaseBean;
import models.TopicBean;
import models.actions.Action;
import models.actions.FollowConvoAction;
import models.actions.FollowTalkerAction;
import models.actions.GiveThanksAction;
import models.actions.JoinConvoAction;
import models.actions.ProfileCommentAction;
import models.actions.ProfileReplyAction;
import models.actions.StartConvoAction;
import models.actions.UpdateProfileAction;
import dao.ActivityDAO;
import dao.CommentsDAO;
import dao.DiseaseDAO;
import dao.HealthItemDAO;
import dao.TalkerDAO;
import dao.TalkerDiseaseDAO;
import dao.TopicDAO;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;

@With(Secure.class)
public class PublicProfile extends Controller {
	
	enum ProfileCompletion {
		BASIC(25, "Sign Up"),
		JOIN_CONVO(5, "Join a Conversation to get to "),
		START_CONVO(10, "Start a Conversation to get to "),
		GIVE_THANKYOU(10, "Give a Thank you to get to "),
		COMMENT_CONVO(10, "Comment on a Conversation to get to "),
		FOLLOW(10, "Follow another member to get to "),
		COMPLETE_PERSONAL(15, "Complete your Personal Info to get to "),
		COMPLETE_HEALTH(10, "Complete your Health Details to get to "),
		WRITE_SUMMARY(5, "Write or edit a summary of a Conversation to get to ");
		
		private final int value;
		private final String description;
		
		private ProfileCompletion(int value, String description) {
			this.value = value;
			this.description = description;
		}

		public int getValue() {
			return value;
		}

		public String getDescription() {
			return description;
		}
	}
	
	public static void view(String userName) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByUserName(userName);
		
		notFoundIfNull(talker);
		
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
		
		render(talker, disease, talkerDisease, healthItemsMap, currentTalker);
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


	public static void userBasedActions(String userName, String action) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByUserName(userName);
		notFoundIfNull(talker);
		
		talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
		talker.setProfileCommentsList(CommentsDAO.loadProfileComments(talker.getId()));
		
		render(talker, currentTalker, action);
	}
	
	public static void loadMoreThankYous(String userName, int start) {
		TalkerBean talker = TalkerDAO.getByUserName(userName);
		notFoundIfNull(talker);
		
		render("@thankYousAjax", talker, start);
	}
	
	public static void loadMoreFollowlist(String userName, String followType, int start) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByUserName(userName);
		notFoundIfNull(talker);
		
		List<TalkerBean> followList = null;
		if (followType.equals("following")) {
			followList = talker.getFollowingList();
		}
		else {
			followList = TalkerDAO.loadFollowers(talker.getId());
		}
		
		render("@followlistAjax", talker, currentTalker, start, followList);
	}
	
	
	//TODO: load more for topics?
	public static void conversationsFollowing(String userName) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByUserName(userName);
		notFoundIfNull(talker);
		
		talker.setFollowingTopicsFullList(TalkerDAO.loadFollowingTopics(talker.getId()));
		
		render(talker, currentTalker);
	}
	
	public static void conversationsStarted(String userName) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByUserName(userName);
		notFoundIfNull(talker);
		
		String topicsType = "Started";
		List<TopicBean> topicsList = TopicDAO.loadTopics(talker.getId(), "START_CONVO");
		
		render("@conversationsList", talker, currentTalker, topicsType, topicsList);
	}
	
	public static void conversationsJoined(String userName) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByUserName(userName);
		notFoundIfNull(talker);
		
		String topicsType = "Joined";
		List<TopicBean> topicsList = TopicDAO.loadTopics(talker.getId(), "JOIN_CONVO");
		
		render("@conversationsList", talker, currentTalker, topicsType, topicsList);
	}
	
}

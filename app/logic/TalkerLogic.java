package logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import play.templates.JavaExtensions;
import dao.HealthItemDAO;
import dao.TopicDAO;

import util.CommonUtil;

import models.HealthItemBean;
import models.TalkerBean;
import models.TalkerDiseaseBean;
import models.TopicBean;
import models.ConversationBean.ConvoType;
import models.actions.Action;
import models.actions.Action.ActionType;

public class TalkerLogic {
	
	private static Map<String, List<String>> fieldsDataMap;
	
	public static void setFieldsDataMap(Map<String, List<String>> fieldsDataMap) {
		TalkerLogic.fieldsDataMap = fieldsDataMap;
	}
	
	public static List<String> getFieldsData(String fieldName, String talkerType) {
		String key = fieldName+"|"+talkerType;
		if (fieldsDataMap.containsKey(key)) {
			return fieldsDataMap.get(key);
		}
		else {
			key = fieldName+"|all";
			return fieldsDataMap.get(key);
		}
	}

	enum ProfileCompletion {
		BASIC(25, "Sign Up"),
		UPDATE_HEALTH(5, 
			"Share your <a href='"+CommonUtil.generateAbsoluteURL("Profile.healthDetails")+"'>Health Info</a>",
			"So we can match you with Members, Topics, and Conversations most relevant to you."),
		UPDATE_PERSONAL(10, 
			"Update your <a href='"+CommonUtil.generateAbsoluteURL("Profile.edit")+"'>Profile Info</a>",
			"So other member similar to you can find you if you choose."
			),
		VIEW_PRIVACY(10,
			"Update your <a href='"+CommonUtil.generateAbsoluteURL("Profile.preferences")+"'>Privacy Settings</a>",
			"So other member similar to you can find and reach out to you if you choose."),
		ASK_QUESTION(10, "Ask a <a href='#' onclick='return showQuestionDialog();'>Question</a>"),
		COMMENT_CONVO(10, "Answer a <a href='"+CommonUtil.generateAbsoluteURL("Explore.openQuestions")+"'>Question</a>"),
		GIVE_THANKYOU(10, "Give a Thank you"),
		COMMENT_THOUGHTS(5, "Comment in your <a href='"+
				CommonUtil.generateAbsoluteURL("PublicProfile.thoughts", "userName", "<username>")+"'>Thoughts Feed</a>"),
		FOLLOW(5, "Follow <a href='"+
				CommonUtil.generateAbsoluteURL("Community.browseMembers", "action", "active")+"'>another member</a>"),
		FOLLOW_TOPIC(5, "Follow a <a href='"+CommonUtil.generateAbsoluteURL("Explore.browseTopics")+"'>Topic</a>"),
		START_OR_JOIN_TALK(5, "Start or join a <a href='"+CommonUtil.generateAbsoluteURL("Explore.liveTalks")+"'>Live Talk</a>");
		
		
		private final int value;
		private final String description;
		private final String stepMessage;
		private final String stepNote;
		
		private ProfileCompletion(int value, String stepMessage) {
			this(value, null, stepMessage, null);
		}
		
		private ProfileCompletion(int value, String stepMessage, String stepNote) {
			this(value, null, stepMessage, stepNote);
		}
		
		private ProfileCompletion(int value, String description, String stepMessage, String stepNote) {
			this.value = value;
			this.description = description;
			this.stepMessage = stepMessage;
			this.stepNote = stepNote;
		}

		public int getValue() {
			return value;
		}
		public String getDescription() {
			if (description == null) {
				return stepMessage+" to get to ";
			}
			return description;
		}
		public String getStepMessage() {
			return stepMessage;
		}
		public String getStepNote() {
			return stepNote;
		}
	}
	
	public static void calculateProfileCompletion(TalkerBean talker) {
		//check what items are completed
		EnumSet<ProfileCompletion> profileActions = EnumSet.of(ProfileCompletion.BASIC);
		
		//TODO: if user deletes info after entering?
//		COMPLETE_PERSONAL(15, "Complete your Personal Info to get to "),
//		COMPLETE_HEALTH(10, "Complete your Health Details to get to "),
		
		for (Action action : talker.getActivityList()) {
			ActionType type = action.getType();
			switch (type) {
			case START_CONVO:
				if (action.getConvo().getConvoType() == ConvoType.CONVERSATION) {
					profileActions.add(ProfileCompletion.START_OR_JOIN_TALK);
				}
				else {
					profileActions.add(ProfileCompletion.ASK_QUESTION);
				}
				break;
			case JOIN_CONVO:
				profileActions.add(ProfileCompletion.START_OR_JOIN_TALK);
				break;
			case ANSWER_CONVO:
				profileActions.add(ProfileCompletion.COMMENT_CONVO);
				break;
			case GIVE_THANKS:
				profileActions.add(ProfileCompletion.GIVE_THANKYOU);
				break;
			case UPDATE_PERSONAL:
				profileActions.add(ProfileCompletion.UPDATE_PERSONAL);
				break;
			case UPDATE_HEALTH:
				profileActions.add(ProfileCompletion.UPDATE_HEALTH);
				break;
			case PERSONAL_PROFILE_COMMENT:
				profileActions.add(ProfileCompletion.COMMENT_THOUGHTS);
				break;
			}
		}
		
		if (!talker.getFollowingList().isEmpty()) {
			profileActions.add(ProfileCompletion.FOLLOW);
		}
		if (!talker.getFollowingTopicsList().isEmpty()) {
			profileActions.add(ProfileCompletion.FOLLOW_TOPIC);
		}
		if (talker.getHiddenHelps().contains("privacyViewed")) {
			profileActions.add(ProfileCompletion.VIEW_PRIVACY);
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
		talker.setProfileCompletionValue(sum);
		
		if (sum != 100) {
			int nextSum = sum + nextItem.getValue();
			String nextMessage = nextItem.getStepMessage();
			nextMessage = nextMessage.replaceAll("<username>", talker.getUserName());
			String message = nextMessage+" to go to "+nextSum+"%.";
			
			talker.setProfileCompletionMessage(message);
			talker.setNextStepMessage(nextMessage);
			talker.setNextStepNote(nextItem.getStepNote());
		}
	}
	
	public static List<TopicBean> getRecommendedTopics(TalkerDiseaseBean talkerDisease) {
		List<TopicBean> recommendedTopics = new ArrayList<TopicBean>();
		
		List<List<String>> stringHealthInfo = new ArrayList<List<String>>();
		
		Map<String, List<String>> healthInfo = talkerDisease.getHealthInfo();
		for (String key : healthInfo.keySet()) {
			stringHealthInfo.add(healthInfo.get(key));
		}
		
		Set<String> healthItems = talkerDisease.getHealthItems();
		List<HealthItemBean> allHealthItems = HealthItemDAO.getAllHealthItems(null);
		for (HealthItemBean healthItem : allHealthItems) {
			if (healthItems.contains(healthItem.getId())) {
				stringHealthInfo.add(Arrays.asList(healthItem.getName()));
			}
		}
		
		Map<String, List<String>> otherHealthItems = talkerDisease.getOtherHealthItems();
		for (String key : otherHealthItems.keySet()) {
			stringHealthInfo.add(otherHealthItems.get(key));
		}
		
		for (List<String> hi : stringHealthInfo) {
			for (String possibleTopic : hi ) {
				if (possibleTopic != null && possibleTopic.trim().length() != 0) {
					possibleTopic = JavaExtensions.capitalizeWords(possibleTopic);
					TopicBean topic = TopicDAO.getByTitle(possibleTopic);
					if (topic != null) {
						recommendedTopics.add(topic);
					}
				}
			}
		}
		
		return recommendedTopics;
	}

}

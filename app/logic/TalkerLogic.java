package logic;

import java.util.EnumSet;

import util.CommonUtil;

import models.TalkerBean;
import models.actions.Action;
import models.actions.Action.ActionType;

public class TalkerLogic {
	
	enum ProfileCompletion {
		BASIC(25, "Sign Up"),
		UPDATE_HEALTH(5, 
			"Share your <a href='"+CommonUtil.generateAbsoluteURL("Profile.healthDetails")+"'>Health Info</a>",
			"So we can match you with Members, Topics, and Conversations most relevant to you."),
		UPDATE_PERSONAL(10, 
			"Update your <a href='"+CommonUtil.generateAbsoluteURL("Profile.edit")+"'>Profile Info</a>",
			"So other members similar to you can find you."
			),
		VIEW_PRIVACY(10,
			"Update your <a href='"+CommonUtil.generateAbsoluteURL("Profile.preferences")+"'>Privacy Settings</a>",
			"So other members can find and reach out to you."),
		START_CONVO(10, "Ask a <a href='#' onclick='return showQuestionDialog();'>Question</a>"),
		COMMENT_CONVO(10, "Answer a <a href='"+CommonUtil.generateAbsoluteURL("Explore.openQuestions")+"'>Question</a>"),
		GIVE_THANKYOU(10, "Give a Thank you"),
		COMMENT_THOUGHTS(5, "Comment in your <a href='"+
				CommonUtil.generateAbsoluteURL("PublicProfile.thoughts", "userName", "<username>")+"'>Thoughts Feed</a>"),
		FOLLOW(5, "Follow <a href='"+
				CommonUtil.generateAbsoluteURL("Community.browseMembers", "action", "active")+"'>another member</a>"),
		FOLLOW_TOPIC(5, "Follow a <a href='"+CommonUtil.generateAbsoluteURL("Explore.browseTopics")+"'>Topic</a>"),
		JOIN_CONVO(5, "Join a <a href='"+CommonUtil.generateAbsoluteURL("Explore.liveTalks")+"'>Live Talk</a>");
		
		
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
				profileActions.add(ProfileCompletion.START_CONVO);
				break;
			case JOIN_CONVO:
				profileActions.add(ProfileCompletion.JOIN_CONVO);
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

}

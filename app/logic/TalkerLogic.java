package logic;

import java.util.EnumSet;

import util.CommonUtil;

import models.TalkerBean;
import models.actions.Action;
import models.actions.Action.ActionType;

public class TalkerLogic {
	
	enum ProfileCompletion {
		BASIC(25, "Sign Up"),
		UPDATE_HEALTH(5, "Share your <a href='"+CommonUtil.generateAbsoluteURL("Profile.healthDetails")+"'>Health Info</a> to get to "),
		UPDATE_PERSONAL(10, "Update your <a href='"+CommonUtil.generateAbsoluteURL("Profile.edit")+"'>Profile Info</a> to get to "),
		VIEW_PRIVACY(10, "Update your <a href='"+CommonUtil.generateAbsoluteURL("Profile.preferences")+"'>Privacy Settings</a> to get to "),
		START_CONVO(10, "Ask a <a href='#' onclick='return showQuestionDialog();'>Question</a> to get to "),
		COMMENT_CONVO(10, "Answer a <a href='"+CommonUtil.generateAbsoluteURL("Explore.openQuestions")+"'>Question</a> to get to "),
		GIVE_THANKYOU(10, "Give a Thank you to get to "),
		COMMENT_THOUGHTS(5, "Comment in your <a href='"+
				CommonUtil.generateAbsoluteURL("PublicProfile.thoughts", "userName", "<username>")+"'>Thoughts Feed</a> to get to "),
		FOLLOW(5, "Follow <a href='"+
				CommonUtil.generateAbsoluteURL("Community.browseMembers", "action", "active")+"'>another member</a> to get to "),
		FOLLOW_TOPIC(5, "Follow a <a href='"+CommonUtil.generateAbsoluteURL("Explore.browseTopics")+"'>Topic</a> to get to "),
		JOIN_CONVO(5, "Join a <a href='"+CommonUtil.generateAbsoluteURL("Explore.liveTalks")+"'>Live Talk</a> to get to ");
		
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
		String message = null;
		if (sum != 100) {
			int nextSum = sum + nextItem.getValue();
			String nextDescription = nextItem.getDescription();
			nextDescription = nextDescription.replaceAll("<username>", talker.getUserName());
			message = nextDescription+" "+nextSum+"%.";
		}
		
		talker.setProfileCompletionMessage(message);
		talker.setProfileCompletionValue(sum);
	}

}

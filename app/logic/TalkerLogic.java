package logic;

import java.util.EnumSet;

import models.TalkerBean;
import models.actions.Action;
import models.actions.Action.ActionType;

public class TalkerLogic {
	
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
			case REPLY_CONVO:
				profileActions.add(ProfileCompletion.COMMENT_CONVO);
				break;
			case GIVE_THANKS:
				profileActions.add(ProfileCompletion.GIVE_THANKYOU);
				break;
			case UPDATE_PERSONAL:
				profileActions.add(ProfileCompletion.COMPLETE_PERSONAL);
				break;
			case UPDATE_HEALTH:
				profileActions.add(ProfileCompletion.COMPLETE_HEALTH);
				break;
			case SUMMARY_ADDED:
			case SUMMARY_EDITED:
				profileActions.add(ProfileCompletion.WRITE_SUMMARY);
				break;
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

}

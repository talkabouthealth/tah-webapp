package logic;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import dao.ActionDAO;

import models.ConversationBean;
import models.TalkerBean;
import models.actions.Action;
import models.actions.Action.ActionType;

public class FeedsLogic {
	
	public static final int FEEDS_PER_PAGE = 40;
	
	public static Set<Action> getConvoFeed(TalkerBean talker, String afterActionId) {
		Set<Action> convoFeedActions = ActionDAO.loadConvoFeed(talker);
		
		Set<Action> convoFeed = filter(convoFeedActions, afterActionId);
		
		return convoFeed;
	}
	
	public static Set<Action> getCommunityFeed(String afterActionId) {
		Set<Action> communityFeedActions = ActionDAO.loadCommunityFeed();
		
		Set<Action> communityFeed = filter(communityFeedActions, afterActionId);
		
		return communityFeed;
	}
	
	private static Set<Action> filter(Set<Action> feedActions, String afterActionId) {
		//!!! Conversations should not appear more than once in the Conversation Feed
		//except for Answers, Replies, and Add and Edit Summaries. 
		EnumSet<ActionType> okActions = EnumSet.of(
			ActionType.ANSWER_CONVO, ActionType.REPLY_CONVO, 
			ActionType.SUMMARY_ADDED, ActionType.SUMMARY_EDITED,
			ActionType.ANSWER_VOTED
		);
		
		Set<Action> convoFeed = new LinkedHashSet<Action>();
		Set<ConversationBean> addedConvos = new HashSet<ConversationBean>();
		//we can add items only after given action (for paging)
		boolean canAdd = (afterActionId == null);
		for (Action action : feedActions) {
			ConversationBean actionConvo = action.getConvo();
			//check repeated conversations
			if (actionConvo != null && !okActions.contains(action.getType())) {
				if (!addedConvos.contains(actionConvo)) {
					if (canAdd) {
						convoFeed.add(action);
					}
					addedConvos.add(actionConvo);
				}
			}
			else {
				if (canAdd) {
					convoFeed.add(action);
				}
			}
			
			if (convoFeed.size() >= FEEDS_PER_PAGE) {
				break;
			}
			
			if (action.getId().equals(afterActionId)) {
				canAdd = true;
			}
		}
		
		return convoFeed;
	}

}

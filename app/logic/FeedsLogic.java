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
	
	public static Set<Action> getConvoFeed(TalkerBean talker, String afterActionId) {
		
		Set<Action> convoFeedActions = ActionDAO.loadConvoFeed(talker);
		
//		Community Convo Feed
//		if (convoFeedActions.size() < 40) {
//			List<Action> communityConvoFeed = ActionDAO.loadCommunityConvoFeed();
//			convoFeedActions.addAll(communityConvoFeed);
//		}
	
		//!!! Conversations should not appear more than once in the Conversation Feed
		//except for Answers, Replies, and Add and Edit Summaries. 
		EnumSet<ActionType> okActions = EnumSet.of(
			ActionType.ANSWER_CONVO, ActionType.REPLY_CONVO, 
			ActionType.SUMMARY_ADDED, ActionType.SUMMARY_EDITED,
			ActionType.ANSWER_VOTED
		);
		
		Set<Action> convoFeed = new LinkedHashSet<Action>();
		Set<ConversationBean> addedConvos = new HashSet<ConversationBean>();
		boolean canAdd = (afterActionId == null);
		for (Action action : convoFeedActions) {
			ConversationBean actionConvo = action.getConvo();
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
			
			if (convoFeed.size() >= 20) {
				break;
			}
			
			if (action.getId().equals(afterActionId)) {
				canAdd = true;
			}
		}
		
		return convoFeed;
	}

}

package logic;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import dao.ActionDAO;

import models.ConversationBean;
import models.TalkerBean;
import models.TopicBean;
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
		Set<Action> communityFeed = new LinkedHashSet<Action>();
		Set<ConversationBean> addedConvos = new HashSet<ConversationBean>();
		
		boolean exit = false;
		String nextActionId = null;
		while (true) {
			Set<Action> communityFeedActions = ActionDAO.loadCommunityFeed(nextActionId);
			if (communityFeedActions.size() == 0) {
				//no more feeds
				break;
			}
			if (communityFeedActions.size() < 80 ) {
				exit = true;
			}
			
			nextActionId = filter2(communityFeed, communityFeedActions, addedConvos, afterActionId);
			
			if (exit || communityFeed.size() >= FEEDS_PER_PAGE) {
				break;
			}
		}
		
		return communityFeed;
	}
	
	public static Set<Action> getTopicFeed(TopicBean topic, String afterActionId) {
		Set<Action> topicFeedActions = ActionDAO.loadLatestByTopic(topic);
		
		Set<Action> topicFeed = filter(topicFeedActions, afterActionId);
		return topicFeed;
	}
	
	private static String filter2(Set<Action> feed, 
			Set<Action> feedActions, Set<ConversationBean> addedConvos, String afterActionId) {
		Action lastAction = null;
		boolean canAdd = (afterActionId == null);
		for (Action action : feedActions) {
			ConversationBean actionConvo = action.getConvo();
			//check repeated conversations
			if (actionConvo != null) {
				if (!addedConvos.contains(actionConvo)) {
					if (canAdd) {
						feed.add(action);
					}
					addedConvos.add(actionConvo);
				}
			}
			else {
				if (canAdd) {
					feed.add(action);
				}
			}

			lastAction = action;
			
			if (feed.size() >= FEEDS_PER_PAGE) {
				break;
			}
			
			if (action.getId().equals(afterActionId)) {
				canAdd = true;
			}
		}
		
		if (lastAction != null) {
			return lastAction.getId();
		}
		else {
			return null;
		}
	}
	
	private static Set<Action> filter(Set<Action> feedActions, String afterActionId) {
		//!!! Conversations should not appear more than once in the feeds
		//except for Answers, Replies, and Add and Edit Summaries. 
		EnumSet<ActionType> okActions = EnumSet.noneOf(ActionType.class);
//			ActionType.ANSWER_CONVO, ActionType.REPLY_CONVO, 
//			ActionType.SUMMARY_ADDED, ActionType.SUMMARY_EDITED
//		);
		
		Set<Action> feed = new LinkedHashSet<Action>();
		Set<ConversationBean> addedConvos = new HashSet<ConversationBean>();
		//we can add items only after given action (for paging)
		boolean canAdd = (afterActionId == null);
		for (Action action : feedActions) {
//			if (action.getType() == ActionType.PERSONAL_PROFILE_COMMENT)
			
			ConversationBean actionConvo = action.getConvo();
			//check repeated conversations
			if (actionConvo != null && !okActions.contains(action.getType())) {
				if (!addedConvos.contains(actionConvo)) {
					if (canAdd) {
						feed.add(action);
						addedConvos.add(actionConvo);
					}
				}
			}
			else {
				if (canAdd) {
					feed.add(action);
				}
			}
			
			if (feed.size() >= FEEDS_PER_PAGE) {
				break;
			}
			
			if (action.getId().equals(afterActionId)) {
				canAdd = true;
			}
		}
		
		return feed;
	}

}

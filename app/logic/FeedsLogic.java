package logic;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import dao.ActionDAO;

import models.ConversationBean;
import models.TalkerBean;
import models.TopicBean;
import models.actions.Action;
import models.actions.Action.ActionType;

public class FeedsLogic {
	
	public static final int FEEDS_PER_PAGE = 40;
	public static final int ACTIONS_PRELOAD = 80;
	
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
		boolean canAdd = (afterActionId == null);
		while (true) {
			List<Action> communityFeedActions = ActionDAO.loadCommunityFeed(nextActionId);
			if (communityFeedActions.size() == 0) {
				//no more feeds
				break;
			}
			if (communityFeedActions.size() < ACTIONS_PRELOAD ) {
				exit = true;
			}
			
			canAdd = filter2(communityFeed, communityFeedActions, 
					addedConvos, afterActionId, canAdd);
			
			if (exit || communityFeed.size() >= FEEDS_PER_PAGE) {
				break;
			}
			
			//id for next pre-load from db
			int s = communityFeedActions.size();
			nextActionId = communityFeedActions.get(s-1).getId();
		}
		
		return communityFeed;
	}
	
	public static Set<Action> getTopicFeed(TopicBean topic, String afterActionId) {
		Set<Action> topicFeedActions = ActionDAO.loadLatestByTopic(topic);
		
		Set<Action> topicFeed = filter(topicFeedActions, afterActionId);
		return topicFeed;
	}
	
	private static boolean filter2(Set<Action> feed, 
			List<Action> feedActions, Set<ConversationBean> addedConvos, String afterActionId, boolean canAdd) {
		Action lastAction = null;
		
		for (Action action : feedActions) {
			ConversationBean actionConvo = action.getConvo();
			//check repeated conversations
			if (actionConvo != null) {
//				System.out.println("T: "+actionConvo.getTopic()+", add: "+canAdd);
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
			
//			System.out.println(action.getId());
//			System.out.println(afterActionId);
			if (action.getId().equals(afterActionId)) {
				canAdd = true;
			}
		}
		
		return canAdd;
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

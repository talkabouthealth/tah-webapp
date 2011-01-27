package logic;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import play.Logger;

import dao.ActionDAO;

import models.ConversationBean;
import models.TalkerBean;
import models.TopicBean;
import models.actions.Action;
import models.actions.Action.ActionType;
import models.actions.PreloadAction;

public class FeedsLogic {
	
	enum FeedType {
		CONVERSATION, COMMUNITY, TALKER, TOPIC
	}
	
	//Actions per page for Talker Feed and all other feeds
	public static final int TALKERFEEDS_PER_PAGE = 20;
	public static final int FEEDS_PER_PAGE = 40;
	
	//Number of actions to preload from DB (later filtered)
	public static final int ACTIONS_PRELOAD = 500;
	
	public static Set<Action> getConvoFeed(TalkerBean talker, String afterActionId) {
		return loadFeed(FeedType.CONVERSATION, afterActionId, talker, null, true, FEEDS_PER_PAGE);
	}
	
	public static Set<Action> getCommunityFeed(String afterActionId, boolean loggedIn) {
		return loadFeed(FeedType.COMMUNITY, afterActionId, null, null, loggedIn, FEEDS_PER_PAGE);
	}
	
	public static Set<Action> getTalkerFeed(TalkerBean talker, String afterActionId) {
		return loadFeed(FeedType.TALKER, afterActionId, talker, null, true, TALKERFEEDS_PER_PAGE);
	}
	
	public static Set<Action> getTopicFeed(TopicBean topic, String afterActionId) {
		return loadFeed(FeedType.TOPIC, afterActionId, null, topic, true, FEEDS_PER_PAGE);
	}
	
	/**
	 * Load feed page based on given parameters.
	 * Feeds must not have actions with the same conversation, so actions are filtered.
	 * 
	 * @param feedType
	 * @param afterActionId last action from previous page (load actions created before it)
	 * @param talker
	 * @param topic
	 * @param loggedIn Is current user authenticated? Required for CommunityFeed.
	 * @param feedsPerPage
	 * @return
	 */
	private static Set<Action> loadFeed(FeedType feedType, String afterActionId,
			TalkerBean talker, TopicBean topic, boolean loggedIn, int feedsPerPage) {
		Set<Action> feed = new LinkedHashSet<Action>();
		//conversations that are already added to the feed
		Set<ConversationBean> addedConvos = new HashSet<ConversationBean>();
		
		String nextActionId = null;
		//can we add 
		boolean canAdd = (afterActionId == null);
		while (true) {
			List<Action> feedActions = new ArrayList<Action>();
			switch (feedType) {
				case CONVERSATION: 
					feedActions = ActionDAO.loadConvoFeed(talker, nextActionId);
					break;
				case COMMUNITY: 
					feedActions = ActionDAO.loadCommunityFeed(nextActionId, loggedIn);
					break;
				case TALKER: 
					feedActions = ActionDAO.loadTalkerFeed(talker.getId(), nextActionId);
					break;
				case TOPIC: 
					feedActions = ActionDAO.loadLatestByTopic(topic, nextActionId);
					break;
			}
			
			canAdd = filter(feed, feedActions, 
					addedConvos, afterActionId, canAdd, feedsPerPage);
			
			//exit if no more actions to preload or feed is big enough for this page
			if (feedActions.size() < ACTIONS_PRELOAD || feed.size() >= feedsPerPage) {
				break;
			}
			
			//id for next preload from db
			nextActionId = feedActions.get(feedActions.size()-1).getId();
		}
		
		return feed;
	}
	
	private static boolean filter(Set<Action> feed, 
			List<Action> feedActions, Set<ConversationBean> addedConvos, 
			String afterActionId, boolean canAdd, int feedSize) {
		for (Action action : feedActions) {
			ConversationBean actionConvo = action.getConvo();
			//check repeated conversations
			if (actionConvo != null) {
				if (!addedConvos.contains(actionConvo)) {
					if (canAdd) {
						PreloadAction preAction = (PreloadAction)action;
						feed.add(preAction.getFullAction());
					}
					addedConvos.add(actionConvo);
				}
			}
			else {
				if (canAdd) {
					PreloadAction preAction = (PreloadAction)action;
					Action fullAction = preAction.getFullAction();
					if (!(fullAction.getType() == ActionType.PERSONAL_PROFILE_COMMENT
							&& fullAction.getTalker().isSuspended())) {
						feed.add(fullAction);
					}
				}
			}

			if (feed.size() >= feedSize) {
				break;
			}
			if (action.getId().equals(afterActionId)) {
				canAdd = true;
			}
		}
		
		return canAdd;
	}
	
}

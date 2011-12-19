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
	
	public enum FeedType {
		CONVERSATION, COMMUNITY, TALKER, TOPIC, ALL_CANCER
	}
	
	//Actions per page for Talker Feed and all other feeds
	public static final int TALKERFEEDS_PER_PAGE = 20;
	public static final int FEEDS_PER_PAGE = 20;
	
	//Number of actions to preload from DB (later filtered)
	public static final int ACTIONS_PRELOAD = 200;
	
	public static Set<Action> getConvoFeed(TalkerBean talker, String afterActionId) {
		return loadFeed(FeedType.CONVERSATION, afterActionId, talker, null, true, FEEDS_PER_PAGE);
	}
	
	public static Set<Action> getCommunityFeed(String afterActionId, boolean loggedIn,TalkerBean talker) {
		return loadFeed(FeedType.COMMUNITY, afterActionId, talker, null, loggedIn, FEEDS_PER_PAGE);
	}
	
	public static Set<Action> getTalkerFeed(TalkerBean talker, String afterActionId) {
		return loadFeed(FeedType.TALKER, afterActionId, talker, null, true, TALKERFEEDS_PER_PAGE);
	}
	
	public static Set<Action> getTopicFeed(TalkerBean talker,TopicBean topic, String afterActionId) {
		return loadFeed(FeedType.TOPIC, afterActionId, talker, topic, true, FEEDS_PER_PAGE);
	}
	
	public static Set<Action> getAllCancerFeed(String afterActionId, boolean loggedIn,TalkerBean talker) {
		return loadFeed(FeedType.ALL_CANCER, afterActionId, talker, null, loggedIn, FEEDS_PER_PAGE);
	}
	
	// TODO request update for feed
	public static Set<Action> updateFeed(FeedType feedType, String beforeActionId,
			TalkerBean talker, boolean loggedIn) {
		TopicBean topic = null;
		Set<Action> feed = new LinkedHashSet<Action>();
		
		//conversations that are already added to the feed
		Set<ConversationBean> addedConvos = new HashSet<ConversationBean>();
		
		String nextActionId = null;
		while (true) {
			List<Action> feedActions = new ArrayList<Action>();
			switch (feedType) {
				case CONVERSATION: 
					feedActions = ActionDAO.loadConvoFeed(talker, nextActionId);
					break;
				case COMMUNITY: 
					feedActions = ActionDAO.loadCommunityFeed(nextActionId, loggedIn,talker);
					break;
				case TALKER: 
					feedActions = ActionDAO.loadTalkerFeed(talker.getId(), nextActionId);
					break;
				case TOPIC: 
					feedActions = ActionDAO.loadLatestByTopic(talker,topic, nextActionId);
					break;
				case ALL_CANCER: 
					feedActions = ActionDAO.loadAllCancerFeed(nextActionId, loggedIn,talker);
					break;
			}
			if(feedActions == null)
				break;
			if(feedActions != null && feedActions.isEmpty())
				break;
			
			boolean canAdd = true;
			for (Action action : feedActions) {
				if (canAdd && action.getId().equals(beforeActionId)) {
					canAdd = false;
				}
				
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
						//we do not include thoughts of suspended users
						if (!(fullAction.getType() == ActionType.PERSONAL_PROFILE_COMMENT
								&& fullAction.getTalker().isSuspended())) {
							feed.add(fullAction);
						}
					}
				}
			}
			
			//id for next preload from db
			if(feedActions != null && feedActions.size() > 0)
				nextActionId = feedActions.get(feedActions.size()-1).getId();
			
			//exit if no more actions to preload or feed is big enough for this page
			if (nextActionId.equals(beforeActionId) || !canAdd || feedActions.size() < ACTIONS_PRELOAD || feed.size() >= 100) {
				break;
			}
		}
		return feed;
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
		boolean canAdd = (afterActionId == null);
		while (true) {
			List<Action> feedActions = new ArrayList<Action>();
			switch (feedType) {
				case CONVERSATION: 
					feedActions = ActionDAO.loadConvoFeed(talker, nextActionId);
					break;
				case COMMUNITY: 
					feedActions = ActionDAO.loadCommunityFeed(nextActionId, loggedIn,talker);
					break;
				case TALKER: 
					feedActions = ActionDAO.loadTalkerFeed(talker.getId(), nextActionId);
					break;
				case TOPIC: 
					feedActions = ActionDAO.loadLatestByTopic(talker,topic, nextActionId);
					break;
				case ALL_CANCER: 
					feedActions = ActionDAO.loadAllCancerFeed(nextActionId, loggedIn,talker);
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
	
	/**
	 * Filters given feed actions to new Feed.
	 * 
	 * @param feed Result feed
	 * @param feedActions Source feed actions
	 * @param addedConvos Conversations that are already in the feed
	 * @param afterActionId Latest id from previous page - we need to load only older answer
	 * @param canAdd Determines if we can add feed items (we don't need items already loaded in previous page)
	 * @param feedSize Size of the feed that we need
	 * @return
	 */
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
					//we do not include thoughts of suspended users
					if (!(fullAction.getType() == ActionType.PERSONAL_PROFILE_COMMENT
							&& fullAction.getTalker().isSuspended())) {
						feed.add(fullAction);
					}
				}
			}

			//enough size?
			if (feed.size() >= feedSize) {
				break;
			}
			if (!canAdd && action.getId().equals(afterActionId)) {
				canAdd = true;
			}
		}
		
		return canAdd;
	}
	
	public static List<String> getCancerType(TalkerBean talker){
		String cancerType = "Breast Cancer";
		List<String> cat = new ArrayList<String>(2);
		if (talker != null) {
			if(talker.getCategory() == null){
				cat.add(null);
				cat.add(cancerType);
			} else if(cancerType.equals(talker.getCategory())) {
				cat.add(null);
				cat.add(cancerType);
			} else{
				cat.add(talker.getCategory());
			}
		} else {
			cat.add(null);
			cat.add(cancerType);
		}
		return cat;
	}
}

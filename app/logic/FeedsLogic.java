package logic;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import play.Logger;

import dao.ActionDAO;

import models.ConversationBean;
import models.TalkerBean;
import models.TopicBean;
import models.actions.Action;
import models.actions.PreloadAction;
import models.actions.Action.ActionType;
import dao.ActionDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;

public class FeedsLogic {
	
	public enum FeedType {
		CONVERSATION, COMMUNITY, TALKER, TOPIC, ALL_CANCER
	}
	
	//Actions per page for Talker Feed and all other feeds
	public static final int TALKERFEEDS_PER_PAGE = 20;
	public static final int FEEDS_PER_PAGE = 20;
	
	//Number of actions to preload from DB (later filtered)
	public static final int ACTIONS_PRELOAD = 50;
	
	public static Set<Action> getConvoFeed(TalkerBean talker, String afterActionId) {
		return loadFeed(FeedType.CONVERSATION, afterActionId, talker, null, true, FEEDS_PER_PAGE);
	}
	
	public static Set<Action> getCommunityFeed(String afterActionId, boolean loggedIn,TalkerBean talker) {
		return loadFeed(FeedType.COMMUNITY, afterActionId, talker, null, loggedIn, FEEDS_PER_PAGE);
	}
	
	public static Set<Action> getTalkerFeed(TalkerBean talker, String afterActionId) {
		return loadFeed(FeedType.TALKER, afterActionId, talker, null, true, TALKERFEEDS_PER_PAGE);
	}
	
	public static Set<Action> getTopicFeed(TalkerBean talker,TopicBean topic, String afterActionId,boolean isExp) {
		return loadFeed(FeedType.TOPIC, afterActionId, talker, topic, isExp, FEEDS_PER_PAGE);
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
		int count = 0;
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
					feedActions = ActionDAO.loadLatestByTopic(talker,topic, nextActionId,loggedIn);
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
				} else {
					if (canAdd) {
						PreloadAction preAction = (PreloadAction)action;
						Action fullAction = preAction.getFullAction();
						//we do not include thoughts of suspended users
						if (!(fullAction.getType() == ActionType.PERSONAL_PROFILE_COMMENT && fullAction.getTalker().isSuspended())) {
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
			
			count++;
		}
		Logger.info("updateFeed - "+ count);
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
		
		String nextActionId = afterActionId;
		boolean canAdd = true;
		List<Action> feedActions = new ArrayList<Action>();
		int count = 0;
		while (true) {
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
					feedActions = ActionDAO.loadLatestByTopic(talker,topic, nextActionId,loggedIn);
					break;
				case ALL_CANCER: 
					feedActions = ActionDAO.loadAllCancerFeed(nextActionId, loggedIn,talker);
					break;
			}
			
			canAdd = filter(feed, feedActions, 
					addedConvos, afterActionId, canAdd, feedsPerPage,feedType,loggedIn);
			
			//exit if no more actions to preload or feed is big enough for this page
			if (feedActions.size() < ACTIONS_PRELOAD || feed.size() >= feedsPerPage) {
				break;
			}
			
			//id for next preload from db
			nextActionId = feedActions.get(feedActions.size()-1).getId();
			count++;
		}
		Logger.info("loadFeed : " + count);
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
			String afterActionId, boolean canAdd, int feedSize, FeedType feedType,boolean loggedIn) {
		boolean isAdd = true;
		for (Action action : feedActions) {
			ConversationBean actionConvo = action.getConvo();
			//check repeated conversations
			isAdd = true;
			if (actionConvo != null) {
				
					switch (feedType) {
						case TOPIC:
							TalkerBean talker = action.getTalker();
							String con = TalkerDAO.getTalkerConnection(talker.getId());
							if(loggedIn && TalkerBean.PROFESSIONAL_CONNECTIONS_LIST.contains(con))
								 isAdd = true;
							 else if(!loggedIn && !TalkerBean.PROFESSIONAL_CONNECTIONS_LIST.contains(con))
								 isAdd = true;
							 else
								 isAdd = false;
							 break;
						case ALL_CANCER: 
							if (!addedConvos.contains(actionConvo)) {
								String str[]={"opened"};
								ConversationBean convo = ConversationDAO.getConvoCategories(actionConvo.getId(),str);
								
								if(convo.isOpened() == true) 
									isAdd = false;
								else
									isAdd = true;
							}else
								isAdd = false;
							break;
					}
					if (canAdd) {
						PreloadAction preAction = (PreloadAction)action;
						if(isAdd)
							feed.add(preAction.getFullAction());
					}
					addedConvos.add(actionConvo);
				
			} else {
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
		//String cancerType = "Breast Cancer";
		List<String> cat = new ArrayList<String>(2);
		if (talker != null) {
			if(talker.getCategory() == null){
				cat.add(null);
				//cat.add(cancerType);
				/*} else if(cancerType.equals(talker.getCategory())) {
				cat.add(null);
				cat.add(cancerType);*/
			} else{
				cat.add(talker.getCategory());
			}
		} else {
			cat.add(null);
			//cat.add(cancerType);
		}
		return cat;
	}
}

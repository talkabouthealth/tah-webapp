package dao;

import java.util.ArrayList;

import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import util.DBUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import models.TalkerBean;
import models.ConversationBean;
import models.TopicBean;
import models.actions.AbstractAction;
import models.actions.Action;
import models.actions.Action.ActionType;
import models.actions.AnswerConvoAction;
import models.actions.AnswerVotedAction;
import models.actions.FollowConvoAction;
import models.actions.FollowTalkerAction;
import models.actions.GiveThanksAction;
import models.actions.JoinConvoAction;
import models.actions.PersonalProfileCommentAction;
import models.actions.StartConvoAction;
import models.actions.SummaryConvoAction;
import models.actions.UpdateProfileAction;

import static util.DBUtil.*;

public class ActionDAO {
	
	private static final EnumSet<ActionType> CONVO_FEED_ACTIONS = EnumSet.of(
			ActionType.START_CONVO, ActionType.RESTART_CONVO, 
			ActionType.ANSWER_CONVO, ActionType.REPLY_CONVO, 
			ActionType.SUMMARY_ADDED, ActionType.SUMMARY_EDITED 
		);
	
	private static final EnumSet<ActionType> ACTIVITY_FEED_ACTIONS = EnumSet.of(
			ActionType.START_CONVO, ActionType.RESTART_CONVO, ActionType.JOIN_CONVO, 
			ActionType.ANSWER_CONVO, ActionType.REPLY_CONVO, 
			ActionType.SUMMARY_ADDED, ActionType.SUMMARY_EDITED,
			ActionType.PERSONAL_PROFILE_COMMENT, ActionType.PERSONAL_PROFILE_REPLY
		);
	
	private static final EnumSet<ActionType> COMMUNITY_CONVO_FEED_ACTIONS = EnumSet.of(
			ActionType.START_CONVO, ActionType.RESTART_CONVO, 
			ActionType.ANSWER_CONVO, 
			ActionType.SUMMARY_ADDED, ActionType.SUMMARY_EDITED,
			ActionType.ANSWER_VOTED
		);
	
	
	//TODO: rename to "action?"
	public static final String ACTIVITIES_COLLECTION = "activities";
	
	public static List<Action> load(String talkerId) {
		DBCollection activitiesColl = getCollection(ACTIVITIES_COLLECTION);
		
		DBRef talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talkerId);
		DBObject query = new BasicDBObject("uid", talkerRef);
		List<DBObject> activitiesDBList = 
			activitiesColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		List<Action> activitiesList = new ArrayList<Action>();
		for (DBObject actionDBObject : activitiesDBList) {
			Action action = actionFromDB(actionDBObject);
			activitiesList.add(action);
		}
		return activitiesList;
	}
	
	//------------------ Feeds --------------------
	
//	- New conversation started in a topic that is being followed
//	- Conversation restarted in a topic or Conversation(?) that is being followed
//	- New answer or reply in a Topic or Conversation that is being followed
//	- TODO?: Summary created or edited in Conversation that is being followed.
	public static List<Action> loadConvoFeed(TalkerBean talker) {
		//TODO: move to FeedLogic?
		
		//prepare list of followed convos/topics
		Set<DBRef> convosDBSet = new HashSet<DBRef>();
		for (String convoId : talker.getFollowingConvosList()) {
			convosDBSet.add(createRef(ConversationDAO.CONVERSATIONS_COLLECTION, convoId));
		}
		for (TopicBean topic : talker.getFollowingTopicsList()) {	
			convosDBSet.addAll(getConversationsByTopic(topic));
		}
		
		//list of needed actions for this Feed
		Set<String> actionTypes = new HashSet<String>();
		for (ActionType actionType : CONVO_FEED_ACTIONS) {
			actionTypes.add(actionType.toString());
		}
		
		//load actions for this criterias
		DBCollection activitiesColl = getCollection(ACTIVITIES_COLLECTION);
		
		DBObject query = BasicDBObjectBuilder.start()
			.add("topicId", new BasicDBObject("$in", convosDBSet))
			.add("type", new BasicDBObject("$in", actionTypes))
			.get();
		List<DBObject> activitiesDBList = 
			activitiesColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		List<Action> activitiesList = new ArrayList<Action>();
		for (DBObject actionDBObject : activitiesDBList) {
			Action action = actionFromDB(actionDBObject);
			activitiesList.add(action);
		}
		return activitiesList;
	}
	
	public static List<Action> loadActivityFeed(TalkerBean talker) {
		//TODO: move to FeedLogic?
		
		//prepare list of followed talkers
		Set<DBRef> talkersDBSet = new HashSet<DBRef>();
		for (TalkerBean followingTalker : talker.getFollowingList()) {
			talkersDBSet.add(createRef(TalkerDAO.TALKERS_COLLECTION, followingTalker.getId()));
		}
		
		//list of needed actions for this Feed
		Set<String> actionTypes = new HashSet<String>();
		for (ActionType actionType : ACTIVITY_FEED_ACTIONS) {
			actionTypes.add(actionType.toString());
		}
		
		//load actions for this criterias
		DBCollection activitiesColl = getCollection(ACTIVITIES_COLLECTION);
		
		DBObject query = BasicDBObjectBuilder.start()
			.add("uid", new BasicDBObject("$in", talkersDBSet))
			.add("type", new BasicDBObject("$in", actionTypes))
			.get();
		List<DBObject> activitiesDBList = 
			activitiesColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		List<Action> activitiesList = new ArrayList<Action>();
		for (DBObject actionDBObject : activitiesDBList) {
			Action action = actionFromDB(actionDBObject);
			activitiesList.add(action);
		}
		return activitiesList;
	}
	
	public static List<Action> loadCommunityConvoFeed() {
		//list of needed actions for this Feed
		Set<String> actionTypes = new HashSet<String>();
		for (ActionType actionType : COMMUNITY_CONVO_FEED_ACTIONS) {
			actionTypes.add(actionType.toString());
		}
		
		//load actions for this criterias
		DBCollection activitiesColl = getCollection(ACTIVITIES_COLLECTION);
		
		DBObject query = BasicDBObjectBuilder.start()
			.add("type", new BasicDBObject("$in", actionTypes))
			.get();
		List<DBObject> activitiesDBList = 
			activitiesColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		List<Action> activitiesList = new ArrayList<Action>();
		for (DBObject actionDBObject : activitiesDBList) {
			Action action = actionFromDB(actionDBObject);
			activitiesList.add(action);
		}
		return activitiesList;
	}
	
	public static List<Action> loadLatestByTopic(TopicBean topic) {
		DBCollection activitiesColl = getCollection(ACTIVITIES_COLLECTION);
		
		Set<DBRef> convosDBSet = getConversationsByTopic(topic);
		DBObject query = new BasicDBObject("topicId", new BasicDBObject("$in", convosDBSet));
		List<DBObject> activitiesDBList = 
			activitiesColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		List<Action> activitiesList = new ArrayList<Action>();
		for (DBObject actionDBObject : activitiesDBList) {
			Action action = actionFromDB(actionDBObject);
			activitiesList.add(action);
		}
		return activitiesList;
	}
	
	/**
	 * Includes conversations in children topics also.
	 * @param topic
	 * @return
	 */
	private static Set<DBRef> getConversationsByTopic(TopicBean topic) {
		Set<DBRef> convosDBSet = new HashSet<DBRef>();
		for (String convoId : topic.getConversationsIds()) {
			convosDBSet.add(createRef(ConversationDAO.CONVERSATIONS_COLLECTION, convoId));
		}
		for (TopicBean child : topic.getChildren()) {
			TopicBean fullChild = TopicDAO.getById(child.getId());
			convosDBSet.addAll(getConversationsByTopic(fullChild));
		}
		return convosDBSet;
	}
	
	public static List<Action> loadLatestByConversation(ConversationBean convo) {
		DBCollection activitiesColl = getCollection(ACTIVITIES_COLLECTION);
		
		DBRef convoRef = createRef(ConversationDAO.CONVERSATIONS_COLLECTION, convo.getId());
		DBObject query = new BasicDBObject("topicId", convoRef);
		List<DBObject> activitiesDBList = 
			activitiesColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		List<Action> activitiesList = new ArrayList<Action>();
		for (DBObject actionDBObject : activitiesDBList) {
			Action action = actionFromDB(actionDBObject);
			if (action != null) {
				activitiesList.add(action);
			}
		}
		return activitiesList;
	}
	
	private static Action actionFromDB(DBObject dbObject) {
		String strType = (String)dbObject.get("type");
		
		if (strType == null) {
			return null;
		}
		
		ActionType type = ActionType.valueOf(strType);
		switch (type) {
			case START_CONVO:
			case RESTART_CONVO:
				return new StartConvoAction(dbObject);
			case JOIN_CONVO:
				return new JoinConvoAction(dbObject);
				
			case ANSWER_CONVO:
			case REPLY_CONVO:
				return new AnswerConvoAction(dbObject);
				
			case ANSWER_VOTED:
				return new AnswerVotedAction(dbObject);
				
			case SUMMARY_ADDED:
			case SUMMARY_EDITED:
				return new SummaryConvoAction(dbObject);
				
			case GIVE_THANKS:
				return new GiveThanksAction(dbObject);
			case FOLLOW_CONVO:
				return new FollowConvoAction(dbObject);
			case FOLLOW_TALKER:
				return new FollowTalkerAction(dbObject);
			case PERSONAL_PROFILE_COMMENT:
			case PERSONAL_PROFILE_REPLY:
				return new PersonalProfileCommentAction(dbObject);
				
			case UPDATE_BIO:
			case UPDATE_HEALTH:
			case UPDATE_PERSONAL:
				return new UpdateProfileAction(dbObject);
			
			default:
				throw new IllegalArgumentException("Bad Action Type");
		}
	}
	
	public static void saveAction(Action action) {
		DBCollection activitiesColl = getCollection(ACTIVITIES_COLLECTION);

		DBObject actionDBObject = action.toDBObject();
		activitiesColl.save(actionDBObject);
	}
	

	public static void main(String[] args) {
//		System.out.println(ActionDAO.load("4c2cb43160adf3055c97d061"));
	}

}

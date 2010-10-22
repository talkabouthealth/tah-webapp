package dao;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import util.DBUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import models.CommentBean;
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
import models.actions.TopicAddedAction;
import models.actions.UpdateProfileAction;

import static util.DBUtil.*;

public class ActionDAO {
	
	/*
So the following member actions trigger items in the feed:
- Member followed starts/restarts a conversation or asks a question
- Member followed joins conversation
- Member followed answers or replies to a question
- Member followed edits or adds a Summary
- Member followed leaves comment on their own journal or another member leaves a comment in their journal
- Member followed voted for an answer

Conversation actions that trigger feeds:
- Conversation/question started or restarted in a topic that is being followed
- New answer in a Topic or Conversation that is being followed
- Reply in a conversation that is being followed
- Summary created or edited in Conversation/Topic that is being followed.
- Conversation added to a topic being followed	 
	 */
	
	private static final EnumSet<ActionType> CONVO_FEED_ACTIONS = EnumSet.of(
			ActionType.START_CONVO, ActionType.RESTART_CONVO, ActionType.JOIN_CONVO,
			ActionType.ANSWER_CONVO, ActionType.REPLY_CONVO, 
			ActionType.SUMMARY_ADDED, ActionType.SUMMARY_EDITED,
			ActionType.ANSWER_VOTED,
			ActionType.TOPIC_ADDED,
			ActionType.PERSONAL_PROFILE_COMMENT, ActionType.PERSONAL_PROFILE_REPLY
		);
	
//	- conversation started
//	- question answered
//	- reply to answer
//	- summary added or edited
//	- answer received vote
	private static final EnumSet<ActionType> COMMUNITY_CONVO_FEED_ACTIONS = EnumSet.of(
			ActionType.START_CONVO,
			ActionType.ANSWER_CONVO, ActionType.REPLY_CONVO, 
			ActionType.SUMMARY_ADDED, ActionType.SUMMARY_EDITED,
			ActionType.ANSWER_VOTED
		);
	
	
	//FIXME: rename to "action?"
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
	
	//It contains items based on actions from topics, questions, 
	//and other users (including comments) that are followed.
	public static Set<Action> loadConvoFeed(TalkerBean talker) {
		DBRef currentTalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talker.getId());
		
		//prepare list of followed convos/topics
		Set<DBRef> convosDBSet = new HashSet<DBRef>();
		for (String convoId : talker.getFollowingConvosList()) {
			convosDBSet.add(createRef(ConversationDAO.CONVERSATIONS_COLLECTION, convoId));
		}
		for (TopicBean topic : talker.getFollowingTopicsList()) {	
			convosDBSet.addAll(ConversationDAO.getConversationsByTopic(topic));
		}
		
		//prepare list of followed talkers
		Set<DBRef> talkersDBSet = new HashSet<DBRef>();
		for (TalkerBean followingTalker : talker.getFollowingList()) {
			talkersDBSet.add(createRef(TalkerDAO.TALKERS_COLLECTION, followingTalker.getId()));
		}
		
		//list of needed actions for this Feed
		Set<String> actionTypes = new HashSet<String>();
		for (ActionType actionType : CONVO_FEED_ACTIONS) {
			actionTypes.add(actionType.toString());
		}
		
		//load actions for this criterias
		DBCollection activitiesColl = getCollection(ACTIVITIES_COLLECTION);
		DBObject query = BasicDBObjectBuilder.start()
			.add("$or", Arrays.asList(
							BasicDBObjectBuilder.start()
								.add("type", ActionType.START_CONVO.toString())
								.add("uid", currentTalkerRef)
								.get(),
							BasicDBObjectBuilder.start()
								.add("topicId", new BasicDBObject("$in", convosDBSet))
								.add("uid", new BasicDBObject("$ne", currentTalkerRef))
								.get(),
							new BasicDBObject("uid", new BasicDBObject("$in", talkersDBSet)),
							//load all comments from followings' journals
							BasicDBObjectBuilder.start()
								.add("otherTalker", new BasicDBObject("$in", talkersDBSet))
								.add("uid", new BasicDBObject("$ne", currentTalkerRef))
								.add("type", new BasicDBObject("$in", 
										Arrays.asList(ActionType.PERSONAL_PROFILE_COMMENT.toString(), 
												ActionType.PERSONAL_PROFILE_REPLY.toString()))
									)
								.get()
						))
			.add("type", new BasicDBObject("$in", actionTypes))
//			.add("uid", new BasicDBObject("$ne", currentTalkerRef))
//			.add("$or", Arrays.asList(
//						user shouldn't see personal actions in the ConvoFeed - only Started Question/Talk
//						new BasicDBObject("uid", new BasicDBObject("$ne", currentTalkerRef))
//						new BasicDBObject("type", ActionType.START_CONVO.toString()) 
//					))
			.get();

		List<DBObject> activitiesDBList = 
			activitiesColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		Set<Action> activitiesSet = new LinkedHashSet<Action>();
		for (DBObject actionDBObject : activitiesDBList) {
			Action action = actionFromDB(actionDBObject);
			activitiesSet.add(action);
		}
		return activitiesSet;
	}
	
	public static Set<Action> loadCommunityFeed() {
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
		//FIXME: make db paging for community feed
		List<DBObject> activitiesDBList = 
			activitiesColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		Set<Action> activitiesSet = new LinkedHashSet<Action>();
		for (DBObject actionDBObject : activitiesDBList) {
			Action action = actionFromDB(actionDBObject);
			activitiesSet.add(action);
		}
		return activitiesSet;
	}
	
	public static Set<Action> loadLatestByTopic(TopicBean topic) {
		DBCollection activitiesColl = getCollection(ACTIVITIES_COLLECTION);
		
		Set<DBRef> convosDBSet = ConversationDAO.getConversationsByTopic(topic);
		DBObject query = new BasicDBObject("topicId", new BasicDBObject("$in", convosDBSet));
		List<DBObject> activitiesDBList = 
			activitiesColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		Set<Action> activitiesSet = new LinkedHashSet<Action>();
		for (DBObject actionDBObject : activitiesDBList) {
			Action action = actionFromDB(actionDBObject);
			activitiesSet.add(action);
		}
		return activitiesSet;
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
				
			case TOPIC_ADDED:
				return new TopicAddedAction(dbObject);
				
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
	
	public static void deleteActionByProfileComment(CommentBean comment) {
		DBCollection activitiesColl = getCollection(ACTIVITIES_COLLECTION);

		DBRef commentRef = createRef(CommentsDAO.PROFILE_COMMENTS_COLLECTION, comment.getId());
		DBObject query = new BasicDBObject("$or", 
				Arrays.asList(
					new BasicDBObject("profile_comment", commentRef),
					new BasicDBObject("profile_reply", commentRef)
				)
			);
		activitiesColl.remove(query);
	}

	public static void main(String[] args) {
//		System.out.println(ActionDAO.load("4c2cb43160adf3055c97d061"));
	}

}

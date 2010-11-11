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

import play.Logger;

import util.DBUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import logic.FeedsLogic;
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
import models.actions.PreloadAction;
import models.actions.StartConvoAction;
import models.actions.SummaryConvoAction;
import models.actions.TopicAddedAction;
import models.actions.UpdateProfileAction;

import static util.DBUtil.*;

public class ActionDAO {
	
	/*
- Conversation actions that trigger feeds:
- Talk/Question started or restarted in a Topic or Conversation that is being followed
- New answer in a Topic or Conversation that is being followed
- Summary created or edited in Conversation or Topic that is being followed.
- Conversation added to a Topic being followed

- Member actions that trigger feeds:
- Member followed starts/restarts a Talk or asks a Question
- Member followed joins Conversation
- Member followed Answers a question
- Member followed edits or adds a Summary
- Member followed leaves Comment in their own Journal or another member leaves a comment in their journal
- Member followed voted for an answer 
	 */
	
	private static final EnumSet<ActionType> CONVO_FEED_ACTIONS = EnumSet.of(
			//TODO: join convo for following convo?
			ActionType.START_CONVO, ActionType.RESTART_CONVO, ActionType.JOIN_CONVO,
			ActionType.ANSWER_CONVO,
			ActionType.SUMMARY_ADDED, ActionType.SUMMARY_EDITED,
			ActionType.ANSWER_VOTED,
			ActionType.TOPIC_ADDED,
			ActionType.PERSONAL_PROFILE_COMMENT, ActionType.PERSONAL_PROFILE_REPLY
		);
	
//	Community Conversation Feed:
//		- Talk started or Question asked
//		- Question answered
//		- Summary added or edited
//		- Answer received vote
	private static final EnumSet<ActionType> COMMUNITY_CONVO_FEED_ACTIONS = EnumSet.of(
			ActionType.START_CONVO,
			ActionType.ANSWER_CONVO, 
			ActionType.SUMMARY_ADDED, ActionType.SUMMARY_EDITED,
			ActionType.ANSWER_VOTED
		);
	private static final EnumSet<ActionType> TOPIC_FEED_ACTIONS = COMMUNITY_CONVO_FEED_ACTIONS;
	
	public static final String ACTIVITIES_COLLECTION = "activities";
	
	public static List<Action> load(String talkerId) {
		DBCollection activitiesColl = getCollection(ACTIVITIES_COLLECTION);
		
		DBRef talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talkerId);
		DBObject query = new BasicDBObject("uid", talkerRef);
		List<DBObject> activitiesDBList = 
			activitiesColl.find(query).sort(new BasicDBObject("time", -1)).limit(34).toArray();
		
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
	public static List<Action> loadConvoFeed(TalkerBean talker, String nextActionId) {
		DBRef currentTalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talker.getId());
		
		Date firstActionTime = null;
		if (nextActionId != null) {
			firstActionTime = getActionTime(nextActionId);
		}
		
//		Logger.error("After 1:");
		
		//prepare list of followed convos/topics
		Set<DBRef> convosDBSet = new HashSet<DBRef>();
		for (String convoId : talker.getFollowingConvosList()) {
			convosDBSet.add(createRef(ConversationDAO.CONVERSATIONS_COLLECTION, convoId));
		}
		
//		Logger.error("After 2:");
		
//		for (TopicBean topic : talker.getFollowingTopicsList()) {	
//			convosDBSet.addAll(ConversationDAO.getConversationsByTopic(topic));
//		}
		convosDBSet.addAll(ConversationDAO.getConversationsByTopics(talker.getFollowingTopicsList()));
//		System.out.println("2S"+convosDBSet.size());
//		for (DBRef r : convosDBSet) {
//			System.out.println(r);
//		}
		
		
//		Logger.error("After preparing data111:");
		
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
		BasicDBObjectBuilder queryBuilder = BasicDBObjectBuilder.start()
			.add("$or", Arrays.asList(
							BasicDBObjectBuilder.start()
								.add("type", ActionType.START_CONVO.toString())
								.add("uid", currentTalkerRef)
								.get(),
							BasicDBObjectBuilder.start()
								.add("convoId", new BasicDBObject("$in", convosDBSet))
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
			.add("type", new BasicDBObject("$in", actionTypes));
		if (firstActionTime != null) {
			queryBuilder.add("time", new BasicDBObject("$lt", firstActionTime));
		}
		DBObject query = queryBuilder.get();

		List<DBObject> activitiesDBList = 
			activitiesColl.find(query).sort(new BasicDBObject("time", -1)).limit(FeedsLogic.ACTIONS_PRELOAD).toArray();
		
		Logger.error("After request111:");
		
		List<Action> activitiesSet = new ArrayList<Action>();
		for (DBObject actionDBObject : activitiesDBList) {
//			Action action = actionFromDB(actionDBObject);
			Action action = new PreloadAction(actionDBObject);
			activitiesSet.add(action);
		}
		
		Logger.error("After request222:");
		
		return activitiesSet;
	}
	
	public static List<Action> loadCommunityFeed(String nextActionId) {
		//list of needed actions for this Feed
		Set<String> actionTypes = new HashSet<String>();
		for (ActionType actionType : COMMUNITY_CONVO_FEED_ACTIONS) {
			actionTypes.add(actionType.toString());
		}
		
		Date firstActionTime = null;
		if (nextActionId != null) {
			firstActionTime = getActionTime(nextActionId);
		}
		
		//load actions for this criterias
		DBCollection activitiesColl = getCollection(ACTIVITIES_COLLECTION);
		
		BasicDBObjectBuilder queryBuilder = 
			BasicDBObjectBuilder.start()
				.add("type", new BasicDBObject("$in", actionTypes));
		if (firstActionTime != null) {
			queryBuilder.add("time", new BasicDBObject("$lt", firstActionTime));
		}
		DBObject query = queryBuilder.get();
			
//		System.out.println("SSS2"+FeedsLogic.ACTIONS_PRELOAD);
		List<DBObject> activitiesDBList = 
			activitiesColl.find(query).sort(new BasicDBObject("time", -1)).limit(FeedsLogic.ACTIONS_PRELOAD).toArray();
		
		List<Action> activitiesSet = new ArrayList<Action>();
		for (DBObject actionDBObject : activitiesDBList) {
//			Action action = actionFromDB(actionDBObject);
			Action action = new PreloadAction(actionDBObject);
			activitiesSet.add(action);
		}
//		System.out.println("RES: "+nextActionId+", "+activitiesSet.size());
		return activitiesSet;
	}
	
	public static List<Action> loadLatestByTopic(TopicBean topic, String nextActionId) {
		DBCollection activitiesColl = getCollection(ACTIVITIES_COLLECTION);
		
		Date firstActionTime = null;
		if (nextActionId != null) {
			firstActionTime = getActionTime(nextActionId);
		}
		
		//list of needed actions for this Feed
		Set<String> actionTypes = new HashSet<String>();
		for (ActionType actionType : TOPIC_FEED_ACTIONS) {
			actionTypes.add(actionType.toString());
		}
		Set<DBRef> convosDBSet = ConversationDAO.getConversationsByTopics(Arrays.asList(topic));
		
		BasicDBObjectBuilder queryBuilder = 
			BasicDBObjectBuilder.start()
				.add("type", new BasicDBObject("$in", actionTypes))
				.add("convoId", new BasicDBObject("$in", convosDBSet));
		if (firstActionTime != null) {
			queryBuilder.add("time", new BasicDBObject("$lt", firstActionTime));
		}
		DBObject query = queryBuilder.get();
		
		List<DBObject> activitiesDBList = 
			activitiesColl.find(query).sort(new BasicDBObject("time", -1)).limit(FeedsLogic.ACTIONS_PRELOAD).toArray();
		
		List<Action> activitiesSet = new ArrayList<Action>();
		for (DBObject actionDBObject : activitiesDBList) {
//			Action action = actionFromDB(actionDBObject);
			Action action = new PreloadAction(actionDBObject);
			activitiesSet.add(action);
		}
		return activitiesSet;
	}
	
	public static List<Action> loadLatestByConversation(ConversationBean convo) {
		DBCollection activitiesColl = getCollection(ACTIVITIES_COLLECTION);
		
		DBRef convoRef = createRef(ConversationDAO.CONVERSATIONS_COLLECTION, convo.getId());
		DBObject query = new BasicDBObject("convoId", convoRef);
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
	
	public static Action actionFromDB(DBObject dbObject) {
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
	
	public static Date getActionTime(String actionId) {
		DBCollection activitiesColl = getCollection(ACTIVITIES_COLLECTION);

		DBObject query = new BasicDBObject("_id", new ObjectId(actionId));
		DBObject actionDBObject = activitiesColl.findOne(query);
		if (actionDBObject != null) {
			return (Date)actionDBObject.get("time");
		}
		return null;
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
	
	public static void updateProfileCommentAction(String commentId) {
		DBCollection activitiesColl = getCollection(ACTIVITIES_COLLECTION);
		
		DBRef commentRef = createRef(CommentsDAO.PROFILE_COMMENTS_COLLECTION, commentId);
		DBObject query = BasicDBObjectBuilder.start()
			.add("type", ActionType.PERSONAL_PROFILE_COMMENT.toString())
			.add("profile_comment", commentRef)
			.get();
		
		activitiesColl.update(query,
				new BasicDBObject("$set", new BasicDBObject("time", new Date())));
	}
	
	public static void deleteActionsByAnswer(CommentBean answer) {
		DBCollection activitiesColl = getCollection(ACTIVITIES_COLLECTION);

		DBRef answerRef = createRef(CommentsDAO.CONVO_COMMENTS_COLLECTION, answer.getId());
		DBObject query = new BasicDBObject("$or", 
				Arrays.asList(
					new BasicDBObject("answer", answerRef),
					new BasicDBObject("reply", answerRef)
				)
			);
		activitiesColl.remove(query);
	}

	public static void deleteActionsByConvo(ConversationBean convo) {
		DBCollection activitiesColl = getCollection(ACTIVITIES_COLLECTION);

		DBRef convoRef = createRef(ConversationDAO.CONVERSATIONS_COLLECTION, convo.getId());
		DBObject query = new BasicDBObject("convoId", convoRef);
		activitiesColl.remove(query);
	}

}

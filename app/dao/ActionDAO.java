package dao;

import java.util.ArrayList;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
import models.actions.FollowConvoAction;
import models.actions.FollowTalkerAction;
import models.actions.GiveThanksAction;
import models.actions.JoinConvoAction;
import models.actions.ProfileCommentAction;
import models.actions.ProfileReplyAction;
import models.actions.StartConvoAction;
import models.actions.UpdateProfileAction;

import static util.DBUtil.*;

public class ActionDAO {
	
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
	
	public static List<Action> loadLatestByTopic(TopicBean topic) {
		DBCollection activitiesColl = getCollection(ACTIVITIES_COLLECTION);
		
		List<DBRef> convosDBList = new ArrayList<DBRef>();
		for (ConversationBean convo : topic.getConversations()) {
			convosDBList.add(createRef(ConversationDAO.CONVERSATIONS_COLLECTION, convo.getId()));
		}
		DBObject query = new BasicDBObject("topicId", new BasicDBObject("$in", convosDBList));
		List<DBObject> activitiesDBList = 
			activitiesColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		List<Action> activitiesList = new ArrayList<Action>();
		for (DBObject actionDBObject : activitiesDBList) {
			Action action = actionFromDB(actionDBObject);
			activitiesList.add(action);
		}
		return activitiesList;
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
			activitiesList.add(action);
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
				return new StartConvoAction(dbObject);
			case JOIN_CONVO:
				return new JoinConvoAction(dbObject);
			case GIVE_THANKS:
				return new GiveThanksAction(dbObject);
			case FOLLOW_CONVO:
				return new FollowConvoAction(dbObject);
			case FOLLOW_TALKER:
				return new FollowTalkerAction(dbObject);
			case PROFILE_COMMENT:
				return new ProfileCommentAction(dbObject);
			case PROFILE_REPLY:
				return new ProfileReplyAction(dbObject);
			case UPDATE_BIO:
			case UPDATE_HEALTH:
			case UPDATE_PERSONAL:
				return new UpdateProfileAction(dbObject);
			case ANSWER_CONVO:
				return new AnswerConvoAction(dbObject);
		}
		
		return null;
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

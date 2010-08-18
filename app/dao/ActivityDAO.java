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

import models.ActivityBean;
import models.TalkerBean;
import models.TopicBean;
import models.actions.AbstractAction;
import models.actions.Action;
import models.actions.AnswerConvoAction;
import models.actions.FollowConvoAction;
import models.actions.FollowTalkerAction;
import models.actions.GiveThanksAction;
import models.actions.JoinConvoAction;
import models.actions.ProfileCommentAction;
import models.actions.ProfileReplyAction;
import models.actions.StartConvoAction;
import models.actions.UpdateProfileAction;

public class ActivityDAO {
	
	public static final String ACTIVITIES_COLLECTION = "activities";
	
	public static void save(ActivityBean activity) {
		DBCollection activitiesColl = DBUtil.getCollection(ACTIVITIES_COLLECTION);

		DBRef talkerRef = new DBRef(DBUtil.getDB(), 
				TalkerDAO.TALKERS_COLLECTION, activity.getTalker().getId());
		DBObject activityDBObject = BasicDBObjectBuilder.start()
				.add("uid", talkerRef)
				.add("time", new Date())
				.add("text", activity.getText())
				.get();

		activitiesColl.save(activityDBObject);
	}
	
	public static void createActivity(String talkerId, String text) {
		ActivityBean activity = new ActivityBean();
		activity.setTalker(new TalkerBean(talkerId));
		activity.setText(text);
		ActivityDAO.save(activity);
	}
	
	public static List<Action> load(String talkerId) {
		DBCollection activitiesColl = DBUtil.getCollection(ACTIVITIES_COLLECTION);
		
		DBRef talkerRef = new DBRef(DBUtil.getDB(), TalkerDAO.TALKERS_COLLECTION, new ObjectId(talkerId));
		DBObject query = new BasicDBObject("uid", talkerRef);
		List<DBObject> activitiesDBList = 
			activitiesColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		List<Action> activitiesList = new ArrayList<Action>();
		for (DBObject activityDBObject : activitiesDBList) {
			Action action = actionFromDB(activityDBObject);
			activitiesList.add(action);
		}
		return activitiesList;
	}
	
	private static Action actionFromDB(DBObject dbObject) {
		String type = (String)dbObject.get("type");
		
		if (type == null) {
			return null;
		}
		
		if (type.equals("START_CONVO")) {
			return new StartConvoAction(dbObject);
		}
		else if (type.equals("JOIN_CONVO")) {
			return new JoinConvoAction(dbObject);
		}
		else if (type.equals("GIVE_THANKS")) {
			return new GiveThanksAction(dbObject);
		}
		else if (type.equals("FOLLOW_CONVO")) {
			return new FollowConvoAction(dbObject);
		}
		else if (type.equals("FOLLOW_TALKER")) {
			return new FollowTalkerAction(dbObject);
		}
		else if (type.equals("PROFILE_COMMENT")) {
			return new ProfileCommentAction(dbObject);
		}
		else if (type.equals("PROFILE_REPLY")) {
			return new ProfileReplyAction(dbObject);
		}
		else if (type.startsWith("UPDATE_")) {
			return new UpdateProfileAction(dbObject);
		}
		else if (type.equals("ANSWER_CONVO")) {
			return new AnswerConvoAction(dbObject);
		}
		else if (type.equals("ANSWER_CONVO")) {
			return new AnswerConvoAction(dbObject);
		}
		else if (type.equals("ANSWER_CONVO")) {
			return new AnswerConvoAction(dbObject);
		}
		else if (type.equals("ANSWER_CONVO")) {
			return new AnswerConvoAction(dbObject);
		}
		else if (type.equals("ANSWER_CONVO")) {
			return new AnswerConvoAction(dbObject);
		}
		
		return null;
	}

	public static void main(String[] args) {
//		ActivityBean activity = new ActivityBean();
//		activity.setTalker(new TalkerBean("4c2cb43160adf3055c97d061"));
//		activity.setText("First activity by kangaroo!!");
//		ActivityDAO.save(activity);
		
		System.out.println(ActivityDAO.load("4c2cb43160adf3055c97d061"));
	}

	public static void saveActivity(Action action) {
		DBCollection activitiesColl = DBUtil.getCollection(ACTIVITIES_COLLECTION);

		DBObject activityDBObject = action.toDBObject();
		activitiesColl.save(activityDBObject);
	}

}

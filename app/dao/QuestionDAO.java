package dao;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import models.NotificationBean;
import models.TalkerBean;
import models.actions.Action.ActionType;

import org.bson.types.ObjectId;

import util.DBUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import static util.DBUtil.*;

public class QuestionDAO {
	
	public static final String QUESTION_COLLECTION = "question";
	public static final String TYPE = "QUESTION";
	
	public static void saveQuestionNotification(String talkerId, String convoId) {
		DBCollection notificationsColl = getCollection(QUESTION_COLLECTION);
		
		DBRef talkerRef = new DBRef(getDB(), "talkers", new ObjectId(talkerId));
		DBRef convoRef = new DBRef(getDB(), "convos", new ObjectId(convoId));
		DBObject notificationDBObject = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("convoid", convoRef)
			.add("type", TYPE)
			.add("flag", "false")
			.add("time", new Date())
			.get();
		notificationsColl.save(notificationDBObject);
	}
	
	public static List<NotificationBean> loadAllQuestions() {
		DBCollection notificationsColl = getCollection(QUESTION_COLLECTION);
		DBObject query = BasicDBObjectBuilder.start().add("flag", "false").get();
		List<DBObject> topicsDBList = notificationsColl.find(query).toArray();
		List<NotificationBean> list = new ArrayList<NotificationBean>();
		for (DBObject questionDBObject : topicsDBList) {
			NotificationBean notificationBean = new NotificationBean();
			notificationBean.parseDBObject(questionDBObject);
			list.add(notificationBean);
		}
		return list;
	}
	
	public static NotificationBean loadNotification(String id){
		DBCollection notificationsColl = getCollection(QUESTION_COLLECTION);
		NotificationBean notificationBean = new NotificationBean();
		DBObject dbObject = notificationsColl.findOne(new BasicDBObject("_id", new ObjectId(id)));
		notificationBean.parseDBObject(dbObject);
		return notificationBean;
	}
	
	public static boolean updateNotification(NotificationBean notificationBean){
		boolean updateFlag = true;
		DBCollection notificationsColl = getCollection(QUESTION_COLLECTION);
		DBObject query = new BasicDBObject("_id", new ObjectId(notificationBean.getId()));
		notificationsColl.update(query,new BasicDBObject("$set", new BasicDBObject("flag", "true")), false, true);
		return updateFlag; 
	}
}

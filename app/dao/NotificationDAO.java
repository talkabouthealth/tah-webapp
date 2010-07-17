package dao;

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

public class NotificationDAO {
	
	public static final String NOTIFICATIONS_COLLECTION = "notifications";
	
	/**
	 * Returns number of notifications for last 24 hours
	 */
	public static int getNumOfNotifications(String talkerId) {
		DBCollection notificationsColl = DBUtil.getDB().getCollection(NOTIFICATIONS_COLLECTION);
		
		Calendar oneDayBeforeDate = Calendar.getInstance();
		oneDayBeforeDate.add(Calendar.DAY_OF_MONTH, -1);
		
		DBRef talkerRef = new DBRef(DBUtil.getDB(), 
				TalkerDAO.TALKERS_COLLECTION, new ObjectId(talkerId));
		DBObject query = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("time", new BasicDBObject("$gt", oneDayBeforeDate.getTime()))
			.get();
		
		return notificationsColl.find(query).count();
	}
	
	public static Date getLatestNotification(String talkerId) {
		DBCollection notificationsColl = DBUtil.getDB().getCollection(NOTIFICATIONS_COLLECTION);
		
		DBRef talkerRef = new DBRef(DBUtil.getDB(), 
				TalkerDAO.TALKERS_COLLECTION, new ObjectId(talkerId));
		DBObject query = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.get();
		
		List<DBObject> notificationsDBList =
			notificationsColl.find(query).sort(new BasicDBObject("time", -1)).limit(1).toArray();
		if (!notificationsDBList.isEmpty()) {
			return (Date)notificationsDBList.get(0).get("time");
		}
		else {
			return null;
		}
	}
	
	public static int getNotiNumByTopic(String topicId) {
		DBCollection notificationsColl = DBUtil.getDB().getCollection(NOTIFICATIONS_COLLECTION);
		
		DBRef topicRef = new DBRef(DBUtil.getDB(), TopicDAO.TOPICS_COLLECTION, new ObjectId(topicId));
		DBObject query = BasicDBObjectBuilder.start()
			.add("topic_id", topicRef)
			.get();
		
		int numOfNotifications = notificationsColl.find(query).count();
		return numOfNotifications;
	}

}

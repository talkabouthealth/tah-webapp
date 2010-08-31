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

import static util.DBUtil.*;

public class NotificationDAO {
	
	public static final String NOTIFICATIONS_COLLECTION = "notifications";
	
	/**
	 * Returns number of notifications for last 24 hours
	 */
	public static int numOfNotificationsForDay(String talkerId) {
		Calendar oneDayBeforeNow = Calendar.getInstance();
		oneDayBeforeNow.add(Calendar.DAY_OF_MONTH, -1);
		
		return numOfNotificationsForTime(talkerId, oneDayBeforeNow);
	}
	
	/**
	 * Returns number of notifications for talker after given time
	 */
	public static int numOfNotificationsForTime(String talkerId, Calendar time) {
		DBCollection notificationsColl = getCollection(NOTIFICATIONS_COLLECTION);
		
		DBRef talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talkerId);
		DBObject query = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("time", new BasicDBObject("$gt", time.getTime()))
			.get();
		
		return notificationsColl.find(query).count();
	}
	
	public static Date getLatestNotification(String talkerId) {
		DBCollection notificationsColl = getCollection(NOTIFICATIONS_COLLECTION);
		
		DBRef talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talkerId);
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
		DBCollection notificationsColl = getCollection(NOTIFICATIONS_COLLECTION);
		
		DBRef topicRef = createRef(ConversationDAO.CONVERSATIONS_COLLECTION, topicId);
		DBObject query = BasicDBObjectBuilder.start()
			.add("topic_id", topicRef)
			.get();
		
		return notificationsColl.find(query).count();
	}

}

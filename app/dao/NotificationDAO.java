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
	 * 
	 * @param type 'TWITTER', 'FACEBOOK' or null
	 */
	public static int numOfNotificationsForDay(String talkerId, String type) {
		Calendar oneDayBeforeNow = Calendar.getInstance();
		oneDayBeforeNow.add(Calendar.DAY_OF_MONTH, -1);
		
		return numOfNotificationsForTime(talkerId, oneDayBeforeNow, type);
	}
	
	/**
	 * Returns number of notifications for talker after given time
	 */
	public static int numOfNotificationsForTime(String talkerId, Calendar time, String type) {
		DBCollection notificationsColl = getCollection(NOTIFICATIONS_COLLECTION);
		
		DBRef talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talkerId);
		BasicDBObjectBuilder queryBuilder = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("time", new BasicDBObject("$gt", time.getTime()));
		//no type or given type
		if (type == null) {
			queryBuilder.add("type", new BasicDBObject("$exists", false));
		}
		else {
			queryBuilder.add("type", type);
		}
		
		DBObject query = queryBuilder.get();
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
	
	/**
	 * Get number of notifications for given conversation
	 */
	public static int getNotiNumByConvo(String convoId) {
		DBCollection notificationsColl = getCollection(NOTIFICATIONS_COLLECTION);
		
		DBRef convoRef = createRef(ConversationDAO.CONVERSATIONS_COLLECTION, convoId);
		DBObject query = BasicDBObjectBuilder.start()
			.add("convoId", convoRef)
			.get();
		
		return notificationsColl.find(query).count();
	}
	
	public static void saveTwitterNotification(String talkerId, String convoId) {
		DBCollection notificationsColl = getCollection(NOTIFICATIONS_COLLECTION);
		
		DBRef talkerRef = new DBRef(getDB(), "talkers", new ObjectId(talkerId));
		DBRef convoRef = new DBRef(getDB(), "convos", new ObjectId(convoId));
		DBObject notificationDBObject = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("convoId", convoRef)
			.add("type", "TWITTER")
			.add("time", new Date())
			.get();
		
		notificationsColl.save(notificationDBObject);
	}

}

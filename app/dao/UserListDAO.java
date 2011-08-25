package dao;

import static util.DBUtil.getCollection;
import models.NotificationBean;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class UserListDAO {

	public static final String Talkers_COLLECTION = "talkers";
	
	public static void updatePassword(String id, String password){
		DBCollection notificationsColl = getCollection(Talkers_COLLECTION);
		DBObject query = new BasicDBObject("_id", new ObjectId(id));
		notificationsColl.update(query,new BasicDBObject("$set", new BasicDBObject("pass", password)), false, true);
	}
}

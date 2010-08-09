package dao;

import java.util.Date;

import org.bson.types.ObjectId;

import util.DBUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

public class ApplicationDAO {
	
	public static final String LOGIN_HISTORY_COLLECTION = "logins";
	public static final String UPDATES_EMAIL_COLLECTION = "emails";
	
	public static void saveLogin(String talkerId, Date loginTime) {
		DBCollection loginsColl = DBUtil.getCollection(LOGIN_HISTORY_COLLECTION);
		
		DBRef talkerRef = new DBRef(DBUtil.getDB(), 
				TalkerDAO.TALKERS_COLLECTION, new ObjectId(talkerId));
		DBObject loginHistoryObject = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("log_time", loginTime)
			.get();

		loginsColl.save(loginHistoryObject);
	}
	
	public static void saveEmail(String email) {
		DBCollection emailsColl = DBUtil.getCollection(UPDATES_EMAIL_COLLECTION);
		
		//TODO: same email?
		emailsColl.save(new BasicDBObject("email", email));
	}

}


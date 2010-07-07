package dao;

import java.util.Date;

import org.bson.types.ObjectId;

import util.DBUtil;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

public class LoginHistoryDAO {
	
	public static final String LOGIN_HISTORY_COLLECTION = "logins";
	
	public static void save(String talkerId, Date loginTime) {
		DBCollection loginsColl = DBUtil.getCollection(LOGIN_HISTORY_COLLECTION);
		
		DBRef talkerRef = new DBRef(DBUtil.getDB(), 
				TalkerDAO.TALKERS_COLLECTION, new ObjectId(talkerId));
		DBObject loginHistoryObject = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("log_time", loginTime)
			.get();

		loginsColl.save(loginHistoryObject);
	}

}


package dao;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import models.TalkerBean;

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
	
	//Latest users to log in -  for one day
	public static Set<TalkerBean> getActiveTalkers() {
		DBCollection loginsColl = DBUtil.getCollection(LOGIN_HISTORY_COLLECTION);
		
		Calendar oneDayBeforeNow = Calendar.getInstance();
		oneDayBeforeNow.add(Calendar.DAY_OF_MONTH, -1);
		
		DBObject query = BasicDBObjectBuilder.start()
			.add("log_time", new BasicDBObject("$gt", oneDayBeforeNow.getTime()))
			.get();
		
		List<DBObject> loginsDBList = 
			loginsColl.find(query).sort(new BasicDBObject("log_time", -1)).toArray();
		
		Set<TalkerBean> activeTalkers = new LinkedHashSet<TalkerBean>();
		for (DBObject loginDBObject : loginsDBList) {
			DBObject talkerDBObject = ((DBRef)loginDBObject.get("uid")).fetch();
			
			TalkerBean talker = new TalkerBean();
			//TODO: not parse same talkers?
			talker.parseBasicFromDB(talkerDBObject);
			
			activeTalkers.add(talker);
		}
		
		return activeTalkers;
	}
	
	//Latest users to signup -  for one day
	public static Set<TalkerBean> getNewTalkers() {
		DBCollection loginsColl = DBUtil.getCollection(TalkerDAO.TALKERS_COLLECTION);
		
		Calendar oneDayBeforeNow = Calendar.getInstance();
		oneDayBeforeNow.add(Calendar.DAY_OF_MONTH, -1);
		
		DBObject query = BasicDBObjectBuilder.start()
			.add("timestamp", new BasicDBObject("$gt", oneDayBeforeNow.getTime()))
			.get();
		
		List<DBObject> talkersDBList = 
			loginsColl.find(query).sort(new BasicDBObject("timestamp", -1)).toArray();
		
		Set<TalkerBean> activeTalkers = new LinkedHashSet<TalkerBean>();
		for (DBObject talkerDBObject : talkersDBList) {
			TalkerBean talker = new TalkerBean();
			//TODO: not parse same talkers?
			talker.parseBasicFromDB(talkerDBObject);
			
			activeTalkers.add(talker);
		}
		
		return activeTalkers;
	}

}


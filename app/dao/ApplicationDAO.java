package dao;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import models.TalkerBean;

import org.bson.types.ObjectId;

import play.templates.JavaExtensions;

import util.DBUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import controllers.Application;

import static util.DBUtil.*;

public class ApplicationDAO {
	
	public static final String LOGIN_HISTORY_COLLECTION = "logins";
	public static final String UPDATES_EMAIL_COLLECTION = "emails";
	public static final String NAMES_COLLECTION = "names";
	
	public static void saveLogin(String talkerId) {
		DBCollection loginsColl = getCollection(LOGIN_HISTORY_COLLECTION);
		
		DBRef talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talkerId);
		DBObject loginHistoryObject = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("log_time", new Date())
			.get();

		loginsColl.save(loginHistoryObject);
	}
	
	//save email for updates & notifications
	public static void saveEmail(String email) {
		DBCollection emailsColl = getCollection(UPDATES_EMAIL_COLLECTION);
		emailsColl.save(new BasicDBObject("email", email));
	}
	
	//Latest users to log in - for one day
	public static Set<TalkerBean> getActiveTalkers() {
		DBCollection loginsColl = getCollection(LOGIN_HISTORY_COLLECTION);
		
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
			talker.setId(getString(talkerDBObject, "_id"));
			
			if (!activeTalkers.contains(talker)) {
				talker.parseBasicFromDB(talkerDBObject);
				activeTalkers.add(talker);
			}
		}
		
		return activeTalkers;
	}
	
	//Latest users to signup -  for one day
	public static Set<TalkerBean> getNewTalkers() {
		DBCollection loginsColl = getCollection(TalkerDAO.TALKERS_COLLECTION);
		
		Calendar twoWeeksBeforeNow = Calendar.getInstance();
		twoWeeksBeforeNow.add(Calendar.WEEK_OF_YEAR, -2);
		
		System.out.println(twoWeeksBeforeNow.getTime());
		
		DBObject query = BasicDBObjectBuilder.start()
			.add("timestamp", new BasicDBObject("$gt", twoWeeksBeforeNow.getTime()))
			.get();
		
		List<DBObject> talkersDBList = 
			loginsColl.find(query).sort(new BasicDBObject("timestamp", -1)).toArray();
		
		Set<TalkerBean> activeTalkers = new LinkedHashSet<TalkerBean>();
		for (DBObject talkerDBObject : talkersDBList) {
			TalkerBean talker = new TalkerBean();
			talker.setId(getString(talkerDBObject, "_id"));
			
			if (!activeTalkers.contains(talker)) {
				talker.parseBasicFromDB(talkerDBObject);
				activeTalkers.add(talker);
			}
		}
		
		return activeTalkers;
	}
	
	public static boolean isURLNameExists(String name) {
		if (Application.RESERVED_WORDS.contains(name)) {
			return true;
		}
		
		DBCollection namesColl = getCollection(NAMES_COLLECTION);
		
		DBObject query = new BasicDBObject("name", name);
		return namesColl.findOne(query) != null;
	}
	
	public static String getURLName(String name) {
		name = prepareName(name);
		
		DBCollection namesColl = getCollection(NAMES_COLLECTION);
		
		DBObject query = new BasicDBObject("name", name);
		DBObject fields = new BasicDBObject("cnt", 1);
		DBObject update = BasicDBObjectBuilder.start()
			.add("$inc", new BasicDBObject("cnt", 1))
			.get();
		
		//Mongo bug: new items aren't added with "returnNew" == false
		DBObject nameDBObject = 
			namesColl.findAndModify(query, fields, null, false, update, true, true);
		
		Integer cnt = (Integer)nameDBObject.get("cnt");
		if (cnt == 1) {
			//if new URL name is a reserved path - create other
			if (Application.RESERVED_WORDS.contains(name)) {
				return getURLName(name);
			}
			return name;
		}
		else {
			//duplicate name
			return name+"_"+(cnt-1);
		}
	}
	
	/**
	 * Make URL-like name (without not-allowed symbols).
	 * Also replaces "_" as it's used for duplicate names.
	 * Build upon JavaExtensions.slugify() method.
	 */
	private static String prepareName(String name) {
		name = JavaExtensions.noAccents(name);
        return name.replaceAll("[\\W_]", "-").replaceAll("-{2,}", "-").replaceAll("-$", "").toLowerCase();
	}
	
	public static void main(String[] args) {
//		System.out.println(ApplicationDAO.getURLName(JavaExtensions.slugify("hello_world")));
//		System.out.println(prepareName("_cool test topic___with unusual _s_symbols!!_"));
	}

}


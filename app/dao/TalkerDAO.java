package dao;

import java.util.Calendar;

import models.TalkerBean;

import org.bson.types.ObjectId;

import util.DBUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class TalkerDAO {
	
	public static final String TALKERS_COLLECTION = "talkers";
	
	public static boolean save(TalkerBean talker) {
		DBCollection talkersColl = DBUtil.getCollection("talkers");

		DBObject talkerDBObject = BasicDBObjectBuilder.start()
				.add("uname", talker.getUserName())
				.add("pass", talker.getPassword())
				.add("email", talker.getEmail())
				.add("dob", talker.getDob())
				.add("gender", talker.getGender())
				.add("timestamp",  Calendar.getInstance().getTime())
				.add("im", talker.getIm())
				.add("im_uname", talker.getImUsername())
				.add("newsletter", talker.isNewsletter())
				.add("act_type", talker.getAccountType())
				.add("act_id", talker.getAccountId())
				.add("invites", 100)
				.get();

		talkersColl.save(talkerDBObject);
		return true;
	}
	
	public static void updateTalker(TalkerBean talker) {
		DBCollection talkersColl = DBUtil.getCollection(TALKERS_COLLECTION);
		
		DBObject talkerObject = BasicDBObjectBuilder.start()
			.add("uname", talker.getUserName())
			.add("pass", talker.getPassword())
			.add("email", talker.getEmail())
			.add("dob", talker.getDob())
			.add("gender", talker.getGender())
			.add("mar_status", talker.getMariStat())
			.add("city", talker.getCity())
			.add("state", talker.getState())
			.add("country", talker.getCountry())
			.add("category", talker.getCategory())
			.add("ch_num", talker.getChildrenNum())
			.add("invites", talker.getInvitations())
			.add("prefs", talker.profilePreferencesToInt())
			.add("im", talker.getIm())
			.add("im_uname", talker.getImUsername())
			.add("img", talker.getImagePath())
			.add("nfreq", talker.getNfreq())
			.add("ntime", talker.getNtime())
			.add("ctype", talker.getCtype())
			.get();
		
		DBObject talkerId = new BasicDBObject("_id", new ObjectId(talker.getId()));
		//"$set" is used for updating fields
		talkersColl.update(talkerId, new BasicDBObject("$set", talkerObject));
	}
	
	public static TalkerBean getByUserName(String userName) {
		return getByField("uname", userName);
	}
	
	public static TalkerBean getByEmail(String email) {
		return getByField("email", email);
	}
	
	/**
	 * For now, there will only be the breast cancer community,
	 * so this method returns total number of users (talkers) in the DB.
	 */
	public static long getNumberOfTalkers() {
		DBCollection talkersColl = DBUtil.getCollection(TALKERS_COLLECTION);
		return talkersColl.count();
	}
	
	public static TalkerBean getTalkerByLoginInfo(String userName, String password) {
		DBCollection talkersColl = DBUtil.getCollection(TALKERS_COLLECTION);
		
		DBObject query = new BasicDBObject();
		query.put("uname", userName);
		query.put("pass", password);
		
		DBObject talkerDBObject = talkersColl.findOne(query);
		
		TalkerBean talker = null;
		if (talkerDBObject != null) {
			talker = new TalkerBean();
			talker.parseFromDB(talkerDBObject);
		}
		return talker;
	}
	
	public static TalkerBean getTalkerByAccount(String accountType, String accountId) {
		DBCollection talkersColl = DBUtil.getCollection(TALKERS_COLLECTION);
		
		DBObject query = new BasicDBObject();
		query.put("act_type", accountType);
		query.put("act_id", accountId);
		
		DBObject talkerDBObject = talkersColl.findOne(query);
		if (talkerDBObject == null) {
			return null;
		}
		else {
			TalkerBean talker = new TalkerBean();
			talker.parseFromDB(talkerDBObject);
			return talker;
		}
	}
	
	/*
	 * Loads talker bean by custom field
	 */
	private static TalkerBean getByField(String fieldName, Object fieldValue) {
		DBCollection talkersColl = DBUtil.getCollection(TALKERS_COLLECTION);
		
		DBObject query = new BasicDBObject(fieldName, fieldValue);
		DBObject talkerDBObject = talkersColl.findOne(query);
		
		if (talkerDBObject == null) {
			return null;
		}
		else {
			TalkerBean talker = new TalkerBean();
			talker.parseFromDB(talkerDBObject);
			return talker;
		}
	}

}


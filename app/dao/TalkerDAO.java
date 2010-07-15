package dao;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import models.TalkerBean;
import models.TalkerDiseaseBean;
import models.ThankYouBean;

import org.bson.types.ObjectId;

import util.DBUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

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
			.add("mar_status", talker.getMaritalStatus())
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
	
	
	/* --------------------- "Thank you" feature ----------------------------- */
	public static void saveThankYou(ThankYouBean thankYouBean) {
		DBCollection talkersColl = DBUtil.getCollection(TALKERS_COLLECTION);
		
		DBRef fromTalkerRef = new DBRef(DBUtil.getDB(), TALKERS_COLLECTION, new ObjectId(thankYouBean.getFrom()));
		DBObject thankYouObject = BasicDBObjectBuilder.start()
			.add("time", thankYouBean.getTime())
			.add("note", thankYouBean.getNote())
			.add("from", fromTalkerRef)
			.get();
		
		DBObject talkerId = new BasicDBObject("_id", new ObjectId(thankYouBean.getTo()));
		//For creating/adding to array: { $push : { field : value } }
		talkersColl.update(talkerId, new BasicDBObject("$push", new BasicDBObject("thankyous", thankYouObject)));
	}
	
	/* ---------------------- Following/Followers feature --------------------------- */
	
	/**
	 * Follows or unfollows depending on third parameter
	 */
	public static void followAction(String followerId, String followingId, boolean follow) {
		DBCollection talkersColl = DBUtil.getCollection(TALKERS_COLLECTION);
		
		DBRef followingTalkerRef = new DBRef(DBUtil.getDB(), 
				TALKERS_COLLECTION, new ObjectId(followingId));
		DBObject talkerId = new BasicDBObject("_id", new ObjectId(followerId));
		
		String action = null;
		if (follow) {
			//add to array
			action = "$push";
		}
		else {
			//remove from array
			action = "$pull";
		}
		DBObject updateQuery = new BasicDBObject(action, new BasicDBObject("following", followingTalkerRef));
		talkersColl.update(talkerId, updateQuery);
	}
	
	public static List<TalkerBean> loadFollowers(String talkerId) {
		DBCollection talkersColl = DBUtil.getCollection(TALKERS_COLLECTION);

		DBRef followingTalkerRef = new DBRef(DBUtil.getDB(), 
				TALKERS_COLLECTION, new ObjectId(talkerId));
		BasicDBObject query = new BasicDBObject("following", followingTalkerRef);
		List<DBObject> followerDBList = talkersColl.find(query).toArray();
		
		List<TalkerBean> followerList = new ArrayList<TalkerBean>();
		for (DBObject followerDBObject : followerDBList) {
			//TODO: same as following?
			TalkerBean followerTalker = new TalkerBean();
			followerTalker.setId(followerDBObject.get("_id").toString());
			followerTalker.setUserName(followerDBObject.get("uname").toString());
			followerTalker.setImagePath(followerDBObject.get("img").toString());
			
			followerList.add(followerTalker);
		}
		
		return followerList;
	}
	
	public static void main(String[] args) {
//		TalkerDAO.follow("4c2cb43160adf3055c97d061", "4c35dbeb5165f305eebfc5f2", true);
		
		System.out.println(loadFollowers("4c35dbeb5165f305eebfc5f2"));
	}

}


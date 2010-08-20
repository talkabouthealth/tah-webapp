package dao;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.CommentBean;
import models.EmailBean;
import models.TalkerBean;
import models.ThankYouBean;
import models.TopicBean;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.cn.ChineseTokenizer;
import org.bson.types.ObjectId;

import util.DBUtil;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import static util.DBUtil.*;

public class TalkerDAO {
	
	public static final String TALKERS_COLLECTION = "talkers";
	
	public static boolean save(TalkerBean talker) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject talkerDBObject = BasicDBObjectBuilder.start()
				.add("uname", talker.getUserName())
				.add("pass", talker.getPassword())
				.add("email", talker.getEmail())
				.add("verify_code", talker.getVerifyCode())
				
				.add("dob", talker.getDob())
				.add("timestamp",  Calendar.getInstance().getTime())
				.add("im", talker.getIm())
				.add("im_uname", talker.getImUsername())
				
				.add("connection", talker.getConnection())
				.add("connection_verified", talker.isConnectionVerified())
				
				.add("newsletter", talker.isNewsletter())
				.add("act_type", talker.getAccountType())
				.add("act_id", talker.getAccountId())
				.add("invites", talker.getInvitations())
				
				.add("prefs", talker.profilePreferencesToInt())
				.add("email_settings", talker.emailSettingsToList())
				
				.add("nfreq", talker.getNfreq())
				.add("ntime", talker.getNtime())
				.add("ctype", talker.getCtype())
				.get();

		talkersColl.save(talkerDBObject);
		return true;
	}
	
	public static void updateTalker(TalkerBean talker) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject talkerObject = BasicDBObjectBuilder.start()
			.add("uname", talker.getUserName())
			.add("pass", talker.getPassword())
			.add("email", talker.getEmail())
			.add("verify_code", talker.getVerifyCode())
			.add("deactivated", talker.isDeactivated())
			
			.add("connection", talker.getConnection())
			.add("connection_verified", talker.isConnectionVerified())
			
			.add("im", talker.getIm())
			.add("im_uname", talker.getImUsername())
			
			.add("dob", talker.getDob())
			.add("gender", talker.getGender())
			.add("mar_status", talker.getMaritalStatus())
			.add("city", talker.getCity())
			.add("state", talker.getState())
			.add("country", talker.getCountry())
			.add("category", talker.getCategory())
			.add("newsletter", talker.isNewsletter())
			.add("nfreq", talker.getNfreq())
			.add("ntime", talker.getNtime())
			.add("ctype", talker.getCtype())
			.add("firstname", talker.getFirstName())
			.add("lastname", talker.getLastName())
			.add("zip", talker.getZip())
			.add("webpage", talker.getWebpage())
			.add("bio", talker.getBio())
			.add("ch_ages", talker.getChildrenAges())
			.add("ch_num", talker.getChildrenNum())
			.add("invites", talker.getInvitations())
			.add("keywords", talker.getKeywords())
			
			.add("prefs", talker.profilePreferencesToInt())
			.add("email_settings", talker.emailSettingsToList())
			.add("following_topics", talker.getFollowingTopicsList())
			
			.get();
		
		DBObject talkerId = new BasicDBObject("_id", new ObjectId(talker.getId()));
		//"$set" is used for updating fields
		talkersColl.update(talkerId, new BasicDBObject("$set", talkerObject));
	}
	
	public static void updateTalkerImage(TalkerBean talker, byte[] imageArray) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject talkerObject = BasicDBObjectBuilder.start()
			.add("img", imageArray)
			.get();
		
		DBObject talkerId = new BasicDBObject("_id", new ObjectId(talker.getId()));
		//"$set" is used for updating fields
		talkersColl.update(talkerId, new BasicDBObject("$set", talkerObject));
	}
	
	public static TalkerBean getByUserName(String userName) {
		return getByField("uname", userName);
	}
	
	public static TalkerBean getById(String talkerId) {
		return getByField("_id", new ObjectId(talkerId));
	}
	
	public static TalkerBean getByEmail(String email) {
		return getByField("email", email);
	}
	
	public static TalkerBean getByVerifyCode(String verifyCode) {
		TalkerBean talker = getByField("verify_code", verifyCode);
		
		//TODO: later we can use OR in Mongo?
		if (talker == null) {
			//check non-primary emails
			DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
			
			DBObject query = new BasicDBObject("emails.verify_code", verifyCode);
			DBObject talkerDBObject = talkersColl.findOne(query);
			
			if (talkerDBObject == null) {
				return null;
			}
			else {
				talker = new TalkerBean();
				talker.parseFromDB(talkerDBObject);
			}
		}
		
		return talker;
	}
	
	/*
	 * Loads talker bean by custom field
	 */
	private static TalkerBean getByField(String fieldName, Object fieldValue) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
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
	
	/**
	 * For now, there will only be the breast cancer community,
	 * so this method returns total number of users (talkers) in the DB.
	 */
	public static long getNumberOfTalkers() {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		return talkersColl.count();
	}
	
	public static TalkerBean getTalkerByLoginInfo(String usernameOrEmail, String password) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject query = new BasicDBObject();
		query.put("uname", usernameOrEmail);
		query.put("pass", password);
		
		DBObject talkerDBObject = talkersColl.findOne(query);
		
		/*
		 	Login by Username or Email
		 	Current version (1.4.4) doesn't support "$or" query (will be supported in 1.5.3),
		 	so we make second query
		 	TODO: Later use "$or" operator
		*/
		if (talkerDBObject == null) {
			query = new BasicDBObject();
			query.put("email", usernameOrEmail);
			query.put("pass", password);
			
			talkerDBObject = talkersColl.findOne(query);
		}
		
		TalkerBean talker = null;
		if (talkerDBObject != null) {
			talker = new TalkerBean();
			talker.parseFromDB(talkerDBObject);
		}
		return talker;
	}
	
	public static TalkerBean getTalkerByAccount(String accountType, String accountId) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
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
	
	
	public static List<TalkerBean> loadAllTalkers() {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		List<DBObject> talkersDBObjectList = talkersColl.find().toArray();
		
		List<TalkerBean> talkerList = new ArrayList<TalkerBean>();
		for (DBObject talkerDBObject : talkersDBObjectList) {
			TalkerBean talker = new TalkerBean();
			talker.parseFromDB(talkerDBObject);
			talkerList.add(talker);
		}
		
		return talkerList;
	}
	
	public static List<Map<String, String>> loadTalkersForDashboard() {
		DBCollection talkersColl = getDB().getCollection(TALKERS_COLLECTION);
		DateFormat dateFormat = new SimpleDateFormat("MM.dd.yyyy HH:mm:ss");
		
		//TODO: sort by last notification!
		List<DBObject> talkersDBList = talkersColl.find().toArray();
		
		List<Map<String, String>> talkersInfoList = new ArrayList<Map<String,String>>();
		for (DBObject talkerDBObject : talkersDBList) {
			Map<String, String> talkerInfoMap = new HashMap<String, String>();
			
			talkerInfoMap.put("id", talkerDBObject.get("_id").toString());
			talkerInfoMap.put("uname", talkerDBObject.get("uname").toString());
			talkerInfoMap.put("email", talkerDBObject.get("email").toString());
			talkerInfoMap.put("imService", talkerDBObject.get("im").toString());
			talkerInfoMap.put("imUsername", talkerDBObject.get("im_uname").toString());
			
			Date latestNotification = NotificationDAO.getLatestNotification(talkerInfoMap.get("id"));
			if (latestNotification != null) {
				talkerInfoMap.put("latestNotification", dateFormat.format(latestNotification));
			}
			long numOfNotifications = NotificationDAO.numOfNotificationsForDay(talkerInfoMap.get("id"));
			talkerInfoMap.put("numOfNotifications", ""+numOfNotifications);
			
			talkersInfoList.add(talkerInfoMap);
		}
		
		return talkersInfoList;
	}
	
	public static byte[] loadTalkerImage(String userName) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject query = new BasicDBObject("uname", userName);
		DBObject fields = BasicDBObjectBuilder.start()
			.add("img", 1)
			.add("deactivated", 1)
			.get();
		DBObject talkerDBObject = talkersColl.findOne(query, fields);
		
		if (talkerDBObject == null) {
			return null;
		}
		
		if (getBoolean(talkerDBObject, "deactivated")) {
			//Show default image
			return null;
		}
		
		return (byte[])talkerDBObject.get("img");
	}
	
	
	/* --------------------- "Thank you" feature ----------------------------- */
	public static void saveThankYou(ThankYouBean thankYouBean) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBRef fromTalkerRef = createRef(TALKERS_COLLECTION, thankYouBean.getFromTalker().getId());
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
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBRef followingTalkerRef = createRef(TALKERS_COLLECTION, followingId);
		DBObject talkerId = new BasicDBObject("_id", new ObjectId(followerId));
		
		//add or remove to/from array
		String action = follow ? "$push" : "$pull";

		DBObject updateQuery = new BasicDBObject(action, 
				new BasicDBObject("following", followingTalkerRef));
		talkersColl.update(talkerId, updateQuery);
	}
	
	public static List<TalkerBean> loadFollowers(String talkerId) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);

		DBRef followingTalkerRef = createRef(TALKERS_COLLECTION, talkerId);
		BasicDBObject query = new BasicDBObject("following", followingTalkerRef);
		List<DBObject> followerDBList = talkersColl.find(query).toArray();
		
		List<TalkerBean> followerList = new ArrayList<TalkerBean>();
		for (DBObject followerDBObject : followerDBList) {
			TalkerBean followerTalker = new TalkerBean();
			
			boolean isDeactivated = followerTalker.getBoolean(followerDBObject.get("deactivated"));
			if (isDeactivated) {
				continue;
			}
			
			followerTalker.parseBasicFromDB(followerDBObject);
			followerList.add(followerTalker);
		}
		
		return followerList;
	}
	
	public static List<TopicBean> loadFollowingTopics(String talkerId) {
		TalkerBean talker = getById(talkerId);
		
		if (talker == null) {
			return new ArrayList<TopicBean>();
		}
		
		//TODO: use DBRef for topics?
		List<TopicBean> followingTopicsList = new ArrayList<TopicBean>();
		for (String topicId : talker.getFollowingTopicsList()) {
			TopicBean topic = TopicDAO.getByTopicId(topicId);
			followingTopicsList.add(topic);
		}
		
		return followingTopicsList;
	}
	
	public static void saveEmail(TalkerBean talker, EmailBean email) {
		saveOrDeleteEmail(talker, email, "$push");
	}
	
	public static void deleteEmail(TalkerBean talker, EmailBean email) {
		saveOrDeleteEmail(talker, email, "$pull");
	}
	
	private static void saveOrDeleteEmail(TalkerBean talker, EmailBean email, String action) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject emailObject = BasicDBObjectBuilder.start()
			.add("value", email.getValue())
			.get();
		if (email.getVerifyCode() != null) {
			emailObject.put("verify_code", email.getVerifyCode());
		}
		
		DBObject talkerId = new BasicDBObject("_id", new ObjectId(talker.getId()));
		talkersColl.update(talkerId, new BasicDBObject(action, new BasicDBObject("emails", emailObject)));
	}
	
	
	public static void main(String[] args) {
//		TalkerDAO.follow("4c2cb43160adf3055c97d061", "4c35dbeb5165f305eebfc5f2", true);
//		System.out.println(loadFollowers("4c35dbeb5165f305eebfc5f2"));
		
//		TalkerDAO.saveProfileComment("4c35dbeb5165f305eebfc5f2", "4c2cb43160adf3055c97d061", "Teeeext");
//		TalkerDAO.loadProfileComments("4c35dbeb5165f305eebfc5f2");
//		TalkerDAO.saveProfileReply("4c35dbeb5165f305eebfc5f2", "4c35dbeb5165f305eebfc5f2", "Reply2222");
		
		
//		TalkerBean talker = TalkerDAO.getByVerifyCode("e7b279a1-7e41-4be6-8e3e-2bbb8821e57d");
//		System.out.println(talker.getUserName());
		
		System.out.println("cool");
	}
}


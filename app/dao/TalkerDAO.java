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
import models.IMAccountBean;
import models.TalkerBean;
import models.ThankYouBean;
import models.ConversationBean;

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
	
	// --------------------- Save/Update ---------------------------
	
	public static boolean save(TalkerBean talker) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject talkerDBObject = BasicDBObjectBuilder.start()
				.add("uname", talker.getUserName())
				.add("pass", talker.getPassword())
				.add("email", talker.getEmail())
				.add("verify_code", talker.getVerifyCode())
				
				.add("dob", talker.getDob())
				.add("timestamp",  Calendar.getInstance().getTime())
				
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
		
		talker.setId(talkerDBObject.get("_id").toString());
		return true;
	}
	
	public static void updateTalker(TalkerBean talker) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject talkerObject = BasicDBObjectBuilder.start()
			.add("uname", talker.getUserName())
			.add("pass", talker.getPassword())
			.add("email", talker.getEmail())
			.add("verify_code", talker.getVerifyCode())
			.add("emails", setToDB(talker.getEmails()))
			
			.add("orig_uname", talker.getOriginalUserName())
			.add("deactivated", talker.isDeactivated())
			.add("suspended", talker.isSuspended())
			
			.add("connection", talker.getConnection())
			.add("connection_verified", talker.isConnectionVerified())
			
			.add("im_accounts", setToDB(talker.getImAccounts()))
			
			.add("hide_profilehelp", talker.isHideProfileHelp())
			.add("hide_healthhelp", talker.isHideHealthHelp())
			.add("hide_topicmanagehelp", talker.isHideTopicManageHelp())
			
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
			.add("following_convos", talker.getFollowingConvosList())
			
			.add("following_topics", talker.followingTopicsToList())
			.add("topics_info", talker.topicsInfoToDB())
			
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
	
	
	// --------------------- Query ---------------------------
	
	public static TalkerBean getByUserName(String userName) {
		return getByField("uname", userName);
	}
	
	public static TalkerBean getById(String talkerId) {
		return getByField("_id", new ObjectId(talkerId));
	}
	
	public static TalkerBean getByOriginalUsername(String userName) {
		return getByField("orig_uname", userName);
	}
	
	public static TalkerBean getByEmail(String email) {
		TalkerBean talker = getByField("email", email);
		
		if (talker == null) {
			//check non-primary emails
			DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
			
			DBObject query = new BasicDBObject("emails.value", email);
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
	
	public static TalkerBean getByVerifyCode(String verifyCode) {
		TalkerBean talker = getByField("verify_code", verifyCode);
		
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
	
	public static TalkerBean getByLoginInfo(String usernameOrEmail, String password) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject usernameQuery = BasicDBObjectBuilder.start()
			.add("uname", usernameOrEmail)
			.add("pass", password)
			.get();
		DBObject emailQuery = BasicDBObjectBuilder.start()
			.add("email", usernameOrEmail)
			.add("pass", password)
			.get();
		DBObject notPrimaryEmailQuery = BasicDBObjectBuilder.start()
			.add("emails.value", usernameOrEmail)
			.add("pass", password)
			.get();
		DBObject deactivatedUsernameQuery = BasicDBObjectBuilder.start()
			.add("orig_uname", usernameOrEmail)
			.add("pass", password)
			.get();
		
		DBObject query = new BasicDBObject("$or", 
				Arrays.asList(usernameQuery, emailQuery, notPrimaryEmailQuery, deactivatedUsernameQuery)
			);
		
		DBObject talkerDBObject = talkersColl.findOne(query);
		
		TalkerBean talker = null;
		if (talkerDBObject != null) {
			talker = new TalkerBean();
			talker.parseFromDB(talkerDBObject);
		}
		return talker;
	}
	
	public static TalkerBean getByAccount(String accountType, String accountId) {
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
	
	public static TalkerBean getByIMAccount(IMAccountBean imAccount) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject imAccountDBObject = BasicDBObjectBuilder.start()
			.add("uname", imAccount.getUserName())
			.add("service", imAccount.getService())
			.get();
		DBObject query = new BasicDBObject("im_accounts", imAccountDBObject);
		DBObject talkerDBObject = talkersColl.findOne(query);
		
		if (talkerDBObject == null) {
			return null;
		}
		
		TalkerBean talker = new TalkerBean();
		talker.parseFromDB(talkerDBObject);
		return talker;
	}
	
	//Checks userName and original userName (deactivated users)
	public static boolean isUserNameUnique(String userName) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject usernameQuery = BasicDBObjectBuilder.start()
			.add("uname", userName)
			.get();
		DBObject originalUsernameQuery = BasicDBObjectBuilder.start()
			.add("orig_uname", userName)
			.get();
		
		DBObject query = new BasicDBObject("$or", 
				Arrays.asList(usernameQuery, originalUsernameQuery)
			);
		
		DBObject talkerDBObject = talkersColl.findOne(query);
		return (talkerDBObject == null);
	}
	
	
	public static List<TalkerBean> loadAllTalkers() {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		List<DBObject> talkersDBObjectList = 
			talkersColl.find().sort(new BasicDBObject("uname", 1)).toArray();
		
		List<TalkerBean> talkerList = new ArrayList<TalkerBean>();
		for (DBObject talkerDBObject : talkersDBObjectList) {
			TalkerBean talker = new TalkerBean();
			talker.parseFromDB(talkerDBObject);
			talkerList.add(talker);
		}
		
		return talkerList;
	}
	
	
	// --------------------- Other ---------------------------
	
	public static List<TalkerBean> loadTalkersForDashboard() {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		//TODO: sort by last notification!
		List<DBObject> talkersDBList = talkersColl.find().toArray();
		
		List<TalkerBean> talkersList = new ArrayList<TalkerBean>();
		for (DBObject talkerDBObject : talkersDBList) {
			TalkerBean talker = new TalkerBean();
			talker.parseFromDB(talkerDBObject);
			
			Date latestNotification = NotificationDAO.getLatestNotification(talker.getId());
			talker.setLatestNotification(latestNotification);
			
			long numOfNotifications = NotificationDAO.numOfNotificationsForDay(talker.getId());
			talker.setNumOfNotifications(numOfNotifications);
			
			talkersList.add(talker);
		}
		
		return talkersList;
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
	
	/**
	 * For now, there will only be the breast cancer community,
	 * so this method returns total number of users (talkers) in the DB.
	 */
	public static long getNumberOfTalkers() {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		return talkersColl.count();
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
	
	public static List<ConversationBean> loadFollowingConversations(String talkerId) {
		TalkerBean talker = getById(talkerId);
		
		if (talker == null) {
			return new ArrayList<ConversationBean>();
		}
		
		//TODO: use DBRef for convos?
		List<ConversationBean> followingConvoList = new ArrayList<ConversationBean>();
		for (String convoId : talker.getFollowingConvosList()) {
			ConversationBean convo = ConversationDAO.getByConvoId(convoId);
			followingConvoList.add(convo);
		}
		
		return followingConvoList;
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


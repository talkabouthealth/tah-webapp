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
import java.util.Set;
import java.util.regex.Pattern;

import logic.TalkerLogic;
import models.CommentBean;
import models.EmailBean;
import models.IMAccountBean;
import models.PrivacySetting;
import models.PrivacySetting.PrivacyType;
import models.PrivacySetting.PrivacyValue;
import models.ServiceAccountBean.ServiceType;
import models.TalkerBean;
import models.ThankYouBean;
import models.ConversationBean;
import models.actions.Action;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.cn.ChineseTokenizer;
import org.bson.types.ObjectId;

import play.Logger;

import util.DBUtil;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.QueryBuilder;

import static util.DBUtil.*;

public class TalkerDAO {
	
	public static final String TALKERS_COLLECTION = "talkers";
	
	// --------------------- Save/Update ---------------------------
	public static boolean save(TalkerBean talker) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject talkerDBObject = BasicDBObjectBuilder.start()
				.add("uname", talker.getUserName())
				.add("anon_name", talker.getAnonymousName())
				.add("pass", talker.getPassword())
				.add("email", talker.getEmail())
				.add("verify_code", talker.getVerifyCode())
				.add("dob", talker.getDob())
				.add("timestamp",  Calendar.getInstance().getTime())
				
				.add("connection", talker.getConnection())
				.add("connection_verified", talker.isConnectionVerified())
				
				.add("nfreq", talker.getNfreq())
				.add("ntime", talker.getNtime())
				.add("ctype", talker.getCtype())
				.add("ctype_other", talker.getOtherCtype())
				.add("im_notify", talker.isImNotify())
				.add("service_accounts", setToDB(talker.getServiceAccounts()))
				
				.add("privacy_settings", setToDB(talker.getPrivacySettings()))
				.add("email_settings", talker.emailSettingsToList())
				
				.add("hidden_helps", talker.getHiddenHelps())
				
				.add("newsletter", talker.isNewsletter())
				.add("invites", talker.getInvitations())
				.add("ch_num", -1)
				.get();

		talkersColl.save(talkerDBObject);
		
		talker.setId(talkerDBObject.get("_id").toString());
		return true;
	}
	
	public static void updateTalker(TalkerBean talker) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject talkerObject = BasicDBObjectBuilder.start()
			.add("uname", talker.getUserName())
			.add("anon_name", talker.getAnonymousName())
			.add("pass", talker.getPassword())
			.add("email", talker.getEmail())
			.add("verify_code", talker.getVerifyCode())
			.add("emails", setToDB(talker.getEmails()))
			
			.add("orig_uname", talker.getOriginalUserName())
			.add("deactivated", talker.isDeactivated())
			.add("suspended", talker.isSuspended())
			
			.add("connection", talker.getConnection())
			.add("connection_verified", talker.isConnectionVerified())
			.add("prof_info", talker.getProfInfo())
			.add("insurance_accept", talker.getInsuranceAccepted())
			
			.add("im_accounts", setToDB(talker.getImAccounts()))
			.add("im_notify", talker.isImNotify())
			.add("service_accounts", setToDB(talker.getServiceAccounts()))
			
			.add("hidden_helps", talker.getHiddenHelps())
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
			.add("ctype_other", talker.getOtherCtype())
			.add("firstname", talker.getFirstName())
			.add("lastname", talker.getLastName())
			.add("zip", talker.getZip())
			.add("webpage", talker.getWebpage())
			.add("bio", talker.getBio())
			.add("prof_statement", talker.getProfStatement())
			.add("ch_ages", talker.getChildrenAges())
			.add("ch_num", talker.getChildrenNum())
			.add("ethnicities", talker.getEthnicities())
			.add("religion", talker.getReligion())
			.add("religion_serious", talker.getReligionSerious())
			.add("languages", talker.languagesToDB())
			
			.add("invites", talker.getInvitations())
			.add("keywords", talker.getKeywords())
			
			.add("privacy_settings", setToDB(talker.getPrivacySettings()))
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
	/**
	 * Loads a talker by particular field
	 */
	private static TalkerBean getByField(String fieldName, Object fieldValue) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject query = new BasicDBObject(fieldName, fieldValue);
		DBObject talkerDBObject = talkersColl.findOne(query, new BasicDBObject("img", 0));
		
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
	 * Get by userName or anonymous name
	 */
	public static TalkerBean getByURLName(String urlName) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		talkersColl.ensureIndex(new BasicDBObject("uname", 1));
		talkersColl.ensureIndex(new BasicDBObject("anon_name", 1));
		
		DBObject usernameQuery = new BasicDBObject("uname", urlName);
		DBObject anonymousQuery = new BasicDBObject("anon_name", urlName);
		DBObject query = new BasicDBObject("$or", Arrays.asList(usernameQuery, anonymousQuery));
		DBObject talkerDBObject = talkersColl.findOne(query);
		
		TalkerBean talker = null;
		if (talkerDBObject != null) {
			talker = new TalkerBean();
			talker.parseFromDB(talkerDBObject);
		}
		return talker;
	}
	
	/**
	 * Get by main or non-primary emails.
	 */
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
	
	/**
	 * Get by verify code of main or non-primary emails
	 */
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
	
	/**
	 * Get talker by:
	 * - username/password;
	 * - email/password;
	 * - non-primary email/password;
	 * - original username (before deactivation)/password.
	 */
	public static TalkerBean getByLoginInfo(String usernameOrEmail, String password) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		//for case insensitive search we use regex
		Pattern usernameOrEmailRegex = Pattern.compile("^"+usernameOrEmail+"$", Pattern.CASE_INSENSITIVE);
		
		Object passwordRegex = password;
		if (password == null) {
			//every password would be ok - we search only by login
			passwordRegex = Pattern.compile(".*");
		}
		
		DBObject usernameQuery = BasicDBObjectBuilder.start()
			.add("uname", usernameOrEmailRegex)
			.add("pass", passwordRegex)
			.get();
		DBObject emailQuery = BasicDBObjectBuilder.start()
			.add("email", usernameOrEmailRegex)
			.add("pass", passwordRegex)
			.get();
		DBObject notPrimaryEmailQuery = BasicDBObjectBuilder.start()
			.add("emails.value", usernameOrEmailRegex)
			.add("pass", passwordRegex)
			.get();
		DBObject deactivatedUsernameQuery = BasicDBObjectBuilder.start()
			.add("orig_uname", usernameOrEmailRegex)
			.add("pass", passwordRegex)
			.get();
		
		DBObject query = new BasicDBObject("$or", 
				Arrays.asList(usernameQuery, emailQuery, notPrimaryEmailQuery, deactivatedUsernameQuery)
			);

		DBObject fields = getBasicTalkerFields();
		DBObject talkerDBObject = talkersColl.findOne(query, fields);
		
		TalkerBean talker = null;
		if (talkerDBObject != null) {
			talker = new TalkerBean();
			talker.parseBasicFromDB(talkerDBObject);
		}
		return talker;
	}
	
	/**
	 * Get by Twitter or Facebook account
	 * @param serviceType
	 * @param accountId
	 * @return
	 */
	public static TalkerBean getByAccount(ServiceType serviceType, String accountId) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject query = new BasicDBObject();
		query.put("service_accounts.type", serviceType.toString());
		query.put("service_accounts.id", accountId);
		
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
	
	
	public static TalkerBean parseTalker(DBObject dbObject, String name) {
		DBRef talkerRef = (DBRef)dbObject.get(name);
		if (talkerRef == null) {
			return null;
		}
		return parseTalker(talkerRef);
	}
	
	public static TalkerBean parseTalker(DBRef talkerRef) {
		return parseTalker(talkerRef.getId().toString());
	}
	
	public static TalkerBean parseTalker(String talkerId) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject fields = getBasicTalkerFields();
		
		DBObject query = new BasicDBObject("_id", new ObjectId(talkerId));
		DBObject talkerDBObject = talkersColl.findOne(query, fields);
		
		if (talkerDBObject == null) {
			return null;
		}
		else {
			TalkerBean talker = new TalkerBean();
			talker.parseBasicFromDB(talkerDBObject);
			return talker;
		}
	}
	
	// ---------------------- Other methods -----------------------
	
	/**
	 * Checks userName and original userName (deactivated users)
	 * @param userName
	 * @return
	 */
	public static boolean isUserNameUnique(String userName) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject usernameQuery = BasicDBObjectBuilder.start()
			.add("uname", userName)
			.get();
		DBObject originalUsernameQuery = BasicDBObjectBuilder.start()
			.add("orig_uname", userName)
			.get();
		DBObject anonNameQuery = BasicDBObjectBuilder.start()
			.add("anon_name", userName)
			.get();
		
		DBObject query = new BasicDBObject("$or", 
				Arrays.asList(usernameQuery, originalUsernameQuery, anonNameQuery)
			);
		
		DBObject talkerDBObject = talkersColl.findOne(query);
		return (talkerDBObject == null);
	}
	/**
	 * Checks userName and original userName (deactivated users)
	 * @param email
	 * @return
	 */
	public static boolean isEmailUnique(String email) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject emailQuery = BasicDBObjectBuilder.start()
			.add("email", email)
			.get();	
		
		DBObject talkerDBObject = talkersColl.findOne(emailQuery);
		return (talkerDBObject == null);
	}
	
	/**
	 * Load several talkers specified by ids
	 * @param ids List of ids
	 * @return
	 */
	public static List<TalkerBean> loadSetTalkers(List<String> ids) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		QueryBuilder query = QueryBuilder.start();
		
		for(String id: ids) {
			query.or(new BasicDBObject("_id", new ObjectId(id)));
		}
				
		List<DBObject> talkersDBObjectList = 
			talkersColl.find(query.get()).toArray();
		
		List<TalkerBean> talkerList = new ArrayList<TalkerBean>();
		for (DBObject talkerDBObject : talkersDBObjectList) {
			TalkerBean talker = new TalkerBean();
			talker.parseBasicFromDB(talkerDBObject);
			talkerList.add(talker);
		}
		
		return talkerList;
	}	
	/**
	 * Load all (deactivated and suspended also) talkers.
	 * @param basicInfo Load full or only basic info
	 * @return
	 */
	public static List<TalkerBean> loadAllTalkers(boolean basicInfo) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);

		talkersColl.ensureIndex(new BasicDBObject("uname", 1));
		
		List<DBObject> talkersDBObjectList = null;
		if (basicInfo) {
			DBObject fields = getBasicTalkerFields();
			talkersDBObjectList = talkersColl.find(null, fields).sort(new BasicDBObject("uname", 1)).toArray();
		}
		else {
			talkersDBObjectList = talkersColl.find().sort(new BasicDBObject("uname", 1)).toArray();
		}
		
		List<TalkerBean> talkerList = new ArrayList<TalkerBean>();
		for (DBObject talkerDBObject : talkersDBObjectList) {
			TalkerBean talker = new TalkerBean();
			if (basicInfo) {
				talker.parseBasicFromDB(talkerDBObject);
			}
			else {
				talker.parseFromDB(talkerDBObject);
			}
			talkerList.add(talker);
		}
		
		return talkerList;
	}
	public static List<TalkerBean> loadAllTalkers() {
		return loadAllTalkers(false);
	}

		
	// --------------------- Other ---------------------------
	
	public static List<TalkerBean> loadTalkersForDashboard() {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		List<DBObject> talkersDBList = talkersColl.find().toArray();
		
		List<TalkerBean> talkersList = new ArrayList<TalkerBean>();
		for (DBObject talkerDBObject : talkersDBList) {
			TalkerBean talker = new TalkerBean();
			talker.parseFromDB(talkerDBObject);
			
			Date latestNotification = NotificationDAO.getLatestNotification(talker.getId());
			talker.setLatestNotification(latestNotification);
			
			long numOfNotifications = NotificationDAO.numOfNotificationsForDay(talker.getId(), null);
			talker.setNumOfNotifications(numOfNotifications);
			
			talkersList.add(talker);
		}
		
		return talkersList;
	}
	
	/**
	 * Returns 'null' if Privacy Settings do not allow to show image.
	 * 
	 * @param userName
	 * @param currentUser
	 * @return
	 */
	public static byte[] loadTalkerImage(String userName, String currentUser) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject usernameQuery = new BasicDBObject("uname", userName);
		DBObject anonymousQuery = new BasicDBObject("anon_name", userName);
		DBObject query = new BasicDBObject("$or", Arrays.asList(usernameQuery, anonymousQuery));
		DBObject fields = BasicDBObjectBuilder.start()
			.add("img", 1)
			.add("deactivated", 1)
			.add("privacy_settings", 1)
			.get();
		DBObject talkerDBObject = talkersColl.findOne(query, fields);
		
		if (talkerDBObject == null) {
			return null;
		}
		
		//User can always see his/her own image
		if (!userName.equals(currentUser)) {
			//If user has private image, display the default profile image.
			TalkerBean talker = new TalkerBean();
			talker.setPrivacySettings(parseSet(PrivacySetting.class, talkerDBObject, "privacy_settings"));
			
			PrivacyValue privacyValue = talker.getPrivacyValue(PrivacyType.PROFILE_IMAGE);
			if ( (currentUser == null && privacyValue != PrivacyValue.PUBLIC)
					|| privacyValue == PrivacyValue.PRIVATE
					|| getBoolean(talkerDBObject, "deactivated") ) {
				//show default image
				return null;
			}
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
	
	
	public static void saveThankYou(ThankYouBean thankYouBean) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBRef fromTalkerRef = createRef(TALKERS_COLLECTION, thankYouBean.getFromTalker().getId());
		DBObject thankYouObject = BasicDBObjectBuilder.start()
			.add("id", new ObjectId().toString())
			.add("time", thankYouBean.getTime())
			.add("note", thankYouBean.getNote())
			.add("from", fromTalkerRef)
			.get();
		
		DBObject query = new BasicDBObject("_id", new ObjectId(thankYouBean.getTo()));
		//For creating/adding to array: { $push : { field : value } }
		talkersColl.update(query, new BasicDBObject("$push", new BasicDBObject("thankyous", thankYouObject)));
	}
	
	public static void deleteThankYou(ThankYouBean thankYou) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject thankYouObject = new BasicDBObject("id", thankYou.getId());
		DBObject query = new BasicDBObject("_id", new ObjectId(thankYou.getTo()));
		talkersColl.update(query,
				new BasicDBObject("$pull", new BasicDBObject("thankyous", thankYouObject)));
	}
	
	/**
	 * Follows or unfollows depending on third parameter
	 * @param followerId This talker follows/unfollows
	 * @param followingId This talker is followed/unfollowed
	 * @param follow
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
		
		List<DBObject> followerDBList = talkersColl.find(query, new BasicDBObject("_id", 1)).toArray();
		
		List<TalkerBean> followerList = new ArrayList<TalkerBean>();
		for (DBObject followerDBObject : followerDBList) {
			TalkerBean followerTalker = 
				TalkerLogic.loadTalkerFromCache(followerDBObject.get("_id").toString());
			followerList.add(followerTalker);
		}
		
		return followerList;
	}
	
	public static void main(String[] args) {
//		TalkerDAO.follow("4c2cb43160adf3055c97d061", "4c35dbeb5165f305eebfc5f2", true);
//		System.out.println(loadFollowers("4c35dbeb5165f305eebfc5f2"));
		
//		TalkerDAO.saveProfileComment("4c35dbeb5165f305eebfc5f2", "4c2cb43160adf3055c97d061", "Teeeext");
//		TalkerDAO.loadProfileComments("4c35dbeb5165f305eebfc5f2");
//		TalkerDAO.saveProfileReply("4c35dbeb5165f305eebfc5f2", "4c35dbeb5165f305eebfc5f2", "Reply2222");
		
		
//		TalkerBean talker = TalkerDAO.getByVerifyCode("e7b279a1-7e41-4be6-8e3e-2bbb8821e57d");
//		System.out.println(talker.getUserName());
	}

	public static DBObject getBasicTalkerFields() {
		DBObject fields = BasicDBObjectBuilder.start()
			.add("following_topics", 0)
			.add("following_convos", 0)
			.add("img", 0)
			.add("following", 0)
			.add("topics_info", 0)
			.add("thankyous", 0)
			.add("hidden_helps", 0)
			.add("ch_ages", 0)
			.add("keywords", 0)
			.add("ethnicities", 0)
			.add("languages", 0)
			.add("insurance_accept", 0)
			.get();
		return fields;
	}
}


package dao;

import static util.DBUtil.createRef;
import static util.DBUtil.getBoolean;
import static util.DBUtil.getCollection;
import static util.DBUtil.parseSet;
import static util.DBUtil.setToDB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import logic.ConversationLogic;
import logic.FeedsLogic;
import logic.TalkerLogic;
import logic.TopicLogic;
import models.CommentBean;
import models.ConversationBean;
import models.IMAccountBean;
import models.NewsLetterBean;
import models.PrivacySetting;
import models.TalkerBean;
import models.TalkerImageBean;
import models.ThankYouBean;
import models.TopicBean;
import models.ConversationBean.ConvoType;
import models.PrivacySetting.PrivacyType;
import models.PrivacySetting.PrivacyValue;
import models.ServiceAccountBean.ServiceType;

import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;

import play.Logger;
import play.cache.Cache;

import util.EmailUtil;
import util.EmailUtil.EmailTemplate;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.QueryBuilder;
import com.sun.xml.internal.fastinfoset.util.StringArray;

import controllers.Conversations;

public class TalkerDAO {
	
	public static final String TALKERS_COLLECTION = "talkers";
	public static final String TALKERS_NEW_COLLECTION = "talkersnew";
	public static final String TALKERS_MERGE_COLLECTION = "talkers_merge";
	
	// --------------------- Save/Update ---------------------------
	public static boolean save(TalkerBean talker) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject talkerDBObject = BasicDBObjectBuilder.start()
				.add("uname", talker.getUserName())
				.add("profilename", talker.getProfileName())
				.add("anon_name", talker.getAnonymousName())
				.add("pass", talker.getPassword())
				.add("email", talker.getEmail())
				//.add("verify_code", talker.getVerifyCode())
				.add("verify_code", null)
				//.add("dob", talker.getDob())
				.add("dob_private", talker.getDob())
				.add("dod", talker.getDod())
				.add("timestamp",  Calendar.getInstance().getTime())
				.add("category", talker.getCategory() == null ? null : talker.getCategory().equals("") ? null : talker.getCategory())
				.add("connection", talker.getConnection())
				.add("connection_verified", talker.isConnectionVerified())
				.add("suspended",false)
				.add("nfreq", talker.getNfreq())
				.add("ntime", talker.getNtime())
				.add("ctype", talker.getCtype())
				.add("ctype_other", talker.getOtherCtype())
				.add("im_notify", talker.isImNotify())
				.add("service_accounts", setToDB(talker.getServiceAccounts()))
				.add("privacy_settings", setToDB(talker.getPrivacySettings()))
				.add("email_settings", talker.emailSettingsToList())
				.add("hidden_helps", talker.getHiddenHelps())
				//.add("newsletter", talker.isNewsletter())
				.add("newsletter", true)
				.add("workshop", true)
				.add("workshopSummery", true)
				.add("invites", talker.getInvitations())
				.add("ch_num", -1)
				.add("password_update", talker.isPasswordUpdate())
				.add("isImg", false)
				.add("answerCnt", 0)
				.get();
		talkersColl.save(talkerDBObject);
		/* Date : 27 June 2011
		 * Added subscribe to newsletter feature. Here added user's name used by user while registration
		 * */
		if(talker.isNewsletter() || talker.getNewsLetterBean() != null && talker.getNewsLetterBean().getNewsLetterType().length > 0){
			Logger.info("save news letter:::::"+talker.getEmail());
			
			String talkerNLType[]=talker.getNewsLetterBean().getNewsLetterType();
			String newsLetterTypes[]=new String[talkerNLType.length+3];
			for(int i=0;i<talkerNLType.length;i++){newsLetterTypes[i]=talkerNLType[i];}
			newsLetterTypes[talkerNLType.length]="CLINICAL_TRIALS_UPDATE";
			newsLetterTypes[talkerNLType.length+1]="PRODUCT_AND_PROGRAMS_UPDATE";
			newsLetterTypes[talkerNLType.length+2]="MARKET_RESEARCH_UPDATE";
			
			talker.getNewsLetterBean().setNewsLetterType(newsLetterTypes);
			ApplicationDAO.addToNewsLetter(talker.getEmail(), talker.getNewsLetterBean().getNewsLetterType());
			Map<String, String> vars = new HashMap<String, String>();
    		vars.put("username", talker.getUserName());
        	EmailUtil.sendEmail(EmailTemplate.WELCOME_NEWSLETTER, talker.getEmail(), vars, null, false);
		}
		talker.setId(talkerDBObject.get("_id").toString());
		return true;
	}
	public static void updateTalker(TalkerBean talker) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject talkerObject = BasicDBObjectBuilder.start()
			.add("uname", talker.getUserName())
			.add("profilename", talker.getProfileName())
			.add("anon_name", talker.getAnonymousName())
			.add("pass", talker.getPassword())
			.add("email", talker.getEmail())
			.add("verify_code", talker.getVerifyCode())
			.add("old_verify_code", talker.getOldVerifyCode())
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
			.add("dod", talker.getDod())
			.add("gender", talker.getGender())
			.add("mar_status", talker.getMaritalStatus())
			.add("city", talker.getCity())
			.add("state", talker.getState())
			.add("country", talker.getCountry())
			.add("category", talker.getCategory())
			.add("newsletter", talker.isNewsletter())
			.add("workshop", talker.isWorkshop())
			.add("workshopSummery", talker.isWorkshopSummery())
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
			.add("otherCategories", talker.getOtherCategories())
			.add("password_update", talker.isPasswordUpdate())
			.get();
		
		DBObject talkerId = new BasicDBObject("_id", new ObjectId(talker.getId()));
		//"$set" is used for updating fields
		talkersColl.update(talkerId, new BasicDBObject("$set", talkerObject));
	}
	
	public static void updateTalkerImage(TalkerBean talker, byte[] imageArray,String fileExt) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		boolean isImage = true;
		if(imageArray == null)
			isImage = false;
		
		DBObject talkerObject = BasicDBObjectBuilder.start()
			.add("img", imageArray)
			.add("isImg", isImage)
			.add("fileExt",fileExt)
			.get();
		
		DBObject talkerId = new BasicDBObject("_id", new ObjectId(talker.getId()));
		//"$set" is used for updating fields
		talkersColl.update(talkerId, new BasicDBObject("$set", talkerObject));
	}
	
	public static void updateTalkerImageCoords(TalkerBean talker, String[] imageCoords) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject talkerObject = BasicDBObjectBuilder.start()
			.add("imgcoords", imageCoords)
			.get();
		
		DBObject talkerId = new BasicDBObject("_id", new ObjectId(talker.getId()));
		//"$set" is used for updating fields
		talkersColl.update(talkerId, new BasicDBObject("$set", talkerObject));
	}
	
	public static String [] getTalkerCoords(String userName){
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject usernameQuery = new BasicDBObject("uname", userName);
		DBObject anonymousQuery = new BasicDBObject("anon_name", userName);
		DBObject query = new BasicDBObject("$or", Arrays.asList(usernameQuery, anonymousQuery));
		
		DBObject fields = BasicDBObjectBuilder.start()
			.add("imgcoords", 1)
			.get();
		DBObject talkerDBObject = talkersColl.findOne(query, fields);
		
		if (talkerDBObject == null) {
			return null;
		} else {
			Collection<String> otherCategories = (Collection<String>)talkerDBObject.get("imgcoords");
			if (otherCategories != null) {
				return otherCategories.toArray(new String[]{});
			} else {
				return null;
			}
		}
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
		
		//talkersColl.ensureIndex(new BasicDBObject("uname", 1));
		//talkersColl.ensureIndex(new BasicDBObject("anon_name", 1));
		
		DBObject usernameQuery = new BasicDBObject("uname", Pattern.compile(urlName , Pattern.CASE_INSENSITIVE));
		DBObject anonymousQuery = new BasicDBObject("anon_name", Pattern.compile(urlName , Pattern.CASE_INSENSITIVE));
		//DBObject usernameQuery = new BasicDBObject("uname", urlName);
		//DBObject anonymousQuery = new BasicDBObject("anon_name", urlName);
		DBObject query = new BasicDBObject("$or", Arrays.asList(usernameQuery, anonymousQuery));
		
		List<DBObject> talkersDBObjectList = new ArrayList<DBObject>();//talkersColl.find(query).toArray();
		DBCursor talkerCur=talkersColl.find(query);
		while(talkerCur.hasNext()){
			talkersDBObjectList.add(talkerCur.next());
		}
		
		TalkerBean talker = null;
		if(talkersDBObjectList != null) {
			for (DBObject talkerDBObject : talkersDBObjectList) {
				TalkerBean talkerTemp = new TalkerBean();
				talkerTemp.parseFromDB(talkerDBObject);
				if(((talkerTemp.getUserName() != null && talkerTemp.getUserName().toLowerCase().equals(urlName.toLowerCase())) || (talkerTemp.getAnonymousName() != null &&  talkerTemp.getAnonymousName()
						.toLowerCase().equals(urlName.toLowerCase()))) && !talkerTemp.isSuspended()) {
					talker = talkerTemp;
				}
			}
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
			} else {
				talker = new TalkerBean();
				talker.parseFromDB(talkerDBObject);
			}
		}
		return talker;
	}
	
	/**
	 * Get by main or non-primary emails.
	 */
	public static TalkerBean getByEmailNotSuspended(String email) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		TalkerBean talker = null;
		DBObject usernameQuery = BasicDBObjectBuilder.start()
				.add("email", email)
				.add("suspended",new BasicDBObject("$ne",true))
				.get();
		DBObject talkerDBObject = talkersColl.findOne(usernameQuery, new BasicDBObject("img", 0));
		
		if (talkerDBObject != null) {
			talker = new TalkerBean();
			talker.parseFromDB(talkerDBObject);
			return talker;
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
	 * Get by Old verify code of main or non-primary emails
	 */
	public static TalkerBean getByOldVerifyCode(String verifyCode) {
		TalkerBean talker = getByField("old_verify_code", verifyCode);
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
			.add("suspended",new BasicDBObject("$ne",true))
			.get();
		DBObject emailQuery = BasicDBObjectBuilder.start()
			.add("email", usernameOrEmailRegex)
			.add("pass", passwordRegex)
			.add("suspended",new BasicDBObject("$ne",true))
			.get();
		DBObject notPrimaryEmailQuery = BasicDBObjectBuilder.start()
			.add("emails.value", usernameOrEmailRegex)
			.add("pass", passwordRegex)
			.add("suspended",new BasicDBObject("$ne",true))
			.get();
		DBObject deactivatedUsernameQuery = BasicDBObjectBuilder.start()
			.add("orig_uname", usernameOrEmailRegex)
			.add("pass", passwordRegex)
			.add("suspended",new BasicDBObject("$ne",true))
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
		DBObject talkerDBObject = null;
		try{
			talkerDBObject = talkersColl.findOne(query, fields);
		}catch (Exception e){
			e.printStackTrace();
		}
		
		if (talkerDBObject == null) {
			return null;
		}
		else {
			TalkerBean talker = new TalkerBean();
			if(talkerDBObject != null)
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
		
		List<DBObject> talkersDBObjectList = new ArrayList<DBObject>();//List<DBObject> talkersDBObjectList = talkersColl.find(query.get()).toArray();
		DBCursor talkerCur=talkersColl.find(query.get());
		while(talkerCur.hasNext()){
			talkersDBObjectList.add(talkerCur.next());
		}
		
		
		List<TalkerBean> talkerList = new ArrayList<TalkerBean>();
		for (DBObject talkerDBObject : talkersDBObjectList) {
			TalkerBean talker = new TalkerBean();
			talker.parseBasicFromDB(talkerDBObject);
			talkerList.add(talker);
		}
		
		return talkerList;
	}	
	
	public static List<TalkerBean> loadAllTalkersByCategory(boolean basicInfo,List<String> categoryList){
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		talkersColl.ensureIndex(new BasicDBObject("uname", 1));
		
		BasicDBObjectBuilder queryBuilder = BasicDBObjectBuilder.start()
			//.add("category", new BasicDBObject("$in", categoryList) )
			.add("suspended", false)
			//.add("otherCategories", new BasicDBObject("$in", categoryList) )
			.add("$or", 
				Arrays.asList(
						new BasicDBObject("category", new BasicDBObject("$in", categoryList)),
						new BasicDBObject("otherCategories", new BasicDBObject("$in", categoryList))
					)
			);

		List<DBObject> talkersDBObjectList = null;
		if (basicInfo) {
			DBObject fields = getBasicTalkerFields();
			
			 talkersDBObjectList = new ArrayList<DBObject>();//talkersDBObjectList = talkersColl.find(queryBuilder.get(), fields).sort(new BasicDBObject("uname", 1)).toArray();
			DBCursor talkerCur=talkersColl.find(queryBuilder.get(), fields).sort(new BasicDBObject("uname", 1));
			while(talkerCur.hasNext()){
				talkersDBObjectList.add(talkerCur.next());
			}
		} else {
			talkersDBObjectList = new ArrayList<DBObject>();//talkersDBObjectList = talkersColl.find().sort(new BasicDBObject("uname", 1)).toArray();
			DBCursor talkerCur=talkersColl.find().sort(new BasicDBObject("uname", 1));
			while(talkerCur.hasNext()){
				talkersDBObjectList.add(talkerCur.next());
			}
		}

		List<TalkerBean> talkerList = new ArrayList<TalkerBean>();
		for (DBObject talkerDBObject : talkersDBObjectList) {
			TalkerBean talker = new TalkerBean();
			if (basicInfo) {
				talker.parseBasicFromDB(talkerDBObject);
			} else {
				talker.parseFromDB(talkerDBObject);
			}
			talkerList.add(talker);
		}
		return talkerList;
	}
	
	public static List<TalkerBean> loadAllTalkers(boolean basicInfo,TalkerBean currentTalker,String cancerType) {
		
		//List<String> cat = FeedsLogic.getCancerType(currentTalker);
		
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		talkersColl.ensureIndex(new BasicDBObject("uname", 1));
		
		//BasicDBObjectBuilder queryBuilder = BasicDBObjectBuilder.start().add("category", new BasicDBObject("$in", cat) ).add("suspended", false);
		BasicDBObjectBuilder queryBuilder = BasicDBObjectBuilder.start().add("suspended", false);
		
		if(StringUtils.isNotBlank(cancerType)){
			List<String> cat = new ArrayList<String>();
			cat.add(cancerType);
			cat.add(ConversationBean.ALL_CANCERS);
			queryBuilder.add("$or", 
				Arrays.asList(
						new BasicDBObject("otherCategories", new BasicDBObject("$in", cat)),
						new BasicDBObject("category", new BasicDBObject("$in", cat))
				)
			);
			
		}
		
		List<DBObject> talkersDBObjectList = null;
		if (basicInfo) {
			DBObject fields = getBasicTalkerFields();
			talkersDBObjectList=new ArrayList<DBObject>();//talkersDBObjectList = talkersColl.find(queryBuilder.get(), fields).sort(new BasicDBObject("uname", 1)).toArray();
			DBCursor talkerCur= talkersColl.find(queryBuilder.get(), fields).sort(new BasicDBObject("uname", 1));
			while(talkerCur.hasNext()) {
				talkersDBObjectList.add(talkerCur.next());
			}
		} else {
			talkersDBObjectList=new ArrayList<DBObject>();//talkersDBObjectList = talkersColl.find().sort(new BasicDBObject("uname", 1)).toArray();
			DBCursor talkerCur=talkersColl.find().sort(new BasicDBObject("uname", 1));
			while(talkerCur.hasNext()) {
				talkersDBObjectList.add(talkerCur.next());
			}
		}

		List<TalkerBean> talkerList = new ArrayList<TalkerBean>();
		for (DBObject talkerDBObject : talkersDBObjectList) {
			TalkerBean talker = new TalkerBean();
			if (basicInfo) {
				talker.parseBasicFromDB(talkerDBObject);
			} else {
				talker.parseFromDB(talkerDBObject);
			}
			talkerList.add(talker);
		}
		return talkerList;
	}
	public static List<TalkerBean> loadAllTalkers() {
		return loadAllTalkers(false);
	}

	/*Changes for loading all talkers with no suspened flag*/
	public static List<TalkerBean> loadAllTalker(boolean basicInfo){
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		talkersColl.ensureIndex(new BasicDBObject("uname", 1));
		
		BasicDBObjectBuilder queryBuilder = BasicDBObjectBuilder.start().add("suspended", false);
		
		List<DBObject> talkersDBObjectList = null;
		if (basicInfo) {
			DBObject fields = getBasicTalkerFields();
			talkersDBObjectList=new ArrayList<DBObject>();//talkersDBObjectList = talkersColl.find(queryBuilder.get(), fields).sort(new BasicDBObject("uname", 1)).toArray();
			DBCursor talkerCur=talkersColl.find(queryBuilder.get(), fields).sort(new BasicDBObject("uname", 1));
			while(talkerCur.hasNext()){
				talkersDBObjectList.add(talkerCur.next());
			}
			
		} else {
			talkersDBObjectList=new ArrayList<DBObject>();//talkersDBObjectList = talkersColl.find(queryBuilder.get()).sort(new BasicDBObject("uname", 1)).toArray();
			DBCursor talkerCur=talkersColl.find(queryBuilder.get()).sort(new BasicDBObject("uname", 1));
			while(talkerCur.hasNext()){
				talkersDBObjectList.add(talkerCur.next());
			}
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
	
	/*Changes for loading all talkers with no suspened flag*/
	public static List<TalkerBean> loadAllActiveTalker(boolean basicInfo) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		talkersColl.ensureIndex(new BasicDBObject("uname", 1));
		
		BasicDBObjectBuilder queryBuilder = BasicDBObjectBuilder.start().add("suspended", false).add("service_accounts", new BasicDBObject("$type", 3));
		List<DBObject> talkersDBObjectList = null;
		if (basicInfo) {
			DBObject fields = getBasicTalkerFields();
			
			DBCursor talkerCur=talkersColl.find(queryBuilder.get(), fields).sort(new BasicDBObject("uname", 1));
			
			talkersDBObjectList = new ArrayList<DBObject>();//talkersColl.find(queryBuilder.get(), fields).sort(new BasicDBObject("uname", 1)).toArray();
			while(talkerCur.hasNext()){
				talkersDBObjectList.add(talkerCur.next());
			}
			
		} else {
			talkersDBObjectList=new ArrayList<DBObject>();//talkersDBObjectList = talkersColl.find(queryBuilder.get()).sort(new BasicDBObject("uname", 1)).toArray();
			DBCursor talkerCur=talkersColl.find(queryBuilder.get()).sort(new BasicDBObject("uname", 1));
			while(talkerCur.hasNext()){
				talkersDBObjectList.add(talkerCur.next());
			}
			
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
	
	/**
	 * Load all (deactivated and suspended also) talkers.
	 * @param basicInfo Load full or only basic info
	 * @return
	 */
	public static List<TalkerBean> loadAllTalkers(boolean basicInfo) {
		
		List<TalkerBean> talkerList = new ArrayList<TalkerBean>();
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);

		talkersColl.ensureIndex(new BasicDBObject("uname", 1));
		
		List<DBObject> talkersDBObjectList = null;
		
		if (basicInfo) {
			DBObject fields = getBasicTalkerFields();
			talkersDBObjectList = new ArrayList<DBObject>();//talkersColl.find(null, fields).sort(new BasicDBObject("uname", 1)).toArray();
			
			DBCursor talkerCur=talkersColl.find(null, fields).sort(new BasicDBObject("uname", 1));
			while(talkerCur.hasNext()){
				talkersDBObjectList.add(talkerCur.next());
			}
			
			//talkersDBObjectList = talkersColl.find(null, fields).sort(new BasicDBObject("timestamp", -1)).toArray();
		} else {
			talkersDBObjectList=new ArrayList<DBObject>();//talkersDBObjectList = talkersColl.find(null, new BasicDBObject("img", 1)).sort(new BasicDBObject("uname", 1)).toArray();
			DBCursor talkerCur=talkersColl.find().sort(new BasicDBObject("uname", 1));
			while(talkerCur.hasNext()){
				talkersDBObjectList.add(talkerCur.next());
			}
			
			//talkersDBObjectList = talkersColl.find(null, new BasicDBObject("img", 1)).sort(new BasicDBObject("timestamp", -1)).toArray();
		}

		if(talkersDBObjectList != null && talkerList.size() < 10){
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
		}
		return talkerList;
	}

	public static List<TalkerBean> loadAllTalkersForMe(boolean basicInfo) {
		
		List<TalkerBean> talkerList = new ArrayList<TalkerBean>();
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);

		talkersColl.ensureIndex(new BasicDBObject("uname", 1));
		
		List<DBObject> talkersDBObjectList = null;
		if (basicInfo) {
			DBObject fields = getBasicTalkerFields();
			
			DBCursor talkerCur=talkersColl.find(null, fields).sort(new BasicDBObject("uname", 1));
			
			talkersDBObjectList = new ArrayList<DBObject>();//talkersColl.find(queryBuilder.get(), fields).sort(new BasicDBObject("uname", 1)).toArray();
			while(talkerCur.hasNext()){
				talkersDBObjectList.add(talkerCur.next());
			}
			
			//talkersDBObjectList = talkersColl.find(null, fields).sort(new BasicDBObject("uname", 1)).toArray();
		} else {
			talkersDBObjectList=new ArrayList<DBObject>();//talkersDBObjectList = talkersColl.find().sort(new BasicDBObject("uname", 1)).toArray();
			DBCursor talkerCur=talkersColl.find().sort(new BasicDBObject("uname", 1));
			while(talkerCur.hasNext()){
				talkersDBObjectList.add(talkerCur.next());
			}
			
		}

		if(talkersDBObjectList != null && talkerList.size() < 10){
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
		}
		return talkerList;
	}
	
	/**
	 * load talker who create from last minute 
	 * for search indexer
	 */
	
	
	public static List<TalkerBean> loadUpdatedTalker(int limit) {
		boolean basicInfo=true;
		
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);

		talkersColl.ensureIndex(new BasicDBObject("uname", 1));
		
		Calendar cal= Calendar.getInstance();
		cal.add(Calendar.MINUTE, -limit);
		Date date=cal.getTime();
		BasicDBObject time = new BasicDBObject("$gt", date);
		
		DBObject query = BasicDBObjectBuilder.start()
		.add("timestamp", time)
		.get();
	
		List<DBObject> talkersDBObjectList = null;
		if (basicInfo) {
			DBObject fields = getBasicTalkerFields();
			talkersDBObjectList=new ArrayList<DBObject>();//talkersDBObjectList = talkersColl.find(query, fields).sort(new BasicDBObject("uname", 1)).toArray();
			DBCursor talkerCur=talkersColl.find(query, fields).sort(new BasicDBObject("uname", 1));
			while(talkerCur.hasNext()){
				talkersDBObjectList.add(talkerCur.next());
			}
		} else {
			talkersDBObjectList=new ArrayList<DBObject>();//talkersDBObjectList = talkersColl.find(query).sort(new BasicDBObject("uname", 1)).toArray();
			DBCursor talkerCur=talkersColl.find(query).sort(new BasicDBObject("uname", 1));
			while(talkerCur.hasNext()){
				talkersDBObjectList.add(talkerCur.next());
			}
			
		}

		List<TalkerBean> talkerList = new ArrayList<TalkerBean>();
		
		for (DBObject talkerDBObject : talkersDBObjectList) {
			TalkerBean talker = new TalkerBean();
			if (basicInfo) {
				talker.parseBasicFromDB(talkerDBObject);
			} else {
				talker.parseFromDB(talkerDBObject);
			}
			talkerList.add(talker);
		}
		return talkerList;
	}
	
	//db.talkers.find().skip(50).limit(20);
		
	// --------------------- Other ---------------------------
	
	public static List<TalkerBean> loadTalkersForDashboard() {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		List<DBObject>  talkersDBList=new ArrayList<DBObject>();//talkersDBList = talkersColl.find().toArray();
		DBCursor talkerCur=talkersColl.find();
		while(talkerCur.hasNext()){
			talkersDBList.add(talkerCur.next());
		}
		
		
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
		
		if (talkerDBObject.get("img") == null) 
			return null;
		else
			return (byte[])talkerDBObject.get("img");
	}
	
	/**
	 * Returns 'null' if Privacy Settings do not allow to show image.
	 * 
	 * @param userName
	 * @param currentUser
	 * @return
	 */
	public static TalkerImageBean loadTalkerImageBean(String userName, String currentUser) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		TalkerImageBean imageBean = new TalkerImageBean();
		DBObject usernameQuery = new BasicDBObject("uname", userName);
		DBObject anonymousQuery = new BasicDBObject("anon_name", userName);
		DBObject query = new BasicDBObject("$or", Arrays.asList(usernameQuery, anonymousQuery));
		DBObject fields = BasicDBObjectBuilder.start()
			.add("img", 1)
			.add("deactivated", 1)
			.add("privacy_settings", 1)
			.add("imgcoords", 1)
			.add("fileExt", 1)
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
				imageBean.setImageArray(null);
				imageBean.setImageType("gif");
				imageBean.setCoords(null);
				//return null;
			}
		}

		if (talkerDBObject.get("img") == null) { 
			imageBean.setImageArray(null);
			imageBean.setImageType("gif");
			imageBean.setCoords(null);
		} else {
			imageBean.setImageArray((byte[])talkerDBObject.get("img"));

			//Co ordinations
			Collection<String> otherCategories = (Collection<String>)talkerDBObject.get("imgcoords");
			if (otherCategories != null) {
				imageBean.setCoords(otherCategories.toArray(new String[]{}));
			} else {
				imageBean.setCoords(null);
			}

			if(talkerDBObject.get("fileExt") != null) {
				imageBean.setImageType(talkerDBObject.get("fileExt").toString());
			} else {
				imageBean.setImageType("gif");
			}
		}
		return imageBean;
	}

	/**
	 * Returns 'null' if Privacy Settings do not allow to show image.
	 * 
	 * @param userName
	 * @param currentUser
	 * @return
	 */
	public static boolean isTalkerImage(String userId) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		DBObject query = new BasicDBObject("_id", new ObjectId(userId));
		DBObject talkerDBObject = talkersColl.findOne(query, new BasicDBObject("img", 1));
		if (talkerDBObject == null) {
			return false;
		} else {
			if (talkerDBObject.get("img") == null) {
				return false;
			//} else if(getBoolean(talkerDBObject, "isImg")) {
			//	return true;
			} else
				return true;
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
		
		List<DBObject> followerDBList=new ArrayList<DBObject>();//List<DBObject> followerDBList = talkersColl.find(query, new BasicDBObject("_id", 1)).toArray();
		DBCursor talkerCur=talkersColl.find(query, new BasicDBObject("_id", 1));
		while(talkerCur.hasNext()){
			followerDBList.add(talkerCur.next());
		}
		
		
		List<TalkerBean> followerList = new ArrayList<TalkerBean>();
		for (DBObject followerDBObject : followerDBList) {
			TalkerBean followerTalker = TalkerLogic.loadTalkerFromCache(followerDBObject.get("_id").toString());
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
			
			.add("hidden_helps", 0)
			.add("nfreq", 0)
			.add("ntime", 0)
			.add("ctype", 0)
			.add("ctype_other", 0)
			.add("newsletter", 0)
			.add("workshop", 0)
			.add("gender", 0)
			.add("dob", 0)
			.add("city", 0)
			.add("state", 0)
			.add("country", 0)
			.add("ch_num", 0)
			.add("mar_status", 0)
			.add("firstname", 0)
			.add("lastname", 0)
			.add("zip", 0)
			.add("ch_ages", 0)
			.add("ethnicities", 0)
			.add("religion", 0)
			.add("religion_serious", 0)
			.add("languages", 0)
			.add("insurance_accept", 0)
			.add("following", 0)
			.add("following_convos", 0)
			.add("following_topics", 0)
			.add("topics_info", 0)
			.get();
		return fields;
	}
	
	/**
	 * Load talkers by user name
	 * @param String
	 * @return
	 */
	public static List<TalkerBean> searchTalkers(String searchString) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);

		Pattern userNamePattern = Pattern.compile(searchString, Pattern.CASE_INSENSITIVE);
		BasicDBObject query = new BasicDBObject("uname",  userNamePattern );

		List<DBObject> talkersDBObjectList = null;
		talkersDBObjectList=new ArrayList<DBObject>();//talkersDBObjectList = talkersColl.find(query).toArray();
		DBCursor talkerCur=talkersColl.find(query);
		while(talkerCur.hasNext()){
			talkersDBObjectList.add(talkerCur.next());
		}
		
		
		List<TalkerBean> talkerList = new ArrayList<TalkerBean>();
		for (DBObject talkerDBObject : talkersDBObjectList) {
			TalkerBean talker = new TalkerBean();
			talker.parseFromDB(talkerDBObject);
			talkerList.add(talker);
		}
		
		return talkerList;
	}
	
	/**
	 * Get talker by thank you id
	 * @param id
	 * @return TalkerBean
	 */
	public static TalkerBean getByThankYouId(String id) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		DBObject query = BasicDBObjectBuilder.start()
			.add("thankyous.id", id)
			.get();
		DBObject talkerDBObject = talkersColl.findOne(query);
		
		if (talkerDBObject == null) {
			return null;
		}
		TalkerBean talker = new TalkerBean();
		talker.parseFromDB(talkerDBObject);
		return talker;
	}
	
	public static List<TalkerBean> getTalkersForWorkshopMail(){
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);

		List<DBObject> talkersDBObjectList = null;
		DBObject query = BasicDBObjectBuilder.start()
		.add("workshop", new BasicDBObject("$ne", false))
		.get();
		
		talkersDBObjectList=new ArrayList<DBObject>();//talkersDBObjectList = talkersColl.find(query).toArray();
		DBCursor talkerCur=talkersColl.find(query);
		while(talkerCur.hasNext()){
			talkersDBObjectList.add(talkerCur.next());
		}
		
		
		List<TalkerBean> talkerList = new ArrayList<TalkerBean>();
		for (DBObject talkerDBObject : talkersDBObjectList) {
			TalkerBean talker = new TalkerBean();
			talker.parseFromDB(talkerDBObject);
			talkerList.add(talker);
		}
		
		return talkerList;
	}
	
	/**
	 * Loads a talker by userName by ignoring case
	 */
	public static TalkerBean getByUserNameIgnoreCase(String userName) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		DBObject query = new BasicDBObject("uname",Pattern.compile("^"+userName+"$" , Pattern.CASE_INSENSITIVE));
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
	 * Load no of followers for particular user.
	 * @param talkerId
	 * @return int
	 */
	public static int getFollowersCount(String talkerId){
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		DBRef followingTalkerRef = createRef(TALKERS_COLLECTION, talkerId);
		BasicDBObject query = new BasicDBObject("following", followingTalkerRef);
		return talkersColl.find(query, new BasicDBObject("_id", 1)).count();
	}
	
	/**
	 * Loads a talker by particular field
	 */
	public static TalkerBean getByFieldBasicInfo(String fieldName, Object fieldValue) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject query = new BasicDBObject(fieldName, fieldValue);
		DBObject talkerDBObject = talkersColl.findOne(query, new BasicDBObject("img", 0));
		
		if (talkerDBObject == null) {
			return null;
		}
		else {
			TalkerBean talker = new TalkerBean();
			talker.parseBasicFromDB(talkerDBObject);
			return talker;
		}
	}
	public static void updateTalkerForDisease(TalkerBean talker) {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		
		DBObject talkerObject = BasicDBObjectBuilder.start()
			.add("category", talker.getCategory())
			.add("otherCategories", talker.getOtherCategories())
			.get();
		
		DBObject talkerId = new BasicDBObject("_id", new ObjectId(talker.getId()));
		//"$set" is used for updating fields
		talkersColl.update(talkerId, new BasicDBObject("$set", talkerObject));
	}
	
	public static void addUserAnswerCount(String userId){
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		DBObject talkerId = new BasicDBObject("_id", new ObjectId(userId));
		DBObject update = BasicDBObjectBuilder.start()
		.add("$inc", new BasicDBObject("answerCnt", 1))
		.get();
		talkersColl.findAndModify(talkerId, update);
	}
	
	public static void updateLogTime() {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		DBCollection loginsColl = getCollection(ApplicationDAO.LOGIN_HISTORY_COLLECTION);
		List<DBObject> talkersDBObjectList = talkersColl.find().toArray();
		DBObject talkerObject = null;
		DBObject talkerId = null,query=null;
		Date date = null;
		DBCursor loginCur = null;
		DBRef talkerRef =null;
		for (DBObject talkerDBObject : talkersDBObjectList) {
			date = new Date();
			talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talkerDBObject.get("_id").toString());
			query = BasicDBObjectBuilder.start().add("uid", talkerRef).get();
			loginCur = loginsColl.find(query).sort(new BasicDBObject("log_time", 1));
			if(loginCur != null && loginCur.hasNext()) {
				DBObject logObj = loginCur.next();
				date = (Date) logObj.get("log_time");
			}
			
			talkerObject = BasicDBObjectBuilder.start().add("log_time", date).get();
			talkerId = new BasicDBObject("_id", new ObjectId(talkerDBObject.get("_id").toString()));
			talkersColl.update(talkerId, new BasicDBObject("$set", talkerObject));
		}
	}
	
	public static void updateTalkerForImage() {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		List<DBObject> talkersDBObjectList = talkersColl.find().toArray();
		DBObject talkerObject = null;
		boolean isImage = false;
		DBObject talkerId = null;
		for (DBObject talkerDBObject : talkersDBObjectList) {
			isImage = isTalkerImage(talkerDBObject.get("_id").toString());
			talkerObject = BasicDBObjectBuilder.start()
			.add("isImg", isImage)
			.get();
			talkerId = new BasicDBObject("_id", new ObjectId(talkerDBObject.get("_id").toString()));
			talkersColl.update(talkerId, new BasicDBObject("$set", talkerObject));
		}
	}
	
	public static void updateTalkerForAnswerCount() {
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		List<DBObject> talkersDBObjectList = talkersColl.find().toArray();
		DBObject talkerObject = null;
		int ansCount = 0;
		DBObject talkerId = null;
		for (DBObject talkerDBObject : talkersDBObjectList) {
			ansCount = CommentsDAO.getTalkerNumberOfAnswers(talkerDBObject.get("_id").toString(),null);
			talkerObject = BasicDBObjectBuilder.start()
			.add("answerCnt", ansCount)
			.get();
			talkerId = new BasicDBObject("_id", new ObjectId(talkerDBObject.get("_id").toString()));
			talkersColl.update(talkerId, new BasicDBObject("$set", talkerObject));
		}
	}
	 public static void updateIds(){
			
		DBCollection talkersColl = getCollection(TALKERS_COLLECTION);
		List<DBObject> talkersDBObjectList = talkersColl.find().toArray();
		for (DBObject talkerDBObject : talkersDBObjectList) {
			
			String id=talkerDBObject.get("_id").toString();
			
			DBObject talkerId = new BasicDBObject("_id",id);
			
			DBObject talkerobj= talkersColl.findOne(talkerId);
			
			if(talkersColl.findOne(talkerId)!=null){
				
				System.out.println(id);
				play.Logger.info("Username :"+talkerobj.get("uname")+"   id :"+id);
				talkersColl.remove(talkerobj);
				
			    talkerobj.put("_id", new ObjectId(id));
				
				talkersColl.save(talkerobj);
			}
		}
		
	}

	 public static List<TalkerBean> getSortedTalkerList(List<TalkerBean> talkerList,boolean loggedIn) {
		 
		 /*Sort by login time*/
		 Collections.sort(talkerList, new Comparator<TalkerBean>() {
			@Override
			public int compare(TalkerBean o1, TalkerBean o2) {
				if(o1.getLogTime() != null) {
					if(o2.getLogTime() != null)
						return o2.getLogTime().compareTo(o1.getLogTime());
					else
						return 1;
				} else
					return 0;
			}
		});
		 
		 /* Sort using answer count*/
		 Collections.sort(talkerList, new Comparator<TalkerBean>() {
			@Override
			public int compare(TalkerBean o1, TalkerBean o2) {
				int user1 = 0;
				int user2 = 0;
				if(o1.getAnsCount() != 0)
					user1 = 1;
				if(o2.getAnsCount() != 0)
					user2 = 1;
				return user2 - user1;
			}
		 });
		 
		 /* Sort using answer count*/
		 Collections.sort(talkerList, new Comparator<TalkerBean>() {
			@Override
			public int compare(TalkerBean o1, TalkerBean o2) {
				int user1 = 0;
				int user2 = 0;
				int result = 0;
				if(o1.isImageFlag())
					user1 = 1;
				if(o2.isImageFlag())
					user2 = 1;
				result = user2 - user1;
				return result;
			}
		});

		 /*Sort by permissions only if not logged in*/
		 if(!loggedIn) {
			 Collections.sort(talkerList, new Comparator<TalkerBean>() {
				@Override
				public int compare(TalkerBean o1, TalkerBean o2) {

					int user1 = 0;
					int user2 = 0;
					if(o1.getPrivacyValue(PrivacyType.USERNAME) != null && PrivacyValue.PUBLIC.equals(o1.getPrivacyValue(PrivacyType.USERNAME)))
						user1 = 1;
					
					if(o1.getPrivacyValue(PrivacyType.PROFILE_IMAGE) != null && PrivacyValue.PUBLIC.equals(o1.getPrivacyValue(PrivacyType.PROFILE_IMAGE)))
						user1 = 1;
					else
						user1 = 0;
					
					if(o2.getPrivacyValue(PrivacyType.USERNAME) != null && PrivacyValue.PUBLIC.equals(o2.getPrivacyValue(PrivacyType.USERNAME)))
						user2 = 1;
					
					if(o2.getPrivacyValue(PrivacyType.PROFILE_IMAGE) != null && PrivacyValue.PUBLIC.equals(o2.getPrivacyValue(PrivacyType.PROFILE_IMAGE)))
						user2 = 1;
					else
						user2 = 0;
					return user2 - user1;
				}
			});
		 }
		return talkerList;
	 }
	 
	 public static String getTalkerConnection(String talkerId) {
		 DBCollection talkerColl = getCollection(TALKERS_COLLECTION);
		 DBObject query=new BasicDBObject("_id",new ObjectId(talkerId));
		String connection=talkerColl.findOne(query, new BasicDBObject("connection",1)).get("connection").toString();
		return connection;
		
	 }
}
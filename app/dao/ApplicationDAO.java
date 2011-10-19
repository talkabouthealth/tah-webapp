package dao;

import static util.DBUtil.createRef;
import static util.DBUtil.getCollection;
import static util.DBUtil.getString;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import logic.FeedsLogic;
import models.TalkerBean;
import play.templates.JavaExtensions;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.QueryOperators;

import controllers.Application;

public class ApplicationDAO {
	
	public static final String LOGIN_HISTORY_COLLECTION = "logins";
	public static final String UPDATES_EMAIL_COLLECTION = "emails";
	public static final String NAMES_COLLECTION = "names";
	public static final String WAITING_COLLECTION = "waiting";
	public static final String NEWSLETTER_COLLECTION = "newsletter";
	public static final String IPLIST_COLLECTION = "iplist";
	
	/**
	 * Save login record.
	 */
	public static void saveLogin(String talkerId, String from) {
		DBCollection loginsColl = getCollection(LOGIN_HISTORY_COLLECTION);
		
		DBRef talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talkerId);
		DBObject loginHistoryObject = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("log_time", new Date())
			.add("from", from)
			.get();

		loginsColl.save(loginHistoryObject);
	}
	
	/**
	 * Save email for updates & notifications
	 */
	public static void saveEmail(String email) {
		DBCollection emailsColl = getCollection(UPDATES_EMAIL_COLLECTION);
		emailsColl.save(new BasicDBObject("email", email));
	}
	
	/**
	 * Active users - users logged in after given time (or 1 month by default)
	 */
	public static List<TalkerBean> getActiveTalkers(Date afterTime) {
		DBCollection loginsColl = getCollection(LOGIN_HISTORY_COLLECTION);
		
		loginsColl.ensureIndex(new BasicDBObject("log_time", 1));
		
		if (afterTime == null) {
			Calendar monthBeforeNow = Calendar.getInstance();
			monthBeforeNow.add(Calendar.DAY_OF_MONTH, -30);
			afterTime = monthBeforeNow.getTime();
		}
		
		DBObject query = BasicDBObjectBuilder.start().add("log_time", new BasicDBObject("$gt", afterTime)).get();
		List<DBObject> loginsDBList = loginsColl.find(query).sort(new BasicDBObject("log_time", -1)).toArray();
		
		List<TalkerBean> activeTalkers = new ArrayList<TalkerBean>();
		for (DBObject loginDBObject : loginsDBList) {
			DBRef talkerDBRef = (DBRef)loginDBObject.get("uid");
			TalkerBean talker = new TalkerBean();
			talker.setId(talkerDBRef.getId().toString());
			if (talker != null &&  !activeTalkers.contains(talker)) {
				talker = TalkerDAO.parseTalker(talkerDBRef);
				if (talker != null &&  !talker.isSuspended() && !talker.isDeactivated()) {
					activeTalkers.add(talker);
				}
			}
		}
		return activeTalkers;
	}
	
	/**
	 * New users - users signed up in last 2 weeks
	 */
	public static List<TalkerBean> getNewTalkers() {
		//{"_id":{$gt : ObjectId("4e6efaff36882ba357358498")}}
		DBCollection loginsColl = getCollection(TalkerDAO.TALKERS_COLLECTION);
		
		Calendar twoWeeksBeforeNow = Calendar.getInstance();
		twoWeeksBeforeNow.add(Calendar.WEEK_OF_YEAR, -2);
		DBObject query = null;
		query = BasicDBObjectBuilder.start().add("timestamp", new BasicDBObject("$gt", twoWeeksBeforeNow.getTime())).get();

		DBObject fields = TalkerDAO.getBasicTalkerFields();
		
		List<DBObject> talkersDBList = loginsColl.find(query, fields).sort(new BasicDBObject("timestamp", -1)).toArray();
		
		List<TalkerBean> newTalkers = new ArrayList<TalkerBean>();
		for (DBObject talkerDBObject : talkersDBList) {
			TalkerBean talker = new TalkerBean();
			talker.setId(getString(talkerDBObject, "_id"));
			if (!newTalkers.contains(talker)) {
				talker.parseBasicFromDB(talkerDBObject);
				if (!talker.isSuspended()) {
					newTalkers.add(talker);
				}
			}
		}
		
		return newTalkers;
	}
	
	public static List<TalkerBean> getRecommendedTalker(boolean memberFlag){
		List<TalkerBean> newTalkers = new ArrayList<TalkerBean>();
		
		//TODO
		DBCollection loginsColl = getCollection(LOGIN_HISTORY_COLLECTION);
		loginsColl.ensureIndex(new BasicDBObject("log_time", 1));

		Calendar oneWeekBeforeNow = Calendar.getInstance();
		oneWeekBeforeNow.add(Calendar.WEEK_OF_YEAR, -2);
		Date afterTime = oneWeekBeforeNow.getTime();
		
		if (afterTime == null) {
			Calendar monthBeforeNow = Calendar.getInstance();
			monthBeforeNow.add(Calendar.DAY_OF_MONTH, -30);
			afterTime = monthBeforeNow.getTime();
		}

		DBObject query = BasicDBObjectBuilder.start().add("log_time", new BasicDBObject("$gt", afterTime)).get();
		List<DBObject> loginsDBList = loginsColl.find(query).sort(new BasicDBObject("log_time", -1)).toArray();
		List<String> list = TalkerBean.PROFESSIONAL_CONNECTIONS_LIST;

		for (DBObject loginDBObject : loginsDBList) {
			DBRef talkerDBRef = (DBRef)loginDBObject.get("uid");
			TalkerBean talker = new TalkerBean();
			talker.setId(talkerDBRef.getId().toString());
			if (talker != null &&  !newTalkers.contains(talker)) {
				talker = TalkerDAO.parseTalker(talkerDBRef);
				if (talker != null &&  !talker.isSuspended() && !talker.isDeactivated() && !talker.isAdmin()) {
					if(memberFlag){
						if(list.contains(talker.getConnection()) && talker.isConnectionVerified()){
							newTalkers.add(talker);
						}
					}else{
						if(!list.contains(talker.getConnection())){
							newTalkers.add(talker);	
						}
					}
				}
			}
		}
		//TODO
		return newTalkers;
	}

	/**
	 * New users/Experts - users signed up or logged in date descending order
	 * Using to 20 users only for the list on profile page for recommendations.
	 */
	public static List<TalkerBean> getTalkersInOrder(TalkerBean talkerBean,boolean memberFlag) {
		DBCollection loginsColl = getCollection(TalkerDAO.TALKERS_COLLECTION);
		DBCollection loginHistoryCollection = getCollection(LOGIN_HISTORY_COLLECTION);
		List<String> list = TalkerBean.PROFESSIONAL_CONNECTIONS_LIST;
		BasicDBList basicDBList = new BasicDBList();

		List<String> cat = FeedsLogic.getCancerType(talkerBean);
		
		for (String element : list) {
			basicDBList.add(element);
		}
		DBObject query = null;
		if(memberFlag){
			query = BasicDBObjectBuilder.start()
				.add("connection",new BasicDBObject(QueryOperators.IN, basicDBList ))
				.add("category", new BasicDBObject("$in", cat) )
				.add("connection_verified",true)
				.get();
		}else{
			query = BasicDBObjectBuilder.start()
				.add("connection",new BasicDBObject(QueryOperators.NIN, basicDBList ))
				.add("category", new BasicDBObject("$in", cat) )
				.get();
		}
		
		DBObject fields = TalkerDAO.getBasicTalkerFields();
		List<DBObject> talkersDBList = loginsColl.find(query, fields).toArray();
		List<TalkerBean> newTalkers = new ArrayList<TalkerBean>();
		for (DBObject talkerDBObject : talkersDBList) {
			TalkerBean talker = new TalkerBean();
			talker.setId(getString(talkerDBObject, "_id"));
			if (!newTalkers.contains(talker)) {
				talker.parseBasicFromDB(talkerDBObject);
				if (!(talker.isSuspended() || talker.isDeactivated() || talker.isAdmin() || talkerBean.equals(talker) || talkerBean.getFollowingList().contains(talker))){
					DBRef talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talker.getId());
					query = BasicDBObjectBuilder.start().add("uid", talkerRef).get();
					List<DBObject> loginTime= loginHistoryCollection.find(query).toArray();
					if(loginTime.size()>=2){
						DBObject logTimeObj = loginTime.get(0);
						talker.setRegDate((Date)logTimeObj.get("log_time"));
						newTalkers.add(talker);
					}
				}
			}
		}
		
		//Collections.sort(newTalkers, new TalkerBean());
		return newTalkers;
	}
	
	/**
	 * Checks if given userName already exists or reserved.
	 */
	public static boolean isURLNameExists(String userName) {
		//String uNameL = userName;
		//String uNameU = userName;
		if (userName != null) {
			userName = userName.toLowerCase(); 
			//uNameL = userName.toLowerCase();
			//uNameU = userName.toUpperCase();
		}
		if (Application.RESERVED_WORDS.contains(userName)) {
			return true;
		}
		
		DBCollection namesColl = getCollection(NAMES_COLLECTION);
		DBObject query = new BasicDBObject("name", userName);
		//query.put("name", uNameL);
		//query.put("name", uNameU);
		return namesColl.findOne(query) != null;
	}
	
	/**
	 * Creates from simple userName/convo title/topic title/etc. correct URL name.
	 * Later this URL name is used at 'http://talkabouthealth.com/{URL_name}'.
	 * 
	 * If the same name already exists - adds "_X" to the name, where X - counter.
	 */
	public static String createURLName(String name, boolean isUsername) {
		if (!isUsername) {
			//We convert name to url for all objects except username
			name = prepareName(name);
		}
		
		DBCollection namesColl = getCollection(NAMES_COLLECTION);
		DBObject query = new BasicDBObject("name", name);
		DBObject fields = new BasicDBObject("cnt", 1);
		//if we find the same name - increment counter
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
				return createURLName(name, isUsername);
			}
			return name;
		}
		else {
			//duplicate name
			return name+"_"+(cnt-1);
		}
	}
	public static String createURLName(String name) {
		return createURLName(name, false);
	}
	
	/**
	 * Make URL-like name (without not-allowed symbols).
	 * Also replaces "_" as it's used for duplicate names.
	 * Build upon Play! JavaExtensions.slugify() method.
	 */
	private static String prepareName(String name) {
		name = JavaExtensions.noAccents(name);
        return name.replaceAll("[\\W_]", "-").replaceAll("-{2,}", "-").replaceAll("-$", "").toLowerCase();
	}
	
	
	/* ----------------- Waiting list actions --------------- */
	
	public static void addToWaitingList(String community, String email) {
		DBCollection waitingColl = getCollection(WAITING_COLLECTION);
		
		DBObject waitingDBObject = BasicDBObjectBuilder.start()
			.add("community", community)
			.add("email", email)
			.get();
		waitingColl.save(waitingDBObject);
	}
	
	/**
	 * Loads all communities that users are waiting for and
	 * number of users waiting for each community.
	 * 
	 * @return
	 */
	public static Map<String, Integer> getWaitingCommunitiesInfo() {
		DBCollection waitingColl = getCollection(WAITING_COLLECTION);
		List<DBObject> waitingDBList = waitingColl.find().toArray();
		
		Map<String, Integer> waitingInfo = new HashMap<String, Integer>();
		for (DBObject waitingDBObject : waitingDBList) {
			String community = (String)waitingDBObject.get("community");
			if (waitingInfo.containsKey(community)) {
				int numOfWaiting = waitingInfo.get(community);
				waitingInfo.put(community, numOfWaiting+1);
			}
			else {
				waitingInfo.put(community, 1);
			}
		}
		return waitingInfo;
	}

	/* ----------------- Newsletter Signup action --------------- */
	
	public static void addToNewsLetter(String email) {

		DBCollection waitingColl = getCollection(NEWSLETTER_COLLECTION);

		DBObject waitingDBObject = BasicDBObjectBuilder.start()
			.add("email", email)
			.get();
		waitingColl.save(waitingDBObject);
	}
	
	/**
	 * Checks if given email already exists or reserved for newsletter.
	 */
	public static boolean isEmailExists(String userName) {
		if (userName != null) {
			userName = userName.toLowerCase();
		}
		if (Application.RESERVED_WORDS.contains(userName)) {
			return true;
		}
		
		DBCollection namesColl = getCollection(NEWSLETTER_COLLECTION);
		DBObject query = new BasicDBObject("email", userName);
		return namesColl.findOne(query) != null;
	}
	
	/**
	 * Check if the ip address is available in the iplist database table.
	 * @param ip
	 * @param duration
	 * @return boolean
	 */
	public static boolean isIpUsed(String ip, int duration){
		DBCollection namesColl = getCollection(IPLIST_COLLECTION);
		Calendar dayBeforeNow = Calendar.getInstance();
		dayBeforeNow.add(Calendar.HOUR_OF_DAY, duration);
		BasicDBObjectBuilder queryBuilder = BasicDBObjectBuilder.start().add("ipaddr", ip).add("time", new BasicDBObject("$gt", dayBeforeNow.getTime()));
		return namesColl.findOne(queryBuilder.get()) == null;
	}
	
	/**
	 * Add user ip in the iplist database table. 
	 * @param ip
	 * @return boolean
	 */
	public static boolean addUserIp(String ip){
		DBCollection namesColl = getCollection(IPLIST_COLLECTION);
		DBObject userIpDBObject = BasicDBObjectBuilder.start()
		.add("ipaddr", ip)
		.add("time", Calendar.getInstance().getTime())
		.get();
		namesColl.save(userIpDBObject);
		return true;
	}
}
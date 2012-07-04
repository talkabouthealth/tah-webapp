package dao;

import groovy.util.ObjectGraphBuilder.DefaultReferenceResolver;
import static util.DBUtil.createRef;
import static util.DBUtil.getBoolean;
import static util.DBUtil.getCollection;
import static util.DBUtil.getString;
import static util.DBUtil.setToDB;

import java.awt.image.DataBufferByte;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import logic.ConversationLogic;
import logic.FeedsLogic;
import logic.TalkerLogic;
import models.CommentBean;
import models.ConversationBean;
import models.DiseaseBean;
import models.MessageBean;
import models.TalkerBean;
import models.ConversationBean;
import models.TopicBean;
import models.actions.Action.ActionType;

import org.bson.types.ObjectId;

import play.Logger;

import util.DBUtil;
import util.CommonUtil;
import util.EmailUtil;
import util.EmailUtil.EmailTemplate;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;

import controllers.AnswerNotification;

import static util.DBUtil.*;

public class ConversationDAO {
	
	public static final String CONVERSATIONS_COLLECTION = "convos";
	
	//------------------- Save/Update methods -----------------------
	
	/**
	 * Save conversation.
	 * Tries to insert convo 5 times - to prevent not-unique 'tid'
	 */
	public static void save(ConversationBean convo) {
		int tid = saveInternal(convo, 5);
		
		if (tid != -1) {
			convo.setTid(tid);
		} else {
			Logger.error("Couldn't save Conversation");
			return;
		}
	}
	
	/**
	 * TODO: later - better implementation of synchro saving? Maybe use some internal Mongo functionality?
	 * Tries to insert convo 'count' times (in case of duplicate key error on 'tid' field)
	 * Returns -1 in case of failure
	 */
	private static int saveInternal(ConversationBean convo, int count) {
		if (count == 0) {
			return -1;
		}
		
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		//get last tid
		DBCursor convosCursor = 
			convosColl.find(null, new BasicDBObject("tid", "")).sort(new BasicDBObject("tid", -1)).limit(1);
		int tid = convosCursor.hasNext() ? ((Integer)convosCursor.next().get("tid")) + 1 : 1;
		
		DBRef talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, convo.getUid());
		DBObject convoObject = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("tid", tid)
			.add("type", convo.getConvoType().toString())
			.add("topic", convo.getTopic())
			.add("cr_date", convo.getCreationDate())
			.add("main_url", convo.getMainURL())
			.add("topics", convo.topicsToDB())
			.add("details", convo.getDetails())
			
			.add("opened", convo.isOpened())
			
			.add("bitly", convo.getBitly())
			.add("bitly_chat", convo.getBitlyChat())
			.add("category", convo.getCategory())
			.get();

		//Only with SAFE WriteConcern we receive exception on duplicate key
		try {
			convosColl.save(convoObject, WriteConcern.SAFE);
		}
		catch (MongoException me) {
			//E11000 duplicate key error index
			Logger.error(me,"ConversationDAO.java : saveInternal");
			if (me.getCode() == 11000) {
				Logger.error("Duplicate key error while saving convo");
				return saveInternal(convo, --count);
			}
			
		}
		
		String convoId = convoObject.get("_id").toString();
		convo.setId(convoId);
		
		return (Integer)convoObject.get("tid");
	}
	
	public static void updateConvo(ConversationBean convo) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		List<DBRef> sumContributorsDBList = new ArrayList<DBRef>();
		if (convo.getSumContributors() != null) {
			for (TalkerBean talker : convo.getSumContributors()) {
				DBRef talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talker.getId());
				sumContributorsDBList.add(talkerRef);
			}
		}
		
		DBRef mergedWithRef = null;
		if (convo.getMergedWith() != null) {
			mergedWithRef = createRef(ConversationDAO.CONVERSATIONS_COLLECTION, convo.getMergedWith());
		}
		DBObject convoObject = BasicDBObjectBuilder.start()
			.add("topic", convo.getTopic())
			.add("main_url", convo.getMainURL())
			.add("old_names", setToDB(convo.getOldNames()))
			.add("cr_date", convo.getCreationDate())
			
			.add("merged_with", mergedWithRef)
			
			.add("deleted", convo.isDeleted())
			.add("opened", convo.isOpened())
			
			.add("details", convo.getDetails())
			.add("topics", convo.topicsToDB())
			.add("related_convos", convo.relatedConvosToDB())
			.add("followup_convos", convo.followupConvosToDB())
			.add("summary", convo.getSummary())
			.add("sum_authors", sumContributorsDBList)
			
			.add("bitly", convo.getBitly())
			.add("bitly_chat", convo.getBitlyChat())
			
			.add("from", convo.getFrom())
			.add("from_id", convo.getFromId())
			
			.add("modified_date", new Date())
			.add("question_state", convo.getQuestionState())
			
			.add("category", convo.getCategory())
			.add("other_disease_categories", convo.getOtherDiseaseCategories())
			.add("answer_subscription", convo.getSubcribeEmails())
			.get();
		
		DBObject convoId = new BasicDBObject("_id", new ObjectId(convo.getId()));
		convosColl.update(convoId, new BasicDBObject("$set", convoObject));
	}
	
	
	//----------------------- Query methods ------------------------
	public static ConversationBean getById(String convoId) {
		
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject query = new BasicDBObject("_id", new ObjectId(convoId));
		DBObject convoDBObject = convosColl.findOne(query);
		
		return parseConvoFromDBObject(convoDBObject, false);
	}
	
	public static ConversationBean getByIdBasicQuestion(String convoId) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);

		DBObject fields = getBasicConversationFields();
		DBObject query = new BasicDBObject("_id", new ObjectId(convoId));
		DBObject convoDBObject = convosColl.findOne(query, fields);
		
		return parseConvoFromDBObject(convoDBObject, true);
	}
	
	public static ConversationBean getByIdBasic(String convoId) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);

		DBObject fields = getBasicConversationFields();
		
		DBObject query = new BasicDBObject("_id", new ObjectId(convoId));
		DBObject convoDBObject = convosColl.findOne(query, fields);
		
		return parseConvoFromDBObject(convoDBObject, true);
	}

	/**
	 * @return
	 */
	public static DBObject getBasicConversationFields() {
		DBObject fields = BasicDBObjectBuilder.start()
			.add("summary", 0)
			.add("sum_authors", 0)
			.add("related_convos", 0)
			.add("followup_convos", 0)
			.add("messages", 0)
			
			.add("summary", 0)
			.add("sum_authors", 0)
			.add("related_convos", 0)
			.add("followup_convos", 0)
			//.add("uid", 0)
			.add("category", 0)
		
			.get();
		return fields;
	}

	public static ConversationBean getByTid(Integer tid) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject query = new BasicDBObject("tid", tid);
		DBObject convoDBObject = convosColl.findOne(query);
		
		return parseConvoFromDBObject(convoDBObject, false);
	}
	
	public static ConversationBean getByTitle(String title) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject query = new BasicDBObject("topic", title);
		DBObject convoDBObject = convosColl.findOne(query);
		
		return parseConvoFromDBObject(convoDBObject, true);
	}
	
	public static ConversationBean getByFromInfo(String from, String fromId) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject query = BasicDBObjectBuilder.start()
			.add("from", from)
			.add("from_id", fromId)
			.get();
		DBObject convoDBObject = convosColl.findOne(query);
		
		return parseConvoFromDBObject(convoDBObject, true);
	}
	
	/**
	 * Find by main URL (current) or old urls.
	 */
	public static ConversationBean getByURL(String url) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject query = BasicDBObjectBuilder.start()
			.add("$or", 
				Arrays.asList(
						new BasicDBObject("main_url", url),
						new BasicDBObject("old_names.url", url)
					)
			)
			.get();
		DBObject convoDBObject = convosColl.findOne(query);
		
		return parseConvoFromDBObject(convoDBObject, false);
	}
	
	/**
	 * @param convoDBObject
	 * @return
	 */
	public static ConversationBean parseConvoFromDBObject(DBObject convoDBObject, boolean onlyBasicInfo) {
		if (convoDBObject == null) {
			return null;
		}
		ConversationBean convo = new ConversationBean();
		if (onlyBasicInfo) {
			convo.parseBasicFromDB(convoDBObject);
		}
		else {
			convo.parseFromDB(convoDBObject);
		}
		return convo;
	}
	
	/**
	 * Get all conversations (deleted also).
	 */
	public static List<ConversationBean> loadAllConversations(boolean basicInfo) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		convosColl.ensureIndex(new BasicDBObject("cr_date", 1));
		
		List<DBObject> convosDBList = null;
		if (basicInfo) {
			DBObject fields = getBasicConversationFields();
			
			convosDBList=new ArrayList<DBObject>();//convosDBList = convosColl.find(null, fields).sort(new BasicDBObject("cr_date", -1)).toArray();
			DBCursor convoCur=convosColl.find(null, fields).sort(new BasicDBObject("cr_date", -1));
			while(convoCur.hasNext()){
				convosDBList.add(convoCur.next());
			}
			
		}
		else {
			convosDBList=new ArrayList<DBObject>();//convosDBList = convosColl.find().sort(new BasicDBObject("cr_date", -1)).toArray();
			DBCursor convoCur=convosColl.find().sort(new BasicDBObject("cr_date", -1));
			while(convoCur.hasNext()){
				convosDBList.add(convoCur.next());
			}
			
		}
		
		List<ConversationBean> convosList = new ArrayList<ConversationBean>();
		for (DBObject convoDBObject : convosDBList) {
	    	convosList.add(parseConvoFromDBObject(convoDBObject, basicInfo));
		}
		return convosList;
	}
	public static List<ConversationBean> loadAllConversations() {
		return loadAllConversations(false);
	}
	
	
	/**
	 * last updated convos for indexer
	 * 
	 * @return convolist
	 */
	
	public static List<ConversationBean> loadUpdatedConversations(int limit) {
		boolean basicInfo=true;
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		convosColl.ensureIndex(new BasicDBObject("cr_date", 1));
		
		Calendar cal= Calendar.getInstance();
		cal.add(Calendar.MINUTE, -limit);
		Date date=cal.getTime();
		BasicDBObject time = new BasicDBObject("$gt", date);
		
		DBObject query = BasicDBObjectBuilder.start()
		.add("cr_date", time)
		.get();
		
		List<DBObject> convosDBList = null;
		if (basicInfo) {
			DBObject fields = getBasicConversationFields();
			convosDBList=new ArrayList<DBObject>();//	convosDBList = convosColl.find(query, fields).sort(new BasicDBObject("cr_date", -1)).toArray();
			DBCursor convoCur=convosColl.find(query, fields).sort(new BasicDBObject("cr_date", -1));
			while(convoCur.hasNext()){
				convosDBList.add(convoCur.next());
			}
		
		}
		else {
			convosDBList=new ArrayList<DBObject>();//convosColl.find(query).sort(new BasicDBObject("cr_date", -1)).toArray();
			DBCursor convoCur=convosColl.find(query).sort(new BasicDBObject("cr_date", -1));
			while(convoCur.hasNext()){
				convosDBList.add(convoCur.next());
			}
				
		}
		
		ConversationBean convo = null;
		List<ConversationBean> convosList = new ArrayList<ConversationBean>();
		for (DBObject convoDBObject : convosDBList) {
	    	convo = new ConversationBean();
			convo.setId(convoDBObject.get("_id").toString());
			convo.setTopic((String)convoDBObject.get("topic"));
			convo.setMainURL((String)convoDBObject.get("main_url"));
			convo.setDeleted(getBoolean(convoDBObject, "deleted"));
			convo.setBitly((String)convoDBObject.get("bitly"));
			convo.setBitlyChat((String)convoDBObject.get("bitly_chat"));
			convo.setCategory((String)convoDBObject.get("category"));
	    	Collection<String> otherDiseaseCategories = (Collection<String>)convoDBObject.get("other_disease_categories");
			if (otherDiseaseCategories != null) {
				convo.setOtherDiseaseCategories(otherDiseaseCategories.toArray(new String[]{}));
			}
			convosList.add(convo);
		}
		return convosList;
	}
	
	
	
	/**
	 * Get number of all conversations in this community
	 */
	public static int getNumberOfConversations() {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		int numberOfQuestions = convosColl.find().size();
		return numberOfQuestions;
	}
	
	/**
	 * Popularity is based on number of views.
	 */
	public static List<ConversationBean> loadPopularConversations(String convoId) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		convosColl.ensureIndex(new BasicDBObject("views", -1));
		
		//added for paging
		int views = 0;
		if (convoId != null) {
			views = getViews(convoId);
		}
		
		DBObject fields = getBasicConversationFields();
		
		//checking for views 
		BasicDBObjectBuilder queryBuilder =  BasicDBObjectBuilder.start()
		.add("deleted", new BasicDBObject("$ne", true));
	
		if (views != 0) {
			queryBuilder.add("views", new BasicDBObject("$lte", views));
		}
	
		DBObject query = queryBuilder.get();
	
		/*DBObject query = BasicDBObjectBuilder.start()
			.add("deleted", new BasicDBObject("$ne", true))
			.get();*/
		
		List<DBObject> convosDBList=new ArrayList<DBObject>();//convosColl.find(query, fields).sort(new BasicDBObject("views", -1)).limit(20).toArray();
		DBCursor convoCur=convosColl.find(query, fields).sort(new BasicDBObject("views", -1)).limit(20);
		while(convoCur.hasNext()){
			convosDBList.add(convoCur.next());
		}
			
		
		List<ConversationBean> convosList = new ArrayList<ConversationBean>();
		for (DBObject convoDBObject : convosDBList) {
			ConversationBean convo = new ConversationBean();
			convo.parseBasicFromDB(convoDBObject);
			convo.setComments(CommentsDAO.loadConvoAnswers(convo.getId()));
	    	convosList.add(convo);
		}
		
		return convosList;
	}
	
	public static int getViews(String convoId) {
		DBCollection activitiesColl = getCollection(CONVERSATIONS_COLLECTION);

		DBObject query = new BasicDBObject("_id", new ObjectId(convoId));
		DBObject actionDBObject = activitiesColl.findOne(query);
		if (actionDBObject != null) {
			return (Integer)actionDBObject.get("views");
		}
		return 0;
	}
	
	/**
	 * Get all current Live Chats.
	 */
	public static List<ConversationBean> getLiveConversations() {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject query = BasicDBObjectBuilder.start()
			.add("talkers",
				BasicDBObjectBuilder.start()
					.add("$exists", true)
					.add("$not", new BasicDBObject("$size", 0))
					.get()
				)
			.add("deleted", new BasicDBObject("$ne", true))
			.get();
		
		List<DBObject> convosDBList=new ArrayList<DBObject>();//convosColl.find(query).sort(new BasicDBObject("cr_date", -1)).toArray();
		DBCursor convoCur=convosColl.find(query).sort(new BasicDBObject("cr_date", -1));
		while(convoCur.hasNext()){
			convosDBList.add(convoCur.next());
		}
			
		
		List<ConversationBean> convosList = new ArrayList<ConversationBean>();
		for (DBObject convoDBObject : convosDBList) {
			ConversationBean convo = new ConversationBean();
			convo.parseBasicFromDB(convoDBObject);
			
			//For displaying answers sequence wise
			List<CommentBean> answerList = CommentsDAO.loadConvoAnswers(convo.getId());
			List<CommentBean> commentList = answerList;
			for(int index = 0; index < answerList.size(); index++){
				CommentBean commentBean= answerList.get(index);
				if(commentBean.getText().contains(convo.getTopic())){
					commentList.remove(index);
					commentList.add(0, commentBean);
				}
			}
			
			convo.setComments(commentList);
	    	convosList.add(convo);
		}
		return convosList;
	}
	
	/**
	 * Opened Questions - not answered, which are marked with 'opened' flag.
	 */
	public static List<ConversationBean> getOpenQuestions(TalkerBean talker, boolean loggedIn) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		List<String> cat = new ArrayList<String>();
		DBObject query = null;
		if(loggedIn) {
			cat = FeedsLogic.getCancerType(talker);
			query = BasicDBObjectBuilder.start()
			.add("opened", true)
			.add("deleted", new BasicDBObject("$ne", true))
			.add("category", new BasicDBObject("$in", cat))
			.get();
		} else {
			query = BasicDBObjectBuilder.start()
			.add("opened", true)
			.add("deleted", new BasicDBObject("$ne", true))
			.get();
		}
		
		List<DBObject> convosDBList=new ArrayList<DBObject>();//convosColl.find(query).sort(new BasicDBObject("cr_date", -1)).toArray();
		DBCursor convoCur=convosColl.find(query).sort(new BasicDBObject("cr_date", -1));
		while(convoCur.hasNext()){
			convosDBList.add(convoCur.next());
		}
		
		 
		
		List<ConversationBean> convosList = new ArrayList<ConversationBean>();
		for (DBObject convoDBObject : convosDBList) {
			ConversationBean convo = new ConversationBean();
			convo.parseBasicFromDB(convoDBObject);
	    	convosList.add(convo);
		}
		cat.clear();
		return convosList;
	}
	
	//------------------- Other ----------------------
	
	public static int getNumOfStartedConvos(String talkerId) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBRef talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talkerId);
		DBObject query = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("deleted", new BasicDBObject("$ne", true))
			.get();
		int numOfStartedConvos = convosColl.find(query).count();
		return numOfStartedConvos;
	}
	
	/**
	 * @param talkerId
	 * @param nextConvoId Id of the last convo from the previous page
	 * @param numOfConversations Number of convos to load (-1 for all)
	 * @return
	 */
	public static List<ConversationBean> getStartedConvos(String talkerId, 
			String nextConvoId, int numOfConversations) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		Date firstConvoTime = null;
		if (nextConvoId != null) {
			ConversationBean convo = TalkerLogic.loadConvoFromCache(nextConvoId);
			firstConvoTime = convo.getCreationDate();
		}
		
		//List<String> cat = FeedsLogic.getCancerType(TalkerDAO.getById(talkerId));
		
		convosColl.ensureIndex(new BasicDBObject("cr_date", 1));
		
		DBObject fields = getBasicConversationFields();
		DBRef talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talkerId);
		BasicDBObjectBuilder queryBuilder = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			//.add("category", new BasicDBObject("$in", cat) )
			.add("deleted", new BasicDBObject("$ne", true));
		if (firstConvoTime != null) {
			queryBuilder.add("cr_date", new BasicDBObject("$lt", firstConvoTime));
		}

		DBCursor dbCursor = convosColl.find(queryBuilder.get(), fields).sort(new BasicDBObject("cr_date", -1));
		if (numOfConversations != -1) {
			dbCursor = dbCursor.limit(numOfConversations);
		}
		
		List<ConversationBean> convosList = new ArrayList<ConversationBean>();
		while (dbCursor.hasNext()) {
			ConversationBean convo = new ConversationBean();
			convo.parseBasicFromDB(dbCursor.next());
	    	convosList.add(convo);
		}
		CommonUtil.log("ConversationDAO.getStartedConvos", ""+dbCursor.size());
		//Logger.info("getStartedConvos - "+ dbCursor.size());
		return convosList;
	}
	
	
	/**
	 * Returns followers of the given conversation
	 */
	public static List<TalkerBean> getConversationFollowers(String convoId) {
    	DBCollection talkersColl = getCollection(TalkerDAO.TALKERS_COLLECTION);
    	
    	DBObject query = new BasicDBObject("following_convos", convoId);
    	List<DBObject> followersDBList = null;
    	try {
    		followersDBList=new ArrayList<DBObject>();//followersDBList = talkersColl.find(query).toArray();
			DBCursor convoCur=talkersColl.find(query);
			while(convoCur.hasNext()){
				followersDBList.add(convoCur.next());
			}
    		
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	List<TalkerBean> followers = new ArrayList<TalkerBean>();
    	for (DBObject followerDBObject : followersDBList) {
    		TalkerBean followerTalker = new TalkerBean();
    		followerTalker.parseBasicFromDB(followerDBObject);
			followers.add(followerTalker);
    	}
		return followers;
	}
	
	/**
	 * Get id of the last created conversation.
	 */
	public static String getLastConvoId() {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject convoDBObject = convosColl.find().sort(new BasicDBObject("cr_date", -1)).next();
		if (convoDBObject == null) {
			return null;
		}
		else {
			return convoDBObject.get("_id").toString();
		}
	}
	
	public static List<Map<String, String>> loadConvosForDashboard(boolean withNotifications) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		DateFormat dateFormat = new SimpleDateFormat("MM.dd.yyyy HH:mm:ss");
		
		DBObject query = BasicDBObjectBuilder.start()
			.add("deleted", new BasicDBObject("$ne", true))
			.get();
		List<DBObject> topicsDBList=new ArrayList<DBObject>();//List<DBObject> topicsDBList = convosColl.find(query).sort(new BasicDBObject("cr_date", -1)).toArray();
		DBCursor convoCur=convosColl.find(query).sort(new BasicDBObject("cr_date", -1));
		while(convoCur.hasNext()){
			topicsDBList.add(convoCur.next());
		}
		
		
		List<Map<String, String>> topicsInfoList = new ArrayList<Map<String,String>>();
		for (DBObject topicDBObject : topicsDBList) {
			Map<String, String> topicInfoMap = new HashMap<String, String>();
			
			//noti_history.noti_time is null
			int numOfNotifications = NotificationDAO.getNotiNumByConvo(topicDBObject.get("_id").toString());
			if (withNotifications && numOfNotifications == 0) {
				continue;
			}
			else if (!withNotifications && numOfNotifications > 0) {
				continue;
			}
			
			//convert data to map
			DBRef talkerRef = (DBRef)topicDBObject.get("uid");
			DBObject talkerDBObject = talkerRef.fetch();
			
			topicInfoMap.put("topicId", topicDBObject.get("_id").toString());
			topicInfoMap.put("topic", (String)topicDBObject.get("topic"));
			Date creationDate = (Date)topicDBObject.get("cr_date");
			topicInfoMap.put("cr_date", dateFormat.format(creationDate));
			
			topicInfoMap.put("uid", talkerDBObject.get("_id").toString());
			topicInfoMap.put("uname", talkerDBObject.get("uname").toString());
			topicInfoMap.put("gender", (String)talkerDBObject.get("gender"));
			
			if (withNotifications) {
				int notificationsNum = NotificationDAO.getNotiNumByConvo(topicInfoMap.get("topicId"));
				topicInfoMap.put("notificationsNum", ""+notificationsNum);
			}
			
			topicsInfoList.add(topicInfoMap);
		}
		
		return topicsInfoList;
	}
	
	public static void incrementConvoViews(String convoId) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject convoIdDBObject = new BasicDBObject("_id", new ObjectId(convoId));
		convosColl.update(convoIdDBObject, 
				new BasicDBObject("$inc", new BasicDBObject("views", 1)));
	}
	
	/**
	 * Load convos for given activity type - started, joined, etc.
	 */
	public static Set<ConversationBean> loadConversations(String talkerId, ActionType type) {
		DBCollection activitiesColl = getCollection(ActionDAO.ACTIVITIES_COLLECTION);
		
		DBRef talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talkerId);
		DBObject query = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("type", type.toString())
			.get();
		List<DBObject> activitiesDBList=new ArrayList<DBObject>();//activitiesColl.find(query, new BasicDBObject("convoId", 1)).toArray();
		DBCursor convoCur=activitiesColl.find(query, new BasicDBObject("convoId", 1));
		while(convoCur.hasNext()){
			activitiesDBList.add(convoCur.next());
		}
			
		
		//prepare list of matching conversations
		List<ObjectId> convoIds = new ArrayList<ObjectId>();
		for (DBObject activityDBObject : activitiesDBList) {
			DBRef convoRef = (DBRef)activityDBObject.get("convoId");
			convoIds.add((ObjectId)convoRef.getId());
		}
		
		return getConvosByIds(convoIds, null, -1);
	}

	/**
	 * @param convoIds
	 * @return
	 */
	public static Set<ConversationBean> getConvosByIds(List<ObjectId> convoIds, 
			String nextConvoId, int numOfConversations) {
		DBCollection convosColl = getCollection(ConversationDAO.CONVERSATIONS_COLLECTION);
		convosColl.ensureIndex(new BasicDBObject("cr_date", -1));
		
		Date firstConvoTime = null;
		if (nextConvoId != null) {
			ConversationBean convo = TalkerLogic.loadConvoFromCache(nextConvoId);
			firstConvoTime = convo.getCreationDate();
		}
		
		DBObject fields = getBasicConversationFields();
		BasicDBObjectBuilder queryBuilder = BasicDBObjectBuilder.start()
			.add("_id", new BasicDBObject("$in", convoIds))
			.add("deleted", new BasicDBObject("$ne", true));
		if (firstConvoTime != null) {
			queryBuilder.add("cr_date", new BasicDBObject("$lt", firstConvoTime));
		}
		
		DBCursor dbCursor = 
			convosColl.find(queryBuilder.get(), fields).sort(new BasicDBObject("cr_date", -1));
		if (numOfConversations != -1) {
			dbCursor = dbCursor.limit(numOfConversations);
		}
		
		Set<ConversationBean> convosSet = new LinkedHashSet<ConversationBean>();
		while (dbCursor.hasNext()) {
			ConversationBean convo = new ConversationBean();
			convo.parseBasicFromDB(dbCursor.next());
			convosSet.add(convo);
		}
		CommonUtil.log("ConversationDAO.getConvosByIds", ""+dbCursor.size());
		//Logger.info("getConvosByIds - "+ dbCursor.size());
		return convosSet;
	}
	
	/*
	 * We need this because some convo could be deleted.
	 */
	public static int getNumOfConvosByIds(List<ObjectId> convoIds) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject query = BasicDBObjectBuilder.start()
			.add("_id", new BasicDBObject("$in", convoIds))
			.add("deleted", new BasicDBObject("$ne", true))
			.get();
		
		int numOfConvos = convosColl.find(query).count();
		return numOfConvos;
	}
	
	
	
	/**
	 * Get all conversations that have at least one given topic.
	 */
	public static Set<DBRef> getConversationsByTopics(Set<TopicBean> topics) {
		Set<DBRef> convosDBSet = new HashSet<DBRef>();
		if (topics == null || topics.size() == 0) {
			return convosDBSet;
		}
		
		List<DBRef> allTopics = new ArrayList<DBRef>();
		for (TopicBean topic : topics) {
			//TopicDAO.loadSubTopicsAsTree(allTopics, topic);
			DBRef topicRef = createRef(TopicDAO.TOPICS_COLLECTION, topic.getId());
			allTopics.add(topicRef);
		}
		
		DBCollection convosColl = getCollection(ConversationDAO.CONVERSATIONS_COLLECTION);
		DBObject fields = new BasicDBObject("_id", "1");
		DBObject query = new BasicDBObject("topics", new BasicDBObject("$in", allTopics));
		
		List<DBObject> convosDBList=new ArrayList<DBObject>();//convosColl.find(query, fields).toArray();
		DBCursor convoCur=convosColl.find(query, fields);
		while(convoCur.hasNext()){
			convosDBList.add(convoCur.next());
		}
		
		allTopics.clear();
		
		for (DBObject convoDBObject : convosDBList) {
			convosDBSet.add(createRef(ConversationDAO.CONVERSATIONS_COLLECTION, getString(convoDBObject, "_id")));
		}
		
		return convosDBSet;
	}
	
	/**
	 * Load conversations that have given topic.
	 */
	public static List<ConversationBean> loadConversationsByTopic(String topicId) {
		DBCollection convosColl = getCollection(ConversationDAO.CONVERSATIONS_COLLECTION);
		
		DBRef topicRef = createRef(TopicDAO.TOPICS_COLLECTION, topicId);
		DBObject query = BasicDBObjectBuilder.start()
			.add("topics", topicRef)
			.add("deleted", new BasicDBObject("$ne", true))
			.get();
		List<DBObject> convosDBList=new ArrayList<DBObject>();//convosColl.find(query, new BasicDBObject("_id", 1)).sort(new BasicDBObject("cr_date", -1)).toArray();
		DBCursor convoCur=convosColl.find(query, new BasicDBObject("_id", 1)).sort(new BasicDBObject("cr_date", -1));
		while(convoCur.hasNext()){
			convosDBList.add(convoCur.next());
		}
		
		
		List<ConversationBean> convosList = new ArrayList<ConversationBean>();
		for (DBObject convoDBObject : convosDBList) {
			ConversationBean convo = TalkerLogic.loadConvoFromCache(convoDBObject.get("_id").toString());
			//convo.parseBasicFromDB(convoDBObject);
			convosList.add(convo);
		}
		return convosList;
	}
	
	/**
	 * Closes given LiveChat manually - deletes all live talkers.
	 */
	public static void closeLiveChat(String convoId) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject convoObject = BasicDBObjectBuilder.start()
			.add("talkers", new ArrayList<DBObject>())
			.get();
		
		DBObject query = new BasicDBObject("_id", new ObjectId(convoId));
		convosColl.update(query, new BasicDBObject("$set", convoObject));
	}
	
	/**
	 * Deletes message with given index from given LiveChat
	 */
	public static void deleteChatMessage(String conversationId, int index) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		//delete 'index' message
		DBObject messageObj = new BasicDBObject("messages."+index+".deleted", true);
		DBObject convoId = new BasicDBObject("_id", new ObjectId(conversationId));
		convosColl.update(convoId, new BasicDBObject("$set", messageObj));
	}
	
	/**
	 * Get conversation  by id.
	 */
	public static ConversationBean getConvoById(String convoId) {
		DBCollection convoColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject query = new BasicDBObject("_id", new ObjectId(convoId));
		DBObject answerDBObject = convoColl.findOne(query);
		
		ConversationBean conversation = new ConversationBean();
		conversation.parseFromDB(answerDBObject);
		return conversation;
	}
	
	public static void main(String[] args) {
		deleteChatMessage("4cc94ac5b8682ba9efeba5f2", 0);
		
//		TopicBean topic = new TopicBean();
//		topic.setTopic("test");
//		topic.setUid("4c2cb43160adf3055c97d061");
//		Date currentDate = Calendar.getInstance().getTime();
//		topic.setCreationDate(currentDate);
//		TopicDAO.save(topic);
		
//		updateLiveTalkers(3, "4cc94906b8682ba9e5eba5f2", "kangaroo", true);
		
		
//		List<DBRef> allTopics = new ArrayList<DBRef>();
//		getAllTopics(allTopics, TopicDAO.getById("4cc944b0b8682ba9e3eba5f2"));
//		System.out.println(allTopics.size());
	}

	//used for testing
//	private static void updateLiveTalkers(int tid, String talkerId, 
//			String talkerName, boolean connected) {
//		DBCollection convosColl = getDB().getCollection(CONVERSATIONS_COLLECTION);
//		
//		DBRef talkerRef = new DBRef(getDB(), "talkers", new ObjectId(talkerId));
//		DBObject talkerDBObject = BasicDBObjectBuilder.start()
//			.add("uid", talkerRef)
//			.add("uname", talkerName)
//			.get();
//		
//		DBObject tidDBObject = new BasicDBObject("tid", tid);
//		String operation = "$pull"; //for disconnected
//		if (connected) {
//			operation = "$push";
//		}
//		convosColl.update(tidDBObject, 
//				new BasicDBObject(operation, new BasicDBObject("talkers", talkerDBObject)));
//	}

/**
	 * Load conversations for mobile application
	 */
	public static List<ConversationBean> loadConversationsForMob(String convoId) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		convosColl.ensureIndex(new BasicDBObject("cr_date", 1));
		
		Date firstActionTime = null;
		if (convoId != null) {
			firstActionTime = getConvos(convoId);
		}
		
		DBObject fields = getBasicConversationFields();
		
		//checking for views 
		BasicDBObjectBuilder queryBuilder =  BasicDBObjectBuilder.start()
		.add("deleted", new BasicDBObject("$ne", true));
		
		if (firstActionTime != null) {
			queryBuilder.add("cr_date", new BasicDBObject("$lt", firstActionTime));
		}
	
		DBObject query = queryBuilder.get();
	
		List<DBObject> convosDBList=new ArrayList<DBObject>();//convosColl.find(query, fields).sort(new BasicDBObject("cr_date", -1)).limit(20).toArray();
		DBCursor convoCur=convosColl.find(query, fields).sort(new BasicDBObject("cr_date", -1)).limit(20);
		while(convoCur.hasNext()){
			convosDBList.add(convoCur.next());
		}
		List<ConversationBean> convosList = new ArrayList<ConversationBean>();
		for (DBObject convoDBObject : convosDBList) {
			ConversationBean convo = new ConversationBean();
			convo.parseBasicFromDB(convoDBObject);
			List<CommentBean> comments = CommentsDAO.loadConvoAnswers(convo.getId());
			List<CommentBean> convoComments = new ArrayList<CommentBean>();
			for(int index=0; index<comments.size();index++){
				CommentBean comment = CommentsDAO.getConvoCommentById(comments.get(index).getId());
				if(comment != null && comment.getModerate() != null){
					if(comment.getModerate().equalsIgnoreCase(AnswerNotification.APPROVE_ANSWER) || comment.getModerate().equalsIgnoreCase(AnswerNotification.NOT_HELPFUL)
							|| comment.getModerate().equals("") || comment.getModerate().equalsIgnoreCase("Ignore")){
						convoComments.add(comment);
					}
				}
			}
			convo.setComments(convoComments);
	    	convosList.add(convo);
	    	convoComments.clear();
		}
		
		return convosList;
	}
	
	public static Date getConvos(String convoId) {
		DBCollection activitiesColl = getCollection(CONVERSATIONS_COLLECTION);

		DBObject query = new BasicDBObject("_id", new ObjectId(convoId));
		DBObject actionDBObject = activitiesColl.findOne(query);
		if (actionDBObject != null) {
			return (Date)actionDBObject.get("cr_date");
		}
		return null;
	}
	
	public static List <ConversationBean> loadExpertsAnswer(String afterActionId){
		List<ConversationBean> convosList = new ArrayList<ConversationBean>();
		while(true){
				List<DBObject> commentDBlist=getExpertsAnswerFromDB(afterActionId);
				convosList.addAll(getConvoForExpAnswer(commentDBlist,convosList.size()));
				if(convosList.size()>=20 || commentDBlist.size()<40)
					break;
				afterActionId=commentDBlist.get(commentDBlist.size()-1).get("_id").toString();
		}
		return convosList;
	}
	
	private static List<ConversationBean> getConvoForExpAnswer(List<DBObject> commentDBlist,int convosize){
		DBCollection talkerColl = getCollection(TalkerDAO.TALKERS_COLLECTION);
		DBCollection ConvoColl = getCollection(ConversationDAO.CONVERSATIONS_COLLECTION);
		
		List<ConversationBean> convolist=new ArrayList<ConversationBean>();
		for(DBObject obj:commentDBlist){
			//TalkerBean talker=TalkerLogic.loadTalkerFromCache(obj, "from");
			DBObject query=new BasicDBObject("_id",new ObjectId(((DBRef)obj.get("from")).getId().toString()));
			String connection=talkerColl.findOne(query, new BasicDBObject("connection",1)).get("connection").toString();
			if(connection !=null && TalkerBean.PROFESSIONAL_CONNECTIONS_LIST.contains(connection)){
				DBObject fields = getBasicConversationFields();
				DBObject convoQuery=BasicDBObjectBuilder.start()
				.add("_id", new ObjectId(((DBRef)obj.get("convo")).getId().toString()))
				.add("deleted", new BasicDBObject("$ne", true))
				.get();
				
				DBObject convoObj=ConvoColl.findOne(convoQuery,fields);
				if(convoObj!=null){
						ConversationBean convoBean=new ConversationBean();
						convoBean.parseBasicFromDB(convoObj);
						CommentBean answer=new CommentBean();
								answer.parseFromDB(obj);
								List <CommentBean> answerList=new ArrayList<CommentBean>();
								answerList.add(answer);
								for(int i=1;i<CommentsDAO.getConvoAnswersCount(convoBean.getId());i++){
									answerList.add(null);
								}
								
						convoBean.setComments(answerList);
						//convoBean.setComments(loadAllAnswers(convoBean.getId()));
						convolist.add(convoBean);
						convosize++;
						if(convosize>=20)
								break;
				}
			}
		}
		return convolist;
	}
	
	private static List<DBObject> getExpertsAnswerFromDB(String afterActionId) {
		DBCollection commentsColl = getCollection(CommentsDAO.CONVO_COMMENTS_COLLECTION);
		commentsColl.ensureIndex(new BasicDBObject("time", -1));
		
		BasicDBObjectBuilder queryBuilder =  BasicDBObjectBuilder.start()
		.add("deleted", new BasicDBObject("$ne", true))
		.add("answer",true);
	
		if (afterActionId != null && !afterActionId.equals("")) {
				DBObject fields=BasicDBObjectBuilder.start()		
					.add("time" , 1).get();
				Date firstActionTime = new Date();
				DBObject comment=commentsColl.findOne(new BasicDBObject("_id", new ObjectId(afterActionId)),fields);
				
				firstActionTime=(Date)comment.get("time");
				
				if(firstActionTime!=null){
					queryBuilder.add("time", new BasicDBObject("$lt", firstActionTime));
				}
		}
		DBObject fields=BasicDBObjectBuilder.start()		
		.add("_id" , 1)
		.add("convo" , 1)
		.add("from" , 1 )
		.add("time",1)
		.add("text",1)
		.get();
		
		DBObject query = queryBuilder.get();
		DBCursor commentsCur=commentsColl.find(query,fields).sort(new BasicDBObject("time", -1)).limit(40);
		List <DBObject> commentObjList=new ArrayList<DBObject>();
		while(commentsCur.hasNext()){
			commentObjList.add(commentsCur.next());
		}
		return commentObjList;
	}
	
	

	/** Load conversations as per popularity and which have answers
	 * @param type
	 * @param convoId
	 * @return List<ConversationBean>
	 */
	public static List<ConversationBean> loadPopularAnswers(String type,String afterActionId){
		List<ConversationBean> convosList = new ArrayList<ConversationBean>();
		int count = 0;
		while(true){
			List<DBObject> convoDblist =getPopularAnswersFromDb( type, afterActionId);
			
			convosList.addAll(getConvoList(convoDblist,type,convosList.size()));
			
			if(convosList.size()>=20 || convoDblist.size()<40)
			break;
			
			afterActionId=convoDblist.get(convoDblist.size()-1).get("_id").toString();
			count++;
		}
		CommonUtil.log("ConversationDAO.loadPopularAnswer", ""+count);
		//Logger.info("loadPopularAnswers - "+ count);
		Collections.sort(convosList);
		return convosList;
	}
	
	
	public static List<DBObject> getPopularAnswersFromDb(String type,String convoId){
		
		DBCollection convosColl = getCollection(ConversationDAO.CONVERSATIONS_COLLECTION);
		
			convosColl.ensureIndex(new BasicDBObject("views", -1));
		
		//load actions for this criterias
		
		//added for paging
		
		DBObject fields = getBasicConversationFields();
		
		//checking for views 
		BasicDBObjectBuilder queryBuilder =  BasicDBObjectBuilder.start()
		.add("deleted", new BasicDBObject("$ne", true));
	
		if (convoId != null) {
			
				int views = 0;
				views = ConversationDAO.getViews(convoId);
				if (views != 0) {
					queryBuilder.add("views", new BasicDBObject("$lt", views));
				}
		}
		
		DBObject query = queryBuilder.get();
	
		List<DBObject> convosDBList=new ArrayList<DBObject>();//
		DBCursor convoCur;
		
			convoCur=convosColl.find(query, fields).limit(40).sort(new BasicDBObject("views", -1));
		
		
		while(convoCur.hasNext()){
			convosDBList.add(convoCur.next());
		}
			
		
		//List<ConversationBean> convosList = getConvoList(convosDBList,type);
		return convosDBList;
	}
	
	public static List<ConversationBean>  getConvoList(List<DBObject> convosDBList,String type,int size){ 
		List<ConversationBean> convosList = new ArrayList<ConversationBean>();
		for (DBObject convoDBObject : convosDBList) {
			ConversationBean convo = new ConversationBean();
			convo.parseBasicFromDB(convoDBObject);
			convo.setComments(loadAllAnswers(convo.getId()));
			if(convo.getComments() != null && convo.getComments().size() > 0){
				List<CommentBean> commentList = convo.getComments();
				if(commentList != null && commentList.size() > 0){
					for(CommentBean comment :commentList){
						CommentBean commentBean = CommentsDAO.getConvoCommentById(comment.getId());
						if(commentBean != null){
							if(type != null && type.equalsIgnoreCase("expert")){
								//Add only those questions which have expert's answers
								if(commentBean.getFromTalker().isProf()){
									if(convosList.size() < 20)
										convosList.add(convo);
									size++;
									break;
								}
							}else{
								//Add questions which have answers
								if(convosList.size() < 20)
									convosList.add(convo);
								size++;
								break;
							}
						}
					}
				}
			}
			if(size==20){
				break;	
			}
		}
		return convosList;
	}

	/**
	 * Load all not-deleted answers for given conversation,
	 * answers have only id.
	 * @param convoId
	 * @return List<CommentBean>
	 */
	public static List<CommentBean> loadAllAnswers(String convoId) {
		DBCollection commentsColl = getCollection(CommentsDAO.CONVO_COMMENTS_COLLECTION);
		
		DBRef convoRef = createRef(ConversationDAO.CONVERSATIONS_COLLECTION, convoId);
		DBObject query = BasicDBObjectBuilder.start()
			.add("convo", convoRef)
			.add("deleted", new BasicDBObject("$ne", true))
			.add("answer", true)
			.get();
		List<DBObject> commentsList = commentsColl.find(query).toArray();
		
		List<CommentBean> answersList = new ArrayList<CommentBean>();
		for (DBObject answerDBObject : commentsList) {
			CommentBean answer = new CommentBean();
			answer.parseFromDB(answerDBObject);
			answersList.add(answer);
		}
		return answersList;
	}
	/**
	 * @param talkerId
	 * @param nextConvoId Id of the last convo from the previous page
	 * @param numOfConversations Number of convos to load (-1 for all)
	 * @return
	 */
	public static List<String> getStartedConvosForEmailReminderJob(String talkerId) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject fields = BasicDBObjectBuilder.start()
			.add("_id", 1)
			.get();
		DBRef talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talkerId);
		BasicDBObjectBuilder queryBuilder = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("deleted", new BasicDBObject("$ne", true));

		
		List<DBObject> convoList=new ArrayList<DBObject>();//  convosColl.find(queryBuilder.get(), fields).sort(new BasicDBObject("cr_date", -1)).toArray();
		DBCursor convoCur=convosColl.find(queryBuilder.get(), fields).sort(new BasicDBObject("cr_date", -1));
		while(convoCur.hasNext()){
			convoList.add(convoCur.next());
		}
		
		String id = "";
		List<String> convoIdList = new ArrayList<String>();
		for(DBObject dbObj : convoList){
			id = dbObj.get("_id").toString();
			if(!id.equals(""))
				convoIdList.add(id);
		}
		return convoIdList;
	}
	
	/**
	 * Method used for getting conversations for indexer job
	 * @return
	 */
	public static List<ConversationBean> getAllConvosForScheduler() {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject fields = BasicDBObjectBuilder.start()
			.add("_id", 1)
			.add("topic", 1)
			.add("main_url", 1)
			.add("deleted", 1)
			.add("bitly", 1)
			.add("bitly_chat", 1)
			.add("category", 1)
			.add("other_disease_categories", 1)
			.get();

		BasicDBObject basicDBObject = new BasicDBObject("_id", new BasicDBObject("$ne", ""));
		
		List<DBObject> convoList=new ArrayList<DBObject>();//convosColl.find(basicDBObject,fields).sort(new BasicDBObject("cr_date", -1))
		DBCursor convoCur=convosColl.find(basicDBObject,fields).sort(new BasicDBObject("cr_date", -1));
		while(convoCur.hasNext()){
			convoList.add(convoCur.next());
		}
		 
		ConversationBean convo = null;
		List<ConversationBean> conversationList = new ArrayList<ConversationBean>();
		for(DBObject convoDBObject : convoList){
			convo = new ConversationBean();
			convo.setId(convoDBObject.get("_id").toString());
			convo.setTopic((String)convoDBObject.get("topic"));
			convo.setMainURL((String)convoDBObject.get("main_url"));
			convo.setDeleted(getBoolean(convoDBObject, "deleted"));
			convo.setBitly((String)convoDBObject.get("bitly"));
			convo.setBitlyChat((String)convoDBObject.get("bitly_chat"));
			convo.setCategory((String)convoDBObject.get("category"));
	    	Collection<String> otherDiseaseCategories = (Collection<String>)convoDBObject.get("other_disease_categories");
			if (otherDiseaseCategories != null) {
				convo.setOtherDiseaseCategories(otherDiseaseCategories.toArray(new String[]{}));
			}
			conversationList.add(convo);
		}
		return conversationList;
	}
	
	/**
	 * Updated the conversation 
	 * @param convo
	 */
	public static void updateConvoForScheduler(ConversationBean convo,String name,String value){
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject convoObject = BasicDBObjectBuilder.start()
		.add(name, value)
		.get();
		
		DBObject convoId = new BasicDBObject("_id", new ObjectId(convo.getId()));
		convosColl.update(convoId, new BasicDBObject("$set", convoObject),false,true);
	}
	
	/**
	 * Getting no of conversation for topic 
	 * @param topicId
	 * @return no of convos for topic
	 */
	public static int getNoOfconvosForTopic(String topicId) {
		DBCollection convosColl = getCollection(ConversationDAO.CONVERSATIONS_COLLECTION);
		DBRef topicRef = createRef(TopicDAO.TOPICS_COLLECTION, topicId);
		DBObject query = BasicDBObjectBuilder.start()
			.add("topics", topicRef)
			.add("deleted", new BasicDBObject("$ne", true))
			.get();
		return convosColl.find(query, new BasicDBObject("_id", 1)).count();
	}
	
	/**
	 * Getting categories of conversation
	 * @param convoId
	 * @param args
	 * @return
	 */
	public static ConversationBean getConvoCategories(String convoId,String []args ){
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		DBObject fields=new BasicDBObject();
			for(String str:args){
				fields.put(str, 1);
			}
			DBObject query = new BasicDBObject("_id", new ObjectId(convoId));
			
			DBObject convoDBObject = convosColl.findOne(query, fields);
			
			ConversationBean convo=new ConversationBean();
			
			convo.setId(convoDBObject.get("_id").toString());
			convo.setCategory((String)convoDBObject.get("category"));
			convo.setOpened(getBoolean(convoDBObject, "opened"));
			Collection<String> otherDiseaseCategories = (Collection<String>)convoDBObject.get("other_disease_categories");
			
			if (otherDiseaseCategories != null) {
				convo.setOtherDiseaseCategories(otherDiseaseCategories.toArray(new String[]{}));
			}
			
			return convo;			
	}

	public static boolean subcribeConvoForAnsNotification(String email,String convoId){
		
		TalkerBean talker=TalkerDAO.getByEmail(email);
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		DBObject query = new BasicDBObject("_id", new ObjectId(convoId));
		DBObject convoDBObject = convosColl.findOne(query);
		System.out.println(convoDBObject.get("_id").toString());
		if(talker==null){
			Collection<String> emails = (Collection<String>)convoDBObject.get("answer_subscription");
			if(emails==null || emails.size()==0){
				String []emaillist={email};
				DBObject emailsObj=new BasicDBObject("answer_subscription", emaillist);
				
				DBObject Id = new BasicDBObject("_id", new ObjectId(convoDBObject.get("_id").toString()));
				convosColl.update(Id, new BasicDBObject("$set", emailsObj),false ,true);
				return false;
			}else if(!emails.contains(email)){
				emails.add(email);
				DBObject emailsObj=new BasicDBObject("answer_subscription",  emails.toArray(new String[]{}));
				DBObject Id = new BasicDBObject("_id", new ObjectId(convoDBObject.get("_id").toString()));
				convosColl.update(Id, new BasicDBObject("$set", emailsObj),false,true);
				return false;
			}else
				return true;
		}else{
			if (!talker.getFollowingConvosList().contains(convoId)) {
				
				talker.getFollowingConvosList().add(convoId);
				
				TalkerDAO.updateTalker(talker);
	    		TalkerBean mailSendtalker = TalkerLogic.loadTalkerFromCache(convoDBObject, "uid");// TalkerDAO.getByEmail(convo.getTalker().getEmail());
	    		
	    		Map<String, String> vars = new HashMap<String, String>();
	    		vars.put("other_talker", talker.getUserName());
	    		
	    		if(mailSendtalker.getEmailSettings().toString().contains("NEW_FOLLOWER"))
	    			EmailUtil.sendEmail(EmailTemplate.NOTIFICATION_FOLLOWER, mailSendtalker.getEmail(), vars, null, false);
	    	} 
			return false;
		}
			
	}
	
	public static void updateConvoForDisease(ConversationBean convo){
		DBCollection convoColl = getCollection(ConversationDAO.CONVERSATIONS_COLLECTION);
		
		DBObject convoObject = BasicDBObjectBuilder.start()
			.add("category", convo.getCategory())
			.add("other_disease_categories", convo.getOtherDiseaseCategories())
			.get();
		
		DBObject convoId = new BasicDBObject("_id", new ObjectId(convo.getId()));
		//"$set" is used for updating fields
		convoColl.update(convoId, new BasicDBObject("$set", convoObject));
	}
}


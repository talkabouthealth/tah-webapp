package dao;

import groovy.util.ObjectGraphBuilder.DefaultReferenceResolver;

import java.awt.image.DataBufferByte;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.CommentBean;
import models.MessageBean;
import models.TalkerBean;
import models.ConversationBean;
import models.TopicBean;
import models.actions.Action.ActionType;

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
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;

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
			.get();

		//Only with SAFE WriteConcern we receive exception on duplicate key
		try {
			convosColl.save(convoObject, WriteConcern.SAFE);
		}
		catch (MongoException me) {
			//E11000 duplicate key error index
			if (me.getCode() == 11000) {
				Logger.error("Duplicate key error while saving convo");
				return saveInternal(convo, --count);
			}
			me.printStackTrace();
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
			.get();
		
		DBObject convoId = new BasicDBObject("_id", new ObjectId(convo.getId()));
		convosColl.update(convoId, new BasicDBObject("$set", convoObject));
	}
	
	
	//----------------------- Query methods ------------------------
	public static ConversationBean getById(String convoId) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject query = new BasicDBObject("_id", new ObjectId(convoId));
		DBObject convoDBObject = convosColl.findOne(query);
		
		if (convoDBObject == null) {
			return null;
		}
		ConversationBean convo = new ConversationBean();
		convo.parseFromDB(convoDBObject);
		return convo;
	}
	
	public static ConversationBean getByTid(Integer tid) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject query = new BasicDBObject("tid", tid);
		DBObject convoDBObject = convosColl.findOne(query);
		
		//TODO: same code
		if (convoDBObject == null) {
			return null;
		}
		ConversationBean convo = new ConversationBean();
		convo.parseFromDB(convoDBObject);
		return convo;
	}
	
	public static ConversationBean getByTitle(String title) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject query = new BasicDBObject("topic", title);
		DBObject convoDBObject = convosColl.findOne(query);
		
		if (convoDBObject == null) {
			return null;
		}
		ConversationBean convo = new ConversationBean();
		convo.parseBasicFromDB(convoDBObject);
		return convo;
	}
	
	public static ConversationBean getByFromInfo(String from, String fromId) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject query = BasicDBObjectBuilder.start()
			.add("from", from)
			.add("from_id", fromId)
			.get();
		DBObject convoDBObject = convosColl.findOne(query);
		
		if (convoDBObject == null) {
			return null;
		}
		ConversationBean convo = new ConversationBean();
		convo.parseBasicFromDB(convoDBObject);
		return convo;
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
		
		if (convoDBObject == null) {
			return null;
		}
		ConversationBean convo = new ConversationBean();
		convo.parseFromDB(convoDBObject);
		return convo;
	}
	
	/**
	 * Get all conversations (deleted also).
	 */
	public static List<ConversationBean> loadAllConversations() {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		List<DBObject> convosDBList = 
			convosColl.find().sort(new BasicDBObject("cr_date", -1)).toArray();
		
		List<ConversationBean> convosList = new ArrayList<ConversationBean>();
		for (DBObject convoDBObject : convosDBList) {
			ConversationBean convo = new ConversationBean();
			convo.parseFromDB(convoDBObject);
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
	public static List<ConversationBean> loadPopularConversations() {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject query = BasicDBObjectBuilder.start()
			.add("deleted", new BasicDBObject("$ne", true))
			.get();
		List<DBObject> convosDBList = 
			convosColl.find(query).sort(new BasicDBObject("views", -1)).limit(20).toArray();
		
		List<ConversationBean> convosList = new ArrayList<ConversationBean>();
		for (DBObject convoDBObject : convosDBList) {
			ConversationBean convo = new ConversationBean();
			convo.parseFromDB(convoDBObject);
			convo.setComments(CommentsDAO.loadConvoAnswers(convo.getId()));
	    	convosList.add(convo);
		}
		return convosList;
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
		List<DBObject> convosDBList = 
			convosColl.find(query).sort(new BasicDBObject("cr_date", -1)).toArray();
		
		List<ConversationBean> convosList = new ArrayList<ConversationBean>();
		for (DBObject convoDBObject : convosDBList) {
			ConversationBean convo = new ConversationBean();
			convo.parseBasicFromDB(convoDBObject);
			convo.setComments(CommentsDAO.loadConvoAnswers(convo.getId()));
	    	convosList.add(convo);
		}
		return convosList;
	}
	
	/**
	 * Opened Questions - not answered, which are marked with 'opened' flag.
	 */
	public static List<ConversationBean> getOpenQuestions() {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject query = BasicDBObjectBuilder.start()
			.add("opened", true)
			.add("deleted", new BasicDBObject("$ne", true))
			.get();
		List<DBObject> convosDBList = 
			convosColl.find(query).sort(new BasicDBObject("cr_date", -1)).toArray();
		
		List<ConversationBean> convosList = new ArrayList<ConversationBean>();
		for (DBObject convoDBObject : convosDBList) {
			ConversationBean convo = new ConversationBean();
			convo.parseBasicFromDB(convoDBObject);
	    	convosList.add(convo);
		}
		
		return convosList;
	}
	
	//------------------- Other ----------------------
	
	/**
	 * Returns followers of the given conversation
	 */
	public static List<TalkerBean> getConversationFollowers(String convoId) {
    	DBCollection talkersColl = getCollection(TalkerDAO.TALKERS_COLLECTION);
    	
    	DBObject query = new BasicDBObject("following_convos", convoId);
    	List<DBObject> followersDBList = talkersColl.find(query).toArray();
    	
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
		List<DBObject> topicsDBList = convosColl.find(query).sort(new BasicDBObject("cr_date", -1)).toArray();
		
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
		List<DBObject> activitiesDBList = 
			activitiesColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		Set<ConversationBean> convosSet = new LinkedHashSet<ConversationBean>();
		for (DBObject activityDBObject : activitiesDBList) {
			DBObject convoDBObject = ((DBRef)activityDBObject.get("convoId")).fetch();
			
			ConversationBean convo = new ConversationBean();
			convo.parseFromDB(convoDBObject);
			
			if (!convo.isDeleted()) {
				convosSet.add(convo);
			}
		}
		return convosSet;
	}
	
	/**
	 * Get all conversations that have at least one given topic.
	 */
	public static Set<DBRef> getConversationsByTopics(Set<TopicBean> topics) {
		Set<DBRef> convosDBSet = new HashSet<DBRef>();
		if (topics == null || topics.size() == 0) {
			return convosDBSet;
		}
		
		//TODO: later - restructure topics to make it more effective?
		//http://www.mongodb.org/display/DOCS/Trees+in+MongoDB#TreesinMongoDB-ArrayofAncestors
		List<DBRef> allTopics = new ArrayList<DBRef>();
		for (TopicBean topic : topics) {
			getAllTopics(allTopics, topic);
		}
		
		DBCollection convosColl = getCollection(ConversationDAO.CONVERSATIONS_COLLECTION);
		DBObject query = new BasicDBObject("topics", new BasicDBObject("$in", allTopics));
		List<DBObject> convosDBList = convosColl.find(query).toArray();
		
		for (DBObject convoDBObject : convosDBList) {
			convosDBSet.add(createRef(ConversationDAO.CONVERSATIONS_COLLECTION, getString(convoDBObject, "_id")));
		}
		
		return convosDBSet;
	}
	
	/**
	 * Recursive method that saves all subtree of given root topic to 'allTopics' list.
	 */
	//TODO: move to topics dao?
	public static void getAllTopics(List<DBRef> allTopics, TopicBean rootTopic) {
		DBRef topicRef = createRef(TopicDAO.TOPICS_COLLECTION, rootTopic.getId());
		if (!allTopics.contains(topicRef)) {
			allTopics.add(topicRef);
			
			for (TopicBean child : rootTopic.getChildren()) {
				DBCollection topicsColl = getCollection(TopicDAO.TOPICS_COLLECTION);
				DBObject query = new BasicDBObject("_id", new ObjectId(child.getId()));
				DBObject topicDBObject = topicsColl.findOne(query);
				
				TopicBean fullChild = new TopicBean();
				fullChild.setId(getString(topicDBObject, "_id"));
				//children
				Collection<DBRef> childrenDBList = (Collection<DBRef>)topicDBObject.get("children");
				Set<TopicBean> children = new HashSet<TopicBean>();
				if (childrenDBList != null) {
					for (DBRef childDBRef : childrenDBList) {
						children.add(new TopicBean(childDBRef.getId().toString()));
					}
				}
				fullChild.setChildren(children);
				
				getAllTopics(allTopics, fullChild);
			}
		}
	}
	
	/**
	 * Load conversations that have given topic.
	 */
	public static List<ConversationBean> loadConversationsByTopic(String topicId) {
		DBCollection convosColl = getCollection(ConversationDAO.CONVERSATIONS_COLLECTION);
		
		DBRef topicRef = createRef(TopicDAO.TOPICS_COLLECTION, topicId);
		DBObject query = new BasicDBObject("topics", topicRef);
		List<DBObject> convosDBList = convosColl.find(query).toArray();
		
		List<ConversationBean> convosList = new ArrayList<ConversationBean>();
		for (DBObject convoDBObject : convosDBList) {
			ConversationBean convo = new ConversationBean();
			convo.parseFromDB(convoDBObject);
			if (!convo.isDeleted()) {
				//TODO: do we need to load this?
				convo.setComments(CommentsDAO.loadConvoAnswersTree(convo.getId()));
				convosList.add(convo);
			}
		}
		return convosList;
	}
	
	//TODO: check this and previous methods
	public static List<ConversationBean> loadSimpleConversationsByTopic(String topicId) {
		DBCollection convosColl = getCollection(ConversationDAO.CONVERSATIONS_COLLECTION);
		
		DBRef topicRef = createRef(TopicDAO.TOPICS_COLLECTION, topicId);
		DBObject query = new BasicDBObject("topics", topicRef);
		List<DBObject> convosDBList = convosColl.find(query).toArray();
		
		List<ConversationBean> convosList = new ArrayList<ConversationBean>();
		for (DBObject convoDBObject : convosDBList) {
			ConversationBean convo = new ConversationBean();
			convo.setId(convoDBObject.get("_id").toString());
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

}


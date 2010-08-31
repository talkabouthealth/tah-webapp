package dao;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.CommentBean;
import models.MessageBean;
import models.TalkerBean;
import models.ConversationBean;
import models.TopicBean;

import org.bson.types.ObjectId;

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
	
	public static final String CONVERSATIONS_COLLECTION = "topics";
	
	public static void save(ConversationBean topic) {
		//TODO: Same topic? - Do not update anything
		//TODO: move everything to update?
		
		//we try to insert topic 5 times
		int tid = saveInternal(topic, 5);
		
		if (tid != -1) {
			topic.setTid(tid);
		} else {
			//TODO: error handling?
			new Exception("DB Problem - Topic not inserted into DB").printStackTrace();
			return;
		}
	}
	
	/**
	 * Tries to insert topic 'count' times (in case of duplicate key error on 'tid' field)
	 * Returns -1 in case of failure
	 */
	//TODO: move similar code in one jar ?
	private static int saveInternal(ConversationBean convo, int count) {
		if (count == 0) {
			return -1;
		}
		
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		//get last tid
		DBCursor topicsCursor = 
			convosColl.find(null, new BasicDBObject("tid", "")).sort(new BasicDBObject("tid", -1)).limit(1);
		int tid = topicsCursor.hasNext() ? ((Integer)topicsCursor.next().get("tid")) + 1 : 1;
		
		DBRef talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, convo.getUid());
		List<DBRef> topicsDBList = new ArrayList<DBRef>();
		for (TopicBean topic : convo.getTopics()) {
			DBRef topicRef = createRef(TopicDAO.TOPICS_COLLECTION, topic.getId());
			topicsDBList.add(topicRef);
		}
		DBObject topicObject = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("tid", tid)
			.add("topic", convo.getTopic())
			.add("cr_date", convo.getCreationDate())
			.add("disp_date", convo.getDisplayTime())
			.add("main_url", convo.getMainURL())
			.add("topics", topicsDBList)
			.get();

		//Only with STRICT WriteConcern we receive exception on duplicate key
		try {
			convosColl.save(topicObject, WriteConcern.SAFE);
		}
		catch (MongoException me) {
			//E11000 duplicate key error index
			if (me.getCode() == 11000) {
				System.err.println("Duplicate key error while saving topic");
				return saveInternal(convo, --count);
			}
			me.printStackTrace();
		}
		
		String topicId = topicObject.get("_id").toString();
		convo.setId(topicId);
		
		return (Integer)topicObject.get("tid");
	}
	
	public static void updateTopic(ConversationBean convo) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject topicObject = BasicDBObjectBuilder.start()
			.add("topic", convo.getTopic())
			.add("main_url", convo.getMainURL())
			.get();
		
		DBObject convoId = new BasicDBObject("_id", new ObjectId(convo.getId()));
		convosColl.update(convoId, new BasicDBObject("$set", topicObject));
	}
	
	public static ConversationBean getByConvoId(String topicId) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject query = new BasicDBObject("_id", new ObjectId(topicId));
		DBObject topicDBObject = convosColl.findOne(query);
		
		return parseConversationBean(topicDBObject);
	}
	
	public static ConversationBean getByTid(Integer tid) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject query = new BasicDBObject("tid", tid);
		DBObject topicDBObject = convosColl.findOne(query);
		
		return parseConversationBean(topicDBObject);
	}
	
	/**
	 * Find by main URL (current) or old urls.
	 * @param url
	 * @return
	 */
	public static ConversationBean getByURL(String url) {
		DBCollection convosColl = getDB().getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject query = new BasicDBObject("$or", 
				Arrays.asList(
						new BasicDBObject("main_url", url),
						new BasicDBObject("urls", url)
					)
			);
		DBObject topicDBObject = convosColl.findOne(query);
		
		return parseConversationBean(topicDBObject);
	}
	
	private static ConversationBean parseConversationBean(DBObject topicDBObject) {
		if (topicDBObject == null) {
			return null;
		}
		
		//TODO: move to topic bean?
		ConversationBean convo = new ConversationBean();
    	convo.setId(topicDBObject.get("_id").toString());
    	convo.setTid((Integer)topicDBObject.get("tid"));
    	convo.setTopic((String)topicDBObject.get("topic"));
    	convo.setCreationDate((Date)topicDBObject.get("cr_date"));
    	convo.setDisplayTime((Date)topicDBObject.get("disp_date"));
    	
    	convo.setMainURL((String)topicDBObject.get("main_url"));
    	convo.setURLs(getStringSet(topicDBObject, "urls"));
    	
    	convo.setViews(getInt(topicDBObject, "views"));
    	
    	DBObject talkerDBObject = ((DBRef)topicDBObject.get("uid")).fetch();
    	TalkerBean talker = new TalkerBean();
    	talker.parseBasicFromDB(talkerDBObject);
    	convo.setTalker(talker);
    	
    	List<MessageBean> messages = new ArrayList<MessageBean>();
    	Set<String> members = new HashSet<String>();
    	Collection<DBObject> messagesDBList = (Collection<DBObject>)topicDBObject.get("messages");
    	if (messagesDBList != null) {
    		for (DBObject messageDBObject : messagesDBList) {
    			MessageBean message = new MessageBean();
    			message.setText((String)messageDBObject.get("text"));
    			
    			DBObject fromTalkerDBObject = ((DBRef)messageDBObject.get("uid")).fetch();
    			TalkerBean fromTalker = 
    				new TalkerBean(fromTalkerDBObject.get("_id").toString(), (String)fromTalkerDBObject.get("uname"));
    			message.setFromTalker(fromTalker);
    			
    			members.add(fromTalker.getUserName());
    			messages.add(message);
    		}
    	}
    	convo.setMembers(members);
    	convo.setMessages(messages);
    	
    	//followers of this convo
    	DBCollection talkersColl = getCollection(TalkerDAO.TALKERS_COLLECTION);
    	DBObject query = new BasicDBObject("following_topics", convo.getId());
    	DBObject fields = BasicDBObjectBuilder.start()
    		.add("uname", 1)
    		.add("email", 1)
    		.add("bio", 1)
    		.add("email_settings", 1)
    		.get();
    	List<DBObject> followersDBList = talkersColl.find(query, fields).toArray();
    	List<TalkerBean> followers = new ArrayList<TalkerBean>();
    	for (DBObject followerDBObject : followersDBList) {
    		TalkerBean followerTalker = new TalkerBean();
    		followerTalker.parseBasicFromDB(followerDBObject);
			followers.add(followerTalker);
    	}
    	convo.setFollowers(followers);
    	
		return convo;
	}
	
	public static List<ConversationBean> loadAllTopics() {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		List<DBObject> topicsDBList = 
			convosColl.find().sort(new BasicDBObject("disp_date", -1)).toArray();
		
		List<ConversationBean> topicsList = new ArrayList<ConversationBean>();
		for (DBObject topicDBObject : topicsDBList) {
			ConversationBean topic = parseConversationBean(topicDBObject);
	    	topicsList.add(topic);
		}
		
		return topicsList;
	}
	
	public static Map<String, ConversationBean> queryTopics() {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		List<DBObject> topicsList = 
			convosColl.find().sort(new BasicDBObject("disp_date", -1)).limit(20).toArray();
		
		Map <String, ConversationBean> topicsMap = new LinkedHashMap <String, ConversationBean>(20);
		for (DBObject topicDBObject : topicsList) {
			ConversationBean topic = new ConversationBean();
	    	topic.setId(topicDBObject.get("_id").toString());
	    	topic.setTid((Integer)topicDBObject.get("tid"));
	    	topic.setTopic((String)topicDBObject.get("topic"));
	    	topic.setCreationDate((Date)topicDBObject.get("cr_date"));
	    	topic.setDisplayTime((Date)topicDBObject.get("disp_date"));
	    	topic.setMainURL((String)topicDBObject.get("main_url"));
			
	    	DBObject talkerDBObject = ((DBRef)topicDBObject.get("uid")).fetch();
	    	TalkerBean talker = new TalkerBean();
	    	talker.parseFromDB(talkerDBObject);
	    	topic.setTalker(talker);
	    	
	    	topicsMap.put(topic.getId(), topic);
		}
		
		return topicsMap;
	}
	
	/**
	 * # of topics for given Talker ID
	 * TODO: store additional field - quicker access?
	 */
	public static int getNumberOfTopics(String talkerId) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBRef talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talkerId);
		DBObject query = new BasicDBObject("uid", talkerRef);

		return convosColl.find(query).count();
	}
	
	public static String getLastTopicId() {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject topicDBObject = convosColl.find().sort(new BasicDBObject("cr_date", -1)).next();
		if (topicDBObject == null) {
			return null;
		}
		else {
			return topicDBObject.get("_id").toString();
		}
	}
	
	public static List<Map<String, String>> loadTopicsForDashboard(boolean withNotifications) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		DateFormat dateFormat = new SimpleDateFormat("MM.dd.yyyy HH:mm:ss");
		
		List<DBObject> topicsDBList = convosColl.find().sort(new BasicDBObject("cr_date", -1)).toArray();
		
		List<Map<String, String>> topicsInfoList = new ArrayList<Map<String,String>>();
		for (DBObject topicDBObject : topicsDBList) {
			Map<String, String> topicInfoMap = new HashMap<String, String>();
			
			//noti_history.noti_time is null
			int numOfNotifications = NotificationDAO.getNotiNumByTopic(topicDBObject.get("_id").toString());
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
				int notificationsNum = NotificationDAO.getNotiNumByTopic(topicInfoMap.get("topicId"));
				topicInfoMap.put("notificationsNum", ""+notificationsNum);
			}
			
			topicsInfoList.add(topicInfoMap);
		}
		
		return topicsInfoList;
	}
	
	public static void incrementConvoViews(String topicId) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject topicIdDBObject = new BasicDBObject("_id", new ObjectId(topicId));
		convosColl.update(topicIdDBObject, 
				new BasicDBObject("$inc", new BasicDBObject("views", 1)));
	}
	
	//Load topics for given activity type
	public static List<ConversationBean> loadConversations(String talkerId, String type) {
		DBCollection activitiesColl = getCollection(ActivityDAO.ACTIVITIES_COLLECTION);
		
		DBRef talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talkerId);
		DBObject query = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("type", type)
			.get();
		List<DBObject> activitiesDBList = 
			activitiesColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		List<ConversationBean> convosList = new ArrayList<ConversationBean>();
		for (DBObject activityDBObject : activitiesDBList) {
			DBObject convoDBObject = ((DBRef)activityDBObject.get("topicId")).fetch();
			
			ConversationBean convo = parseConversationBean(convoDBObject);
			convosList.add(convo);
		}
		return convosList;
	}
	
	public static List<ConversationBean> loadConversationsByTopic(String topicId) {
		DBCollection convosColl = getCollection(ConversationDAO.CONVERSATIONS_COLLECTION);
		
		DBRef topicRef = createRef(TopicDAO.TOPICS_COLLECTION, topicId);
		DBObject query = new BasicDBObject("topics", topicRef);
		List<DBObject> convosDBList = convosColl.find(query).toArray();
		
		List<ConversationBean> convosList = new ArrayList<ConversationBean>();
		for (DBObject convoDBObject : convosDBList) {
			ConversationBean convo = parseConversationBean(convoDBObject);
			convo.setComments(CommentsDAO.loadConvoComments(convo.getId()));
			convosList.add(convo);
		}
		return convosList;
	}
	
	
	public static void main(String[] args) {
		System.out.println(ConversationDAO.getNumberOfTopics("4c2cb43160adf3055c97d061"));
		
//		TopicBean topic = new TopicBean();
//		topic.setTopic("test");
//		topic.setUid("4c2cb43160adf3055c97d061");
//		Date currentDate = Calendar.getInstance().getTime();
//		topic.setCreationDate(currentDate);
//		topic.setDisplayTime(currentDate);
//		TopicDAO.save(topic);
	}

}


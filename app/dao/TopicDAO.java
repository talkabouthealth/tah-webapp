package dao;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import com.mongodb.DB.WriteConcern;

import static util.DBUtil.*;

public class TopicDAO {
	
	public static final String TOPICS_COLLECTION = "topics";
	
	public static void save(TopicBean topic) {
		//TODO: Same topic? - Do not update anything
		//TODO: one query?
		//TODO: move everything to update?
		boolean uniqueMainURL = (getByMainURL(topic.getMainURL()) == null);
		boolean uniqueOldURL = (getByOldURL(topic.getMainURL()) == null);
		
		//we try to insert topic 5 times
		int tid = saveInternal(topic, 5);
		
		if (tid != -1) {
			topic.setTid(tid);
		} else {
			//TODO: error handling?
			new Exception("DB Problem - Topic not inserted into DB").printStackTrace();
			return;
		}
		
		if (!(uniqueMainURL && uniqueOldURL)) {
			//add 'tid' to mainURL
			topic.setMainURL(topic.getMainURL()+"-"+topic.getTid());
			updateTopic(topic);
		}
	}
	
	/**
	 * Tries to insert topic 'count' times (in case of duplicate key error on 'tid' field)
	 * Returns -1 in case of failure
	 */
	//TODO: move similar code in one jar ?
	private static int saveInternal(TopicBean topic, int count) {
		if (count == 0) {
			return -1;
		}
		
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		//get last tid
		DBCursor topicsCursor = 
			topicsColl.find(null, new BasicDBObject("tid", "")).sort(new BasicDBObject("tid", -1)).limit(1);
		int tid = topicsCursor.hasNext() ? ((Integer)topicsCursor.next().get("tid")) + 1 : 1;
		
		DBRef talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, topic.getUid());
		DBObject topicObject = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("tid", tid)
			.add("topic", topic.getTopic())
			.add("cr_date", topic.getCreationDate())
			.add("disp_date", topic.getDisplayTime())
			.add("main_url", topic.getMainURL())
			.get();

		//Only with STRICT WriteConcern we receive exception on duplicate key
		topicsColl.setWriteConcern(WriteConcern.STRICT);
		try {
			topicsColl.save(topicObject);
		}
		catch (MongoException me) {
			//E11000 duplicate key error index
			if (me.getCode() == 11000) {
				System.err.println("Duplicate key error while saving topic");
				return saveInternal(topic, --count);
			}
			me.printStackTrace();
		}
		
		String topicId = topicObject.get("_id").toString();
		topic.setId(topicId);
		
		return (Integer)topicObject.get("tid");
	}
	
	public static void updateTopic(TopicBean topic) {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		//TODO: update also main url
		DBObject topicObject = BasicDBObjectBuilder.start()
			.add("topic", topic.getTopic())
			.add("main_url", topic.getMainURL())
			.get();
		
		DBObject topicId = new BasicDBObject("_id", new ObjectId(topic.getId()));
		topicsColl.update(topicId, new BasicDBObject("$set", topicObject));
	}
	
	public static TopicBean getByTopicId(String topicId) {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		DBObject query = new BasicDBObject("_id", new ObjectId(topicId));
		DBObject topicDBObject = topicsColl.findOne(query);
		
		return parseTopicBean(topicDBObject);
	}
	
	public static TopicBean getByTid(Integer tid) {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		DBObject query = new BasicDBObject("tid", tid);
		DBObject topicDBObject = topicsColl.findOne(query);
		
		return parseTopicBean(topicDBObject);
	}
	
	public static TopicBean getByMainURL(String mainURL) {
		DBCollection topicsColl = getDB().getCollection(TOPICS_COLLECTION);
		
		DBObject query = new BasicDBObject("main_url", mainURL);
		DBObject topicDBObject = topicsColl.findOne(query);
		
		return parseTopicBean(topicDBObject);
	}
	
	public static TopicBean getByOldURL(String oldURL) {
		DBCollection topicsColl = getDB().getCollection(TOPICS_COLLECTION);
		
		DBObject query = new BasicDBObject("urls", oldURL);
		DBObject topicDBObject = topicsColl.findOne(query);
		
		return parseTopicBean(topicDBObject);
	}
	
	private static TopicBean parseTopicBean(DBObject topicDBObject) {
		if (topicDBObject == null) {
			return null;
		}
		
		//TODO: move to topic bean?
		TopicBean topic = new TopicBean();
    	topic.setId(topicDBObject.get("_id").toString());
    	topic.setTid((Integer)topicDBObject.get("tid"));
    	topic.setTopic((String)topicDBObject.get("topic"));
    	topic.setCreationDate((Date)topicDBObject.get("cr_date"));
    	topic.setDisplayTime((Date)topicDBObject.get("disp_date"));
    	topic.setMainURL((String)topicDBObject.get("main_url"));
    	topic.setViews(getInt(topicDBObject, "views"));
    	
    	DBObject talkerDBObject = ((DBRef)topicDBObject.get("uid")).fetch();
    	TalkerBean talker = new TalkerBean();
    	talker.parseFromDB(talkerDBObject);
    	topic.setTalker(talker);
    	
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
    	topic.setMembers(members);
    	topic.setMessages(messages);
    	
    	//followers of this topic
    	DBCollection talkersColl = getCollection(TalkerDAO.TALKERS_COLLECTION);
    	DBObject query = new BasicDBObject("following_topics", topic.getId());
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
    	topic.setFollowers(followers);
    	
		return topic;
	}
	
	public static List<TopicBean> loadAllTopics() {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		List<DBObject> topicsDBList = 
			topicsColl.find().sort(new BasicDBObject("disp_date", -1)).toArray();
		
		List<TopicBean> topicsList = new ArrayList<TopicBean>();
		for (DBObject topicDBObject : topicsDBList) {
			TopicBean topic = parseTopicBean(topicDBObject);
	    	topicsList.add(topic);
		}
		
		return topicsList;
	}
	
	public static Map<String, TopicBean> queryTopics() {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		List<DBObject> topicsList = 
			topicsColl.find().sort(new BasicDBObject("disp_date", -1)).limit(20).toArray();
		
		Map <String, TopicBean> topicsMap = new LinkedHashMap <String, TopicBean>(20);
		for (DBObject topicDBObject : topicsList) {
			TopicBean topic = new TopicBean();
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
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		DBRef talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talkerId);
		DBObject query = new BasicDBObject("uid", talkerRef);

		return topicsColl.find(query).count();
	}
	
	public static String getLastTopicId() {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		DBObject topicDBObject = topicsColl.find().sort(new BasicDBObject("cr_date", -1)).next();
		if (topicDBObject == null) {
			return null;
		}
		else {
			return topicDBObject.get("_id").toString();
		}
	}
	
	public static List<Map<String, String>> loadTopicsForDashboard(boolean withNotifications) {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		DateFormat dateFormat = new SimpleDateFormat("MM.dd.yyyy HH:mm:ss");
		
		List<DBObject> topicsDBList = topicsColl.find().sort(new BasicDBObject("cr_date", -1)).toArray();
		
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
	
	public static void incrementTopicViews(String topicId) {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		DBObject topicIdDBObject = new BasicDBObject("_id", new ObjectId(topicId));
		topicsColl.update(topicIdDBObject, 
				new BasicDBObject("$inc", new BasicDBObject("views", 1)));
	}
	
	//Load topics for given activity type
	public static List<TopicBean> loadTopics(String talkerId, String type) {
		DBCollection activitiesColl = getCollection(ActivityDAO.ACTIVITIES_COLLECTION);
		
		DBRef talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talkerId);
		DBObject query = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("type", type)
			.get();
		List<DBObject> activitiesDBList = 
			activitiesColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		List<TopicBean> topicsList = new ArrayList<TopicBean>();
		for (DBObject activityDBObject : activitiesDBList) {
			DBObject topicDBObject = ((DBRef)activityDBObject.get("topicId")).fetch();
			
			TopicBean topic = parseTopicBean(topicDBObject);
			topicsList.add(topic);
		}
		return topicsList;
	}
	
	public static void main(String[] args) {
		System.out.println(TopicDAO.getNumberOfTopics("4c2cb43160adf3055c97d061"));
		
//		TopicBean topic = new TopicBean();
//		topic.setTopic("test");
//		topic.setUid("4c2cb43160adf3055c97d061");
//		Date currentDate = Calendar.getInstance().getTime();
//		topic.setCreationDate(currentDate);
//		topic.setDisplayTime(currentDate);
//		TopicDAO.save(topic);
	}

}


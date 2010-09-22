package dao;

import static util.DBUtil.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import util.DBUtil;

import models.CommentBean;
import models.ConversationBean;
import models.TalkerBean;
import models.TopicBean;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

public class TopicDAO {
	
	public static final String TOPICS_COLLECTION = "tags";
	
	public static void save(TopicBean topic) {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		DBObject topicDBObject = BasicDBObjectBuilder.start()
			.add("title", topic.getTitle())
			.add("main_url", topic.getMainURL())
			.add("cr_date", new Date())
			.add("aliases", topic.getAliases())
			.add("children", topic.childrenToList())
			.get();
		topicsColl.save(topicDBObject);
		
		topic.setId(getString(topicDBObject, "_id"));
	}
	
	public static void updateTopic(TopicBean topic) {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		DBObject topicObject = BasicDBObjectBuilder.start()
			.add("title", topic.getTitle())
			.add("main_url", topic.getMainURL())
			.add("old_names", setToDB(topic.getOldNames()))
			.add("aliases", topic.getAliases())
			
			.add("children", topic.childrenToList())
			.add("deleted", topic.isDeleted())
			.get();
		
		DBObject topicId = new BasicDBObject("_id", new ObjectId(topic.getId()));
		topicsColl.update(topicId, new BasicDBObject("$set", topicObject));
	}
	
	/**
	 * Find by main URL (current) or old urls.
	 * @param url
	 * @return
	 */
	public static TopicBean getByURL(String url) {
		DBCollection topicsColl = getDB().getCollection(TOPICS_COLLECTION);
		
		DBObject query = BasicDBObjectBuilder.start()
			.add("$or", 
				Arrays.asList(
						new BasicDBObject("main_url", url),
						new BasicDBObject("old_names.url", url)
					)
			)
			.add("deleted", new BasicDBObject("$ne", true))
			.get();
		DBObject topicDBObject = topicsColl.findOne(query);
		
		if (topicDBObject == null) {
			return null;
		}
		
		TopicBean topic = new TopicBean();
		topic.parseFromDB(topicDBObject);
		return topic;
	}
	
	public static TopicBean getById(String topicId) {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		DBObject query = new BasicDBObject("_id", new ObjectId(topicId));
		DBObject topicDBObject = topicsColl.findOne(query);
		
		TopicBean topicBean = new TopicBean();
		topicBean.parseBasicFromDB(topicDBObject);
		return topicBean;
	}
	
	public static TopicBean getByTitle(String title) {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		DBObject query = new BasicDBObject("title", title);
		DBObject topicDBObject = topicsColl.findOne(query);
		
		if (topicDBObject == null) {
			return null;
		}
		
		TopicBean topicBean = new TopicBean();
		topicBean.parseBasicFromDB(topicDBObject);
		return topicBean;
	}
	
	public static List<TopicBean> loadAllTopics() {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		List<DBObject> topicsDBList = topicsColl.find().toArray();
		
		List<TopicBean> topicsList = new ArrayList<TopicBean>();
		for (DBObject topicDBObject : topicsDBList) {
			TopicBean topic = new TopicBean();
			topic.parseBasicFromDB(topicDBObject);
	    	topicsList.add(topic);
		}
		
		return topicsList;
	}
	
	public static List<TopicBean> getTopics() {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		List<DBObject> topicsDBList = 
			topicsColl.find().sort(new BasicDBObject("title", 1)).toArray();
		List<TopicBean> topics = new ArrayList<TopicBean>();
		for (DBObject topicDBObject : topicsDBList) {
			TopicBean topic = new TopicBean();
			topic.parseBasicFromDB(topicDBObject);
			topics.add(topic);
		}
		return topics;
	}
	
	public static Set<TopicBean> getParentTopics(String topicId) {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		DBRef topicDBRef = createRef(TOPICS_COLLECTION, topicId);
		DBObject query = new BasicDBObject("children", topicDBRef);
		List<DBObject> topicsDBList = topicsColl.find(query).toArray();
		
		Set<TopicBean> parentTopics = new HashSet<TopicBean>();
		for (DBObject topicDBObject : topicsDBList) {
			TopicBean parentTopic = new TopicBean();
			parentTopic.setId(topicDBObject.get("_id").toString());
			parentTopic.setTitle((String)topicDBObject.get("title"));
			parentTopics.add(parentTopic);
		}
		return parentTopics;
	}
	
	
	public static void incrementTopicViews(String topicId) {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		DBObject topicIdDBObject = new BasicDBObject("_id", new ObjectId(topicId));
		topicsColl.update(topicIdDBObject, 
				new BasicDBObject("$inc", new BasicDBObject("views", 1)));
	}
	
//	public static void main(String[] args) {
//    	String newTag = "thirdtopic";
//    	TopicBean topic = new TopicBean();
//    	topic.setTitle(newTag);
//    	topic.setMainURL(ApplicationDAO.createURLName(newTag));
//    	TopicDAO.save(topic);
//	}
}

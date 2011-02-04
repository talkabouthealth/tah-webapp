package dao;

import static util.DBUtil.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import util.DBUtil;

import logic.TopicLogic;
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
	
	public static final String TOPICS_COLLECTION = "topics";
	
	public static void save(TopicBean topic) {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		DBObject topicDBObject = BasicDBObjectBuilder.start()
			.add("title", topic.getTitle())
			.add("main_url", topic.getMainURL())
			.add("cr_date", new Date())
			
			.add("aliases", topic.getAliases())
			.add("bitly", topic.getBitly())
			.add("fixed", topic.isFixed())
			
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
			.add("fixed", topic.isFixed())
			.add("deleted", topic.isDeleted())
			.add("bitly", topic.getBitly())
			.add("summary", topic.getSummary())
			
			.add("children", topic.childrenToList())
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
			.get();
		DBObject topicDBObject = topicsColl.findOne(query);
		
		if (topicDBObject == null) {
			return null;
		}
		
		TopicBean topic = new TopicBean();
		topic.parseFromDB(topicDBObject);
		topic.setConversations(ConversationDAO.loadConversationsByTopic(topic.getId()));
    	topic.setFollowers(getTopicFollowers(topic));
		return topic;
	}
	
	public static TopicBean getById(String topicId) {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		DBObject query = new BasicDBObject("_id", new ObjectId(topicId));
		DBObject topicDBObject = topicsColl.findOne(query);
		
		TopicBean topicBean = new TopicBean();
		topicBean.parseFromDB(topicDBObject);
		return topicBean;
	}
	
	/**
	 * Gets or recreates topic if it was deleted.
	 * @param title
	 * @return
	 */
	public static TopicBean getOrRestoreByTitle(String title) {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		DBObject query = new BasicDBObject("title", title);
		DBObject topicDBObject = topicsColl.findOne(query);
		if (topicDBObject == null) {
			return null;
		}
		
		TopicBean topicBean = new TopicBean();
		topicBean.parseFromDB(topicDBObject);
		
		if (topicBean.isDeleted()) {
			topicBean.setDeleted(false);
			TopicLogic.addToDefaultParent(topicBean);
			updateTopic(topicBean);
		}
		return topicBean;
	}
	
	
	/**
	 * Loads all topics
	 * @param loadBasicInfo Determines how much information to load.
	 * @return
	 */
	public static Set<TopicBean> loadAllTopics(boolean loadBasicInfo) {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		DBObject query = new BasicDBObject("deleted", new BasicDBObject("$ne", true));
		List<DBObject> topicsDBList = topicsColl.find(query).sort(new BasicDBObject("views", -1)).toArray();
		
		Set<TopicBean> topicsSet = new LinkedHashSet<TopicBean>();
		for (DBObject topicDBObject : topicsDBList) {
			TopicBean topic = new TopicBean();
			if (loadBasicInfo) {
				topic.parseBasicFromDB(topicDBObject);
			}
			else {
				topic.parseFromDB(topicDBObject);
			}
			topicsSet.add(topic);
		}
		return topicsSet;
	}
	public static Set<TopicBean> loadAllTopics() {
		return loadAllTopics(false);
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
			parentTopic.setMainURL((String)topicDBObject.get("main_url"));
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
	
	public static List<TalkerBean> getTopicFollowers(TopicBean topic) {
		DBCollection talkersColl = getCollection(TalkerDAO.TALKERS_COLLECTION);
		
    	DBRef topicRef = createRef(TopicDAO.TOPICS_COLLECTION, topic.getId());
    	DBObject query = new BasicDBObject("following_topics", topicRef);
    	List<DBObject> followersDBList = talkersColl.find(query).toArray();
    	
    	List<TalkerBean> followers = new ArrayList<TalkerBean>();
    	for (DBObject followerDBObject : followersDBList) {
    		TalkerBean followerTalker = new TalkerBean();
    		followerTalker.parseBasicFromDB(followerDBObject);
			followers.add(followerTalker);
    	}
    	return followers;
	}
	
}

package dao;

import static util.DBUtil.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

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
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import controllers.Conversations;

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
			
			.add("last_update", new Date())
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
			
			.add("last_update", new Date())
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
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		DBObject query = BasicDBObjectBuilder.start()
			.add("$or", 
				Arrays.asList(
						new BasicDBObject("main_url", Pattern.compile("^"+url+"$" , Pattern.CASE_INSENSITIVE)),
						new BasicDBObject("old_names.url", Pattern.compile("^"+url+"$" , Pattern.CASE_INSENSITIVE))
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
	
	public static TopicBean getByIdBasic(String topicId) {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		DBObject fields = BasicDBObjectBuilder.start()
			.add("title", 1)
			.add("fixed", 1)
			.add("deleted", 1)
			.add("main_url", 1)
			.add("bitly", 1)
			.add("views", 1)
			.add("cr_date", 1)
			.get();
		
		DBObject query = new BasicDBObject("_id", new ObjectId(topicId));
		DBObject topicDBObject = topicsColl.findOne(query, fields);
		
		TopicBean topicBean = new TopicBean();
		topicBean.parseBasicFromDB(topicDBObject);
		return topicBean;
	}
	
	/**
	 * Gets or recreates topic if it was deleted.
	 * @param title
	 * @return
	 */
	public static TopicBean getOrRestoreByTitle(String title) {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		topicsColl.ensureIndex(new BasicDBObject("title", 1));
		
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
	 * Loads all topics, sorted by views number
	 * @param onlyBasicInfo Determines how much information to load.
	 * @return
	 */
	public static Set<TopicBean> loadAllTopics(boolean onlyBasicInfo) {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		topicsColl.ensureIndex(new BasicDBObject("views", 1));
		
		DBObject query = new BasicDBObject("deleted", new BasicDBObject("$ne", true));
		List<DBObject> topicsDBList = null;
		if (onlyBasicInfo) {
			DBObject fields = BasicDBObjectBuilder.start()
				.add("title", 1)
				.add("fixed", 1)
				.add("deleted", 1)
				.add("main_url", 1)
				.add("bitly", 1)
				.add("views", 1)
				.add("cr_date", 1)
				.get();
			
			topicsDBList=new ArrayList<DBObject>();//topicsDBList = topicsColl.find(query, fields).sort(new BasicDBObject("views", -1)).toArray();
			DBCursor topicCur=topicsColl.find(query, fields).sort(new BasicDBObject("views", -1));
			while(topicCur.hasNext()){
				topicsDBList.add(topicCur.next());
			}
			
			
		}
		else {
			topicsDBList=new ArrayList<DBObject>();//topicsDBList = topicsColl.find(query).sort(new BasicDBObject("views", -1)).toArray();
			DBCursor topicCur= topicsColl.find(query).sort(new BasicDBObject("views", -1));
			while(topicCur.hasNext()){
				topicsDBList.add(topicCur.next());
			}
			
		}
		
		Set<TopicBean> topicsSet = new LinkedHashSet<TopicBean>();
		for (DBObject topicDBObject : topicsDBList) {
			TopicBean topic = new TopicBean();
			if (onlyBasicInfo) {
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
	
	
	/**
	 * 
	 * @param timelimit
	 * @return
	 */
	public static Set<TopicBean> loadUpdatedTopics(int limit) {
		boolean onlyBasicInfo=false;
		
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		topicsColl.ensureIndex(new BasicDBObject("views", 1));

		
		
		Calendar cal= Calendar.getInstance();
		cal.add(Calendar.MINUTE, -limit);
		Date date=cal.getTime();
		BasicDBObject time = new BasicDBObject("$gt", date);
		
		DBObject query =BasicDBObjectBuilder.start()
		.add("cr_date", time)
		.add("deleted",new BasicDBObject("$ne", true))
		.get();
		
		List<DBObject> topicsDBList = null;
		
		if (onlyBasicInfo) {
			DBObject fields = BasicDBObjectBuilder.start()
				.add("title", 1)
				.add("fixed", 1)
				.add("deleted", 1)
				.add("main_url", 1)
				.add("bitly", 1)
				.add("views", 1)
				.add("cr_date", 1)
				.get();
			
			
			topicsDBList=new ArrayList<DBObject>();//topicsDBList = topicsColl.find(query, fields).sort(new BasicDBObject("views", -1)).toArray();
			DBCursor topicCur=topicsColl.find(query, fields).sort(new BasicDBObject("views", -1));
			while(topicCur.hasNext()){
				topicsDBList.add(topicCur.next());
			}
			
		}
		else {
			
			topicsDBList=new ArrayList<DBObject>();//topicsDBList = topicsColl.find(query).sort(new BasicDBObject("views", -1)).toArray();
			DBCursor topicCur= topicsColl.find(query).sort(new BasicDBObject("views", -1));
			while(topicCur.hasNext()){
				topicsDBList.add(topicCur.next());
			}
			
		}
		
		Set<TopicBean> topicsSet = new LinkedHashSet<TopicBean>();
		for (DBObject topicDBObject : topicsDBList) {
			TopicBean topic = new TopicBean();
			if (onlyBasicInfo) {
				topic.parseBasicFromDB(topicDBObject);
			}
			else {
				topic.parseFromDB(topicDBObject);
			}
			topicsSet.add(topic);
		}
		return topicsSet;
	}
	
	
	/**
	 * Recursive method that saves all subtree of given root topic to 'allTopics' list.
	 */
	public static void loadSubTopicsAsTree(List<DBRef> allTopics, TopicBean rootTopic) {
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
				
				loadSubTopicsAsTree(allTopics, fullChild);
			}
		}
	}
	
	public static Set<TopicBean> getParentTopics(String topicId) {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		topicsColl.ensureIndex(new BasicDBObject("children", 1));
		
		DBObject fields = BasicDBObjectBuilder.start()
			.add("title", 1)
			.add("main_url", 1)
			.get();
		
		DBRef topicDBRef = createRef(TOPICS_COLLECTION, topicId);
		DBObject query = new BasicDBObject("children", topicDBRef);
		
		List<DBObject> topicsDBList=new ArrayList<DBObject>();//List<DBObject> topicsDBList = topicsColl.find(query, fields).toArray();
		DBCursor topicCur=topicsColl.find(query, fields);
		while(topicCur.hasNext()){
			topicsDBList.add(topicCur.next());
		}
		
		
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
    	
    	List<DBObject> followersDBList=new ArrayList<DBObject>();//List<DBObject> followersDBList = talkersColl.find(query).toArray();
		DBCursor topicCur=talkersColl.find(query);
		while(topicCur.hasNext()){
			followersDBList.add(topicCur.next());
		}
    	
    	
    	List<TalkerBean> followers = new ArrayList<TalkerBean>();
    	for (DBObject followerDBObject : followersDBList) {
    		TalkerBean followerTalker = new TalkerBean();
    		followerTalker.parseBasicFromDB(followerDBObject);
			followers.add(followerTalker);
    	}
    	return followers;
	}

	public static int getTopicFollowersCount(TopicBean topic) {
		DBCollection talkersColl = getCollection(TalkerDAO.TALKERS_COLLECTION);
		
    	DBRef topicRef = createRef(TopicDAO.TOPICS_COLLECTION, topic.getId());
    	DBObject query = new BasicDBObject("following_topics", topicRef);
    	return talkersColl.find(query).count();
	}
	/**
	 * Loads the 20 most recently updated Topics
	 */
	public static List<TopicBean> getRecentTopics() {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		DBObject query = new BasicDBObject("deleted", new BasicDBObject("$ne", true));
		
		List<DBObject> topicsDBList=new ArrayList<DBObject>();//List<DBObject> topicsDBList = topicsColl.find(query).sort(new BasicDBObject("last_update", -1)).toArray();
		DBCursor topicCur=topicsColl.find(query).sort(new BasicDBObject("last_update", -1));
		while(topicCur.hasNext()){
			topicsDBList.add(topicCur.next());
		}
		
		
		List<TopicBean> recentTopics = new ArrayList<TopicBean>();
		for (DBObject topicDBObject : topicsDBList) {
			TopicBean topic = new TopicBean();
			topic.parseBasicFromDB(topicDBObject);
			topic.setConversations(ConversationDAO.loadConversationsByTopic(topic.getId()));
			recentTopics.add(topic);
			
			if (recentTopics.size() == 20) {
				break;
			}
		}
		return recentTopics;
	}
	
	/**
	 * Updates the topic
	 * @param topic
	 */
	public static void updateTopicbyId(TopicBean topic,String name, String value){
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		DBObject topicObject = BasicDBObjectBuilder.start()
		.add(name, value)
		.get();
		
		DBObject topicId = new BasicDBObject("_id", topic.getId());
		topicsColl.update(topicId, new BasicDBObject("$set", topicObject),false,true);
		
	}

	
}

package dao;

import static util.DBUtil.createRef;
import static util.DBUtil.getCollection;
import static util.DBUtil.getDB;
import static util.DBUtil.getString;

import java.util.Arrays;
import java.util.Date;

import org.bson.types.ObjectId;

import util.DBUtil;

import models.CommentBean;
import models.ConversationBean;
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
			.get();
		topicsColl.save(topicDBObject);
		
		topic.setId(getString(topicDBObject, "_id"));
	}
	
	/**
	 * Find by main URL (current) or old urls.
	 * @param url
	 * @return
	 */
	public static TopicBean getByURL(String url) {
		DBCollection topicsColl = getDB().getCollection(TOPICS_COLLECTION);
		
		DBObject query = new BasicDBObject("$or", 
				Arrays.asList(
						new BasicDBObject("main_url", url),
						new BasicDBObject("urls", url)
					)
			);
		DBObject topicDBObject = topicsColl.findOne(query);
		
		TopicBean topic = new TopicBean();
		topic.setId(getString(topicDBObject, "_id"));
		topic.setTitle((String)topicDBObject.get("title"));
		topic.setMainURL((String)topicDBObject.get("main_url"));
		topic.setViews(DBUtil.getInt(topicDBObject, "views"));
		topic.setCreationDate((Date)topicDBObject.get("cr_date"));
		return topic;
	}
	
	public static void incrementTopicViews(String topicId) {
		DBCollection topicsColl = getCollection(TOPICS_COLLECTION);
		
		DBObject topicIdDBObject = new BasicDBObject("_id", new ObjectId(topicId));
		topicsColl.update(topicIdDBObject, 
				new BasicDBObject("$inc", new BasicDBObject("views", 1)));
	}

}

package logic;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.DBRef;

import models.TopicBean;
import dao.ApplicationDAO;
import dao.ConversationDAO;
import dao.TopicDAO;

public class TopicLogic {
	
	public static final String DEFAULT_TOPIC = "Unorganized";
	
	//TODO: not used?
	public static TopicBean findOrCreateTopic(String topicTitle) {
    	TopicBean topic = TopicDAO.getByTitle(topicTitle);
    	if (topic == null) {
    		topic = new TopicBean();
        	topic.setTitle(topicTitle);
        	topic.setMainURL(ApplicationDAO.createURLName(topicTitle));
        	TopicDAO.save(topic);
    	}
    	
    	return topic;
	}

	public static void addToDefaultParent(TopicBean topic) {
		TopicBean defaultTopic = TopicDAO.getByTitle(DEFAULT_TOPIC);
		if (defaultTopic != null) {
			defaultTopic.getChildren().add(topic);
			TopicDAO.updateTopic(defaultTopic);
		}
	}
	
	public static List<String> getTopicsTree(TopicBean parentTopic) {
		List<DBRef> allTopics = new ArrayList<DBRef>();
		ConversationDAO.getAllTopics(allTopics, parentTopic);
		
		List<String> topicsTree = new ArrayList<String>();
		for (DBRef topicRef : allTopics) {
			topicsTree.add(topicRef.getId().toString());
		}
		return topicsTree;
	}

}

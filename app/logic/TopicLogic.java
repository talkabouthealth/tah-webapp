package logic;

import models.TopicBean;
import dao.ApplicationDAO;
import dao.TopicDAO;

public class TopicLogic {
	
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

}

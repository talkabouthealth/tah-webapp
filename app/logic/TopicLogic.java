package logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.mongodb.DBRef;

import models.TopicBean;
import dao.ApplicationDAO;
import dao.ConversationDAO;
import dao.TopicDAO;

public class TopicLogic {
	
	public static final String DEFAULT_TOPIC = "Unorganized";
	
	public static void addToDefaultParent(TopicBean topic) {
		TopicBean defaultTopic = TopicDAO.getOrRestoreByTitle(DEFAULT_TOPIC);
		if (defaultTopic != null) {
			defaultTopic.getChildren().add(topic);
			TopicDAO.updateTopic(defaultTopic);
		}
	}
	
	public static Set<TopicBean> getAllTopicsTree() {
		Set<TopicBean> topics = TopicDAO.loadAllTopics();
    	Map<String, TopicBean> topicsMap = new HashMap<String, TopicBean>();
    	for (TopicBean topic : topics) {
    		topicsMap.put(topic.getId(), topic);
    	}
    	
		Set<TopicBean> topicsTree = new TreeSet<TopicBean>();
    	for (TopicBean topic : topics) {
    		if (topic.getParents() == null || topic.getParents().size() == 0) {
    			topicsTree.add(topic);
    			topic.setChildren(buildTree(topic, topicsMap));
    		}
    	}
    	
    	return topicsTree;
	}
	
	private static Set<TopicBean> buildTree(TopicBean topic, Map<String, TopicBean> topicsMap) {
		Set<TopicBean> newChildren = new TreeSet<TopicBean>();
		for (TopicBean child : topic.getChildren()) {
			TopicBean newChild = topicsMap.get(child.getId());
			newChildren.add(newChild);
			newChild.setChildren(buildTree(newChild, topicsMap));
		}
		return newChildren;
	}
	
	
	public static List<String> getSubTopics(TopicBean parentTopic) {
		List<DBRef> allTopics = new ArrayList<DBRef>();
		ConversationDAO.getAllTopics(allTopics, parentTopic);
		
		List<String> topicsTree = new ArrayList<String>();
		for (DBRef topicRef : allTopics) {
			topicsTree.add(topicRef.getId().toString());
		}
		return topicsTree;
	}

}

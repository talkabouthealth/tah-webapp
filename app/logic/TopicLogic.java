package logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
	
	/**
	 * Default parent topic
	 */
	public static final String DEFAULT_TOPIC = "Unorganized";
	
	public static void addToDefaultParent(TopicBean topic) {
		TopicBean defaultTopic = TopicDAO.getOrRestoreByTitle(DEFAULT_TOPIC);
		if (defaultTopic != null) {
			defaultTopic.getChildren().add(topic);
			TopicDAO.updateTopic(defaultTopic);
		}
	}
	
	/**
	 * Get all topics as tree.
	 */
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
	
	/**
	 * Recursive method for building topic tree
	 * 
	 * @param rootTopic Root topic of this subtree.
	 * @param topicsMap Map of all topics, key - id, value - topic object.
	 * @return
	 */
	private static Set<TopicBean> buildTree(TopicBean rootTopic, Map<String, TopicBean> topicsMap) {
		Set<TopicBean> newChildren = new TreeSet<TopicBean>();
		for (TopicBean child : rootTopic.getChildren()) {
			TopicBean newChild = topicsMap.get(child.getId());
			newChildren.add(newChild);
			newChild.setChildren(buildTree(newChild, topicsMap));
		}
		return newChildren;
	}
	
	
	/**
	 * Get list of all topics in subtree
	 * @param parentTopic
	 * @return
	 */
	public static List<String> getSubTopics(TopicBean parentTopic) {
		List<DBRef> allTopics = new ArrayList<DBRef>();
		TopicDAO.loadSubTopicsAsTree(allTopics, parentTopic);
		
		List<String> topicsTree = new ArrayList<String>();
		for (DBRef topicRef : allTopics) {
			topicsTree.add(topicRef.getId().toString());
		}
		return topicsTree;
	}
	
	/**
	 * Parse set of topics from comma-separated string of topic names
	 * 
	 * @param topics
	 * @return
	 */
	public static Set<TopicBean> parseTopicsFromString(String topics) {
		Set<TopicBean> topicsSet = new HashSet<TopicBean>();
    	String[] topicsArr = topics.split(",");
    	for (String topicTitle : topicsArr) {
    		if (topicTitle.trim().length() != 0) {
    			TopicBean topic = TopicDAO.getOrRestoreByTitle(topicTitle.trim());
        		if (topic != null) {
        			topicsSet.add(topic);
        		}
    		}
    	}
		return topicsSet;
	}

}

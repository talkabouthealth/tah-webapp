package logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import play.Logger;

import com.mongodb.DBRef;

import models.CommentBean;
import models.TalkerBean;
import models.TalkerDiseaseBean;
import models.TopicBean;
import models.actions.Action;
import dao.ApplicationDAO;
import dao.CommentsDAO;
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
		//TODO: improve here? we can keep all in cache?
		Set<TopicBean> topics = TopicDAO.loadAllTopics();
    	Map<String, TopicBean> topicsMap = new HashMap<String, TopicBean>();
    	for (TopicBean topic : topics) {
    		topicsMap.put(topic.getId(), topic);
    	}
    	
    	Set<TopicBean> rootTopics = new TreeSet<TopicBean>();
    	for (TopicBean topic : topics) {
    		if (topic.getParents() == null || topic.getParents().size() == 0) {
    			rootTopics.add(topic);
    			topic.setChildren(buildTree(topic, topicsMap));
    		}
    	}
    	
    	return rootTopics;
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
	
	/**
	 * Return recommended topics for given talker.
	 * Two criterias:
	 * - based on HealthInfo;
	 * - popularity of topics (number of questions)
	 * 
	 * @param afterId id of last topic from previous load (used for paging)
	 */
	public static List<TopicBean> loadRecommendedTopics(TalkerBean talker, 
			TalkerDiseaseBean talkerDisease, String afterId) {
		List<TopicBean> recommendedTopics = new ArrayList<TopicBean>();
		List<TopicBean> loadedTopics = new ArrayList<TopicBean>();
		if (!talker.isProf() && talkerDisease != null) {
			loadedTopics = TalkerLogic.getTopicsByHealthInfo(talkerDisease);
		}
		if (recommendedTopics.isEmpty()) {
			//display most popular Topics based on views
			loadedTopics = new ArrayList<TopicBean>(TalkerLogic.loadAllTopicsFromCache());
		}
		
		final int numberPerPage = 10;
		boolean canAdd = (afterId == null);
		for (TopicBean topic : loadedTopics) {
			if (canAdd && !talker.getFollowingTopicsList().contains(topic)) {
				//recommended topics shouldn't contain default topics
				if (! (topic.getTitle().equals(ConversationLogic.DEFAULT_QUESTION_TOPIC) 
						|| topic.getTitle().equals(ConversationLogic.DEFAULT_TALK_TOPIC)) ) {
					recommendedTopics.add(topic);
				}
			}
			if (topic.getId().equals(afterId)) {
				canAdd = true;
			}
			//enough for this page?
			if (recommendedTopics.size() == numberPerPage) {
				break;
			}
		}
		
		return recommendedTopics;
	}

	/**
	 * Loads the most popular 20 topics, based on number of questions.
	 */
	public static List<TopicBean> loadPopularTopics() {
		List<TopicBean> popularTopics = new ArrayList<TopicBean>();
		for (TopicBean topic : TalkerLogic.loadAllTopicsFromCache()) {
			if (topic.getConversations() == null) {
				topic.setConversations(ConversationDAO.loadConversationsByTopic(topic.getId()));
			}
			popularTopics.add(topic);
		}
		
		//sort by number of questions
		Collections.sort(popularTopics, new Comparator<TopicBean>() {
			@Override
			public int compare(TopicBean o1, TopicBean o2) {
				return o2.getConversations().size() - o1.getConversations().size();
			}		
		});
		if (popularTopics.size() > 20) {
			popularTopics = popularTopics.subList(0, 20);
		}
		return popularTopics;
	}
}

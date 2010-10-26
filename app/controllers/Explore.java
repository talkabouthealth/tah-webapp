package controllers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import play.mvc.Controller;

import models.ConversationBean;
import models.TalkerBean;
import models.TopicBean;
import util.CommonUtil;
import util.SearchUtil;
import dao.ConversationDAO;
import dao.TalkerDAO;
import dao.TopicDAO;

public class Explore extends Controller {
	
	public static void openQuestions() {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	if (talker != null) {
    		talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
    	}
		
		List<ConversationBean> openQuestions = ConversationDAO.getOpenQuestions();
		render(talker, openQuestions);
    }
    
    public static void liveTalks() {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	if (talker != null) {
    		talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
    	}
		
    	List<ConversationBean> liveTalks = ConversationDAO.getLiveConversations();
		render(talker, liveTalks);
    }
    
    
    public static void browseTopics() {
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
    	
    	render(topicsTree);
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
	
	public static void searchConversations() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		String query = params.get("query");
		List<ConversationBean> results = null;
		if (query != null) {
			params.flash("query");
			
			try {
				results = SearchUtil.searchConvo(query);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		render(talker, results);
	}

}

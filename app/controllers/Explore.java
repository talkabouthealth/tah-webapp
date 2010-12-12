package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import play.mvc.Controller;
import play.mvc.With;

import logic.FeedsLogic;
import logic.TalkerLogic;
import models.ConversationBean;
import models.TalkerBean;
import models.TopicBean;
import models.actions.Action;
import util.BitlyUtil;
import util.CommonUtil;
import util.FacebookUtil;
import util.SearchUtil;
import util.TwitterUtil;
import dao.ActionDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;
import dao.TopicDAO;

@With( LoggerController.class )
public class Explore extends Controller {
	
	public static void openQuestions() {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	if (talker != null) {
    		talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
    		talker.setActivityList(ActionDAO.load(talker.getId()));
    		TalkerLogic.calculateProfileCompletion(talker);
    	}
		
		List<ConversationBean> openQuestions = ConversationDAO.getOpenQuestions();
		render(talker, openQuestions);
    }
    
    public static void liveTalks() {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	if (talker != null) {
    		talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
    		talker.setActivityList(ActionDAO.load(talker.getId()));
    		TalkerLogic.calculateProfileCompletion(talker);
    	}
    	
    	//TwitterUtil.importTweets((String)session.get("twitter_token"), (String)session.get("twitter_token_secret"));
    	//TwitterUtil.loadMentions();
    	//FacebookUtil.post("Test cool", (String)session.get("fb_token"));
    	//FacebookUtil.likeTAH((String)session.get("fb_token"));
    	//FacebookUtil.importPosts((String)session.get("fb_token"));
    	
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

	public static void conversations(String action) {
		Set<Action> communityFeed = FeedsLogic.getCommunityFeed(null);
		
		//- "Popular Conversations" - ordered by page views
		List<ConversationBean> popularConvos = ConversationDAO.loadPopularConversations();
		//TODO: move to comparator?
		Collections.sort(popularConvos, new Comparator<ConversationBean>() {
			@Override
			public int compare(ConversationBean o1, ConversationBean o2) {
				return o2.getViews()-o1.getViews();
			}
		});
		
		if (action == null) {
			action = "active";
		}
		render(action, communityFeed, popularConvos);
	}
}

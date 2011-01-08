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

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

import logic.FeedsLogic;
import logic.TalkerLogic;
import logic.TopicLogic;
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
    		TalkerLogic.preloadTalkerInfo(talker);
    	}
		
		List<ConversationBean> openQuestions = ConversationDAO.getOpenQuestions();
		render(talker, openQuestions);
    }
    
    public static void liveTalks() {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	if (talker != null) {
    		TalkerLogic.preloadTalkerInfo(talker);
    	}
    	
    	List<ConversationBean> liveTalks = ConversationDAO.getLiveConversations();
		render(talker, liveTalks);
    }
    
    public static void browseTopics() {
    	Set<TopicBean> topicsTree = TopicLogic.getAllTopicsTree();
    	render(topicsTree);
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
				Logger.error(e, "Search Conversations error");
			}
		}
		
		render(talker, results);
	}

	// Conversations Feed and popular conversations
	public static void conversations(String action) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		boolean loggedIn = (talker != null);
		Set<Action> communityFeed = FeedsLogic.getCommunityFeed(null, loggedIn);
		
		//"Popular Conversations" - ordered by page views
		List<ConversationBean> popularConvos = ConversationDAO.loadPopularConversations();
		Collections.sort(popularConvos, new Comparator<ConversationBean>() {
			@Override
			public int compare(ConversationBean o1, ConversationBean o2) {
				return o2.getViews()-o1.getViews();
			}
		});
		
		if (action == null) {
			action = "feed";
		}
		render(action, communityFeed, popularConvos);
	}
}

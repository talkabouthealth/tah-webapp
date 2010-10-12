package controllers;

import java.util.List;
import java.util.Set;

import models.ConversationBean;
import models.TalkerBean;
import models.actions.Action;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;
import util.SearchUtil;
import dao.ActionDAO;
import dao.ApplicationDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;

@With(Secure.class)
public class Community extends Controller {
	
	public static void viewCommunity() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		List<TalkerBean> communityMembers = TalkerDAO.loadAllTalkers();
		
		List<ConversationBean> liveTalks = ConversationDAO.getLiveConversations();
		List<Action> communityConvoFeed = ActionDAO.loadCommunityConvoFeed();
		
		render(talker, liveTalks, communityConvoFeed);
	}
	
//	For the 4 tabs:
//		Active Users - Latest users to log in
//		New Users - Latest users to sign up
//		Like You - we will implement logic later, I think we can use Lucene or Sphinx
//		Search - implement later with Lucene or Sphinx
	public static void browseMembers(String action) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);

		Set<TalkerBean> activeTalkers = ApplicationDAO.getActiveTalkers();
		Set<TalkerBean> newTalkers = ApplicationDAO.getNewTalkers();
		
		String query = params.get("query");
		List<TalkerBean> results = null;
		if (query != null) {
			params.flash("query");
			
			try {
				results = SearchUtil.searchTalker(query);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		
		if (action == null) {
			action = "active";
		}
		render(currentTalker, action, activeTalkers, newTalkers, results);
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
				//TODO: better handling?
				e.printStackTrace();
			}
		}
		
		render(talker, results);
	}

}

package controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import dao.ApplicationDAO;

import models.TalkerBean;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;
import util.SearchUtil;

@With(Secure.class)
public class Community extends Controller {
	
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
				//TODO: better handling?
				e.printStackTrace();
			}
		}
		
		
		if (action == null) {
			action = "active";
		}
		render(currentTalker, action, activeTalkers, newTalkers, results);
	}
	
	public static void searchConversations() {
		String query = params.get("query");
		List<String> results = null;
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
		
		render(results);
	}

}

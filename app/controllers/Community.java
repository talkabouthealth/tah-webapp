package controllers;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import logic.FeedsLogic;
import models.CommentBean;
import models.ConversationBean;
import models.TalkerBean;
import models.actions.Action;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;
import util.SearchUtil;
import dao.ActionDAO;
import dao.ApplicationDAO;
import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;

//FIXME - check permissions?
public class Community extends Controller {
	
	public static void viewCommunity() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		List<TalkerBean> communityMembers = TalkerDAO.loadAllTalkers();
		for (TalkerBean member : communityMembers) {
			List<CommentBean> answers = CommentsDAO.getTalkerConvoAnswers(member.getId(), null);
			member.setNumOfConvoAnswers(answers.size());
		}
		
		//get only Top 3 by answers
		Collections.sort(communityMembers, new Comparator<TalkerBean>() {
			@Override
			public int compare(TalkerBean o1, TalkerBean o2) {
				return o2.getNumOfConvoAnswers()-o1.getNumOfConvoAnswers();
			}
		});
		if (communityMembers.size() > 3) {
			communityMembers = communityMembers.subList(0, 3);
		}
		
		List<ConversationBean> liveTalks = ConversationDAO.getLiveConversations();
		Set<Action> communityConvoFeed = FeedsLogic.getCommunityFeed(null);
		
		render(talker, liveTalks, communityConvoFeed, communityMembers);
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
				e.printStackTrace();
			}
		}
		
		render(talker, results);
	}

}

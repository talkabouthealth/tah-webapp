package controllers;

import java.util.Set;

import dao.ApplicationDAO;

import models.TalkerBean;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;

@With(Secure.class)
public class Community extends Controller {
	
//	For the 4 tabs:
//		Active Users - Latest users to log in
//		New Users - Latest users to sign up
//		Like You - we will implement logic later, I think we can use Lucene or Sphinx
//		Search - implement later with Lucene or Sphinx
	public static void browseMembers(String action) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);

		Set<TalkerBean> activeTalkers = ApplicationDAO.getActiveTalkers();
		Set<TalkerBean> newTalkers = ApplicationDAO.getNewTalkers();
		
		if (action == null) {
			action = "active";
		}
		render(talker, action, activeTalkers, newTalkers);
	}

}

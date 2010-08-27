package controllers;

import dao.ActivityDAO;
import dao.ConversationDAO;
import models.TalkerBean;
import models.ConversationBean;
import models.actions.JoinConvoAction;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;

@With(Secure.class)
public class Talk extends Controller {
	
	public static void talkApp(Integer convoId) {
		notFoundIfNull(convoId);
		
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		ConversationBean topic = ConversationDAO.getByTid(convoId);
		
		notFoundIfNull(topic);
		
		ActivityDAO.saveActivity(new JoinConvoAction(talker, topic));
		
		render(talker, topic);
	}

}

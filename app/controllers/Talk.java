package controllers;

import dao.ActionDAO;
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
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		ConversationBean topic = ConversationDAO.getByTid(convoId);
		
		notFoundIfNull(topic);
		
		//we do not save "Join Action" for author
		if (!topic.getTalker().equals(talker)) {
			ActionDAO.saveAction(new JoinConvoAction(talker, topic));
		}
		
		render(talker, topic);
	}

}

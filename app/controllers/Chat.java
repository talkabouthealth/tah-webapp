package controllers;

import dao.TopicDAO;
import models.TalkerBean;
import models.TopicBean;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;

@With(Secure.class)
public class Chat extends Controller {
	
	public static void chatApp(String topicId) {
		notFoundIfNull(topicId);
		
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		TopicBean topic = TopicDAO.getById(topicId);
		
		notFoundIfNull(topic);
		
		render(talker, topic);
	}

}

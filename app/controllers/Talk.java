package controllers;

import dao.TopicDAO;
import models.TalkerBean;
import models.TopicBean;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;

@With(Secure.class)
public class Talk extends Controller {
	
	public static void talkApp(Integer topicId) {
		notFoundIfNull(topicId);
		
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		TopicBean topic = TopicDAO.getByTid(topicId);
		
		notFoundIfNull(topic);
		
		render(talker, topic);
	}

}

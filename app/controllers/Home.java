package controllers;

import java.util.LinkedHashMap;
import java.util.Map;

import models.TalkerBean;
import models.TopicBean;
import play.cache.Cache;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;
import webapp.LiveConversationsSingleton;
import dao.TopicDAO;

@With(Secure.class)
public class Home extends Controller {

    public static void index(String newtopic) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	talker.setNumberOfTopics(TopicDAO.getNumberOfTopics(talker.getId()));
		
		Map<String, TopicBean> mapTalkmiTopics = new LinkedHashMap<String, TopicBean>(40);
		LiveConversationsSingleton lcs = LiveConversationsSingleton.getReference();
		if(lcs.getLiveConversationMap().size() < 20) {
			mapTalkmiTopics = TopicDAO.queryTopics();
		}
		Cache.set(session.getId()+"-mapTalkmiTopics", mapTalkmiTopics);
		
		if (newtopic == null) {
			newtopic = "Please enter your Conversation here ...";
		}
		
		//For loading previous/next topics. Do we need to use this?
//		if (count == 1) {
//			SimpleDateFormat SQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//			session.setAttribute("latesttimestamp", SQL_DATE_FORMAT.format(tbTalkmiTopic.getDisplayTime()));
//			//System.out.println("Latest date: " + SQL_DATE_FORMAT.format(tbTalkmiTopic.getDisplayTime()));
//		} else if (count == 40) {
//			SimpleDateFormat SQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//			session.setAttribute("earliesttimestamp", SQL_DATE_FORMAT.format(tbTalkmiTopic.getDisplayTime()));
//			//System.out.println("Earliest date: " + SQL_DATE_FORMAT.format(tbTalkmiTopic.getDisplayTime()));
//		}
		
        render(talker, mapTalkmiTopics, newtopic);
    }

}

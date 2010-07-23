package controllers;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import models.LiveConversationBean;
import models.TalkerBean;
import models.TopicBean;
import play.cache.Cache;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;
import util.DBUtil;
import webapp.LiveConversationsSingleton;
import dao.ActivityDAO;
import dao.TopicDAO;

@With(Secure.class)
public class Topics extends Controller {

    public static void create(String newTopic) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		// initiate new conversation room

		// call notification module
		// start new thread

		// create topic bean
		TopicBean topic = new TopicBean();
		topic.setTopic(newTopic);
		topic.setUid(talker.getId());
		Date currentDate = Calendar.getInstance().getTime();
		topic.setCreationDate(currentDate);
		topic.setDisplayTime(currentDate);

		// insert new topic into database
		int tid = TopicDAO.save(topic);
		if (tid != -1) {
			topic.setTid(tid);
		} else {
			new Exception("DB Problem - Topic not inserted into DB").printStackTrace();
			renderText("|");
		}
		
		// Save as talker activity
		ActivityDAO.createActivity(topic.getUid(), "started the conversation: "+topic.getTopic());

		// create new LiveConvBean
//		LiveConversationBean lcb = new LiveConversationBean();
//		lcb.setTopic(topic);
//		lcb.addTalker(talker.getId(), talker);

		// add LiveConvBean to LiveConversationSingleton
//		LiveConversationsSingleton lcs = LiveConversationsSingleton.getReference();
//		lcs.addConversation(topicId, lcb);

		// add topic to TopicMap in session - keeps track of topics on the page so no duplicates
//		Map<String, TopicBean> mTopics = (Map<String, TopicBean>)Cache.get(session.getId()+"-mapTalkmiTopics");
//		if (mTopics != null) {
//			mTopics.put(topicId, topic);
//		}

		newTopic = newTopic.replaceAll("'", "&#39;");
		newTopic = newTopic.replaceAll("\\|", "&#124;");
		
		renderText(topic.getTid() + "|" + newTopic);
    }
    
    public static void lastTopicId() {
    	String lastTopicId = TopicDAO.getLastTopicId();
		
    	renderText(lastTopicId);
    }

}

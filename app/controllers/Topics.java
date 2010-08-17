package controllers;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import models.LiveConversationBean;
import models.TalkerBean;
import models.TopicBean;
import models.actions.FollowConvoAction;
import models.actions.StartConvoAction;
import play.cache.Cache;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;
import play.templates.JavaExtensions;
import util.CommonUtil;
import util.DBUtil;
import util.EmailUtil;
import util.NotificationUtils;
import webapp.LiveConversationsSingleton;
import dao.ActivityDAO;
import dao.TalkerDAO;
import dao.TopicDAO;

public class Topics extends Controller {
	
	@Before(unless={"viewTopic"})
    static void checkAccess() throws Throwable {
		Secure.checkAccess();
	}

    public static void create(String newTopic) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		TopicBean topic = new TopicBean();
		topic.setTopic(newTopic);
		topic.setUid(talker.getId());
		Date currentDate = Calendar.getInstance().getTime();
		topic.setCreationDate(currentDate);
		topic.setDisplayTime(currentDate);

		String topicURL = JavaExtensions.slugify(topic.getTopic());
		topic.setMainURL(topicURL);
		
		// insert new topic into database
		TopicDAO.save(topic);
		
		//send notifications if Automatic Notifications is On
		NotificationUtils.sendAutomaticNotifications(topic.getId());
		
		// Save as talker activity
//		ActivityDAO.createActivity(topic.getUid(), "started the conversation: "+topic.getTopic());
		ActivityDAO.saveActivity(new StartConvoAction(talker, topic));

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
		
		//TODO: make as template!
		renderText(topic.getTid() + "|" + newTopic + "|" + topic.getId());
    }
    
    public static void restart(String topicId) {
    	NotificationUtils.sendAutomaticNotifications(topicId);
    }
    
    public static void flag(String topicId) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	TopicBean topic = TopicDAO.getByTopicId(topicId);
    	
    	if (topic == null) {
    		return;
    	}
    	
    	Map<String, String> vars = new HashMap<String, String>();
		vars.put("name", talker.getUserName());
		vars.put("email", talker.getEmail());
		vars.put("subject", "FLAGGED CONVERSATION");
		//TODO: render with Play! classes?
		vars.put("message", 
				"Bad conversation: <a href=\"http://talkabouthealth.com:9000/topic/"+topic.getTid()+"\">"+
					topic.getTopic()+"</a>");
		EmailUtil.sendEmail(EmailUtil.CONTACTUS_TEMPLATE, EmailUtil.SUPPORT_EMAIL, vars, null, false);
    }
    
    public static void lastTopicId() {
    	String lastTopicId = TopicDAO.getLastTopicId();
		
    	renderText(lastTopicId);
    }
    
    //follow or unfollow topic
    public static void follow(String topicId) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	
    	if (talker.getFollowingTopicsList().contains(topicId)) {
    		//unfollow action
    		talker.getFollowingTopicsList().remove(topicId);
    	}
    	else {
    		talker.getFollowingTopicsList().add(topicId);
    		
    		ActivityDAO.saveActivity(new FollowConvoAction(talker, new TopicBean(topicId)));
    	}
    	
    	//TODO: put together?
    	TalkerDAO.updateTalker(talker);
    	CommonUtil.updateCachedTalker(session);
    	
    	renderText("Ok!");
    }
    
    public static void updateTopicField(String name, String value) {
    	//TODO: list of allowed fields?
    	
    	
    }
    
    /* --------------- Public -------------------- */
    public static void viewTopic(String url) {
    	notFoundIfNull(url);
		
		TalkerBean talker = null;
		if (Security.isConnected()) {
			talker = CommonUtil.loadCachedTalker(session);
		}
		
		TopicBean topic = TopicDAO.getByMainURL(url);
		if (topic == null) {
			//try by old url
			topic = TopicDAO.getByOldURL(url);
			notFoundIfNull(topic);
			
			//redirect to main url
			viewTopic(topic.getMainURL());
		}		
		
		TopicDAO.incrementTopicViews(topic.getId());
		
		//temporary test data
		topic.setDetails("Suggestions for friends and family...");
		topic.setTags(Arrays.asList("support", "help", "testtag"));
		topic.setSummary("Summary.........");
		topic.setSumContributors(Arrays.asList("murray", "situ"));
		
		render(talker, topic);
    }

}

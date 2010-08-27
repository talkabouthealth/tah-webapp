package controllers;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.CommentBean;
import models.TalkerBean;
import models.TopicBean;
import models.TalkerBean.EmailSetting;
import models.actions.FollowConvoAction;
import models.actions.ProfileCommentAction;
import models.actions.ProfileReplyAction;
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
import dao.ApplicationDAO;
import dao.CommentsDAO;
import dao.TalkerDAO;
import dao.TopicDAO;

@With(Secure.class)
public class Topics extends Controller {
	
    public static void create(String newTopic) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		TopicBean topic = new TopicBean();
		topic.setTopic(newTopic);
		topic.setUid(talker.getId());
		Date currentDate = Calendar.getInstance().getTime();
		topic.setCreationDate(currentDate);
		topic.setDisplayTime(currentDate);

		String topicURL = ApplicationDAO.createURLName(newTopic);
		topic.setMainURL(topicURL);
		
		// insert new topic into database
		TopicDAO.save(topic);
		ActivityDAO.saveActivity(new StartConvoAction(talker, topic));
		
		//send notifications if Automatic Notifications is On
		NotificationUtils.sendAutomaticNotifications(topic.getId());
		
		//automatically follow started topic
		talker.getFollowingTopicsList().add(topic.getId());
		TalkerDAO.updateTalker(talker);
    	CommonUtil.updateCachedTalker(session);
		ActivityDAO.saveActivity(new FollowConvoAction(talker, topic));
		

		newTopic = newTopic.replaceAll("'", "&#39;");
		newTopic = newTopic.replaceAll("\\|", "&#124;");
		
		//TODO: make as template!
		renderText(topic.getTid() + "|" + newTopic + "|" + topic.getId());
    }
    
    public static void restart(String topicId) {
    	//TODO: in other thread?
    	NotificationUtils.sendAutomaticNotifications(topicId);
    	
    	TopicBean topic = TopicDAO.getByTopicId(topicId);
    	for (TalkerBean follower : topic.getFollowers()) {
    		NotificationUtils.sendEmailNotification(EmailSetting.CONVO_RESTART, 
    				follower, "Topic '"+topic.getTopic()+"' is restarted.");
    	}
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
    
    public static void saveTopicComment(String topicId, String parentId, String text) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		TopicBean topic = TopicDAO.getByTopicId(topicId);
		notFoundIfNull(topic);
		
		CommentBean comment = new CommentBean();
		comment.setParentId(parentId.trim().length() == 0 ? null : parentId);
		comment.setTopicId(topicId);
		comment.setFromTalker(talker);
		comment.setText(text);
		comment.setTime(new Date());
		
		String id = CommentsDAO.saveTopicComment(comment);
		comment.setId(id);
		
//		if (comment.getParentId() == null) {
//			ActivityDAO.saveActivity(new ProfileCommentAction(talker, profileTalker));
//		}
//		else {
//			ActivityDAO.saveActivity(new ProfileReplyAction(talker, profileTalker));
//		}
//		NotificationUtils.sendEmailNotification(EmailSetting.RECEIVE_COMMENT, 
//				profileTalker, talker.getUserName()+" left a comment on you profile.");
		
		//render html of new comment using tag
		List<CommentBean> _commentsList = Arrays.asList(comment);
		int _level = (comment.getParentId() == null ? 1 : 2);
		render("tags/topicCommentsTree.html", _commentsList, _level);
	}
    
    /* --------------- Public -------------------- */
    public static void viewTopic(String url) {
    	notFoundIfNull(url);
		
    }

}

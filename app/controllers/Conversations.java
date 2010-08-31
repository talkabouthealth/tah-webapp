package controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.CommentBean;
import models.ConversationBean;
import models.TalkerBean;
import models.TopicBean;
import models.actions.FollowConvoAction;
import models.actions.StartConvoAction;
import dao.ActivityDAO;
import dao.ApplicationDAO;
import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;
import util.EmailUtil;
import util.NotificationUtils;
import util.EmailUtil.EmailTemplate;

@With(Secure.class)
public class Conversations extends Controller {
	
	public static void create(String newTopic) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	
    	//create or get? topics for new convo
//    	String newTag = "thirdtopic";
//    	TopicBean topic = new TopicBean();
//    	topic.setTitle(newTag);
//    	topic.setMainURL(ApplicationDAO.createURLName(newTag));
//    	TopicDAO.save(topic);
    	
    	List<TopicBean> topics = new ArrayList<TopicBean>();
//    	topics.add(topic);
    	
		
		ConversationBean convo = new ConversationBean();
		convo.setTopic(newTopic);
		convo.setUid(talker.getId());
		Date currentDate = Calendar.getInstance().getTime();
		convo.setCreationDate(currentDate);
		convo.setDisplayTime(currentDate);
		convo.setTopics(topics);

		String topicURL = ApplicationDAO.createURLName(newTopic);
		convo.setMainURL(topicURL);
		
		// insert new topic into database
		ConversationDAO.save(convo);
		ActivityDAO.saveActivity(new StartConvoAction(talker, convo));
		
		//send notifications if Automatic Notifications is On
		NotificationUtils.sendAutomaticNotifications(convo.getId());
		
		//automatically follow started topic
		talker.getFollowingConvosList().add(convo.getId());
		TalkerDAO.updateTalker(talker);
    	CommonUtil.updateCachedTalker(session);
		ActivityDAO.saveActivity(new FollowConvoAction(talker, convo));
		

		newTopic = newTopic.replaceAll("'", "&#39;");
		newTopic = newTopic.replaceAll("\\|", "&#124;");
		
		//TODO: make as template!
		renderText(convo.getTid() + "|" + newTopic + "|" + convo.getId() + "|" + convo.getMainURL());
    }
    
    public static void restart(String topicId) {
    	//TODO: in other thread?
    	NotificationUtils.sendAutomaticNotifications(topicId);
    	
    	ConversationBean topic = ConversationDAO.getByConvoId(topicId);
    	for (TalkerBean follower : topic.getFollowers()) {
//    		Map<String, String> vars = new HashMap<String, String>();
//    		vars.put("other_talker", talker.getUserName());
//    		vars.put("thankyou_text", thankYouBean.getNote());
//    		NotificationUtils.sendEmailNotification(EmailSetting.CONVO_RESTART, 
//    				follower, "Topic '"+topic.getTopic()+"' is restarted.");
    	}
    }
    
    public static void flag(String topicId) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	ConversationBean topic = ConversationDAO.getByConvoId(topicId);
    	
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
		EmailUtil.sendEmail(EmailTemplate.CONTACTUS, EmailUtil.SUPPORT_EMAIL, vars, null, false);
    }
    
    //for Dashboard
    public static void lastTopicId() {
    	String lastTopicId = ConversationDAO.getLastTopicId();
    	renderText(lastTopicId);
    }
    
    //follow or unfollow topic
    public static void follow(String topicId) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	
    	if (talker.getFollowingConvosList().contains(topicId)) {
    		//unfollow action
    		talker.getFollowingConvosList().remove(topicId);
    	}
    	else {
    		talker.getFollowingConvosList().add(topicId);
    		ActivityDAO.saveActivity(new FollowConvoAction(talker, new ConversationBean(topicId)));
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
		
		ConversationBean topic = ConversationDAO.getByConvoId(topicId);
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

}

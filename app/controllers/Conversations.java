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
import models.TalkerBean.EmailSetting;
import models.TopicBean;
import models.actions.FollowConvoAction;
import models.actions.StartConvoAction;
import dao.ActionDAO;
import dao.ApplicationDAO;
import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;
import play.mvc.Controller;
import play.mvc.Router;
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
		ActionDAO.saveAction(new StartConvoAction(talker, convo));
		
		//send notifications if Automatic Notifications is On
		NotificationUtils.sendAutomaticNotifications(convo.getId());
		
		//automatically follow started topic
		talker.getFollowingConvosList().add(convo.getId());
		CommonUtil.updateTalker(talker, session);
		
		ActionDAO.saveAction(new FollowConvoAction(talker, convo));

		newTopic = newTopic.replaceAll("'", "&#39;");
		newTopic = newTopic.replaceAll("\\|", "&#124;");
		//TODO: make as template!
		renderText(convo.getTid() + "|" + newTopic + "|" + convo.getId() + "|" + convo.getMainURL());
    }
    
    public static void restart(String topicId) {
    	NotificationUtils.sendAutomaticNotifications(topicId);
    	
    	ConversationBean convo = ConversationDAO.getByConvoId(topicId);
    	//prepare email params
    	Map<String, String> vars = new HashMap<String, String>();
		vars.put("convo", convo.getTopic());
		String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL());
		vars.put("convo_url", convoURL);
		String convoTalkURL = CommonUtil.generateAbsoluteURL("Talk.talkApp", "convoId", convo.getTid());
		vars.put("convo_talk_url", convoTalkURL);
    	for (TalkerBean follower : convo.getFollowers()) {
    		NotificationUtils.sendEmailNotification(EmailSetting.CONVO_RESTART, follower, vars);
    	}
    }
    
    public static void flag(String topicId) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	ConversationBean convo = ConversationDAO.getByConvoId(topicId);
    	
    	notFoundIfNull(convo);
    	
    	Map<String, String> vars = new HashMap<String, String>();
		vars.put("name", talker.getUserName());
		vars.put("email", talker.getEmail());
		vars.put("subject", "FLAGGED CONVERSATION");
		String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL());
		vars.put("message", 
				"Bad conversation/question: <a href=\""+convoURL+"\">"+convo.getTopic()+"</a>");
		EmailUtil.sendEmail(EmailTemplate.CONTACTUS, EmailUtil.SUPPORT_EMAIL, vars, null, false);
    }
    
    //for Dashboard
    public static void lastTopicId() {
    	String lastTopicId = ConversationDAO.getLastConvoId();
    	renderText(lastTopicId);
    }
    
    //follow or unfollow topic
    public static void follow(String topicId) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	
    	if (talker.getFollowingConvosList().contains(topicId)) {
    		//unfollow
    		talker.getFollowingConvosList().remove(topicId);
    	}
    	else {
    		talker.getFollowingConvosList().add(topicId);
    		ActionDAO.saveAction(new FollowConvoAction(talker, new ConversationBean(topicId)));
    	}
    	
    	CommonUtil.updateTalker(talker, session);
    	renderText("Ok!");
    }
    
    public static void updateField(String convoId, String name, String value) {
    	//TODO: list of allowed fields?
    	//TODO: hide edits from not-logined users
    	
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	ConversationBean convo = ConversationDAO.getByConvoId(convoId);
    	notFoundIfNull(convo);
    	
    	if (name.equalsIgnoreCase("title")) {
//    		ConversationDAO.updateConvo(convo);
    	}
    	else if (name.equalsIgnoreCase("details")) {
    		convo.setDetails(value);
    		ConversationDAO.updateConvo(convo);
    	}
    	else if (name.equalsIgnoreCase("summary")) {
    		convo.setSummary(value);
    		convo.getSumContributors().add(talker);
    		ConversationDAO.updateConvo(convo);
    		
        	//prepare email params
        	Map<String, String> vars = new HashMap<String, String>();
    		vars.put("convo", convo.getTopic());
    		vars.put("other_talker", talker.getUserName());
    		vars.put("summary_text", convo.getSummary());
    		String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL());
    		vars.put("convo_url", convoURL);
        	for (TalkerBean follower : convo.getFollowers()) {
        		NotificationUtils.sendEmailNotification(EmailSetting.CONVO_SUMMARY, follower, vars);
        	}
    	}
    	

    }
    
    public static void saveTopicComment(String topicId, String parentId, String text) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		ConversationBean convo = ConversationDAO.getByConvoId(topicId);
		notFoundIfNull(convo);
		
		CommentBean comment = new CommentBean();
		comment.setParentId(parentId.trim().length() == 0 ? null : parentId);
		comment.setTopicId(topicId);
		comment.setFromTalker(talker);
		comment.setText(text);
		comment.setTime(new Date());
		
		String id = CommentsDAO.saveConvoComment(comment);
		comment.setId(id);
		
		if (convo.isOpened()) {
			convo.setOpened(false);
			ConversationDAO.updateConvo(convo);
		}
		
		//notify
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("convo", convo.getTopic());
		vars.put("other_talker", talker.getUserName());
		vars.put("answer_text", comment.getText());
		vars.put("convo_type", convo.getConvoType().stringValue());
		String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL());
		vars.put("convo_url", convoURL);
    	for (TalkerBean follower : convo.getFollowers()) {
    		NotificationUtils.sendEmailNotification(EmailSetting.CONVO_COMMENT, follower, vars);
    	}
		
		//render html of new comment using tag
		List<CommentBean> _commentsList = Arrays.asList(comment);
		int _level = (comment.getParentId() == null ? 1 : 2);
		render("tags/topicCommentsTree.html", _commentsList, _level);
	}
    
}

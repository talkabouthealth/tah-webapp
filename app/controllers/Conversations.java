package controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logic.ConversationLogic;
import models.CommentBean;
import models.CommentBean.Vote;
import models.ConversationBean;
import models.ConversationBean.ConvoName;
import models.TalkerBean;
import models.TalkerBean.EmailSetting;
import models.TopicBean;
import models.actions.AnswerConvoAction;
import models.actions.AnswerVotedAction;
import models.actions.FollowConvoAction;
import models.actions.StartConvoAction;
import models.actions.Action.ActionType;
import models.actions.SummaryConvoAction;
import dao.ActionDAO;
import dao.ApplicationDAO;
import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;
import dao.TopicDAO;
import play.i18n.Messages;
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
    	
    	TopicBean topic = TopicDAO.getByTitle("fatigue");
    	List<TopicBean> topics = new ArrayList<TopicBean>();
//    	topics.add(topic);
    	
    	ConversationBean convo = ConversationLogic.createConvo(newTopic, talker);
		
    	CommonUtil.updateTalker(talker, session);

		newTopic = newTopic.replaceAll("'", "&#39;");
		newTopic = newTopic.replaceAll("\\|", "&#124;");
		//TODO: make as template!
		renderText(convo.getTid() + "|" + newTopic + "|" + convo.getId() + "|" + convo.getMainURL());
    }
    
    public static void restart(String topicId) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	ConversationBean convo = ConversationDAO.getByConvoId(topicId);
    	notFoundIfNull(convo);
    	
    	ActionDAO.saveAction(new StartConvoAction(talker, convo, ActionType.RESTART_CONVO));
    	
    	NotificationUtils.sendAutomaticNotifications(topicId);
    	
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
    
    public static void flag(String topicId, String reason) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	ConversationBean convo = ConversationDAO.getByConvoId(topicId);
    	
    	notFoundIfNull(convo);
    	
    	Map<String, String> vars = new HashMap<String, String>();
    	vars.put("content_type", "Conversation/Question");
    	vars.put("content_link", CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL()));
    	vars.put("reason", reason);
		vars.put("content", convo.getTopic());
		vars.put("name", talker.getUserName());
		vars.put("email", talker.getEmail());
		String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL());
		vars.put("message", 
				"Bad conversation/question: <a href=\""+convoURL+"\">"+convo.getTopic()+"</a>");
		EmailUtil.sendEmail(EmailTemplate.FLAGGED, EmailUtil.SUPPORT_EMAIL, vars, null, false);
    }
    
    public static void vote(String answerId, boolean up) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	CommentBean answer = CommentsDAO.getConvoAnswerById(answerId);
    	notFoundIfNull(answer);
    	
    	Vote newVote = new Vote(talker, up);
    	int voteScore = answer.getVoteScore();
    	voteScore = voteScore + (up ? 1 : -1);
    	
    	Vote oldVote = answer.getVoteByTalker(talker);
    	if (oldVote != null) {
    		if (up == oldVote.isUp()) {
    			//try the same vote - already voted!
    			renderText(Messages.get("vote.exists"));
    			return;
    		}
    		else {
    			//remove previous vote from score
    			voteScore = voteScore + (oldVote.isUp() ? -1 : 1);
    		}
    		answer.getVotes().remove(oldVote);
    	}
    	
    	if (newVote.isUp()) {
    		ConversationBean convo = new ConversationBean(answer.getTopicId());
    		ActionDAO.saveAction(new AnswerVotedAction(talker, convo, answer));
    	}
    	
    	answer.getVotes().add(newVote);
    	answer.setVoteScore(voteScore);
    	CommentsDAO.updateConvoAnswer(answer);
    	renderText("ok");
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
    		ConvoName currentName = new ConvoName(convo.getTopic(), convo.getMainURL());
    		ConvoName newName = new ConvoName(value, null);
    		
    		//find old name with the same title
    		ConvoName oldName = convo.getOldNameByTitle(newName.getTitle());
    		if (oldName != null) {
    			//convo has already had this title, return it to main title/url
    			convo.setTopic(oldName.getTitle());
    			convo.setMainURL(oldName.getUrl());
    			convo.getOldNames().remove(oldName);
    		}
    		else {
    			//new title for this topic - create it
    			String newURL = ApplicationDAO.createURLName(newName.getTitle());
    			convo.setTopic(newName.getTitle());
    			convo.setMainURL(newURL);
    		}
    		convo.getOldNames().add(currentName);
    		ConversationDAO.updateConvo(convo);
    	}
    	else if (name.equalsIgnoreCase("details")) {
    		convo.setDetails(value);
    		ConversationDAO.updateConvo(convo);
    	}
    	else if (name.equalsIgnoreCase("summary")) {
    		String previousSummary = convo.getSummary();
    		convo.setSummary(value);
    		convo.getSumContributors().add(talker);
    		ConversationDAO.updateConvo(convo);
    		
    		if (previousSummary == null || previousSummary.length() == 0) {
    			ActionDAO.saveAction(new SummaryConvoAction(talker, convo, ActionType.SUMMARY_ADDED));
    		}
    		else {
    			ActionDAO.saveAction(new SummaryConvoAction(talker, convo, ActionType.SUMMARY_EDITED));
    		}
    		
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
    	else if (name.equalsIgnoreCase("topic")) {
    		String todo = params.get("todo");
    		if (todo.equalsIgnoreCase("add")) {
    			TopicBean topic = TopicDAO.getByTitle(value);
    			if (topic == null) {
    				//create new topic with this name
        	    	topic = new TopicBean();
        	    	topic.setTitle(value);
        	    	topic.setMainURL(ApplicationDAO.createURLName(value));
        	    	TopicDAO.save(topic);
    			}

    	    	convo.getTopics().add(topic);
    	    	ConversationDAO.updateConvo(convo);
    	    	
    	    	renderText(
    	    			"<a class=\"topicTitle\" href=\""+topic.getMainURL()+"\">"+topic.getTitle()+"</a>&nbsp;" +
    	    			"<a class=\"deleteTopicLink\" href=\"#\" rel=\""+topic.getId()+"\">X</a>");
    		}
    		else if (todo.equalsIgnoreCase("remove")) {
    			TopicBean topic = new TopicBean(value);
    			convo.getTopics().remove(topic);
    	    	ConversationDAO.updateConvo(convo);
    		}
    	}
    }
    
    public static void delete(String convoId) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	ConversationBean convo = ConversationDAO.getByConvoId(convoId);
    	notFoundIfNull(convo);
    	
    	if (!"admin".equalsIgnoreCase(talker.getUserName())) {
    		forbidden();
    		return;
    	}

    	convo.setDeleted(true);
    	ConversationDAO.updateConvo(convo);
    }
    
    public static void updateAnswer(String answerId, String todo, String newText) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
    	CommentBean answer = CommentsDAO.getConvoAnswerById(answerId);
    	if (!answer.getFromTalker().equals(talker) && !talker.getUserName().equals("admin")) {
    		forbidden();
    		return;
    	}
    	
    	if (todo.equalsIgnoreCase("update")) {
    		String oldText = answer.getText();
    		if (!oldText.equals(newText)) {
    			answer.getOldTexts().add(oldText);
    			answer.setText(newText);
    			CommentsDAO.updateConvoAnswer(answer);
    		}
    	}
    	else if (todo.equalsIgnoreCase("delete")) {
    		answer.setDeleted(true);
    		CommentsDAO.updateConvoAnswer(answer);
    	}
    }
    
    public static void saveTopicComment(String topicId, String parentId, String text) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		ConversationBean convo = ConversationDAO.getByConvoId(topicId);
		notFoundIfNull(convo);
		
		CommentBean comment = ConversationLogic.createAnswer(convo, talker, parentId, text);
		
		//render html of new comment using tag
		List<CommentBean> _commentsList = Arrays.asList(comment);
		int _level = (comment.getParentId() == null ? 1 : 2);
		render("tags/convoCommentsTree.html", _commentsList, _level);
	}
    
}

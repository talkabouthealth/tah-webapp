package controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import logic.ConversationLogic;
import models.CommentBean;
import models.CommentBean.Vote;
import models.ConversationBean.ConvoType;
import models.ConversationBean;
import models.TalkerBean;
import models.TalkerBean.EmailSetting;
import models.TopicBean;
import models.URLName;
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
	
	public static void create(String type, String title, String details, String topics) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	
    	//prepare params
    	ConvoType convoType = ConvoType.valueOf(type);
    	
    	Set<TopicBean> topicsSet = new HashSet<TopicBean>();
    	String[] topicsArr = topics.split(" ");
    	for (String topicTitle : topicsArr) {
    		TopicBean topic = TopicDAO.getByTitle(topicTitle);
    		if (topic != null) {
    			topicsSet.add(topic);
    		}
    		
        	//create or get? topics for new convo
//        	String newTag = "thirdtopic";
//        	TopicBean topic = new TopicBean();
//        	topic.setTitle(newTag);
//        	topic.setMainURL(ApplicationDAO.createURLName(newTag));
//        	TopicDAO.save(topic);
    	}
    	
//    	System.out.println(convoType+" : "+title+" : "+details+" : "+topicsSet);
    	
    	ConversationBean convo = ConversationLogic.createConvo(convoType, title, talker, details, topicsSet);
    	CommonUtil.updateTalker(talker, session);
    	
    	String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL());
		renderJSON("{ \"tid\" : \""+convo.getTid()+"\", \"url\" : \""+convoURL+"\" }");
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
    
    public static void flag(String convoId, String reason) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	ConversationBean convo = ConversationDAO.getByConvoId(convoId);
    	
    	notFoundIfNull(convo);
    	
    	Map<String, String> vars = new HashMap<String, String>();
    	vars.put("content_type", "Conversation/Question");
    	vars.put("content_link", CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL()));
    	vars.put("reason", reason);
		vars.put("content", convo.getTopic());
		vars.put("name", talker.getUserName());
		vars.put("email", talker.getEmail());
		EmailUtil.sendEmail(EmailTemplate.FLAGGED, EmailUtil.SUPPORT_EMAIL, vars, null, false);
    }
    
    public static void flagAnswer(String answerId, String reason) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	CommentBean answer = CommentsDAO.getConvoAnswerById(answerId);
    	notFoundIfNull(answer);
    	ConversationBean convo = ConversationDAO.getByConvoId(answer.getTopicId());
    	
    	Map<String, String> vars = new HashMap<String, String>();
    	vars.put("content_type", "Answer/Reply");
    	vars.put("content_link", CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL()));
    	vars.put("reason", reason);
		vars.put("content", answer.getText());
		vars.put("name", talker.getUserName());
		vars.put("email", talker.getEmail());
		EmailUtil.sendEmail(EmailTemplate.FLAGGED, EmailUtil.SUPPORT_EMAIL, vars, null, false);
    }
    
    public static void vote(String answerId, boolean up) {
    	//TODO: talker can vote for his/her answer/reply?
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
    	
    	String nextAction = null;
    	if (talker.getFollowingConvosList().contains(topicId)) {
    		//unfollow
    		talker.getFollowingConvosList().remove(topicId);
    		nextAction = "follow";
    	}
    	else {
    		talker.getFollowingConvosList().add(topicId);
    		ActionDAO.saveAction(new FollowConvoAction(talker, new ConversationBean(topicId)));
    		nextAction = "unfollow";
    	}
    	
    	CommonUtil.updateTalker(talker, session);
    	renderText(nextAction);
    }
    
    public static void updateField(String convoId, String name, String value) {
    	//TODO: list of allowed fields?
    	//TODO: hide edits from not-logined users
    	
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	ConversationBean convo = ConversationDAO.getByConvoId(convoId);
    	notFoundIfNull(convo);
    	
    	if (name.equalsIgnoreCase("title")) {
    		URLName currentName = new URLName(convo.getTopic(), convo.getMainURL());
    		URLName newName = new URLName(value, null);
    		
    		//find old name with the same title
    		URLName oldName = convo.getOldNameByTitle(newName.getTitle());
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
    	else if (name.equalsIgnoreCase("relatedConvos")) {
    		String todo = params.get("todo");
    		if (todo.equalsIgnoreCase("add")) {
    			ConversationBean relatedConvo = ConversationDAO.getByTitle(value);
    			if (relatedConvo != null) {
    				convo.getRelatedConvos().add(relatedConvo);
    				ConversationDAO.updateConvo(convo);
    				
        	    	renderText(
		    			"<p class=\"rcpadtop\"><a href=\""+relatedConvo.getMainURL()+"\">"+relatedConvo.getTopic()+"</a>&nbsp;" +
		    			"<a class=\"deleteConvoLink\" href=\"#\" rel=\""+relatedConvo.getId()+"\">X</a></p>");
    			}
    		}
    		else if (todo.equalsIgnoreCase("remove")) {
    			ConversationBean relatedConvo = new ConversationBean(value);
    			convo.getRelatedConvos().remove(relatedConvo);
    	    	ConversationDAO.updateConvo(convo);
    		}
    	}
    }
    
    public static void delete(String convoId) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	ConversationBean convo = ConversationDAO.getByConvoId(convoId);
    	notFoundIfNull(convo);
    	
    	if ( !("admin".equalsIgnoreCase(talker.getUserName()) || convo.getTalker().equals(talker))) {
    		forbidden();
    		return;
    	}

    	convo.setDeleted(true);
    	ConversationDAO.updateConvo(convo);
    	renderText("ok");
    }
    
    public static void updateAnswer(String answerId, String todo, String newText) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
    	CommentBean answer = CommentsDAO.getConvoAnswerById(answerId);
    	if (todo.equalsIgnoreCase("update") || todo.equalsIgnoreCase("delete")) {
    		//TODO: move to util permission check?
    		if (!answer.getFromTalker().equals(talker) && !talker.getUserName().equals("admin")) {
        		forbidden();
        		return;
        	}
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
    	else if (todo.equalsIgnoreCase("setNotHelpful")) {
    		answer.setNotHelpful(true);
    		CommentsDAO.updateConvoAnswer(answer);
    		
    		//let's send an email to support@talkabouthealth.com
    		//TODO: make one method for flagging?
    		Map<String, String> vars = new HashMap<String, String>();
        	vars.put("content_type", "Answer");
        	vars.put("content_link", "");
        	vars.put("reason", "");
    		vars.put("content", answer.getText());
    		vars.put("name", talker.getUserName());
    		vars.put("email", talker.getEmail());
    		EmailUtil.sendEmail(EmailTemplate.FLAGGED, EmailUtil.SUPPORT_EMAIL, vars, null, false);
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

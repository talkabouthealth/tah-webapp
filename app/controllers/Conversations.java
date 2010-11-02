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
import logic.TopicLogic;
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
import models.actions.TopicAddedAction;
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
import play.templates.JavaExtensions;
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
    	String[] topicsArr = topics.split(",");
    	for (String topicTitle : topicsArr) {
    		TopicBean topic = TopicDAO.getByTitle(topicTitle.trim());
    		if (topic != null) {
    			topicsSet.add(topic);
    		}
    	}
    	
//    	System.out.println(convoType+" : "+title+" : "+details+" : "+topicsSet);
    	
    	//in this case we notify only after new question
    	boolean notifyTalkers = (convoType == ConvoType.QUESTION);
    	ConversationBean convo = 
    		ConversationLogic.createConvo(convoType, title, talker, details, topicsSet, notifyTalkers);
    	CommonUtil.updateTalker(talker, session);
    	
    	String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL());
		renderJSON("{ \"id\" : \""+convo.getId()+"\", \"tid\" : \""+convo.getTid()+"\", \"url\" : \""+convoURL+"\" }");
    }
	
	//start Talk after creating it
	public static void start(String convoId) {
    	ConversationBean convo = ConversationDAO.getByConvoId(convoId);
    	notFoundIfNull(convo);
    	
    	NotificationUtils.sendAutomaticNotifications(convo.getId(), null);
	}
    
    public static void restart(String convoId) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	ConversationBean convo = ConversationDAO.getByConvoId(convoId);
    	notFoundIfNull(convo);
    	
    	ActionDAO.saveAction(new StartConvoAction(talker, convo, ActionType.RESTART_CONVO));
    	
    	NotificationUtils.sendAutomaticNotifications(convoId, talker.getId());
    	
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
    
    //Close LiveTalk
    public static void close(String convoId) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	if (!talker.getUserName().equalsIgnoreCase("admin")) {
    		forbidden();
    		return;
    	}
    	
    	ConversationDAO.closeLiveTalk(convoId);
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
    
    //flag answer or reply
    public static void flagAnswer(String answerId, String reason) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	CommentBean answer = CommentsDAO.getConvoAnswerById(answerId);
    	notFoundIfNull(answer);
    	ConversationBean convo = ConversationDAO.getByConvoId(answer.getConvoId());
    	
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
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	CommentBean answer = CommentsDAO.getConvoAnswerById(answerId);
    	notFoundIfNull(answer);
    	
    	//talker cannot vote for his/her answer/reply
    	if (talker.equals(answer.getFromTalker())) {
    		forbidden();
    		return;
    	}
    	
    	Vote newVote = new Vote(talker, up);
    	int voteScore = answer.getVoteScore();
    	voteScore = voteScore + (up ? 1 : -1);
    	
    	Vote oldVote = answer.getVoteByTalker(talker, answer.getVotes());
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
    		ConversationBean convo = ConversationDAO.getByConvoId(answer.getConvoId());
    		ActionDAO.saveAction(new AnswerVotedAction(talker, convo, answer));
    		
    		//If a "Not Helpful" answer receives a vote, let's make it visible again. 
    		//But also send an email to "support@talkabouthealth.com" add a comment
    		
    		if (answer.isNotHelpful()) {
    			answer.setNotHelpful(false);
    			
        		//TODO: make one method for flagging?
        		Map<String, String> vars = new HashMap<String, String>();
            	vars.put("content_type", "Answer");
            	vars.put("content_link", CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL()));
            	vars.put("reason", "User voted up for this 'Not Helpful' answer");
        		vars.put("content", answer.getText());
        		vars.put("name", talker.getUserName());
        		vars.put("email", talker.getEmail());
        		EmailUtil.sendEmail(EmailTemplate.FLAGGED, EmailUtil.SUPPORT_EMAIL, vars, null, false);
    		}
    	}
    	
    	answer.getVotes().add(newVote);
    	answer.setVoteScore(voteScore);
    	CommentsDAO.updateConvoAnswer(answer);
    	renderText("ok");
    }
    
    //for Dashboard
    public static void lastTopicId() {
    	String lastConvoId = ConversationDAO.getLastConvoId();
    	renderText(lastConvoId);
    }
    
    //follow or unfollow convo
    public static void follow(String convoId) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	
    	String nextAction = null;
    	if (talker.getFollowingConvosList().contains(convoId)) {
    		//unfollow
    		talker.getFollowingConvosList().remove(convoId);
    		nextAction = "follow";
    	}
    	else {
    		talker.getFollowingConvosList().add(convoId);
    		ActionDAO.saveAction(new FollowConvoAction(talker, new ConversationBean(convoId)));
    		nextAction = "unfollow";
    	}
    	
    	CommonUtil.updateTalker(talker, session);
    	renderText(nextAction);
    }
    
    public static void updateField(String convoId, String name, String value) {
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
    			//possible comma-separated list of topics
    			String[] valueArr = value.split(",\\s*");
    			System.out.println(value+" : "+Arrays.toString(valueArr));
    			
    			StringBuilder htmlToRender = new StringBuilder();
    			for (String topicName : valueArr) {
    				topicName = JavaExtensions.capitalizeWords(topicName);
    				TopicBean topic = TopicDAO.getByTitle(topicName);
        			if (topic == null) {
        				//create new topic with this name
            	    	topic = new TopicBean();
            	    	topic.setTitle(topicName);
            	    	topic.setMainURL(ApplicationDAO.createURLName(topicName));
            	    	TopicDAO.save(topic);
            	    	
            	    	TopicLogic.addToDefaultParent(topic);
        			}

        	    	convo.getTopics().add(topic);
        	    	ConversationDAO.updateConvo(convo);
        	    	
        	    	ActionDAO.saveAction(new TopicAddedAction(talker, convo, topic));
        	    	
        	    	htmlToRender.append(
    	    			"<a class=\"topicTitle\" href=\""+topic.getMainURL()+"\">"+topic.getTitle()+"</a>&nbsp;" +
    	    			"<a class=\"deleteTopicLink\" href=\"#\" rel=\""+topic.getId()+"\">X</a>"
        	    	);
    			}
    	    	
    	    	renderText(htmlToRender.toString());
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
    	
    	//remove related actions
    	ActionDAO.deleteActionsByConvo(convo);
    	
    	renderText("ok");
    }
    
    public static void updateAnswer(String answerId, String todo, String newText) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
    	CommentBean answer = CommentsDAO.getConvoAnswerById(answerId);
    	notFoundIfNull(answer);
    	
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
    		
    		//remove related actions
    		ActionDAO.deleteActionsByAnswer(answer);
    	}
    	else if (todo.equalsIgnoreCase("setNotHelpful")) {
    		Vote notHelpfulVote = new Vote(talker, false);
    		
    		Vote oldVote = answer.getVoteByTalker(talker, answer.getNotHelpfulVotes());
    		if (oldVote != null) {
				//try the same vote - already voted!
				renderText(Messages.get("vote.exists"));
				return;
    		}
    		
    		answer.getNotHelpfulVotes().add(notHelpfulVote);
    		
    		//Also, in order for an answer to disappear, let's say it must have 3 "Not Helpful" votes. 
    		//Unless "Admin" marks it as unhelpful.
    		if (!answer.isNotHelpful()) {
    			//check if we should make it nothelpful
    			if (talker.getUserName().equalsIgnoreCase("admin") 
    					|| answer.getNotHelpfulVotes().size() == 3) {
    				answer.setNotHelpful(true);
    			}
    		}
    		
    		CommentsDAO.updateConvoAnswer(answer);
    	}
    }
    
    public static void saveConvoComment(String convoId, String parentId, String text) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		ConversationBean convo = ConversationDAO.getByConvoId(convoId);
		notFoundIfNull(convo);
		
		CommentBean comment = ConversationLogic.createAnswer(convo, talker, parentId, text);
		
		//render html of new comment using tag
		List<CommentBean> _commentsList = Arrays.asList(comment);
		int _level = (comment.getParentId() == null ? 1 : 2);
		render("tags/convoCommentsTree.html", _commentsList, _level);
	}
    
}

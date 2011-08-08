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
import models.actions.Action;
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
import dao.QuestionDAO;
import dao.TalkerDAO;
import dao.TopicDAO;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.data.validation.Validation;
import play.exceptions.UnexpectedException;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Router;
import play.mvc.Scope;
import play.mvc.With;
import play.templates.JavaExtensions;
import play.templates.Template;
import play.templates.TemplateLoader;
import util.BitlyUtil;
import util.CommonUtil;
import util.EmailUtil;
import util.NotificationUtils;
import util.EmailUtil.EmailTemplate;

/**
 * All methods related to conversations and Conversation Summary page.
 *
 */
@With(Secure.class)
public class Conversations extends Controller {
	
	/* ---------- Conversation management actions --------------- */
	/**
	 * Create conversation
	 * @param type 'QUESTION' or 'CONVERSATION' (Chat)
	 * @param topics tags for this convo
	 * @param fromPage page where request was made
	 */
	public static void create(String type, String title, String details, String topics, 
			String fromPage, String parentConvoId, Boolean ccTwitter, Boolean ccFacebook) {

		String targetId="";
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
    	// if passed a targeted question from profiles, parentConvoId contains talkerId of target user
		if(fromPage.equals("profileRight")) {
			targetId=parentConvoId;
			parentConvoId="";
		}		
    	
    	//prepare params
    	ConvoType convoType = ConvoType.valueOf(type);
    	
    	Set<TopicBean> topicsSet = TopicLogic.parseTopicsFromString(topics);
    	
    	ConversationBean parentConvo = null;
    	if (parentConvoId != null && parentConvoId.length() > 0) {
    		parentConvo = ConversationDAO.getById(parentConvoId);
    	}
    	
    	//in this case we notify only after new question (for chats - after chat start)
    	boolean notifyTalkers = (convoType == ConvoType.QUESTION);
    	ConversationBean convo = 
    		ConversationLogic.createConvo(convoType, title, talker, details, 
    				topicsSet, notifyTalkers, parentConvo, ccTwitter, ccFacebook);
    	
    	CommonUtil.updateTalker(talker, session);
    	
    	// send email notification to target user if any
    	if(targetId != null && targetId.length()>0) {
    		TalkerBean targetTalker = TalkerDAO.getById(targetId);
    		if(targetTalker != null) {
    			String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL());
    			String description = convo.getPageOriginalSummary();
    			
				Map<String, String> vars = new HashMap<String, String>();
				vars.put("other_talker", talker.getUserName());
				vars.put("convo", description);
				vars.put("convo_url",convoURL);
    			
    				QuestionDAO.saveQuestionNotification(targetTalker.getId(), convo.getId());
				NotificationUtils.sendEmailNotification(EmailSetting.CONVO_PERSONAL,targetTalker, vars);    			
    		}
    	}
    	
    	renderConvoData(fromPage, talker, convo);
    }

	/**
	 * Renders conversation's data in JSON format, based on parameters
	 */
	private static void renderConvoData(String fromPage, TalkerBean talker,
			ConversationBean convo) {
		//Render needed template to "html" string.
    	String templateName = "";
    	Scope.RenderArgs templateBinding = Scope.RenderArgs.current();
        templateBinding.put("session", Scope.Session.current());
        templateBinding.put("request", Http.Request.current());
        templateBinding.put("_talker", talker);
    	if (fromPage.equalsIgnoreCase("home")) {
    		//#{feedActivity activity: activity, talker: talker /}
    		templateName = "tags/feed/feedActivity.html";
    		Action activity = new StartConvoAction(talker, convo, ActionType.START_CONVO);
    		activity.getConvo().setComments(new ArrayList<CommentBean>());
    		activity.setID(convo.getActionID());
    		templateBinding.put("_activity", activity);
    	}
    	else if (fromPage.equalsIgnoreCase("liveTalks")) {
    		//#{convo/liveChat convo: convo, talker: talker /}
    		templateName = "tags/convo/liveChat.html";
    		templateBinding.put("_convo", convo);
    	}
    	else {
    		//#{convo/openQuestion convo: convo, talker: talker /}
    		templateName = "tags/convo/openQuestion.html";
    		templateBinding.put("_convo", convo);
    		templateBinding.put("_id",convo.getId());
    	}
        Template template = TemplateLoader.load(templateName);
    	String html = template.render(templateBinding.data);
    	
    	System.out.println("New convo - " + convo.getId());
    	
    	Map<String, String> jsonData = new HashMap<String, String>();
    	jsonData.put("id", convo.getId());
    	jsonData.put("tid", ""+convo.getTid());
    	String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL());
    	jsonData.put("url", convoURL);
    	jsonData.put("html", html);
    	renderJSON(jsonData);
	}
	
	//start Chat after creating it
	public static void start(String convoId) {
    	ConversationBean convo = ConversationDAO.getById(convoId);
    	notFoundIfNull(convo);
    	NotificationUtils.sendAllNotifications(convo.getId(), null);
	}
    
    public static void restart(String convoId) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	ConversationBean convo = ConversationDAO.getById(convoId);
    	notFoundIfNull(convo);
    	
    	ActionDAO.saveAction(new StartConvoAction(talker, convo, ActionType.RESTART_CONVO));
    	NotificationUtils.sendAllNotifications(convoId, talker.getId());
    	NotificationUtils.emailNotifyOnConvoRestart(convo);
    	
    	convo.setCreationDate(new Date());
    	ConversationDAO.updateConvo(convo);
    }

    /**
     * Close Live Chat - used by admin
     */
    public static void close(String convoId) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	if (!talker.isAdmin()) {
    		forbidden();
    		return;
    	}
    	
    	ConversationDAO.closeLiveChat(convoId);
	}
    
    /**
     * Marks given conversation as 'deleted'
     */
    public static void delete(String convoId) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	ConversationBean convo = ConversationDAO.getById(convoId);
    	notFoundIfNull(convo);
    	
    	//only admin or author can do this
    	if ( !(talker.isAdmin() || convo.getTalker().equals(talker)) ) {
    		forbidden();
    		return;
    	}

    	convo.setDeleted(true);
    	ConversationDAO.updateConvo(convo);
    	
    	//remove related actions
    	ActionDAO.deleteActionsByConvo(convo);
    	
    	renderText("ok");
    }
    
    public static void flag(String convoId, String reason) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	ConversationBean convo = ConversationDAO.getById(convoId);
    	notFoundIfNull(convo);
    	
    	ConversationLogic.flagContent("Conversation/Question", convo, reason, convo.getTopic(), talker);
    }
    
	/**
	 * Follow/Unfollow given conversation
	 */
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
    
    
    /* ---------------- Conversation Summary page actions ------------------ */
    
    /**
     * Back-end methods for updating fields on ConvoSummary page - 
     * title, details, summary, topic, relatedConvos
     * 
     * @param name name of the field to update
     * @param value new value for the given field
     */
    public static void updateField(String convoId, String name, String value) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	ConversationBean convo = ConversationDAO.getById(convoId);
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
        	NotificationUtils.emailNotifyOnConvoSummary(talker, convo);
    	}
    	else if (name.equalsIgnoreCase("topic")) {
    		String todo = params.get("todo");
    		if (todo.equalsIgnoreCase("add")) {
    			//possible comma-separated list of topics
    			String[] valueArr = value.split(",\\s*");
    			
    			StringBuilder htmlToRender = new StringBuilder();
    			for (String topicName : valueArr) {
    				if (topicName == null || topicName.trim().length() == 0) {
    					continue;
    				}
    				topicName = JavaExtensions.capitalizeWords(topicName);
    				TopicBean topic = TopicDAO.getOrRestoreByTitle(topicName);
        			if (topic == null) {
        				//create new topic with this name
            	    	topic = new TopicBean();
            	    	topic.setTitle(topicName);
            	    	topic.setMainURL(ApplicationDAO.createURLName(topicName));
            	    	String topicURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", topic.getMainURL());
            			topic.setBitly(BitlyUtil.shortLink(topicURL));
            	    	TopicDAO.save(topic);
            	    	
            	    	TopicLogic.addToDefaultParent(topic);
        			}
        			else {
        				//change 'last_update' of the topic
        				TopicDAO.updateTopic(topic);
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
		    			"<a class=\"deleteConvoLink relatedConvos\" href=\"#\" rel=\""+relatedConvo.getId()+"\">X</a></p>");
    			}
    		}
    		else if (todo.equalsIgnoreCase("remove")) {
    			ConversationBean relatedConvo = new ConversationBean(value);
    			convo.getRelatedConvos().remove(relatedConvo);
    	    	ConversationDAO.updateConvo(convo);
    		}
    	}
    	else if (name.equalsIgnoreCase("followupConvos")) {
    		String todo = params.get("todo");
    		if (todo.equalsIgnoreCase("add")) {
    			ConversationBean followupConvo = ConversationDAO.getByTitle(value);
    			if (followupConvo != null) {
    				convo.getFollowupConvos().add(followupConvo);
    				ConversationDAO.updateConvo(convo);
    				
        	    	renderText(
		    			"<p class=\"rcpadtop\"><a href=\""+followupConvo.getMainURL()+"\">"+followupConvo.getTopic()+"</a>&nbsp;" +
		    			"<a class=\"deleteConvoLink\" href=\"#\" rel=\""+followupConvo.getId()+"\">X</a></p>");
    			}
    		}
    		else if (todo.equalsIgnoreCase("remove")) {
    			ConversationBean followupConvo = new ConversationBean(value);
    			convo.getFollowupConvos().remove(followupConvo);
    	    	ConversationDAO.updateConvo(convo);
    		}
    	}
    }


    /* ---------------------- Answer related actions --------------------- */
    
    /**
     * Flag convo answer or reply
     */
    public static void flagAnswer(String answerId, String reason) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	CommentBean answer = CommentsDAO.getConvoCommentById(answerId);
    	notFoundIfNull(answer);
    	ConversationBean convo = ConversationDAO.getById(answer.getConvoId());
    	
    	ConversationLogic.flagContent("Answer/Reply", convo, reason, 
    			CommonUtil.commentToHTML(answer), talker);
    }
    
    /**
     * Vote up or down for the anwer
     */
    public static void vote(String answerId, boolean up) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	CommentBean answer = CommentsDAO.getConvoCommentById(answerId);
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
    		//user already voted for this answer/reply
    		if (up == oldVote.isUp()) {
    			//try the same vote - already voted!
    			renderText("Error");
    			return;
    		}
    		else {
    			//remove previous vote from score
    			voteScore = voteScore + (oldVote.isUp() ? -1 : 1);
    		}
    		answer.getVotes().remove(oldVote);
    	}
    	
    	if (newVote.isUp()) {
    		ConversationBean convo = ConversationDAO.getById(answer.getConvoId());
    		ActionDAO.saveAction(new AnswerVotedAction(talker, convo, answer));
    		
    		//If a "Not Helpful" answer receives a vote, let's make it visible again. 
    		//But also send an email to "support@talkabouthealth.com" add a comment
    		if (answer.isNotHelpful()) {
    			answer.setNotHelpful(false);
    			ConversationLogic.flagContent("Answer", convo, "User voted up for this 'Not Helpful' answer", answer.getText(), talker);
    		}
    	}
    	
    	answer.getVotes().add(newVote);
    	answer.setVoteScore(voteScore);
    	CommentsDAO.updateConvoComment(answer);
    	
    	Set<Vote> _votes = answer.getUpVotes();
    	render("tags/convo/answerVotesInfo.html", _votes);
    }
    
    /**
     * Update/delete or set 'Not Helpful' given answer or reply
     * @param todo action to do - 'update'/'delete'/'setNotHelpful'
     */
    public static void updateAnswer(String answerId, String todo, String newText) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	CommentBean answer = CommentsDAO.getConvoCommentById(answerId);
    	notFoundIfNull(answer);
    	
    	// update and delete are allowed only for author and admin
    	if (todo.equalsIgnoreCase("update") || todo.equalsIgnoreCase("delete")) {
    		if (!answer.getFromTalker().equals(talker) && !talker.isAdmin()) {
        		forbidden();
        		return;
        	}
    	}
    	
    	if (todo.equalsIgnoreCase("update")) {
    		String oldText = answer.getText();
    		if (!oldText.equals(newText)) {
    			answer.getOldTexts().add(oldText);
    			answer.setText(newText);
    			CommentsDAO.updateConvoComment(answer);
    		}
    		
   			newText = CommonUtil.prepareThoughtOrAnswer(newText);
        	renderText(newText);
    	}
    	else if (todo.equalsIgnoreCase("delete")) {
    		answer.setDeleted(true);
    		CommentsDAO.updateConvoComment(answer);
    		
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
    		CommentsDAO.updateConvoComment(answer);
    	}
    }
    
    /**
     * Create answer or reply for given conversation
     * @param parentId id of answer (for replies)
     * @throws Throwable 
     */
    public static void saveAnswerOrReply(String convoId, String parentId, String text) throws Throwable {
    	Secure.checkAccess();
		TalkerBean _talker = CommonUtil.loadCachedTalker(session);
		ConversationBean convo = ConversationDAO.getById(convoId);
		notFoundIfNull(convo);
		
		// YURIY: we only do this if comment is non-empty		
		if(text.trim().length()==0) return;
		
		CommentBean comment = ConversationLogic.createAnswerOrReply(convo, _talker, parentId, text);
		//render html of new comment using tag
		List<CommentBean> _commentsList = Arrays.asList(comment);
		int _level = (comment.getParentId() == null ? 1 : 2);
		
		render("tags/convo/convoCommentsTree.html", _commentsList, _level, _talker);
	}
    
    /**
     * Delete Live Chat message with given index
     * Allowed only for admins
     * @param index index of message (0, 1, 2...) to delete
     */
    public static void deleteChatMessage(String convoId, int index) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	ConversationBean convo = ConversationDAO.getById(convoId);
    	notFoundIfNull(convo);
    	
    	if (!talker.isAdmin()) {
    		forbidden();
    		return;
    	}

    	ConversationDAO.deleteChatMessage(convo.getId(), index);
    	renderText("ok");
    }
    
    /* ----------- Convo replies ------------- */
    /**
     * Create conversation reply for given conversation
     */
    public static void saveConvoReply(String convoId, String text) throws Throwable {
    	Secure.checkAccess();
		TalkerBean _talker = CommonUtil.loadCachedTalker(session);
		ConversationBean convo = ConversationDAO.getById(convoId);
		notFoundIfNull(convo);
		
		CommentBean _reply = ConversationLogic.createConvoReply(convo, _talker, text);
		
		//render html of new comment using tag
		render("tags/convo/convoReply.html", _reply, _talker);
	}
    
	//for Admin Dashboard
    public static void lastTopicId() {
    	String lastConvoId = ConversationDAO.getLastConvoId();
    	renderText(lastConvoId);
    }
    
    /**
     * Merges two conversations together. 
     * After merging there is only one (main) conversation, second is deleted (and redirected to main).
     * 
     * @param convoId Main conversation in merging
     * @param convoToMergeURL Secondary conversation
     * @throws Throwable 
     */
    public static void mergeConvos(String convoId, String convoToMergeURL) throws Throwable {
    	Secure.checkAccess();
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		if (!talker.isAdmin()) {
			forbidden();
			return;
		}
		
		ConversationBean convo = ConversationDAO.getById(convoId);
		if (convoToMergeURL.startsWith("/")) {
			convoToMergeURL = convoToMergeURL.substring(1);
		}
		ConversationBean secondConvo = ConversationDAO.getByURL(convoToMergeURL);
		if (convo == null || secondConvo == null || secondConvo.isDeleted()) {
			notFound();
			return;
		}
		
		//combine answers
		List<CommentBean> secondConvoAnswers = CommentsDAO.loadConvoAnswersTree(secondConvo.getId());
		for (CommentBean answer : secondConvoAnswers) {
			answer.setConvoId(convo.getId());
			CommentsDAO.updateConvoComment(answer);
			
			for (CommentBean reply : answer.getChildren()) {
				reply.setConvoId(convo.getId());
				CommentsDAO.updateConvoComment(reply);
			}
		}
		
		//combine followers
		List<TalkerBean> followersList = convo.getFollowers();
		for (TalkerBean follower : secondConvo.getFollowers()) {
			TalkerBean fullTalker = TalkerDAO.getById(follower.getId());
			if (!followersList.contains(follower)) {
				fullTalker.getFollowingConvosList().add(convo.getId());
			}
			fullTalker.getFollowingConvosList().remove(secondConvo.getId());
			TalkerDAO.updateTalker(fullTalker);
		}
		
		//move actions from second to main convo
    	ActionDAO.updateActionsConvo(secondConvo, convo);
		
		secondConvo.setMergedWith(convo.getId());
		secondConvo.setDeleted(true);
		ConversationDAO.updateConvo(secondConvo);
		
		renderText("ok");
    }
}

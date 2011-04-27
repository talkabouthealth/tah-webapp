package logic;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import play.Logger;
import play.mvc.Scope.Session;

import com.tah.im.IMNotifier;

import util.BitlyUtil;
import util.CommonUtil;
import util.DBUtil;
import util.EmailUtil;
import util.FacebookUtil;
import util.NotificationUtils;
import util.TwitterUtil;
import util.EmailUtil.EmailTemplate;

import static util.DBUtil.*;

import dao.ActionDAO;
import dao.ApplicationDAO;
import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;
import dao.TopicDAO;
import models.CommentBean;
import models.ConversationBean;
import models.ConversationBean.ConvoType;
import models.ServiceAccountBean;
import models.ServiceAccountBean.ServiceType;
import models.TalkerBean;
import models.TopicBean;
import models.TalkerBean.EmailSetting;
import models.actions.Action;
import models.actions.AnswerConvoAction;
import models.actions.AnswerDisplayAction;
import models.actions.FollowConvoAction;
import models.actions.StartConvoAction;
import models.actions.Action.ActionType;

public class ConversationLogic {
	
	public static final String DEFAULT_TALK_TOPIC = "Chats";
	public static final String DEFAULT_QUESTION_TOPIC = "Unorganized";
	
	public static final int CONVERSATIONS_PER_PAGE = 10;
	
	/**
	 * 
	 * @param type
	 * @param title
	 * @param talker creator of the conversation
	 * @param details
	 * @param topicsSet topics/tags
	 * @param notifyTalkers If true - try to send all notifications.
	 * @param parentConvo Parent convo if new question is follow-up
	 * @return
	 */
	public static ConversationBean createConvo(ConvoType type, String title, 
			TalkerBean talker, String details, Set<TopicBean> topicsSet, 
			boolean notifyTalkers, ConversationBean parentConvo, Boolean ccTwitter, Boolean ccFacebook) {
		
		if (topicsSet == null) {
			topicsSet = new HashSet<TopicBean>();
		}
		//when a new topic is created, it automatically has a parent topic of "Unorganized"
		//When a "Live Talk" is started, tag it with the topic "Talks" instead of "Unorganized"
		TopicBean topic = null;
		if (type == ConvoType.QUESTION) {
			topic = TopicDAO.getOrRestoreByTitle(DEFAULT_QUESTION_TOPIC);
		}
		else {
			topic = TopicDAO.getOrRestoreByTitle(DEFAULT_TALK_TOPIC);
		}
		if (topic != null) {
			topicsSet.add(topic);
		}
		
		ConversationBean convo = new ConversationBean();
		convo.setConvoType(type);
		convo.setTopic(title);
		convo.setUid(talker.getId());
		convo.setTalker(talker);
		convo.setCreationDate(Calendar.getInstance().getTime());
		convo.setDetails(details);
		convo.setTopics(topicsSet);
		convo.setOpened(true);
		String topicURL = ApplicationDAO.createURLName(title);
		convo.setMainURL(topicURL);

		String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL());
		convo.setBitly(BitlyUtil.shortLink(convoURL));
		
		ConversationDAO.save(convo);
		
		String convoChatURL = CommonUtil.generateAbsoluteURL("Talk.talkApp", "convoId", convo.getTid());
		convo.setBitlyChat(BitlyUtil.shortLink(convoChatURL));
		ConversationDAO.updateConvo(convo);
		
		String actionID = ActionDAO.saveActionGetId(new StartConvoAction(talker, convo, ActionType.START_CONVO));
		convo.setActionID(actionID);
		
		if (notifyTalkers) {
			//send notifications if Automatic Notifications is On
			NotificationUtils.sendAllNotifications(convo.getId(), null);
		}
		
		//send to Tw & Fb created convo
		for (ServiceAccountBean serviceAccount : talker.getServiceAccounts()) {
			if (serviceAccount.getType() == ServiceType.TWITTER) {
				if (ccTwitter != null && !ccTwitter) {
					continue;
				}
				if (ccTwitter == null && !serviceAccount.isTrue("POST_ON_CONVO")) {
					continue;
				}
			}
			else if (serviceAccount.getType() == ServiceType.FACEBOOK) {
				if (ccFacebook != null && !ccFacebook) {
					continue;
				}
				if (ccFacebook == null && !serviceAccount.isTrue("POST_ON_CONVO")) {
					continue;
				}
			}
			
			String postText = 
				NotificationUtils.preparePostMessageOnConvo(serviceAccount, convo, convoURL, convoChatURL);
			if (serviceAccount.getType() == ServiceType.TWITTER) {
				TwitterUtil.tweet(postText, serviceAccount);
			}
			else if (serviceAccount.getType() == ServiceType.FACEBOOK) {
				FacebookUtil.post(postText, serviceAccount);
			}
		}
		
		//automatically follow started topic
		talker.getFollowingConvosList().add(convo.getId());
		TalkerDAO.updateTalker(talker);
		
		//update parent topic - add new follow-up question
		if (parentConvo != null) {
			parentConvo.getFollowupConvos().add(convo);
			ConversationDAO.updateConvo(parentConvo);
		}

		return convo;
	}
	
	public static CommentBean createAnswerOrReply(ConversationBean convo, TalkerBean talker, String parentId, String text) {
		CommentBean comment = new CommentBean();
		parentId = parentId.trim().length() == 0 ? null : parentId;
		comment.setParentId(parentId);
		comment.setConvoId(convo.getId());
		comment.setFromTalker(talker);
		comment.setText(text);
		comment.setTime(new Date());
		if (parentId == null) {
			//it's an answer (not reply)
			comment.setAnswer(true);
		}
		
		String id = CommentsDAO.saveConvoComment(comment);
		comment.setId(id);
		
		if (convo.isOpened()) {
			convo.setOpened(false);
			ConversationDAO.updateConvo(convo);
		}
		if (comment.isAnswer()) {
			//when user answers a question, they automatically follow the question
			if (!talker.getFollowingConvosList().contains(convo.getId())) {
				talker.getFollowingConvosList().add(convo.getId());
				TalkerDAO.updateTalker(talker);
			}
		}
		
		//actions
		CommentBean answer = null;
		if (parentId == null) {
			ActionDAO.saveAction(new AnswerConvoAction(talker, convo, comment, null, ActionType.ANSWER_CONVO));
		}
		else {
			answer = CommentsDAO.getConvoCommentById(parentId);
			ActionDAO.saveAction(new AnswerConvoAction(talker, convo, answer, comment, ActionType.REPLY_CONVO));
		}
		
		//Email/IM/Twitter/FB notifications
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("convo", convo.getTopic());
		vars.put("other_talker", talker.getUserName());
		if (comment.isAnswer()) {
			vars.put("answer_text", CommonUtil.commentToHTML(comment));
		}
		else {
			vars.put("reply_text", CommonUtil.commentToHTML(comment));
			vars.put("answer_text", CommonUtil.commentToHTML(answer));
		}
		vars.put("convo_type", convo.getConvoType().stringValue());
		String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL());
		vars.put("convo_url", convoURL);
		
		if (comment.isAnswer()) {
			for (TalkerBean follower : convo.getFollowers()) {
	    		if (!follower.equals(talker)) { //do not send notification to himself
	        		NotificationUtils.sendEmailNotification(EmailSetting.CONVO_COMMENT, follower, vars);
	        		
	        		//check Twitter/FB/IM
	        		for (ServiceAccountBean serviceAccount : follower.getServiceAccounts()) {
	        			if (!serviceAccount.isTrue("NOTIFY_ON_ANSWER")) {
            				continue;
            			}
	        			
	        			if (serviceAccount.getType() == ServiceType.TWITTER) {
	        				//New answer by <username> for <question>... http://bit.ly/asdfw
	        				String dmText = TwitterUtil.prepareTwit("New answer by "+
	        						comment.getFromTalker().getUserName()+" for <PARAM> "+convo.getBitly(), convo.getTopic());
	        				TwitterUtil.sendDirect(serviceAccount.getId(), dmText);
	        			}
//	        			else if (serviceAccount.getType() == ServiceType.FACEBOOK) {
//	        				//New answer by <username> for <question>: <answer>
//	        				String dmText = "New answer by "+
//	        						comment.getFromTalker().getUserName()+" for "+convo.getTopic()+": "+comment.getText();
//	        				FacebookUtil.sendDirect(serviceAccount.getId(), dmText);
//	        			}
	        		}
	        		
	        		//For convo author we send other IM notification
	        		if (!follower.equals(convo.getTalker())) {
	        			try {
	        				String imText = "New answer by "+
								comment.getFromTalker().getUserName()+" for "+convo.getTopic()+": "+comment.getText();
	        				System.out.println("NOTIFY: "+imText);
	        				IMNotifier.getInstance().followersAnswerNotification(follower.getId(), convo.getId(), imText);
		    			} catch (Throwable e) {
		    				Logger.error(e, "Sending notifications on Convo answer");
		    			}	        				
	        		}
	    		}
	    	}
			
			
		}
		else {
			//for replies - send to author of answer and question.
			if (!talker.equals(convo.getTalker())) {
        		NotificationUtils.sendEmailNotification(EmailSetting.CONVO_COMMENT, convo.getTalker(), vars);
    		}
			
			// we are going to change this to send emails to all reply authors of the same answer
			//if (!talker.equals(answer.getFromTalker()) && !convo.getTalker().equals(answer.getFromTalker())) {
        	//	NotificationUtils.sendEmailNotification(EmailSetting.CONVO_COMMENT, answer.getFromTalker(), vars);
    		//}			
			String parent = comment.getParentId();
			
			List<String> replies = CommentsDAO.getConvoReplies(parent);
			// include the originating answer in the list of comments to be retrieved
			replies.add(parent);
			List<CommentBean> objreplies = CommentsDAO.getConvoCommentsByIds(replies);
			
			// construct set (single occurrences) of participating users 
			Set<TalkerBean> participants = new HashSet<TalkerBean>();
			for(CommentBean reply : objreplies) {
				TalkerBean thistalker = reply.getFromTalker();
				participants.add(thistalker);
			}	
						
			// distribute emails to all, do not send to yourself + question author (sent above)
			for(TalkerBean thistalker : participants) {
				if(!thistalker.equals(talker) && !thistalker.equals(convo.getTalker())) {
	        		NotificationUtils.sendEmailNotification(EmailSetting.CONVO_COMMENT, thistalker, vars);
				}
			}	
		}
    	
    	
    	if (comment.isAnswer()) {
    		for (ServiceAccountBean serviceAccount : talker.getServiceAccounts()) {
    			if (!serviceAccount.isTrue("POST_ON_ANSWER")) {
    				continue;
    			}
    			
    			//Just answered a question on TalkAboutHealth: "What are the ..." http://bit.ly/lksa
    			String postText = "Just answered a question on TalkAboutHealth: \"<PARAM>\" ";
    			if (serviceAccount.getType() == ServiceType.TWITTER) {
    				postText = postText + convo.getBitly();
    				postText = TwitterUtil.prepareTwit(postText, convo.getTopic());
    				TwitterUtil.tweet(postText, serviceAccount);
    			}
    			else if (serviceAccount.getType() == ServiceType.FACEBOOK) {
    				postText = postText + convoURL;
    				postText = postText.replace("<PARAM>", convo.getTopic());
    				FacebookUtil.post(postText, serviceAccount);
    			}
    		}
    		
    		//If conversation was created from Twitter - send notification to author
    		if (convo.getFrom() != null && convo.getFrom().equals("twitter")) {
    			TalkerBean convoAuthor = TalkerDAO.getById(convo.getTalker().getId());
    			if (!talker.equals(convoAuthor)) {
    				ServiceAccountBean twitterAccount = convoAuthor.serviceAccountByType(ServiceType.TWITTER);
    				if (twitterAccount != null) {
    					//You have received an answer to "<question>...". View the answer at: http://bit.ly/lksa
    					StringBuilder dmText = new StringBuilder();
    					dmText.append("You have received an answer to \"<PARAM>\". ");
    					dmText.append("\nView the answer at: ");
    					dmText.append(convo.getBitly());
    					
    					String dmTextString = TwitterUtil.prepareTwit(dmText.toString(), convo.getTopic());
    					TwitterUtil.sendDirect(twitterAccount.getId(), dmTextString);
    				}
    			}
    		}
    	}
    	
    	//TODO: check if we need to send this (notification settings), add check for all IM notifications
    	//The exception is users will receive IM notifications of answers 
    	//if they started the conversation/question or if they answered a question and 
    	//the user who started the question wants to follow up.
    	Set<String> talkersForNotification = new HashSet<String>();
    	talkersForNotification.add(convo.getTalker().getId());
    	if (answer != null) {
    		talkersForNotification.add(answer.getFromTalker().getId());
    	}
    	//do not send notification to himself
    	talkersForNotification.remove(talker.getId());
    	if (!talkersForNotification.isEmpty()) {
    		System.out.println("In IM segment!");
    		try {    		
    			IMNotifier imNotifier = IMNotifier.getInstance();
				String[] uidArray = talkersForNotification.toArray(new String[talkersForNotification.size()]);
				imNotifier.answerNotify(uidArray, talker.getUserName(), convo.getId(), 
						comment.getParentId(), comment.getId(), comment.getText());
			} catch (Throwable e) {
				Logger.error(e, "Sending notifications on Convo answer");
			}
		}
    	
    	return comment;
	}

	/**
	 * Creates reply to the given conversation
	 * @param convo
	 * @param fromTalker
	 * @param text
	 */
	public static CommentBean createConvoReply(ConversationBean convo, TalkerBean fromTalker, String text) {
		CommentBean convoReply = new CommentBean();
		convoReply.setConvoReply(true);
		convoReply.setParentId(null);
		convoReply.setConvoId(convo.getId());
		convoReply.setFromTalker(fromTalker);
		convoReply.setText(text);
		convoReply.setTime(new Date());
		
		String id = CommentsDAO.saveConvoComment(convoReply);
		convoReply.setId(id);
		
		//When a reply is added, an email is sent to the original author of the question.
		if (!fromTalker.equals(convo.getTalker())) {
			NotificationUtils.emailNotifyOnConvoReply(convo, fromTalker, convoReply);
		}
		
		// we are going to change this to send emails to all reply authors of the same answer
		//  for top replies [children ...] are stored on root convo record
		String parent = convoReply.getConvoId();
		
		List<String> replies = CommentsDAO.getConvoReplies(DBUtil.getCollection(CommentsDAO.CONVERSATIONS_COLLECTION),parent);
		// include the originating answer in the list of comments to be retrieved
		replies.add(parent);
		List<CommentBean> objreplies = CommentsDAO.getConvoCommentsByIds(replies);
		
		// construct set (single occurrences) of participating users 
		Set<TalkerBean> participants = new HashSet<TalkerBean>();
		for(CommentBean reply : objreplies) {
			TalkerBean thistalker = reply.getFromTalker();
			participants.add(thistalker);
		}	
					
		// distribute emails to all, do not send to yourself + question author (sent above)
		for(TalkerBean thistalker : participants) {
			if(!thistalker.equals(fromTalker) && !thistalker.equals(convo.getTalker())) {
				NotificationUtils.emailNotifyOnConvoReply(convo, fromTalker, convoReply,thistalker);
			}
		}		
    	
    	return convoReply;
	}

	/**
	 * Converts given list of conversations to Feed format (for better display)
	 * @param conversations
	 * @return
	 */
	public static List<Action> convosToFeed(Collection<ConversationBean> conversations) {
		List<Action> convosFeed = new ArrayList<Action>();
		for (ConversationBean convo : conversations) {
			convo.setComments(CommentsDAO.loadConvoAnswers(convo.getId()));
			
			TalkerBean activityTalker = convo.getTalker();
			//show top answer or simple convo
			CommentBean topAnswer = null;
			if (!convo.getComments().isEmpty()) {
				topAnswer = convo.getComments().get(0);
				topAnswer = CommentsDAO.getConvoCommentById(topAnswer.getId());
				activityTalker = topAnswer.getFromTalker();
			}
			
			String actionText = null;
			if (topAnswer != null) {
				actionText = "Top Answer";
			}
			
			AnswerDisplayAction convoAction =
				new AnswerDisplayAction(activityTalker, convo, topAnswer, 
						ActionType.ANSWER_CONVO, actionText);
			convoAction.setId(convo.getId());
			convoAction.setTime(convo.getCreationDate());
			
			convosFeed.add(convoAction);
		}
		return convosFeed;
	}

	/**
	 * Send email to TAH Support about flagging some content
	 * 
	 * @param contentType
	 * @param convo
	 * @param reason
	 * @param content
	 * @param talker
	 */
	public static void flagContent(String contentType, ConversationBean convo,
			String reason, String content, TalkerBean talker) {
		Map<String, String> vars = new HashMap<String, String>();
    	vars.put("content_type", contentType);
    	vars.put("content_link", CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL()));
    	vars.put("reason", reason);
		vars.put("content", content);
		vars.put("name", talker.getUserName());
		vars.put("email", talker.getEmail());
		EmailUtil.sendEmail(EmailTemplate.FLAGGED, EmailUtil.SUPPORT_EMAIL, vars, null, false);
	}
}

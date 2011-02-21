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

import play.mvc.Scope.Session;

import com.tah.im.IMNotifier;

import util.BitlyUtil;
import util.CommonUtil;
import util.FacebookUtil;
import util.NotificationUtils;
import util.TwitterUtil;
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
			boolean notifyTalkers, ConversationBean parentConvo) {
		
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
		
		ActionDAO.saveAction(new StartConvoAction(talker, convo, ActionType.START_CONVO));
		
		if (notifyTalkers) {
			//send notifications if Automatic Notifications is On
			NotificationUtils.sendAllNotifications(convo.getId(), null);
		}
		//send to Tw & Fb created convo
		for (ServiceAccountBean serviceAccount : talker.getServiceAccounts()) {
			if (!serviceAccount.isTrue("POST_ON_CONVO")) {
				continue;
			}
//			Just asked a question on TalkAboutHealth: "What are the best hospitals in NYC for breast cancer ..." http://bit.ly/lksa
//			Just started a live chat on TalkAboutHealth: "What are the best hospitals in NYC for breast cancer ..." http://bit.ly/lksa
			
			String postText = null;
			String bitlyLinkText = null;
			String fullLinkText = null;
			if (convo.getConvoType() == ConvoType.CONVERSATION) {
				postText = "Just started a live chat on TalkAboutHealth: \"<PARAM>\" ";
				bitlyLinkText = convo.getBitlyChat();
				fullLinkText = convoChatURL;
			}
			else {
				postText = "Just asked a question on TalkAboutHealth: \"<PARAM>\" ";
				bitlyLinkText = convo.getBitlyChat();
				fullLinkText = convoURL;
			}
			
			if (serviceAccount.getType() == ServiceType.TWITTER) {
				postText = postText + bitlyLinkText;
				postText = TwitterUtil.prepareTwit(postText, convo.getTopic());
				TwitterUtil.tweet(postText, serviceAccount);
			}
			else if (serviceAccount.getType() == ServiceType.FACEBOOK) {
				postText = postText + fullLinkText;
				postText = postText.replace("<PARAM>", convo.getTopic());
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
		
		//Email and IM notifications
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("convo", convo.getTopic());
		vars.put("other_talker", talker.getUserName());
		if (comment.isAnswer()) {
			vars.put("answer_text", comment.getText());
		}
		else {
			vars.put("reply_text", comment.getText());
			vars.put("answer_text", answer.getText());
		}
		vars.put("convo_type", convo.getConvoType().stringValue());
		String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL());
		vars.put("convo_url", convoURL);
		if (comment.isAnswer()) {
			for (TalkerBean follower : convo.getFollowers()) {
	    		if (!talker.equals(follower)) { //do not send notification to himself
	        		NotificationUtils.sendEmailNotification(EmailSetting.CONVO_COMMENT, follower, vars);
	    		}
	    	}
		}
		else {
			//for replies - send to author of answer and question.
			if (!talker.equals(convo.getTalker())) {
        		NotificationUtils.sendEmailNotification(EmailSetting.CONVO_COMMENT, convo.getTalker(), vars);
    		}
			if (!talker.equals(answer.getFromTalker()) && !convo.getTalker().equals(answer.getFromTalker())) {
        		NotificationUtils.sendEmailNotification(EmailSetting.CONVO_COMMENT, answer.getFromTalker(), vars);
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
    	
//    	The exception is users will receive IM notifications of answers 
//    	if they started the conversation/question or if they answered a question and 
//    	the user who started the question wants to follow up.
    	Set<String> talkersForNotification = new HashSet<String>();
    	talkersForNotification.add(convo.getTalker().getId());
    	if (answer != null) {
    		talkersForNotification.add(answer.getFromTalker().getId());
    	}
    	//do not send notification to himself
    	talkersForNotification.remove(talker.getId());
    	if (!talkersForNotification.isEmpty()) {
			IMNotifier imNotifier = IMNotifier.getInstance();
			try {
				String[] uidArray = talkersForNotification.toArray(new String[talkersForNotification.size()]);
				imNotifier.answerNotify(uidArray, talker.getUserName(), convo.getId(), 
						comment.getParentId(), comment.getId(), comment.getText());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
    	
    	return comment;
	}

	/**
	 * Creates reply to the given conversation
	 * @param convo
	 * @param talker
	 * @param text
	 */
	public static CommentBean createConvoReply(ConversationBean convo, TalkerBean talker, String text) {
		CommentBean convoReply = new CommentBean();
		convoReply.setConvoReply(true);
		convoReply.setParentId(null);
		convoReply.setConvoId(convo.getId());
		convoReply.setFromTalker(talker);
		convoReply.setText(text);
		convoReply.setTime(new Date());
		
		String id = CommentsDAO.saveConvoComment(convoReply);
		convoReply.setId(id);
		
		//When a reply is added, an email is sent to the original author of the question.
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("convo", convo.getTopic());
		vars.put("other_talker", talker.getUserName());
		vars.put("convoreply_text", convoReply.getText());
		vars.put("convo_type", convo.getConvoType().stringValue());
		String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL());
		vars.put("convo_url", convoURL);
		if (!talker.equals(convo.getTalker())) {
    		NotificationUtils.sendEmailNotification(EmailSetting.CONVO_COMMENT, convo.getTalker(), vars);
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
			
			AnswerDisplayAction convoAction =
				new AnswerDisplayAction(activityTalker, convo, topAnswer, ActionType.ANSWER_CONVO, topAnswer != null);
			convoAction.setTime(convo.getCreationDate());
			
			convosFeed.add(convoAction);
		}
		return convosFeed;
	}

}

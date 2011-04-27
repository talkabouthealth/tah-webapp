package util;

import improject.IMSession.IMService;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;

import play.Logger;
import util.EmailUtil.EmailTemplate;

import models.ConversationBean;
import models.ConversationBean.ConvoType;
import models.CommentBean;
import models.IMAccountBean;
import models.ServiceAccountBean;
import models.TalkerBean;
import models.ThankYouBean;
import models.ServiceAccountBean.ServiceType;
import models.TalkerBean.EmailSetting;

import com.sun.corba.se.impl.encoding.OSFCodeSetRegistry.Entry;
import com.tah.im.IMNotifier;
import com.tah.im.model.UserInfo;
import com.tah.im.singleton.OnlineUsersSingleton;

import dao.ConfigDAO;
import dao.ConversationDAO;
import dao.NotificationDAO;
import dao.TalkerDAO;

public class NotificationUtils {
	
	//Used for storing configuration for automatic/manual configuration
	public static final String AUTOMATIC_NOTIFICATIONS_CONFIG = "AutomaticNotifications";
	
	/**
	 * Sends IM and Twitter notifications if automatic notification is ON.
	 * Notifies about new question/chat and restart chat.
	 * 
	 * @param convoId
	 * @param restartTalkerId If restarted chat - id of talker who restarted
	 */
	public static void sendAllNotifications(final String convoId, final String restartTalkerId) {
		boolean automaticNotification = ConfigDAO.getBooleanConfig(AUTOMATIC_NOTIFICATIONS_CONFIG);
		if (!automaticNotification) {
			return;
		}
		
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				NotificationUtils.sendIMNotifications(convoId, restartTalkerId);
				NotificationUtils.sendTwitterNotifications(convoId, restartTalkerId);
			}
		});
	}
	
	/**
	 * Rules are:
	 * - notify 10 latest people who came online
	 * - don't notify more than 1x every 3 hours
	 * - don't notify more the 3x per day
	 */
	//TODO: simplify IM notifications & add here comments
	public static void sendIMNotifications(String convoId, String restartTalkerId) {
		OnlineUsersSingleton onlineUsersSingleton = OnlineUsersSingleton.getInstance();
		Map<String, UserInfo> onlineUsers = onlineUsersSingleton.getOnlineUserMap();
		
		Set<String> talkersForNotification = new HashSet<String>();
		//let's have support@talkabouthealth.com receive all notifications via IM
		talkersForNotification.add(EmailUtil.SUPPORT_EMAIL);
		
		//prepare talkers that are ok to notify
		for (UserInfo userInfo : onlineUsers.values()) {
			//notifications for last 3 hours and day
			Calendar threeHoursBeforeNow = Calendar.getInstance();
			threeHoursBeforeNow.add(Calendar.HOUR, -3);
			int threeHoursNotifications = NotificationDAO.numOfNotificationsForTime(userInfo.getUid(), threeHoursBeforeNow, null);
			int dayNotifications = NotificationDAO.numOfNotificationsForDay(userInfo.getUid(), null);
			
			if (threeHoursNotifications == 0 && dayNotifications < 3) {
				talkersForNotification.add(userInfo.getUid());
			}
		}
		
		Logger.info("Notifying: "+talkersForNotification);
		
		if (!talkersForNotification.isEmpty()) {
			IMNotifier imNotifier = IMNotifier.getInstance();
			try {
				String[] uidArray = talkersForNotification.toArray(new String[talkersForNotification.size()]);
				imNotifier.broadcast(uidArray, convoId, restartTalkerId);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void sendTwitterNotifications(String convoId, String restartTalkerId) {
		ConversationBean convo = ConversationDAO.getById(convoId);
		TalkerBean authorTalker = null;
		if (restartTalkerId != null) {
			authorTalker = TalkerDAO.getById(restartTalkerId);
		}
		else {
			authorTalker = convo.getTalker();
		}
		
		String messageText = prepareTwitterNotificationOnConvo(restartTalkerId, convo, authorTalker);
    	
		for (TalkerBean talker : TalkerDAO.loadAllTalkers()) {
			if (talker.isSuspended() || talker.isDeactivated()
					|| talker.equals(authorTalker)) { //do not send notification to author of the event
				continue;
			}
			
			ServiceAccountBean twitterAccount = talker.serviceAccountByType(ServiceType.TWITTER);
			if (twitterAccount != null && twitterAccount.isTrue("NOTIFY")) {
				//only 2 Twitter notifications per day
				int dayNotifications = NotificationDAO.numOfNotificationsForDay(talker.getId(), "TWITTER");
				if (dayNotifications >= 2) {
					continue;
				}
				
				NotificationDAO.saveTwitterNotification(talker.getId(), convoId);
				TwitterUtil.sendDirect(twitterAccount.getId(), messageText);
			}
		}
	}

	/**
	 * Sends email notification if talker turned on given email setting.
	 * 
	 * @param emailSetting Notification belongs to this email setting
	 * @param talker Talker to notify
	 * @param vars
	 */
	public static void sendEmailNotification(EmailSetting emailSetting, 
			TalkerBean talker, Map<String, String> vars) {	

		// always send notification for personal questions %-/
		if (emailSetting == EmailSetting.CONVO_PERSONAL || talker.getEmailSettings().contains(emailSetting)) {
			vars.put("username", talker.getUserName());
			
			if (emailSetting == EmailSetting.CONVO_COMMENT && vars.get("reply_text") != null) {
				//Reply to the convo answer
				EmailUtil.sendEmail(EmailTemplate.NOTIFICATION_CONVO_REPLY_TO_ANSWER, talker.getEmail(), vars, null, true);
			}
			else if (emailSetting == EmailSetting.CONVO_COMMENT && vars.get("convoreply_text") != null) {
				//Reply to the conversation
				EmailUtil.sendEmail(EmailTemplate.NOTIFICATION_CONVO_REPLY, talker.getEmail(), vars, null, true);
			}
			else if (emailSetting == EmailSetting.RECEIVE_COMMENT && vars.get("reply_text") != null) {
				//Reply to the thought
				EmailUtil.sendEmail(EmailTemplate.NOTIFICATION_REPLY_TO_COMMENT_IN_JOURNAL, talker.getEmail(), vars, null, true);
			}
			else if (emailSetting == EmailSetting.CONVO_PERSONAL && vars.get("convo") != null) {
				//Personal question
				EmailUtil.sendEmail(EmailTemplate.NOTIFICATION_PERSONAL_QUESTION, talker.getEmail(), vars, null, true);
			}			
			else {
				EmailUtil.sendEmail(emailSetting.getEmailTemplate(), talker.getEmail(), vars, null, true);
			}
		}
	}
	
	//--------------- Different convenient methods for email notifications --------
	/**
	 * @param convo
	 * @param fromTalker
	 * @param convoReply
	 */
	public static void emailNotifyOnConvoReply(ConversationBean convo,
			TalkerBean fromTalker, CommentBean convoReply) {
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("convo", convo.getTopic());
		vars.put("other_talker", fromTalker.getUserName());
		vars.put("convoreply_text", CommonUtil.commentToHTML(convoReply));
		vars.put("convo_type", convo.getConvoType().stringValue());
		String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL());
		vars.put("convo_url", convoURL);
   		sendEmailNotification(EmailSetting.CONVO_COMMENT, convo.getTalker(), vars);
	}
	
	/**
	 * @param convo
	 * @param fromTalker
	 * @param convoReply
	 */
	public static void emailNotifyOnConvoReply(ConversationBean convo,
			TalkerBean fromTalker, CommentBean convoReply,TalkerBean toTalker) {
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("convo", convo.getTopic());
		vars.put("other_talker", fromTalker.getUserName());
		vars.put("convoreply_text", CommonUtil.commentToHTML(convoReply));
		vars.put("convo_type", convo.getConvoType().stringValue());
		String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL());
		vars.put("convo_url", convoURL);
   		sendEmailNotification(EmailSetting.CONVO_COMMENT, toTalker, vars);
	}
	
	/**
	 * @param fromTalker
	 * @param toTalker
	 * @param thankYouBean
	 */
	public static void emailNotifyOnThankYou(TalkerBean fromTalker,
			TalkerBean toTalker, ThankYouBean thankYouBean) {
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("other_talker", fromTalker.getUserName());
		vars.put("thankyou_text", thankYouBean.getNote());
		sendEmailNotification(EmailSetting.RECEIVE_THANKYOU, 
				toTalker, vars);
	}
	
	/**
	 * @param talker
	 * @param followingTalker
	 */
	public static void emailNotifyOnFollow(TalkerBean talker, TalkerBean followingTalker) {
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("other_talker", talker.getUserName());
		sendEmailNotification(EmailSetting.NEW_FOLLOWER, 
				followingTalker, vars);
	}
	
	/**
	 * @param convo
	 */
	public static void emailNotifyOnConvoRestart(ConversationBean convo) {
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("convo", convo.getTopic());
		String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL());
		vars.put("convo_url", convoURL);
		String convoTalkURL = CommonUtil.generateAbsoluteURL("Talk.talkApp", "convoId", convo.getTid());
		vars.put("convo_talk_url", convoTalkURL);
    	for (TalkerBean follower : convo.getFollowers()) {
    		sendEmailNotification(EmailSetting.CONVO_RESTART, follower, vars);
    	}
	}
	
	/**
	 * @param talker
	 * @param convo
	 */
	public static void emailNotifyOnConvoSummary(TalkerBean talker, ConversationBean convo) {
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("convo", convo.getTopic());
		vars.put("other_talker", talker.getUserName());
		vars.put("summary_text", convo.getSummary());
		String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL());
		vars.put("convo_url", convoURL);
		for (TalkerBean follower : convo.getFollowers()) {
			sendEmailNotification(EmailSetting.CONVO_SUMMARY, follower, vars);
		}
	}
	
	
	// ------------------ Methods for generating share/notification messages -----------
	
	/**
	 * 
	 * @param restartTalkerId Id of talker if convo was restarted (on null)
	 * @param convo
	 * @param authorTalker
	 * @return
	 */
	public static String prepareTwitterNotificationOnConvo(String restartTalkerId,
			ConversationBean convo, TalkerBean authorTalker) {
		StringBuilder message = new StringBuilder();
		message.append(authorTalker.getUserName());
		if (convo.getConvoType() == ConvoType.CONVERSATION || restartTalkerId != null) {
			//mnj5 started the chat: What items come in handy after a mastectomy... To join: http://bit.ly/dfsqe
			message.append(" started the chat: \"<PARAM>\" To join: ");
    		message.append(convo.getBitlyChat());
		}
		else {
    		//mnj5 asked the question: What items come in handy after a mastectomy... To answer: http://bit.ly/dfsqe
			message.append(" asked the question: \"<PARAM>\" To answer: ");
    		message.append(convo.getBitly());
		}
		String messageText = TwitterUtil.prepareTwit(message.toString(), convo.getTopic());
		return messageText;
	}
	
//	Just asked a question on TalkAboutHealth: "What are the best hospitals in NYC for breast cancer ..." http://bit.ly/lksa
//	Just started a live chat on TalkAboutHealth: "What are the best hospitals in NYC for breast cancer ..." http://bit.ly/lksa
	public static String preparePostMessageOnConvo(ServiceAccountBean serviceAccount, 
			ConversationBean convo, String convoURL, String convoChatURL) {
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
			bitlyLinkText = convo.getBitly();
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
		return postText;
	}
	
	/**
	 * @param fromTalker
	 * @param pageType
	 * @param pageInfo Topic's or Conversation's title, depending on page type
	 * @return
	 */
	public static String prepareEmailShareMessage(String fromTalker, String pageType, String pageInfo) {
		String subject = "";
		if (pageType.equalsIgnoreCase("Topic")) {
			subject = fromTalker+" thought you would be interested in the topic \""+pageInfo+"\" on TalkAboutHealth";
		}
		else if (pageType.equalsIgnoreCase("Conversation")) {
			subject = fromTalker+" thought you would be interested in the conversation \""+pageInfo+"\" on TalkAboutHealth";
		}
		else if (pageType.equalsIgnoreCase("TalkAboutHealth")) {
			subject = fromTalker+" has invited you to try TalkAboutHealth";
		}
		return subject;
	}
	
	
	
	
	
	
	/**
	 * Send IM invitation for newly added IM account
	 */
	public static void sendIMInvitation(IMAccountBean imAccount) {
		IMNotifier imNotifier = IMNotifier.getInstance();
		try {
			imNotifier.addContact(imAccount.getService(), imAccount.getUserName());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

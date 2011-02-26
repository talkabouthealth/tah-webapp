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
import models.IMAccountBean;
import models.ServiceAccountBean;
import models.TalkerBean;
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
	//TODO: simplify IM notifications
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
		TalkerBean restartTalker = null;
		if (restartTalkerId != null) {
			restartTalker = TalkerDAO.getById(restartTalkerId);
		}
		else {
			restartTalker = convo.getTalker();
		}
		
		StringBuilder message = new StringBuilder();
		message.append(restartTalker.getUserName());
		if (convo.getConvoType() == ConvoType.CONVERSATION || restartTalkerId != null) {
			//mnj5 started the talk: What items come in handy after a mastectomy... To join the talk: http://bit.ly/dfsqe
			message.append(" started the chat: \"<PARAM>\" To join: ");
    		message.append(convo.getBitlyChat());
		}
		else {
    		//mnj5 asked the question: What items come in handy after a mastectomy... To answer: http://bit.ly/dfsqe
			message.append(" asked the question: \"<PARAM>\" To answer: ");
    		message.append(convo.getBitly());
		}
		String messageText = TwitterUtil.prepareTwit(message.toString(), convo.getTopic());
    	
		for (TalkerBean talker : TalkerDAO.loadAllTalkers()) {
			if (talker.isSuspended() || talker.isDeactivated()
					|| talker.equals(restartTalker)) { //do not send notification to author of the event
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
		if (talker.getEmailSettings().contains(emailSetting)) {
			vars.put("username", talker.getUserName());
			
			//for convo and thoughts replies we have separate templates
			if (emailSetting == EmailSetting.CONVO_COMMENT && vars.get("reply_text") != null) {
				EmailUtil.sendEmail(EmailTemplate.NOTIFICATION_CONVO_REPLY_TO_ANSWER, talker.getEmail(), vars, null, true);
			}
			else if (emailSetting == EmailSetting.CONVO_COMMENT && vars.get("convoreply_text") != null) {
				EmailUtil.sendEmail(EmailTemplate.NOTIFICATION_CONVO_REPLY, talker.getEmail(), vars, null, true);
			}
			else if (emailSetting == EmailSetting.RECEIVE_COMMENT && vars.get("reply_text") != null) {
				EmailUtil.sendEmail(EmailTemplate.NOTIFICATION_REPLY_TO_COMMENT_IN_JOURNAL, talker.getEmail(), vars, null, true);
			}
			else {
				EmailUtil.sendEmail(emailSetting.getEmailTemplate(), talker.getEmail(), vars, null, true);
			}
		}
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

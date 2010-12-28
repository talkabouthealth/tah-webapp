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
	
	public static final String AUTOMATIC_NOTIFICATIONS_CONFIG = "AutomaticNotifications";
	
	//Send IM and Twitter notifications in separate thread
	public static void sendAllNotifications(final String convoId, final String restartTalkerId) {
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				NotificationUtils.sendAutomaticNotifications(convoId, restartTalkerId);
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
	public static void sendAutomaticNotifications(String convoId, String restartTalkerId) {
		boolean automaticNotification = ConfigDAO.getBooleanConfig(AUTOMATIC_NOTIFICATIONS_CONFIG);
		if (!automaticNotification) {
			System.out.println("NO AUTOMAT!");
			return;
		}
		
		OnlineUsersSingleton onlineUsersSingleton = OnlineUsersSingleton.getInstance();
		//TODO: find 10 latest people who came online?
		Map<String, UserInfo> onlineUsers = onlineUsersSingleton.getOnlineUserMap();
		
		Set<String> talkersForNotification = new HashSet<String>();
		
		//let's have support@talkabouthealth.com receive all notifications via IM
		talkersForNotification.add(EmailUtil.SUPPORT_EMAIL);
		
//		talkersForNotification.add(TalkerDAO.getByUserName("osezno1").getId());
		
		for (UserInfo userInfo : onlineUsers.values()) {
			//notifications for last 3 hours and day
			Calendar threeHoursBeforeNow = Calendar.getInstance();
			threeHoursBeforeNow.add(Calendar.HOUR, -3);
			int threeHoursNotifications = NotificationDAO.numOfNotificationsForTime(userInfo.getUid(), threeHoursBeforeNow);
			int dayNotifications = NotificationDAO.numOfNotificationsForDay(userInfo.getUid());
			
			if (threeHoursNotifications == 0 && dayNotifications < 3) {
				talkersForNotification.add(userInfo.getUid());
			}
		}
		
		System.out.println("Notifying: "+talkersForNotification);
		
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
		//TODO: we must send only 2 notifications per day?
		
		ConversationBean convo = ConversationDAO.getByConvoId(convoId);
		TalkerBean restartTalker = null;
		if (restartTalkerId != null) {
			restartTalker = TalkerDAO.getById(restartTalkerId);
		}
		else {
			restartTalker = convo.getTalker();
		}
		
		//TODO: better impl?
		StringBuilder message = new StringBuilder();
		message.append(restartTalker.getUserName());
		if (convo.getConvoType() == ConvoType.CONVERSATION || restartTalkerId != null) {
			//mnj5 started the talk: What items come in handy after a mastectomy... To join the talk: http://bit.ly/dfsqe
			message.append(" started the chat: \"");
    		
    		String convoTitle = convo.getTopic();
    		if (convoTitle.length() > 50) {
    			convoTitle = convoTitle.substring(0, 50)+"...";
    		}
    		message.append(convoTitle+"\" To join the chat:");
    		message.append(convo.getBitlyChat());
		}
		else {
    		//mnj5 asked the question: What items come in handy after a mastectomy... To answer: http://bit.ly/dfsqe
			message.append(" asked the question: \"");
    		
    		String convoTitle = convo.getTopic();
    		if (convoTitle.length() > 55) {
    			convoTitle = convoTitle.substring(0, 55)+"...";
    		}
    		message.append(convoTitle+"\" To answer: ");
    		message.append(convo.getBitly());
		}
    	
		for (TalkerBean talker : TalkerDAO.loadAllTalkers()) {
			if (talker.isSuspended() || talker.isDeactivated()) {
				continue;
			}
			
			//do not send notification to author of the event
			if (talker.equals(restartTalker)) {
				continue;
			}
			
			ServiceAccountBean twitterAccount = talker.serviceAccountByType(ServiceType.TWITTER);
			if (twitterAccount != null && twitterAccount.isTrue("NOTIFY")) {
				TwitterUtil.sendDirect(twitterAccount.getId(), message.toString());
			}
		}
	}
	
	public static void sendEmailNotification(EmailSetting emailSetting, 
			TalkerBean talker, Map<String, String> vars) {
		if (talker.loadEmailSettings().contains(emailSetting)) {
			vars.put("username", talker.getUserName());
			
			if (emailSetting == EmailSetting.CONVO_COMMENT && vars.get("reply_text") != null) {
				//for replies we have separate template
				EmailUtil.sendEmail(EmailTemplate.NOTIFICATION_CONVO_REPLY_TO_ANSWER, talker.getEmail(), vars, null, true);
			}
			else {
				EmailUtil.sendEmail(emailSetting.getEmailTemplate(), talker.getEmail(), vars, null, true);
			}
		}
	}

}

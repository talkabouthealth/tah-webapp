package util;

import improject.IMSession.IMService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import play.Logger;
import util.EmailUtil.EmailTemplate;

import models.ConversationBean;
import models.ConversationBean.ConvoType;
import models.TalkerBean;
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
			
			//Try twitter notifications
			ConversationBean convo = ConversationDAO.getByConvoId(convoId);
			TalkerBean restartTalker = null;
			if (restartTalkerId != null) {
				restartTalker = TalkerDAO.getById(restartTalkerId);
			}
			else {
				restartTalker = convo.getTalker();
			}
			
			StringBuilder message = new StringBuilder();
			message.append(restartTalker.getUserName());
			if (convo.getConvoType() == ConvoType.QUESTION) {
				//mnj5 asked the question: What items come in handy after a mastectomy... To answer: http://bit.ly/dfsqe
				message.append(" asked the question: \"");
	    		
	    		String convoTitle = convo.getTopic();
	    		if (convoTitle.length() > 55) {
	    			convoTitle = convoTitle.substring(0, 55)+"...";
	    		}
	    		message.append(convoTitle+"\" To answer: ");
			}
			else {
				//mnj5 started the talk: What items come in handy after a mastectomy... To join the talk: http://bit.ly/dfsqe
				message.append(" started the chat: \"");
	    		
	    		String convoTitle = convo.getTopic();
	    		if (convoTitle.length() > 50) {
	    			convoTitle = convoTitle.substring(0, 50)+"...";
	    		}
	    		message.append(convoTitle+"\" To join the chat:");
			}
			message.append(convo.getBitly());
	    	
			for (String talkerId : talkersForNotification) {
				//do not send notification to author of the event
				if (talkerId.equals(restartTalker.getId()) || talkerId.equalsIgnoreCase(EmailUtil.SUPPORT_EMAIL)) {
					continue;
				}
				
				TalkerBean talker = TalkerDAO.getById(talkerId);
				if (talker.getAccountId() != null) {
					TwitterUtil.sendDirect(talker.getAccountId(), message.toString());
				}
			}
		}
	}
	
	public static void sendEmailNotification(EmailSetting emailSetting, 
			TalkerBean talker, Map<String, String> vars) {
		//TODO: make "StopFollowing" page for conversation emails?
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

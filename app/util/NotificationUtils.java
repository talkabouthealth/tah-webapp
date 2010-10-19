package util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import play.Logger;
import util.EmailUtil.EmailTemplate;

import models.TalkerBean;
import models.TalkerBean.EmailSetting;

import com.sun.corba.se.impl.encoding.OSFCodeSetRegistry.Entry;
import com.tah.im.IMNotifier;
import com.tah.im.model.UserInfo;
import com.tah.im.singleton.OnlineUsersSingleton;

import dao.ConfigDAO;
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
	public static void sendAutomaticNotifications(String topicId) {
		boolean automaticNotification = ConfigDAO.getBooleanConfig(AUTOMATIC_NOTIFICATIONS_CONFIG);
		if (!automaticNotification) {
			return;
		}
		
		OnlineUsersSingleton onlineUsersSingleton = OnlineUsersSingleton.getInstance();
		//TODO: find 10 latest people who came online?
		Map<String, UserInfo> onlineUsers = onlineUsersSingleton.getOnlineUserMap();
		
		Set<String> talkersForNotification = new HashSet<String>();
		
		//With automatic notification, let's have support@talkabouthealth.com receive all notifications via IM
//		Map<String, String> vars = new HashMap<String, String>();
//		vars.put("name", session.get("username") == null ? "" : session.get("username"));
//		vars.put("email", email);
//		vars.put("subject", subject);
//		vars.put("message", message);
//		EmailUtil.sendEmail(EmailTemplate.CONTACTUS, EmailUtil.SUPPORT_EMAIL, vars, null, false);
		TalkerBean murrayTalker = TalkerDAO.getByEmail(EmailUtil.MURRAY_EMAIL);
		if (murrayTalker == null) {
			Logger.error("Can't send automatic notification to Murray!");
		}
		else {
			talkersForNotification.add(murrayTalker.getId());
		}
		
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
				imNotifier.broadcast(uidArray, topicId);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void sendEmailNotification(EmailSetting emailSetting, 
			TalkerBean talker, Map<String, String> vars) {
		//TODO: make "StopFollowing" page for conversation emails?
		if (talker.loadEmailSettings().contains(emailSetting)) {
			vars.put("username", talker.getUserName());
			EmailUtil.sendEmail(emailSetting.getEmailTemplate(), talker.getEmail(), vars, null, true);
		}
	}

}

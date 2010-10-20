package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import models.TalkerBean;
import models.TalkerBean.ProfilePreference;

import play.mvc.Controller;
import play.mvc.With;
import util.NotificationUtils;

import com.tah.im.IMNotifier;
import com.tah.im.singleton.OnlineUsersSingleton;

import dao.ConfigDAO;
import dao.TalkerDAO;
import dao.ConversationDAO;

@Check("admin")
@With(Secure.class)
public class Dashboard extends Controller {
	
	public static void index() {
		//TODO: use normal beans (not maps)
		List<Map<String, String>> topicsList = ConversationDAO.loadConvosForDashboard(false);
		List<Map<String, String>> topicsWithNotificationsList = ConversationDAO.loadConvosForDashboard(true);
		List<TalkerBean> talkersList = TalkerDAO.loadTalkersForDashboard();
		
		String lastTopicId = ConversationDAO.getLastConvoId(); 
		OnlineUsersSingleton onlineUsersSingleton = OnlineUsersSingleton.getInstance();
		boolean automaticNotification = ConfigDAO.getBooleanConfig(NotificationUtils.AUTOMATIC_NOTIFICATIONS_CONFIG);
		
		render(topicsList, topicsWithNotificationsList, talkersList, lastTopicId, onlineUsersSingleton, automaticNotification);
	}
		
	public static void notification(String[] uidArray, String topicId, String topic) {
		IMNotifier imNotifier = IMNotifier.getInstance();
		try {
			imNotifier.broadcast(uidArray, topicId, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		renderText("ok");
	}
	
	public static void checkNewTopic(String oldLastTopic) {
		String lastTopic = ConversationDAO.getLastConvoId();
		
		boolean isNewTopic = false;
		if (lastTopic != null) {
			isNewTopic = !lastTopic.equals(oldLastTopic);
		}
		renderText(Boolean.toString(isNewTopic));
	}
	
	public static void setAutomaticNotification(boolean newValue) {
		ConfigDAO.saveConfig(NotificationUtils.AUTOMATIC_NOTIFICATIONS_CONFIG, newValue);
	}
	
	
	// --------------- Manage accounts -----------------------
	public static void manageAccounts() {
		List<TalkerBean> talkers = TalkerDAO.loadAllTalkers();
		
		render(talkers);
	}
	
	public static void saveAccounts(List<String> selectedTalkerIds) {
		List<TalkerBean> talkers = TalkerDAO.loadAllTalkers();
		
		selectedTalkerIds = (selectedTalkerIds == null ? Collections.EMPTY_LIST : selectedTalkerIds);
		for (TalkerBean talker : talkers) {
			if (selectedTalkerIds.contains(talker.getId())) {
				talker.setSuspended(true);
			}
			else {
				talker.setSuspended(false);
			}
			TalkerDAO.updateTalker(talker);
		}
		
		manageAccounts();
	}
	
	
	// --------------- Verify Professionals ------------------

	public static void verifyProfessionals() {
		List<TalkerBean> talkers = loadProfessionalTalkers();
		render(talkers);
	}
	
	public static void saveVerifyProfessionals(List<String> selectedTalkerIds) {
		List<TalkerBean> talkers = loadProfessionalTalkers();
		
		if (selectedTalkerIds == null) {
			selectedTalkerIds = Collections.EMPTY_LIST;
		}
		for (TalkerBean talker : talkers) {
			if (selectedTalkerIds.contains(talker.getId())) {
				talker.setConnectionVerified(true);
			}
			else {
				talker.setConnectionVerified(false);
			}
			TalkerDAO.updateTalker(talker);
		}
		
		verifyProfessionals();
	}
	
	private static List<TalkerBean> loadProfessionalTalkers() {
		List<TalkerBean> talkerList = TalkerDAO.loadAllTalkers();
		
		List<TalkerBean> professionalTalkers = new ArrayList<TalkerBean>();
		for (TalkerBean talker : talkerList) {
			if (TalkerBean.PROFESSIONAL_CONNECTIONS_LIST.contains(talker.getConnection())) {
				professionalTalkers.add(talker);
			}
		}
		return professionalTalkers;
	}
}

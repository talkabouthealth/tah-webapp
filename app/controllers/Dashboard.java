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
import dao.TopicDAO;

@Check("admin")
@With(Secure.class)
public class Dashboard extends Controller {
	
	public static void index() {
		//TODO: use normal beans (not maps)
		List<Map<String, String>> topicsList = TopicDAO.loadTopicsForDashboard(false);
		List<Map<String, String>> topicsWithNotificationsList = TopicDAO.loadTopicsForDashboard(true);
		List<TalkerBean> talkersList = TalkerDAO.loadTalkersForDashboard();
		
		String lastTopicId = TopicDAO.getLastTopicId(); 
		OnlineUsersSingleton onlineUsersSingleton = OnlineUsersSingleton.getInstance();
		boolean automaticNotification = ConfigDAO.getBooleanConfig(NotificationUtils.AUTOMATIC_NOTIFICATIONS_CONFIG);
		
		render(topicsList, topicsWithNotificationsList, talkersList, lastTopicId, onlineUsersSingleton, automaticNotification);
	}
		
	//TODO: move to Notification Utils?
	public static void notification(String[] uidArray, String topicId, String topic) {
		IMNotifier imNotifier = IMNotifier.getInstance();
		try {
			imNotifier.broadcast(uidArray, topicId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		renderText("ok");
	}
	
	public static void checkNewTopic(String oldLastTopic) {
		String lastTopic = TopicDAO.getLastTopicId();
		
		boolean isNewTopic = false;
		if (lastTopic != null) {
			isNewTopic = !lastTopic.equals(oldLastTopic);
		}
		renderText(Boolean.toString(isNewTopic));
	}
	
	public static void setAutomaticNotification(boolean newValue) {
		ConfigDAO.saveConfig(NotificationUtils.AUTOMATIC_NOTIFICATIONS_CONFIG, newValue);
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

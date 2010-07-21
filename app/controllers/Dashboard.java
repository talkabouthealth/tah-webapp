package controllers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import play.mvc.Controller;
import play.mvc.With;

import com.tah.im.IMNotifier;
import com.tah.im.userInfo;
import com.tah.im.singleton.googleSingleton;
import com.tah.im.singleton.msnSingleton;
import com.tah.im.singleton.onlineUsersSingleton;
import com.tah.im.singleton.yahooSingleton;

import dao.TalkerDAO;
import dao.TopicDAO;

@Check("admin")
@With(Secure.class)
public class Dashboard extends Controller {
	
	public static void index() {
//		String sqlStatement = 
//		"SELECT DISTINCT topics.topic_id, topics.*, noti_history.noti_time, talkers.* 
//		FROM topics LEFT JOIN noti_history ON topics.topic_id = noti_history.topic_id 
//		LEFT JOIN talkers ON topics.uid = talkers.uid 
//		WHERE noti_history.noti_time is null ORDER BY topics.creation_date";
		List<Map<String, String>> topicsList = TopicDAO.loadTopicsForDashboard(false);
		
		//String sqlStatement3 = 
		//	"SELECT DISTINCT topics.*, noti_history.noti_time, talkers.* FROM topics 
		//	RIGHT JOIN noti_history ON topics.topic_id = noti_history.topic_id 
		//	LEFT JOIN talkers ON topics.uid = talkers.uid 
		//	WHERE noti_history.noti_time is not null 
		//	GROUP BY topics.topic_id ORDER BY topics.creation_date";									
		List<Map<String, String>> topicsWithNotificationsList = TopicDAO.loadTopicsForDashboard(true);
		
		//String sqlStatement2 = "SELECT talkers.*, MAX(noti_history.noti_time) 
		//	FROM talkers LEFT JOIN noti_history ON talkers.uid = noti_history.uid 
		//	GROUP BY talkers.uid ORDER BY MAX(noti_history.noti_time)";
		List<Map<String, String>> talkersList = TalkerDAO.loadTalkersForDashboard();
		
		//FIXME!!!
		IMNotifier imNotifier = new IMNotifier();
//		IMNotifier imNotifier = null;
		String lastTopicId = TopicDAO.getLastTopicId(); 
		
		render(topicsList, topicsWithNotificationsList, talkersList, imNotifier, lastTopicId);
	}
	
	public static void userlist() {
		//String sqlStatement = "SELECT DISTINCT topics.*, noti_history.noti_time, talkers.* 
		//FROM topics LEFT JOIN noti_history ON topics.topic_id = noti_history.topic_id 
		//LEFT JOIN talkers ON topics.uid = talkers.uid 
		//WHERE noti_history.noti_time is null ORDER BY topics.creation_date";
		List<Map<String, String>> topicsList = TopicDAO.loadTopicsForDashboard(false);
		
		//String sqlStatement3 = "SELECT DISTINCT topics.*, noti_history.noti_time, talkers.* 
		//FROM topics RIGHT JOIN noti_history ON topics.topic_id = noti_history.topic_id 
		//LEFT JOIN talkers ON topics.uid = talkers.uid WHERE noti_history.noti_time is not null 
		//GROUP BY topics.topic_id ORDER BY topics.creation_date";
		List<Map<String, String>> topicsWithNotificationsList = TopicDAO.loadTopicsForDashboard(true);
		
		onlineUsersSingleton onlineUserInfo = onlineUsersSingleton.getInstance();
		Map<String, userInfo> onlineUserMap = onlineUserInfo.getOnlineUserMap();
		
		render(topicsList, topicsWithNotificationsList, onlineUserMap);
	}
	
	public static void sendInvite(String imService, String imUsername) {
		if (imService != null && imUsername != null) {
			//select needed singleton
			//TODO: we can make one interface "singleton" and create/get it with Factory pattern
			//TODO: for some services imUsername should be full email?
			try {
				if (imService.equals("GoogleTalk")) {
					//TODO: code conventions?
					googleSingleton _googleSingleton = googleSingleton.getInstance();
					System.out.println("Add google contact!");
					_googleSingleton.addContact(imUsername);
				}
				else if (imService.equals("YahooIM")) {
					yahooSingleton _yahooSingleton = yahooSingleton.getInstance();
					System.out.println("Add yahoo contact!");
					_yahooSingleton.addContact(imUsername);					
				}
				else if (imService.equals("WindowsLive")) {
					msnSingleton _msnSingleton = msnSingleton.getInstance();
					System.out.println("Add msn contact!");
					_msnSingleton.addContact(imUsername);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void notification(String imService, String[] uidArray, String[] emailArray, String convTid) {
//		System.out.println(request.getParameter("convTid").toString());
//		System.out.println(request.getParameter("convOwner").toString());
//		System.out.println(request.getParameter("convTopic").toString());
//		String _uId[] = request.getParameterValues("userId"); 
//		String _uMail [] = request.getParameterValues("userEmail"); 
//		String _tId = request.getParameter("convTid");
		
		//TODO: temporarily for now
		imService = "GoogleTalk";
		
		try {
			if (imService.equals("GoogleTalk")) {
				//TODO: code conventions?
				googleSingleton _googleSingleton = googleSingleton.getInstance();
				_googleSingleton.Broadcast(emailArray, uidArray, convTid);
			}
			else if (imService.equals("YahooIM")) {
				yahooSingleton _yahooSingleton = yahooSingleton.getInstance();
				_yahooSingleton.Broadcast(emailArray, uidArray, convTid);
			}
			else if (imService.equals("WindowsLive")) {
				msnSingleton _msnSingleton = msnSingleton.getInstance();
				_msnSingleton.Broadcast(emailArray, uidArray, convTid);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
//		index();
		renderText("ok!");
	}
	
	public static void checkNewTopic(String oldLastTopic) {
		String lastTopic = TopicDAO.getLastTopicId();
		
		boolean isNewTopic = false;
		if (lastTopic != null) {
			isNewTopic = !lastTopic.equals(oldLastTopic);
		}
		renderText(Boolean.toString(isNewTopic));
	}
}

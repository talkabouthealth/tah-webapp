package controllers;

import java.util.List;
import java.util.Map;

import play.mvc.Controller;
import play.mvc.With;

import com.tah.im.IMNotifier;
import com.tah.im.singleton.OnlineUsersSingleton;

import dao.TalkerDAO;
import dao.TopicDAO;

@Check("admin")
@With(Secure.class)
public class Dashboard extends Controller {
	
	public static void index() {
		//TODO: fix service counter
		
		
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
		
		String lastTopicId = TopicDAO.getLastTopicId(); 
		
		OnlineUsersSingleton onlineUsersSingleton = OnlineUsersSingleton.getInstance();
		
		render(topicsList, topicsWithNotificationsList, talkersList, lastTopicId, onlineUsersSingleton);
	}
		
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
}

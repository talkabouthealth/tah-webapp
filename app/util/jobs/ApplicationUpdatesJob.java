package util.jobs;

import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import models.IMAccountBean;
import models.TalkerBean;
import models.ConversationBean;
import models.TalkerBean.ProfilePreference;
import models.TopicBean;

import dao.ApplicationDAO;
import dao.DiseaseDAO;
import dao.HealthItemDAO;
import dao.TalkerDAO;
import dao.ConversationDAO;
import dao.TopicDAO;

import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import util.DBUtil;
import util.importers.DiseaseImporter;
import util.importers.HealthItemsImporter;
import util.importers.TopicsImporter;

/**
 * Job is used for different updates in db when deploying new features.
 */
@OnApplicationStart
public class ApplicationUpdatesJob extends Job {
	
	public void doJob() throws Exception {
		
		if (ApplicationDAO.isCollectionEmpty(DiseaseDAO.DISEASES_COLLECTION)) {
			DiseaseImporter.importDiseases("diseases.dat");
		}
		
		if (ApplicationDAO.isCollectionEmpty(HealthItemDAO.HEALTH_ITEMS_COLLECTION)) {
			HealthItemsImporter.importHealthItems("healthitems.dat");
		}
		
		//Talkers/Topics/Convos should have different names, stored in 'names' collection
		if (ApplicationDAO.isCollectionEmpty(ApplicationDAO.NAMES_COLLECTION)) {
			for (TalkerBean talker : TalkerDAO.loadAllTalkers()) {
				ApplicationDAO.createURLName(talker.getUserName());
			}
			
			for (TopicBean topic : TopicDAO.loadAllTopics()) {
				String urlName = ApplicationDAO.createURLName(topic.getTitle());
				topic.setMainURL(urlName);
				TopicDAO.updateTopic(topic);
			}
			
			for (ConversationBean convo : ConversationDAO.loadAllConversations()) {
				String urlName = ApplicationDAO.createURLName(convo.getTopic());
				convo.setMainURL(urlName);
				ConversationDAO.updateConvo(convo);
			}
		}
		
		if (ApplicationDAO.isCollectionEmpty(TopicDAO.TOPICS_COLLECTION)) {
			TopicsImporter.importTopics("topics.dat");
		}
		
    }
}

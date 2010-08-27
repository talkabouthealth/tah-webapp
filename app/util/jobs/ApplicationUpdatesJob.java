package util.jobs;

import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import models.IMAccountBean;
import models.TalkerBean;
import models.TopicBean;
import models.TalkerBean.ProfilePreference;

import dao.ApplicationDAO;
import dao.TalkerDAO;
import dao.TopicDAO;

import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

/**
 * Job is used for different updates in db when deploying new features.
 */
@OnApplicationStart
public class ApplicationUpdatesJob extends Job {
	
	public void doJob() {
		//update only if "names" is empty
		if (!ApplicationDAO.isURLNameExists("kangaroo")) {
			//check all talkers/topics new names 
			for (TalkerBean talker : TalkerDAO.loadAllTalkers()) {
				String name = ApplicationDAO.createURLName(talker.getUserName());
				if (!talker.getUserName().equals(name)) {
					System.err.println("Bad username: "+talker.getUserName());
				}
			}
			
			for (TopicBean topic : TopicDAO.loadAllTopics()) {
				String name = ApplicationDAO.createURLName(topic.getTopic());
				
				topic.setMainURL(name);
				TopicDAO.updateTopic(topic);
			}
		}
    }
	
}

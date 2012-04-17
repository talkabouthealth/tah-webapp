package util.jobs;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

import org.eclipse.jdt.internal.codeassist.ThrownExceptionFinder;

import logic.TalkerLogic;
import models.ConversationBean;
import models.TalkerBean;
import models.TopicBean;
import models.TalkerBean.EmailSetting;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import util.BitlyUtil;
import util.CommonUtil;
import util.DBUtil;
import util.importers.DiseaseImporter;
import util.importers.FieldsDataImporter;
import util.importers.HealthItems2Topics;
import util.importers.HealthItemsImporter;
import util.importers.HealthItemsUpdater;
import util.importers.TopicsImporter;
import dao.ApplicationDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;
import dao.TopicDAO;

/**
 * Job for preparing db and app for work.
 * Also is used for different updates in db when deploying new features.
 * 
 */
public class ApplicationUpdatesJob {
	
	public static void main(String[] args) throws Throwable {
		/**
		 * For deploy
		 * 
		 */
		System.out.println("ApplicationUpdatesJob Started::::"+ new Date());
		System.out.println("importing data..");
		//Fields data for Edit Profile
		FieldsDataImporter.importData("fields.dat");
		
		//HealthItems -> Topics mapping (for recommendations)
		HealthItems2Topics.importData("healthitems2topics.dat");
		
		//Create collections if missing
		//if (DBUtil.isCollectionEmpty(DiseaseDAO.DISEASES_COLLECTION)) {
			DiseaseImporter.importDiseases("diseases.dat");
		//}
		//commented if because every time it updates the record in table.
		//if (DBUtil.isCollectionEmpty(HealthItemDAO.HEALTH_ITEMS_COLLECTION)) {
			HealthItemsImporter.importHealthItems("healthitems1.dat");
		//}
		if (DBUtil.isCollectionEmpty(TopicDAO.TOPICS_COLLECTION)) {
			TopicsImporter.importTopics("topics.dat");
		}
		
		//Talkers/Topics/Convos should have different names, stored in 'names' collection
		if (DBUtil.isCollectionEmpty(ApplicationDAO.NAMES_COLLECTION)) {
			
			List<TalkerBean> talkersList = TalkerDAO.loadAllTalkers(true);
			for (TalkerBean talker : talkersList) {
				ApplicationDAO.createURLName(talker.getUserName(), true);
			}
			talkersList.clear();
			
			Set<TopicBean> topicList = TopicDAO.loadAllTopics(true);
			for (TopicBean topic : topicList) {
				String urlName = ApplicationDAO.createURLName(topic.getTitle());
				topic.setMainURL(urlName);
				TopicDAO.updateTopicbyId(topic,"main_url",topic.getMainURL());
			}
			topicList.clear();
			
			List <ConversationBean> convolist=ConversationDAO.getAllConvosForScheduler();
			for (ConversationBean convo : convolist) {
				String urlName = ApplicationDAO.createURLName(convo.getTopic());
				convo.setMainURL(urlName);
				ConversationDAO.updateConvoForScheduler(convo,"main_url",convo.getMainURL());
			}
			convolist.clear();
		}
		
		addAdminUser();
		
		// Updates for different items
		HealthItemsUpdater.updateHealthItems("healthitemsupd.dat");
		System.out.println("ApplicationUpdatesJob Completed::::"+ new Date());
//		createBitlyLinks();
		ApplicationUpdatesJob appUpdateJob = new ApplicationUpdatesJob();
		appUpdateJob.finalize();
    }

	/**
	 * Add admin user if missing
	 */
	private static void addAdminUser() {
		TalkerBean admin = TalkerDAO.getByFieldBasicInfo("uname", "admin");
		if (admin == null) {
			admin = new TalkerBean();
			admin.setUserName("admin");
			admin.setAnonymousName("member000");
			admin.setPassword("admin");
			admin.setDob(new Date());
			admin.setEmail("admin@talkabouthealth.com");
			
			String hashedPassword = CommonUtil.hashPassword(admin.getPassword());
			admin.setPassword(hashedPassword);
			
			admin.setPrivacySettings(TalkerLogic.getDefaultPrivacySettings());
	        
	        //By default all email notifications are checked
	        EnumSet<EmailSetting> emailSettings = EnumSet.allOf(EmailSetting.class);
	        admin.setEmailSettings(emailSettings);
			
			TalkerDAO.save(admin);
		}
	}

	/**
	 * Create BitLy links for convos, chat and topics
	 */
	private static void createBitlyLinks() {
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				try {
					//timeout between API requests
					final int BITLY_TIMEOUT = 60*1000;
					for (ConversationBean convo : ConversationDAO.getAllConvosForScheduler()) {
						if (convo.getBitly() == null || convo.getBitly().equals("RATE_LIMIT_EXCEEDE")) {
							String convoURL = "http://talkabouthealth.com/"+convo.getMainURL();
							convo.setBitly(BitlyUtil.shortLink(convoURL));
							Thread.sleep(BITLY_TIMEOUT);
							
							ConversationDAO.updateConvoForScheduler(convo,"bitly", convo.getBitly());
						}
						if (convo.getBitlyChat() == null || convo.getBitlyChat().equals("RATE_LIMIT_EXCEEDE")) {
							String convoChatURL = "http://talkabouthealth.com/chat/"+convo.getTid();
							convo.setBitlyChat(BitlyUtil.shortLink(convoChatURL));
							Thread.sleep(BITLY_TIMEOUT);
							
							ConversationDAO.updateConvoForScheduler(convo,"bitly_chat", convo.getBitlyChat());
						}
					}
					
					for (TopicBean topic : TopicDAO.loadAllTopics(true)) {
						if (topic.getBitly() == null || topic.getBitly().equals("RATE_LIMIT_EXCEEDE")) {
							String topicURL = "http://talkabouthealth.com/"+topic.getMainURL();
							topic.setBitly(BitlyUtil.shortLink(topicURL));
							Thread.sleep(BITLY_TIMEOUT);
							
							TopicDAO.updateTopicbyId(topic,"bitly",topic.getBitly());
						}
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	protected void finalize() throws Throwable {
		super.finalize();
	}
}

package util.jobs;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import models.CommentBean;
import models.IMAccountBean;
import models.ServiceAccountBean;
import models.TalkerBean;
import models.ConversationBean;
import models.ServiceAccountBean.ServiceType;
import models.TalkerBean.EmailSetting;
import models.TopicBean;

import dao.ApplicationDAO;
import dao.CommentsDAO;
import dao.DiseaseDAO;
import dao.HealthItemDAO;
import dao.TalkerDAO;
import dao.ConversationDAO;
import dao.TopicDAO;

import play.Logger;
import play.Play;
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
import util.oauth.TwitterOAuthProvider;

/**
 * Job for preparing db and app for work.
 * Also is used for different updates in db when deploying new features.
 * 
 * //TODO: iptables + other admin stuff
 * 
 */
@OnApplicationStart
public class ApplicationUpdatesJob extends Job {
	
	public void doJob() throws Exception {
		
		/*
		 *  3. Twitter settings
		 *  4. Change profilepreference everywhere.
		 */
		
//			2011-01-21 08:35:35,279 INFO  ~ ThorAssociates: http://www.facebook.com/profile.php?id=113344035364476
//		ThorAssociates - not for sure, will check with her
			
		//Relation between old and correct ids;
		Map<String, String> fbMap = new HashMap<String, String>();
		fbMap.put("139572626057500", "669580190");
		fbMap.put("105526899479797", "758924921");
		fbMap.put("104880066216147", "639613153");
		fbMap.put("111563985540297", "1603500081");
		fbMap.put("112111905481230", "934807");
		fbMap.put("108424279189115", "542029051");
		
		for (TalkerBean talker : TalkerDAO.loadAllTalkers()) {
			if (talker.getUserName().equals("rkalra")) {
				talker.setUserName("Bentia");
				TalkerDAO.updateTalker(talker);
			}
			
			ServiceAccountBean fbAccount = talker.serviceAccountByType(ServiceType.FACEBOOK);
			if (fbAccount != null) {
				String correctId = fbMap.get(fbAccount.getId());
				if (correctId != null) {
					fbAccount.setId(correctId);
					TalkerDAO.updateTalker(talker);
				}
			}
		}
		
		//Fields data for Edit Profile
		FieldsDataImporter.importData("fields.dat");
		
		//HealthItems -> Topics mapping (for recommendations)
		HealthItems2Topics.importData("healthitems2topics.dat");
		
		//Create collections if missing
		if (ApplicationDAO.isCollectionEmpty(DiseaseDAO.DISEASES_COLLECTION)) {
			DiseaseImporter.importDiseases("diseases.dat");
		}
		if (ApplicationDAO.isCollectionEmpty(HealthItemDAO.HEALTH_ITEMS_COLLECTION)) {
			HealthItemsImporter.importHealthItems("healthitems.dat");
		}
		if (ApplicationDAO.isCollectionEmpty(TopicDAO.TOPICS_COLLECTION)) {
			TopicsImporter.importTopics("topics.dat");
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
		
		addAdminUser();
		
		
		// Updates for different items
		HealthItemsUpdater.updateHealthItems("healthitemsupd.dat");
		
		//update topic from "," to "/"
		for (TopicBean topic : TopicDAO.loadAllTopics()) {
			String topicTitle = topic.getTitle();
			if (topicTitle.contains(",")) {
				topicTitle = topicTitle.replaceAll(", ", "/");
				topicTitle = topicTitle.replaceAll(",", "/");
				topic.setTitle(topicTitle);
			}
			TopicDAO.updateTopic(topic);
		}
		
//		createBitlyLinks();
    }

	/**
	 * Add admin user if missing
	 */
	private void addAdminUser() {
		TalkerBean admin = TalkerDAO.getByUserName("admin");
		if (admin == null) {
			admin = new TalkerBean();
			admin.setUserName("admin");
			admin.setPassword("admin");
			admin.setDob(new Date());
			admin.setEmail("admin@talkabouthealth.com");
			
			String hashedPassword = CommonUtil.hashPassword(admin.getPassword());
			admin.setPassword(hashedPassword);
			
			admin.setPrivacySettings(CommonUtil.getDefaultPrivacySettings());
	        
	        //By default all email notifications are checked
	        EnumSet<EmailSetting> emailSettings = EnumSet.allOf(EmailSetting.class);
	        admin.setEmailSettings(emailSettings);
			
			TalkerDAO.save(admin);
		}
	}

	/**
	 * Create BitLy links for convos, chat and topics
	 */
	private void createBitlyLinks() {
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				try {
					//timeout between API requests
					final int BITLY_TIMEOUT = 60*1000;
					for (ConversationBean convo : ConversationDAO.loadAllConversations()) {
						if (convo.getBitly() == null || convo.getBitly().equals("RATE_LIMIT_EXCEEDE")) {
							//String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL());
							String convoURL = "http://talkabouthealth.com/"+convo.getMainURL();
							convo.setBitly(BitlyUtil.shortLink(convoURL));
							Thread.sleep(BITLY_TIMEOUT);
							
							ConversationDAO.updateConvo(convo);
						}
						if (convo.getBitlyChat() == null || convo.getBitlyChat().equals("RATE_LIMIT_EXCEEDE")) {
	//						String convoChatURL = CommonUtil.generateAbsoluteURL("Talk.talkApp", "convoId", convo.getTid());
							String convoChatURL = "http://talkabouthealth.com/chat/"+convo.getTid();
							convo.setBitlyChat(BitlyUtil.shortLink(convoChatURL));
							Thread.sleep(BITLY_TIMEOUT);
							
							ConversationDAO.updateConvo(convo);
						}
					}
					
					for (TopicBean topic : TopicDAO.loadAllTopics()) {
						if (topic.getBitly() == null || topic.getBitly().equals("RATE_LIMIT_EXCEEDE")) {
	//						String topicURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", topic.getMainURL());
							String topicURL = "http://talkabouthealth.com/"+topic.getMainURL();
							topic.setBitly(BitlyUtil.shortLink(topicURL));
							Thread.sleep(BITLY_TIMEOUT);
							
							TopicDAO.updateTopic(topic);
						}
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}

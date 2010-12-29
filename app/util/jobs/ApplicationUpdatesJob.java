package util.jobs;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
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
import models.TalkerBean.ProfilePreference;
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
 * Job is used for different updates in db when deploying new features.
 */
@OnApplicationStart
public class ApplicationUpdatesJob extends Job {
	
	public void doJob() throws Exception {
		/*
		 	Before deploy:
		 	1. Update health items
		 		
				Navelbine (vinorelbine)|Vinorelbine (Navelbine) - also move this to the medications section
				remove 'Symptoms'
				remove Doxil (doxorubicin)
				
		 		db.healthitems.find({ name : 'Navelbine (vinorelbine)'})
		 		db.healthitems.find({ _id : ObjectId("4cd7bb02c0f6b19baa7c415b")})
		 		db.healthitems.remove({ _id : ObjectId("4cd7bb02c0f6b19baa7c415b")})
		 		
		 		db.healthitems.remove({ name : 'Symptoms'})
		 		db.healthitems.remove({ name : 'Doxil (doxorubicin)'})
		 		
		 	2. Move Twitter/Facebook to the new format?
		 	
		 	3. Add bit.ly links to the old topics/convos
		 	
		 */
		
		//Fields data for Profiles
		FieldsDataImporter.importData("fields.dat");
		
		//HealthItems -> Topics mapping
		HealthItems2Topics.importData("healthitems2topics.dat");
		
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
		
		if (ApplicationDAO.isCollectionEmpty(DiseaseDAO.DISEASES_COLLECTION)) {
			DiseaseImporter.importDiseases("diseases.dat");
		}
		
		if (ApplicationDAO.isCollectionEmpty(HealthItemDAO.HEALTH_ITEMS_COLLECTION)) {
			HealthItemsImporter.importHealthItems("healthitems.dat");
		}
		
		//TODO: iptables + other admin stuff
		
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
		
		//add admin user if missing
		TalkerBean admin = TalkerDAO.getByUserName("admin");
		if (admin == null) {
			admin = new TalkerBean();
			admin.setUserName("admin");
			admin.setPassword("admin");
			admin.setDob(new Date());
			admin.setEmail("admin@talkabouthealth.com");
			
			String hashedPassword = CommonUtil.hashPassword(admin.getPassword());
			admin.setPassword(hashedPassword);
			
			EnumSet<ProfilePreference> defaultPreferences = 
	        	EnumSet.complementOf(
	        		EnumSet.of(
	    				ProfilePreference.PERSONAL_INFO,
	    				ProfilePreference.HEALTH_INFO
	        		)
	        	);
			admin.setProfilePreferences(defaultPreferences);
	        
	        //By default all email notifications are checked
	        EnumSet<EmailSetting> emailSettings = EnumSet.allOf(EmailSetting.class);
	        admin.setEmailSettings(emailSettings);
			
			TalkerDAO.save(admin);
		}
		
//		Executors.newSingleThreadExecutor().execute(new Runnable() {
//			@Override
//			public void run() {
//				try {
//					//timeout between API requests
//					final int BITLY_TIMEOUT = 60*1000;
//					for (ConversationBean convo : ConversationDAO.loadAllConversations()) {
//						if (convo.getBitly() == null || convo.getBitly().equals("RATE_LIMIT_EXCEEDE")) {
//							//String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL());
//							String convoURL = "http://talkabouthealth.com/"+convo.getMainURL();
//							convo.setBitly(BitlyUtil.shortLink(convoURL));
//							Thread.sleep(BITLY_TIMEOUT);
//							
//							ConversationDAO.updateConvo(convo);
//						}
//						if (convo.getBitlyChat() == null || convo.getBitlyChat().equals("RATE_LIMIT_EXCEEDE")) {
////							String convoChatURL = CommonUtil.generateAbsoluteURL("Talk.talkApp", "convoId", convo.getTid());
//							String convoChatURL = "http://talkabouthealth.com/chat/"+convo.getTid();
//							convo.setBitlyChat(BitlyUtil.shortLink(convoChatURL));
//							Thread.sleep(BITLY_TIMEOUT);
//							
//							ConversationDAO.updateConvo(convo);
//						}
//					}
//					
//					for (TopicBean topic : TopicDAO.loadAllTopics()) {
//						if (topic.getBitly() == null || topic.getBitly().equals("RATE_LIMIT_EXCEEDE")) {
////							String topicURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", topic.getMainURL());
//							String topicURL = "http://talkabouthealth.com/"+topic.getMainURL();
//							topic.setBitly(BitlyUtil.shortLink(topicURL));
//							Thread.sleep(BITLY_TIMEOUT);
//							
//							TopicDAO.updateTopic(topic);
//						}
//					}
//				}
//				catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		});
		
		//create indexes
//		activities
//		configs
//		convocomments
//		convos
//		diseases
//		healthitems
//		logins
//		names
//		notifications
//		profilecomments
//		system.indexes
//		talkers
//		topics
		
    }
}

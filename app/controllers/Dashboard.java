package controllers;

import static util.DBUtil.getCollection;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import logic.TalkerLogic;
import models.EmailListBean;
import models.NewsLetterBean;
import models.TalkerBean;
import models.TopicBean;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;
import util.EmailUtil;
import util.NotificationUtils;
import util.SearchUtil;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.sailthru.TriggerMailClient;
import com.tah.im.IMNotifier;
import com.tah.im.singleton.OnlineUsersSingleton;

import dao.ConfigDAO;
import dao.TalkerDAO;
import dao.ConversationDAO;

/**
 * Admin Dashboard
 *
 */
@Check("admin")
@With(Secure.class)
public class Dashboard extends Controller {
	
	public static void index() {
		//from previous code - we need to rework it to use normal beans (not maps)
		List<Map<String, String>> topicsList = ConversationDAO.loadConvosForDashboard(false);
		List<Map<String, String>> topicsWithNotificationsList = ConversationDAO.loadConvosForDashboard(true);
		List<TalkerBean> talkersList = TalkerDAO.loadTalkersForDashboard();
		
		String lastTopicId = ConversationDAO.getLastConvoId(); 
		OnlineUsersSingleton onlineUsersSingleton = OnlineUsersSingleton.getInstance();
		boolean automaticNotification = ConfigDAO.getBooleanConfig(NotificationUtils.AUTOMATIC_NOTIFICATIONS_CONFIG);
		
		render(topicsList, topicsWithNotificationsList, talkersList, lastTopicId, onlineUsersSingleton, automaticNotification);
	}
		
	/**
	 * Send notification to given users
	 * @param uidArray array of ids of users to notify
	 */
	public static void notification(String[] uidArray, String convoId, String topic) {
		IMNotifier imNotifier = IMNotifier.getInstance();
		try {
			imNotifier.broadcast(uidArray, convoId, null);
		} catch (Exception e) {
			Logger.error(e,"Dashboard.java : notification ");
		}
		
		renderText("ok");
	}
	
	/**
	 * Check if new conversation/live chat was created
	 * @param oldLastTopic id of last convo known to the Admin
	 */
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
		List<TalkerBean> talkers =  TalkerDAO.loadAllTalkers(); //TalkerLogic.loadAllTalkersFromCache();
		render(talkers);
	}
	public static void saveAccounts(List<String> selectedTalkerIds) {
		List<TalkerBean> talkers = TalkerDAO.loadAllTalkers();
		
		selectedTalkerIds = (selectedTalkerIds == null ? Collections.EMPTY_LIST : selectedTalkerIds);
		for (TalkerBean talker : talkers) {
			if (selectedTalkerIds.contains(talker.getId())) {
				talker.setSuspended(true);
				try{
					deleteTalkerIndex(talker.getId());
				}catch (Exception e) {
					Logger.error(e,"Dashboard.java : saveAccounts");
				}
			}
			else {
				talker.setSuspended(false);
			}
			try{
				TalkerDAO.updateTalker(talker);	
			}catch (Exception e) {
				Logger.error(e,"Dashboard.java : saveAccounts");
			}
			
		}
		manageAccounts();
	}
	
	public static void updateTalkerImage() {
		TalkerDAO.updateTalkerForImage();
		TalkerDAO.updateTalkerForAnswerCount();
		renderText("OK");
	}
	
	/**
	 * delete the talker foe Search Incex
	 * @param talkerId
	 * @throws Exception
	 */
	
	@SuppressWarnings("deprecation")
	private static void deleteTalkerIndex(String talkerId)throws Exception{
		
		File autoCompleteIndexerFile = new File(SearchUtil.SEARCH_INDEX_PATH+"autocomplete");
 		Directory autoCompleteIndexDir = FSDirectory.open(autoCompleteIndexerFile);
 		
 		File talkerIndexerFile = new File(SearchUtil.SEARCH_INDEX_PATH+"talker");
 		Directory talkerIndexDir = FSDirectory.open(talkerIndexerFile);
 		
    	IndexReader autocompletetalkerIndexReader = IndexReader.open(autoCompleteIndexDir);
    	IndexReader talkerIndexReader = IndexReader.open(talkerIndexDir);
    	
    	Term term = new Term("id",talkerId);
    	try{
			talkerIndexReader.deleteDocuments(term);
			autocompletetalkerIndexReader.deleteDocuments(term);
			talkerIndexReader.flush();
			autocompletetalkerIndexReader.flush();
			talkerIndexReader.close();
			autocompletetalkerIndexReader.close();
			
		}catch(Exception e){
			Logger.error(e,"Dashboard.java : deleteTalkerIndex");
		}
		talkerIndexReader.close();
		autocompletetalkerIndexReader.close();
		talkerIndexDir.close();
		autoCompleteIndexDir.close();
		
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
		List<TalkerBean> talkerList = TalkerDAO.loadAllTalkers(); //TalkerLogic.loadAllTalkersFromCache();
		
		List<TalkerBean> professionalTalkers = new ArrayList<TalkerBean>();
		for (TalkerBean talker : talkerList) {
			if (talker.isProf()) {
				professionalTalkers.add(talker);
			}
		}
		return professionalTalkers;
	}
	
	/**
	 * send Email To SailThrou
	 */
	public static void sendEmailToSailThrou(){
		/*
			- TAH Newsletter
			- TAH Member Verified Emails
			- TAH Member Unverified Emails
			- Patients - not done
			- Experts - not done
			- Organizations - not done
		 * */
		String NEWSLETTER_COLLECTION = "newsletter";
		DBCollection newsLetterCol = getCollection(NEWSLETTER_COLLECTION);
		List<DBObject> newsletterDBList = null;
		newsletterDBList = newsLetterCol.find().toArray();
		ArrayList<EmailListBean> newsLetterList = new ArrayList<EmailListBean>();
		EmailListBean  emailListBean;
		NewsLetterBean newsLetterBean;
		for (DBObject newsletterDBObject : newsletterDBList) {
			newsLetterBean = new NewsLetterBean();
			newsLetterBean.parseBasicFromDB(newsletterDBObject);
			emailListBean = new EmailListBean("TAH-Newsletter",newsLetterBean.getEmail());
			newsLetterList.add(emailListBean);
		}
		
		List<String> pationtList = Arrays.asList("Just Diagnosed","Current Patient","Former Patients");
		List<String> expertList = TalkerBean.PROFESSIONAL_CONNECTIONS_LIST;
		List<String> orgList = TalkerBean.ORGANIZATIONS_CONNECTIONS_LIST;
		
		List<TalkerBean> talkerBeans = TalkerDAO.loadAllTalkers();
		for (Iterator<TalkerBean> iterator = talkerBeans.iterator(); iterator.hasNext();) {
			TalkerBean talkerBean = (TalkerBean) iterator.next();
			if (talkerBean.getVerifyCode() != null) {
				emailListBean = new EmailListBean("TAH-Member-Unverified-Emails",talkerBean.getEmail());
			} else {
				emailListBean = new EmailListBean("TAH-Member-Verified-Emails",talkerBean.getEmail());
			}
			newsLetterList.add(emailListBean);
			
			if(pationtList.contains(talkerBean.getConnection())){
				emailListBean = new EmailListBean("Patients",talkerBean.getEmail());
				newsLetterList.add(emailListBean);
			}
			
			if(expertList.contains(talkerBean.getConnection())){
				emailListBean = new EmailListBean("Experts",talkerBean.getEmail());
				newsLetterList.add(emailListBean);
			}
			
			if(orgList.contains(talkerBean.getConnection())){
				emailListBean = new EmailListBean("Organizations",talkerBean.getEmail());
				newsLetterList.add(emailListBean);
			}
		}

		EmailUtil.setEmail(newsLetterList);
		index();
	}
}
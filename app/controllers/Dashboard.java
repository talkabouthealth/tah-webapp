package controllers;

import static util.DBUtil.getCollection;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import logic.TalkerLogic;
import models.DiseaseBean;
import models.EmailListBean;
import models.NewsLetterBean;
import models.TalkerBean;
import models.TopicBean;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import play.Logger;
import play.cache.Cache;
import play.modules.paginate.ValuePaginator;
import play.mvc.Controller;
import play.mvc.With;
import util.DiseaseChangeUtil;
import util.EmailUtil;
import util.NotificationUtils;
import util.SearchUtil;
import util.importers.DiseaseImporter;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.sailthru.TriggerMailClient;
import com.tah.im.IMNotifier;
import com.tah.im.singleton.OnlineUsersSingleton;

import dao.ActivityLogDAO;
import dao.ConfigDAO;
import dao.DiseaseDAO;
import dao.TalkerDAO;
import dao.ConversationDAO;
import dao.TopicDAO;

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
		TalkerBean newTalker = null;
		selectedTalkerIds = (selectedTalkerIds == null ? Collections.EMPTY_LIST : selectedTalkerIds);
		for (TalkerBean talker : talkers) {
			if (selectedTalkerIds.contains(talker.getId())) {
				talker.setSuspended(true);
				try {
					deleteTalkerIndex(talker.getId());
					TalkerDAO.updateTalker(talker);	
				}catch (Exception e) {
					Logger.error(e,"Dashboard.java : saveAccounts");
				}
			} else {
				//Need to add code here
				newTalker = TalkerDAO.getByEmailNotSuspended(talker.getEmail());
				if(newTalker == null) {
					talker.setSuspended(false);
					try{
						TalkerDAO.updateTalker(talker);	
					}catch (Exception e) {
						Logger.error(e,"Dashboard.java : saveAccounts");
					}
				} else {
					System.out.println("There is a account with this email");
				}
			}
		}
		manageAccounts();
	}
	
	public static void updateTalkerImage() {
		TalkerDAO.updateTalkerForImage();
		//TalkerDAO.updateTalkerForAnswerCount();
		//TalkerDAO.updateLogTime();
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
	
	public static void diseaseUtil(){
		List<DiseaseBean> diseaseList = DiseaseDAO.getCatchedDiseasesList(session);
		render(diseaseList);
	}
	
	public static void changeDisease(String oldName, String newName){
		System.out.println(oldName);
		System.out.println(newName);
		String errorMsg = "";
		if(StringUtils.isBlank(oldName)){
			errorMsg = "Please select old Name";
		} else if(StringUtils.isBlank(newName)) {
			errorMsg = "Please select new Name";
		} else {
			DiseaseChangeUtil.updateDiseaseNameInTalker(oldName, newName);
			DiseaseChangeUtil.updateDiseaseNameInConvo(oldName, newName);
		}
		
		Cache.set("diseasesList", null);
		List<DiseaseBean> diseaseList = DiseaseDAO.getCatchedDiseasesList(session);
		render("@diseaseUtil",diseaseList,errorMsg,oldName,newName);
	}
	
	public static void updateAllDisease() {
		String errorMsg = "Please restart server to get reflections done\nIf you have edited a name Please user change util to reflect it all where";
		try {
			DiseaseImporter.importDiseases("diseases.dat");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			errorMsg = "Internal server error";
		}
		Cache.set("diseasesList", null);
		List<DiseaseBean> diseaseList = DiseaseDAO.getCatchedDiseasesList(session);
		render("@diseaseUtil",diseaseList,errorMsg);
	}
	
	public static void topicStats(int page) {
		List<TopicBean> topicList = new ArrayList<TopicBean>();
		Set<TopicBean> topList = TalkerLogic.loadAllTopicsFromCache();
		if(page == 0){
			page = 1;
		}
		int recoCount = 20;
		int counter = (recoCount * (page - 1));
		int topicCounter = 0;
		for (TopicBean topic : topList) {
			topicCounter++;
			if(topicCounter <= (recoCount * page) &&  topicCounter >= ((recoCount * page) - 20)) {
				if(topic.getNoOfConverstions() <= 0 && counter <= (recoCount * page)) {
					topic.setNoOfConverstions(ConversationDAO.getNoOfconvosForTopic(topic.getId()));
				}
				counter++;
			}
			topicList.add(topic);
		}
		ValuePaginator<TopicBean> paginator = new ValuePaginator(topicList);
		paginator.setPageNumber(page); 
	    render(paginator);
	}
	
	public static void diseaseStats(String fromDate,String toDate) {
		
		String errorMsg = "No/Wrong Date rage will display today's stats";
		DateFormat dateFormat = new SimpleDateFormat("yyyy-dd-MM");//09/04/2012
		Date fromDt = new Date();
		Date toDt = new Date();
		boolean dateError = false;
		if(fromDate != null && !"".equals(fromDate)) {
			try {
				fromDt = dateFormat.parse(fromDate);
				toDt = dateFormat.parse(toDate);
				if(toDt.before(fromDt)){
					dateError = true;
				}
			} catch(Exception e) {
				e.printStackTrace();
				dateError = true;
			}
			if(dateError) {
				errorMsg = "Wrong dates selected";
			}
		} else {
			dateError = true;
		}

		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		List<DiseaseBean> diseaseList = DiseaseDAO.getCatchedDiseasesList(session);
		Map<String, String> diseaseStatsList = new HashMap<String, String>();
		for (DiseaseBean diseaseBean : diseaseList) {
			if(!dateError) {
				diseaseStatsList.put(diseaseBean.getName(), ActivityLogDAO.getDiseaseLogList(diseaseBean.getId(),fromDt,toDt));
			} else {
				diseaseStatsList.put(diseaseBean.getName(), ActivityLogDAO.getDiseaseLogList(diseaseBean.getId(),calendar.getTime(),calendar.getTime()));	
			}
			
		}
		render(diseaseStatsList,errorMsg,fromDate,toDate);
	}
}
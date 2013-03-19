package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ComparatorUtils;
import org.apache.commons.lang.StringUtils;

import logic.ConversationLogic;
import logic.TalkerLogic;
import logic.TopicLogic;
import models.CommentBean;
import models.ConversationBean;
import models.IMAccountBean;
import models.MessageBean;
import models.PrivacySetting;
import models.TalkerBean;
import models.TopicBean;
import models.PrivacySetting.PrivacyType;
import models.PrivacySetting.PrivacyValue;
import play.Logger;
import play.Play;
import play.cache.Cache;
import play.mvc.Http.Request;
import play.mvc.Http;
import play.mvc.Router;
import play.mvc.Router.ActionDefinition;
import play.mvc.Scope.Session;
import util.EmailUtil.EmailTemplate;
import util.importers.DiseaseImporter;

import com.mongodb.DBRef;
import com.tah.im.IMNotifier;
import com.tah.im.model.IMAccount;

import dao.TalkerDAO;
import dao.TopicDAO;

/**
 * Different utility methods used through application
 *
 */
public class CommonUtil {
	
	//pattern for locating links in the text
	public static final String WEB_URL_PATTERN = " ((https?|ftp)://[a-zA-Z0-9+\\-&@#/%?=~_|!:,.;]*[a-zA-Z0-9+&@#/%=~_|])";
	
	private static final MessageDigest MD5_MESSAGE_DIGEST;
	static {
		try {
			MD5_MESSAGE_DIGEST = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException nsae) {
			throw new IllegalStateException(nsae);
		}
	}

	public static String hashPassword(String password) {
		byte[] md5hash = null;
		synchronized (MD5_MESSAGE_DIGEST) {
			try {
				md5hash = MD5_MESSAGE_DIGEST.digest(password.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				Logger.error(e, "CommonUtil.java : hashPassword");
			}
			MD5_MESSAGE_DIGEST.reset();
		}
		if (md5hash == null) {
			return null;
		}
		
		//convert to string
		BigInteger bigInt = new BigInteger(1, md5hash);
		String hashText = bigInt.toString(16);
		//integer can remove leading zeroes - add them back
		while (hashText.length() < 32) {
			hashText = "0"+hashText;
		}
		Logger.info("hashPassword : " + 32);
		return hashText;
	}
	
	/**
	 * Updates given talker in DB and Cache
	 */
	public static void updateTalker(TalkerBean talker, Session session) {
		TalkerDAO.updateTalker(talker);
		CommonUtil.refreshCachedTalker(session);
	}
	/**
	 * Get cached logged-in talker instance or load a new one
	 * @param session
	 * @return
	 */
	public static TalkerBean loadCachedTalker(Session session) {
		TalkerBean talker = Cache.get(session.getId() + "-talker", TalkerBean.class);
	    if (talker == null) {
	        //nothing in cache - load a new one
	    	talker = refreshCachedTalker(session);
	    }
	    return talker;
	}
	/**
	 * Loads logged-in talker instance and updates cache
	 * @param session
	 * @return
	 */
	public static TalkerBean refreshCachedTalker(Session session) {
		String sessionUserName = session.get("username");
		if (sessionUserName == null) {
			return null;
		}
		
		TalkerBean talker = TalkerDAO.getByUserName(sessionUserName);
		if (talker != null) {
			Cache.set(session.getId() + "-talker", talker, "60mn");
		}
        return talker;
	}
	
	public static Date parseDate(int month, int date, int year) {
		if (month == 0 || date == 0 || year == 0) {
			return null;
		}
		Calendar dateCalendar = new GregorianCalendar(year, month-1, date);
		return dateCalendar.getTime();
	}

	/**
	 * Parse given value as comma separated list of strings
	 * @param otherItems
	 * @param defaultValue Default value used in text input, doesn't include it in return list
	 * @return
	 */
	public static List<String> parseCommaSerapatedList(String otherItems, String defaultValue) {
		if (otherItems == null) {
			return null;
		}
		
		String[] otherArray = otherItems.split(",");
		//validate and add
		List<String> itemsList = new ArrayList<String>();
		for (String otherItem : otherArray) {
			otherItem = otherItem.trim();
			if (otherItem.length() != 0 && !otherItem.equalsIgnoreCase(defaultValue)) {
				itemsList.add(otherItem);
			}
		}
		return itemsList;
	}

	/**
	 * Generates unique verification code for emails
	 * @return
	 */
	public static String generateVerifyCode() {
		String verifyCode = null;
		boolean unique = true;
		int count = 0;
		do {
			verifyCode = UUID.randomUUID().toString();
			unique = (TalkerDAO.getByVerifyCode(verifyCode) == null);
			count++;
		} while (!unique);
		CommonUtil.log("CommonUtil.genrateVerificode",""+count);
		
		//Logger.info("generateVerifyCode : " + count);
		return verifyCode;
	}
	
	public static String generateRandomPassword() {
		SecureRandom random = new SecureRandom();
	    String newPassword = new BigInteger(60, random).toString(32);
	    return newPassword;
	}
	
	/**
	 * Generates random username as 'member'+number
	 * @param checkUnique return only unique name?
	 * @return
	 */
	public static String generateRandomUserName(boolean checkUnique) {
		String userName = null;
		Random random = new Random();
		int count = 0;
		while (true) {
			int num = random.nextInt(10000)+1;
			userName = "member"+num;
			if (!checkUnique || TalkerDAO.isUserNameUnique(userName)) {
				break;
			}
			count++;
		}
		CommonUtil.log("CommonUtil.java", ""+count);
		//Logger.info("generateRandomUserName : " + count);

		return userName;
	}
	
	/**
	 * Generate absolute url to some application page
	 * @param action Play action in format "Controller.method", e.g. "ViewDispatcher.view"
	 * @param paramName 
	 * @param paramValue
	 * @return
	 */
	public static String generateAbsoluteURL(String action, String paramName, Object paramValue) {
		//we can't generate url without request (e.g. calling this method from some Job)
		if (Http.Request.current() == null) {
			if (action.equals("ViewDispatcher.view")) {
				return "http://talkabouthealth.com/"+paramValue;
			}
			else if (action.equals("Talk.talkApp")) {
				return "http://talkabouthealth.com/chat/"+paramValue;
			}
			else {
				return null;
			}
		}
		
		//prepare parameters if they exist
		Map<String, Object> args = new HashMap<String, Object>(1);
		if (paramName != null) {
			args.put(paramName, paramValue);
		}
		
		//Generate absolute url from given params
		ActionDefinition actionDef = Router.reverse(action, args);
		actionDef.absolute();
		return actionDef.url;
	}
	public static String generateAbsoluteURL(String action) {
		return generateAbsoluteURL(action, null, null);
	}
	
	/**
	 * Create BufferedReader for importer classes
	 * @param fileName
	 * @return
	 */
	public static BufferedReader createImportReader(String fileName) {
		InputStream is = CommonUtil.class.getResourceAsStream("/util/importers/data/"+fileName);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		return br;
	}
	
	
	/**
	 * Returns user info as html string (username, connection, etc.)
	 * @param talker
	 * @param authenticated Is current user logged in?
	 * @return
	 */
	public static String talkerToHTML(TalkerBean talker, boolean authenticated) {
		if (talker == null) {
			return "";
		}
		String talkerName = null;
		if (authenticated || talker.isPublic(PrivacyType.USERNAME)) {
			talkerName = talker.getUserName();
		}
		else {
			talkerName = talker.getAnonymousName();
			//Check for anonymous user
			if(talkerName == null)
				talkerName = talker.getUserName();
		}
		
		StringBuilder html = new StringBuilder();
		String url = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", talkerName);
		html.append("<a href='"+url+"'>"+talkerName+"</a>");
		
		String additionalInfo = "";
		
		String talkerConnection="";
		
		if (talker.isProf()) {
			if (talker.getConnection().equals("Physician") && talker.getProfInfo().get("prim_specialty") != null) {
				additionalInfo = " - "+talker.getProfInfo().get("prim_specialty");
			}
			if (talker.isConnectionVerified()) {
				additionalInfo += " <span class=\"green12\">(Verified)</span>";
			}
			else {
				additionalInfo += " <span class=\"red12\">(not verified)</span>";
			}
		}
		
		if(talker.getConnection()!=null){
			talkerConnection=talker.getConnection();
		}
		
		if(talkerConnection!="" || additionalInfo!=""){
			html.append(" ("+talkerConnection+additionalInfo+")");	
		}
		return html.toString();
	}
	
	/**
	 * Returns convo topics as html string
	 * @return
	 */
	public static String topicsToHTML(ConversationBean convo) {
		StringBuilder topicsHTML = new StringBuilder();
		
		if(convo != null){
			Set<TopicBean> convoTopics = convo.getTopics();
			if (convoTopics.size() == 1) {
				String topicTitle = convoTopics.iterator().next().getTitle();
				if (topicTitle.equals(ConversationLogic.DEFAULT_QUESTION_TOPIC)
						|| topicTitle.equals(ConversationLogic.DEFAULT_TALK_TOPIC)) {
					//do not show default topic
					return "";
				}
			}
			
			for (TopicBean topic : convoTopics) {
				String topicURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", topic.getMainURL());
				String topicLink = "<a href='"+topicURL+"'>"+topic.getTitle()+"</a>";
	
				topicsHTML.append(topicLink);
				topicsHTML.append(", ");
			}
			int len = topicsHTML.length();
			if (len != 0) {
				topicsHTML.delete(len-2, len);
				topicsHTML.insert(0, " in topic(s) ");
			}
		}
		return topicsHTML.toString();
	}
	
	/**
	 * Parses IM account information (service, username) from email
	 * @param email
	 * @return
	 */
	public static IMAccountBean parseIMAccount(String email) {
		//default
		String imService = "GoogleTalk";
		String imUsername = null;
		if (email.contains("@gmail")) {
			//Google service
			imService = "GoogleTalk";
			imUsername = removeService(email);
		}
		else if (email.contains("@live") || email.contains("@hotmail")) {
			imService = "WindowLive";
			imUsername = removeService(email);
		}
		else if (email.contains("@yahoo")){
			//for now default - Yahoo
			imService = "YahooIM";
			imUsername = removeService(email);
		}
		
		return new IMAccountBean(imUsername, imService);
	}
	private static String removeService(String imUsername) {
		int end = imUsername.indexOf("@");
		if (end != -1) {
			imUsername = imUsername.substring(0, end);
		}
		return imUsername;
	}
	
	/**
	 * Replaces plain text links with html links.
	 * 
	 * @param text
	 * @return
	 */
	public static String linkify(String text) {
		if (text == null) {
			return null;
		}
		
		String replacedText = text.replaceAll(WEB_URL_PATTERN, "<a href=\"$1\" target=\"_blank\">$1</a>");
		return replacedText;
	}
	
	/**
	 * Parse and validate emails from comma-separated string
	 * @param emails
	 * @return
	 */
	public static Set<String> parseEmailsFromString(String emails) {
		Set<String> emailsToSend = new HashSet<String>();
		String[] emailsArr = emails.split(",");	
		for (String email : emailsArr) {
			email = email.trim();
			if (ValidateData.validateEmail(email)) {
				emailsToSend.add(email);
			}
		}
		return emailsToSend;
	}
	
	public static Set<String> loadTalkers(){
		Set<String> allTalkers = new TreeSet<String>(
				new StringLengthComparator());
		for (TalkerBean talker : TalkerLogic.loadAllTalkersFromCache()) {
			allTalkers.add(talker.getUserName());
		}
		return allTalkers;
	}
	
	public static Map<String, String> loadTopics(){
		Map<String, String> allTopics = new TreeMap<String, String>(new StringLengthComparator());
		for (TopicBean topic : TalkerLogic.loadAllTopicsFromCache()) {
			String topicTitle = topic.getTitle().replaceAll(" ", "");
			allTopics.put(topicTitle, topic.getMainURL());
		}
		return allTopics;
	}
	
	private static class StringLengthComparator implements Comparator<String> {
		@Override
		public int compare(String o1, String o2) {
			if(o1 !=null && o2 != null){
				if (o1.length() == o2.length()) {
					return o1.compareTo(o2);
				}
				else {
					return o2.length() - o1.length();
				}
			}
			else{
				return 0;
			}
		}
	}
	
	public static String prepareThoughtOrAnswer(String text) {
		text = CommonUtil.linkify(text);
		if (text.contains("#")) {
			Map<String, String> allTopics = loadTopics();
			Iterator it = allTopics.entrySet().iterator();

			//for(int index =0 ; index < allTopics.size(); index++){
			int count = 0;
			while (it.hasNext()) {
				Entry<String, String> topicEntry =(Map.Entry)it.next();
				if (topicEntry.getKey().length() == 0) {
					continue;
				}
				String key = ValidateData.escapeText(topicEntry.getKey());
				// case insensitive

				/*
				 * text = text.replaceAll("(?i)#("+topicEntry.getKey()+")",
				 * "<a href=\"http://talkabouthealth.com/"
				 * +topicEntry.getValue()+"\">#&$1</a>");
				 */

				text = text.replaceAll("(?i)#(" + key + ")",
						"<a href=\"http://talkabouthealth.com/"
								+ topicEntry.getValue() + "\">#&$1</a>");
				it.remove();
				count++;
			}
			CommonUtil.log("CommonUtil.prepareThoughtOrAnswer", ""+count);
			//Logger.info("prepareThoughtOrAnswer : " + count);
		}
		if (text.contains("@")) {
			Set<String> allTalkers =  loadTalkers();
			for (String talker : allTalkers) {
				// case insensitive
				talker = ValidateData.escapeText(talker);
				text = text.replaceAll("(?i)@(" + talker + ")",
						"<a href=\"http://talkabouthealth.com/" + talker
								+ "\">@&$1</a>");
			}
		}
		text = text.replaceAll(">#&", ">#").replaceAll(">@&", ">@");
		text = text.replace("\n", "<br/>");
		return text;
	}

	public static String prepareTwitterThought(String htmlText) {
		htmlText = CommonUtil.linkify(htmlText);
		
		String searchRegex = "(\\s|\\A)#(\\w+)";
		String userRegex = "(\\s|\\A)@(\\w+)";
		
		htmlText = htmlText.replaceAll(searchRegex, "$1<a href=\"http://twitter.com/search?q=%23$2\" target=\"_blank\">#$2</a>");
		htmlText = htmlText.replaceAll(userRegex, "$1<a href=\"http://twitter.com/$2\" target=\"_blank\">@$2</a>");
		
		return htmlText;
	}

	public static String commentToHTML(CommentBean thoughtOrAnswer) {
		String text = thoughtOrAnswer.getText();
		if (text == null) {
			return "";
		}
		
		//delinkify because previously we store links in db
		//text = text.replaceAll("<a[^>]*>", "");
		//text = text.replaceAll("</a>", "");
		
		String htmlText = text;
		if (thoughtOrAnswer.getFrom() != null && thoughtOrAnswer.getFrom().equalsIgnoreCase("twitter")) {
			htmlText = CommonUtil.prepareTwitterThought(htmlText);
			htmlText = htmlText.replace("\n", "<br/>");
		}
		else {
			htmlText = htmlText.replace("\n", "<br/>");
			
			htmlText = CommonUtil.prepareThoughtOrAnswer(htmlText);
		}
		htmlText = htmlText.replace("<br>", "<br/>");
		return htmlText;
	}
	
	public static String messageToHTML(String text) {
		if (text == null) {
			return "";
		}
		
		//delinkify because previously we store links in db
		text = text.replaceAll("<a[^>]*>", "");
		text = text.replaceAll("</a>", "");
		
		String htmlText = text;
				
		htmlText = htmlText.replace("\n", "<br/>");
		
		return htmlText;
	}
	
	public static void log(String method,String error){
		//System.out.println(""+method+ "  :"+error);
		//Logger.info("method:"+method+"error"+message);
	}
}

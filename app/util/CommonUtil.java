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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;

import logic.ConversationLogic;
import logic.TopicLogic;
import models.ConversationBean;
import models.IMAccountBean;
import models.TalkerBean;
import models.TopicBean;
import play.Play;
import play.cache.Cache;
import play.mvc.Http.Request;
import play.mvc.Router;
import play.mvc.Router.ActionDefinition;
import play.mvc.Scope.Session;
import util.EmailUtil.EmailTemplate;
import util.importers.DiseaseImporter;

import com.mongodb.DBRef;
import com.tah.im.IMNotifier;
import com.tah.im.model.IMAccount;

import dao.TalkerDAO;

//TODO: check util/importers/jobs packages
public class CommonUtil {
	
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
				e.printStackTrace();
			}
			MD5_MESSAGE_DIGEST.reset();
		}
		if (md5hash == null) {
			return null;
		}
		BigInteger bigInt = new BigInteger(1, md5hash);
		String hashText = bigInt.toString(16);
		//integer can remove leading zeroes - add them back
		while (hashText.length() < 32) {
			hashText = "0"+hashText;
		}
		return hashText;
	}
	
	/**
	 * Executes HTTP GET request and return reply as list of strings
	 * @param urlString
	 * @param parameters
	 * @return
	 */
	public static List<String> makeGET(String urlString, String parameters) {
		try {
			URL url = new URL(urlString+"?"+parameters);
			URLConnection urlConnection = url.openConnection();
			
			//read reply
			List<String> lines = new ArrayList<String>();
			BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                    urlConnection.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				lines.add(inputLine);
			}
			in.close();
			return lines;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	//Send IM invitation through Dashboard application
	public static void sendIMInvitation(IMAccountBean imAccount) {
		IMNotifier imNotifier = IMNotifier.getInstance();
		try {
			imNotifier.addContact(imAccount.getService(), imAccount.getUserName());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Updates talker in DB and Cache
	 */
	public static void updateTalker(TalkerBean talker, Session session) {
		TalkerDAO.updateTalker(talker);
		CommonUtil.refreshCachedTalker(session);
	}
	
	public static TalkerBean loadCachedTalker(Session session) {
		TalkerBean talker = Cache.get(session.getId() + "-talker", TalkerBean.class);
	    if (talker == null) {
	        // Cache miss
	    	talker = refreshCachedTalker(session);
	    }
	    return talker;
	}
	
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
	
	public static Date parseDate(String dateString) {
		try {
			DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
			Date date = dateFormat.parse(dateString);
			return date;
		} catch (ParseException e) {}
		
		return null;
	}

	public static Date parseDate(int month, int day, int year) {
		if (month == 0 || day == 0 || year == 0) {
			return null;
		}
		return parseDate(month+"/"+day+"/"+year);
	}

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

	public static String generateVerifyCode() {
		String verifyCode = null;
		boolean unique = true;
		do {
			verifyCode = UUID.randomUUID().toString();
			unique = (TalkerDAO.getByVerifyCode(verifyCode) == null);
		} while (!unique);
		
		return verifyCode;
	}
	
	public static String generateDeactivatedUserName(boolean checkUnique) {
		String userName = null;
		Random random = new Random();
		while (true) {
			int num = random.nextInt(1000)+1;
			userName = "member"+num;
			if (!checkUnique || TalkerDAO.isUserNameUnique(userName)) {
				break;
			}
		}

		return userName;
	}
	
	//Generate url by Play! action and parameters
	public static String generateAbsoluteURL(String action, String paramName, Object paramValue) {
		Map<String, Object> args = new HashMap<String, Object>(1);
		if (paramName != null) {
			args.put(paramName, paramValue);
		}
		
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
	
	
	
	//for displaying info
	public static String talkerToHTML(TalkerBean talker, boolean authenticated) {
		if (talker == null) {
			return "";
		}
		StringBuilder html = new StringBuilder();
		if (authenticated) {
			String url = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", talker.getUserName());
			html.append("<a href='"+url+"'>"+talker.getUserName()+"</a>");
			if (talker.getConnection() != null && talker.getConnection().length() != 0) {
				String notVerifiedStr = "";
				if (TalkerBean.PROFESSIONAL_CONNECTIONS_LIST.contains(talker.getConnection())) {
					if (!talker.isConnectionVerified()) {
						notVerifiedStr = " <span class=\"red12\">(not verified)</span>";
					}
				}
				html.append(" ("+talker.getConnection()+notVerifiedStr+")");
			}
		}
		else {
			html.append(getAnonymousName(talker.getUserName()));
		}
		
		return html.toString();
	}
	
	public static String topicsToHTML(ConversationBean convo) {
		StringBuilder topicsHTML = new StringBuilder();
		
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
		return topicsHTML.toString();
	}
	
	public static String getAnonymousName(String userName) {
		Request req = Request.current();
		Map<String, String> namesMap = (Map<String, String>)req.args.get("namesMap");
		if (namesMap == null) {
			namesMap = new HashMap<String, String>();
			req.args.put("namesMap", namesMap);
		}
		
		String anonymName = namesMap.get(userName);
		if (anonymName == null) {
			anonymName = generateDeactivatedUserName(true);
			namesMap.put(userName, anonymName);
		}
		
		return anonymName;
	}

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

	public static void flagContent(String contentType, ConversationBean convo,
			String reason, String content, TalkerBean talker) {
		Map<String, String> vars = new HashMap<String, String>();
    	vars.put("content_type", contentType);
    	vars.put("content_link", CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL()));
    	vars.put("reason", reason);
		vars.put("content", content);
		vars.put("name", talker.getUserName());
		vars.put("email", talker.getEmail());
		EmailUtil.sendEmail(EmailTemplate.FLAGGED, EmailUtil.SUPPORT_EMAIL, vars, null, false);
	}
}

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
import java.util.UUID;

import org.apache.commons.lang.StringUtils;

import models.ConversationBean;
import models.IMAccountBean;
import models.TalkerBean;
import models.TopicBean;
import play.Play;
import play.cache.Cache;
import play.mvc.Router;
import play.mvc.Router.ActionDefinition;
import play.mvc.Scope.Session;
import util.importers.DiseaseImporter;

import com.mongodb.DBRef;
import com.tah.im.IMNotifier;

import dao.TalkerDAO;

public class CommonUtil {
	
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy");
	
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
			Date date = DATE_FORMAT.parse(dateString);
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

	public static List<String> parseCommaSerapatedList(String otherItems) {
		if (otherItems == null) {
			return null;
		}
		
		String[] otherArray = otherItems.split(",");
		
		//validate and add
		List<String> itemsList = new ArrayList<String>();
		for (String otherItem : otherArray) {
			otherItem = otherItem.trim();
			if (otherItem.length() != 0) {
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
	
	public static String generateDeactivatedUserName() {
		String userName = null;
		Random random = new Random();
		while (true) {
			int num = random.nextInt(1000)+1;
			userName = "member"+num;
			if (TalkerDAO.isUserNameUnique(userName)) {
				break;
			}
		}

		return userName;
	}
	
	//Generate url by Play! action and parameters
	public static String generateAbsoluteURL(String action, String paramName, Object paramValue) {
		Map<String, Object> args = new HashMap<String, Object>(1);
		args.put(paramName, paramValue);
		
		ActionDefinition actionDef = Router.reverse(action, args);
		actionDef.absolute();
		return actionDef.url;
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
	public static String talkerToHTML(TalkerBean talker) {
		if (talker == null) {
			return "";
		}
		String url = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", talker.getUserName());
		String html = "<a href='"+url+"'>"+talker.getUserName()+"</a>";
		if (talker.getConnection() != null && talker.getConnection().length() != 0) {
			html = html+" ("+talker.getConnection()+")";
		}
		return html;
	}
	
	public static String topicsToHTML(ConversationBean convo) {
		StringBuilder topicsHTML = new StringBuilder();
		for (TopicBean topic : convo.getTopics()) {
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
}

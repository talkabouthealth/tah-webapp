package util;

import java.io.BufferedReader;
import java.io.IOException;
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
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import models.TalkerBean;
import play.cache.Cache;
import play.mvc.Scope.Session;

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
	public static void sendIMInvitation(String imService, String imUsername) {
		//TODO: check if such IM exists? 
		if (imService != null && imUsername != null) {
			IMNotifier imNotifier = IMNotifier.getInstance();
			try {
				imNotifier.addContact(imService, imUsername);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static TalkerBean loadCachedTalker(Session session) {
		TalkerBean talker = Cache.get(session.getId() + "-talker", TalkerBean.class);
	    if (talker == null) {
	        // Cache miss
	    	talker = updateCachedTalker(session);
	    }
	    return talker;
	}
	
	public static TalkerBean updateCachedTalker(Session session) {
		TalkerBean talker = TalkerDAO.getByUserName(session.get("username"));
        Cache.set(session.getId() + "-talker", talker, "60mn");
        
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
}

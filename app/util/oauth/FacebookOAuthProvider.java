package util.oauth;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import play.Logger;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.mvc.Http.Request;
import play.mvc.Scope.Session;

import logic.TalkerLogic;
import models.ServiceAccountBean.ServiceType;
import models.ServiceAccountBean;
import models.TalkerBean;
import util.CommonUtil;
import util.TwitterUtil;
import dao.ApplicationDAO;
import dao.TalkerDAO;


public class FacebookOAuthProvider implements OAuthServiceProvider {
	
	private static final String APP_ID = "131545373528131";
	private static final String APP_SECRET = "0620bead67e2ffa4e9e46f60b3376dec";
	private static final String CALLBACK_URL =
		"talkabouthealth.com/oauth/callback?type=facebook";
	
// Test settings	
//	public static final String APP_ID = "126479497379490";
//	public static final String APP_SECRET = "cd4606efec03ea8c5bd9ffb9d49000ff";
//	public static final String CALLBACK_URL =
//		"kan.dev.com:9000/oauth/callback?type=facebook";
	
	public String getAuthURL(Session session, boolean secureRequest) {
		String authURL = null;
		try {
			String callbackURL = (secureRequest ? "https://" : "http://");
			callbackURL = callbackURL+CALLBACK_URL;
			
			authURL = "https://graph.facebook.com/oauth/authorize?" +
				"client_id="+APP_ID+"&redirect_uri="+URLEncoder.encode(callbackURL, "UTF-8")+
				"&scope=email,publish_stream,offline_access";
		} catch (UnsupportedEncodingException e) {
			Logger.error(e, "");
		}
		return authURL;
	}

	public String handleCallback(Session session, Map<String, String> params, boolean secureRequest) throws Exception {
		//code returned by Facebook
		String code = params.get("code");
		if (code == null) {
			return null;
		}
		
		String accessToken = loadAccessToken(secureRequest, code);
		session.put("token", accessToken);
		
		//load user info, parse Facebook id and email from reply
		HttpResponse res = WS.url("https://graph.facebook.com/me?access_token=%s", accessToken).get();
		String responseText = res.getString();
		Logger.info("---------- FACEBOOK INFO --------------");
		Logger.info(responseText);
		
		
		//{"id":"745278081","name":"Murray Nathan Jones","first_name":"Murray","middle_name":"Nathan","last_name":"Jones","link":"http:\/\/www.facebook.com\/murraynathaniel","birthday":"05\/20\/1975","gender":"male","email":"murrayjones\u0040gmail.com","timezone":-5,"locale":"en_US","verified":true,"updated_time":"2011-01-10T17:47:23+0000"}
		//{"id":"100001842920779","name":"Osya Osezno","first_name":"Osya","last_name":"Osezno","link":"http:\/\/www.facebook.com\/profile.php?id=100001842920779","birthday":"01\/01\/1986","location":{"id":"106078429431815","name":"London, United Kingdom"},"gender":"female","email":"ira_osezno\u0040mail.ru","timezone":2,"locale":"en_US"}
		
		String accountId = null;
		String userEmail = null;
		if (responseText.startsWith("{")) {
			Pattern p = Pattern.compile("\"(\\w+)\":\"([\\\\@.\\s\\w]+)\"");
			Matcher m = p.matcher(responseText);
			while (m.find()) {
				String param = m.group(1);
				String value = m.group(2);
				//there are a few "id" fields possible, so we use only the first one
				if (param.equals("id") && accountId == null) {
					accountId = value;
				}
				else if (param.equals("email") && userEmail == null) {
					userEmail = value.replace("\\u0040", "@");
				}
			}
		}
		
		boolean isConnected = session.contains("username");
		if (isConnected) {
			//it's not login/signup - it's notifications or sharing page
			return addAccount(session, accessToken, accountId, userEmail);
		}
		else {
	        return loginOrSignupWithAccount(session, accessToken, accountId,
					userEmail);
		}
	}
	
	private String loadAccessToken(boolean secureRequest, String code) {
		String callbackURL = (secureRequest ? "https://" : "http://");
		callbackURL = callbackURL+CALLBACK_URL;
		
		HttpResponse res = 
			WS.url("https://graph.facebook.com/oauth/access_token" +
					"?client_id=%s&redirect_uri=%s&client_secret=%s&code=%s", APP_ID, callbackURL, APP_SECRET, code).get();
		String responseText = res.getString();
		
		//returned string is:
		//access_token=...token...
		String accessToken = null;
		if (responseText.startsWith("access_token")) {
			accessToken = responseText.substring(13);
		}
		return accessToken;
	}

	/**
	 * Add new FB account to existing user.
	 * @param session
	 * @param accessToken
	 * @param accountId
	 * @param userEmail
	 * @return
	 */
	private String addAccount(Session session, String accessToken,
			String accountId, String userEmail) {
		TalkerBean anotherTalker = TalkerDAO.getByAccount(ServiceType.FACEBOOK, accountId);
		if (anotherTalker != null) {
			//this account is already connected by another user
			return "/profile/notificationsettings?err=notunique&from="+ServiceType.FACEBOOK.toString();
		}
		
		Logger.info("Adding new Facebook account: "+userEmail);
		Logger.info(accessToken);
		
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		if (talker.serviceAccountByType(ServiceType.FACEBOOK) == null) {
			ServiceAccountBean fbAccount = new ServiceAccountBean(accountId, userEmail, ServiceType.FACEBOOK);
			fbAccount.setToken(accessToken);
			
			talker.getServiceAccounts().add(fbAccount);
			CommonUtil.updateTalker(talker, session);
		}
		
		//to sharing or notification settings?
		String redirectURL = session.get("oauth_redirect_url");
		if (redirectURL != null) {
			return redirectURL;
		}
		else {
			return "/profile/notificationsettings";
		}
	}
	
	private String loginOrSignupWithAccount(Session session,
			String accessToken, String accountId, String userEmail) {
		TalkerBean talker = TalkerDAO.getByAccount(ServiceType.FACEBOOK, accountId);
		Logger.info("Loading FB account: <%s>, Result: "+talker, accountId);
		if (talker != null) {
			if (talker.isSuspended()) {
				return "/application/suspendedAccount";
			}
			if (talker.isDeactivated()) {
				talker.setDeactivated(false);
				CommonUtil.updateTalker(talker, session);
			}
			
			ServiceAccountBean fbAccount = talker.serviceAccountByType(ServiceType.FACEBOOK);
			fbAccount.setToken(accessToken);
			CommonUtil.updateTalker(talker, session);
			
			//manual login
			ApplicationDAO.saveLogin(talker.getId(), "facebook");
			session.put("username", talker.getUserName());
			session.put("justloggedin", true);
			
			return "/home";
		}
		else {
			//try to get username from email
			String screenName = null;
			int atIndex = userEmail.indexOf("@");
			if (atIndex != -1) {
				screenName = userEmail.substring(0, atIndex);
			}
			
			session.put("serviceType", ServiceType.FACEBOOK);
			session.put("screenName", screenName);
			session.put("userEmail", userEmail);
			session.put("accountId", accountId);
			
		    return "/application/tosAccept";
		}
	}

}

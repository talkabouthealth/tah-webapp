package util.oauth;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import models.ServiceAccountBean;
import models.TalkerBean;
import models.ServiceAccountBean.ServiceType;
import play.Logger;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import play.mvc.Scope.Session;
import util.CommonUtil;
import dao.ApplicationDAO;
import dao.TalkerDAO;


public class FacebookOAuthProvider implements OAuthServiceProvider {
	
	/*
	Live : TalkAboutHealth
	*/
	private static final String APP_ID = "131545373528131";
	private static final String APP_SECRET = "0620bead67e2ffa4e9e46f60b3376dec";
	private static final String CALLBACK_URL = "talkabouthealth.com/oauth/callback?type=facebook";
	
	/*
	Live : TalkBreastCancer
	*/
	private static final String TBC_APP_ID = "493017477397828";
	private static final String TBC_APP_SECRET = "377e78f1ec32db46795c182d1b3cdf3a";
	private static final String TBC_CALLBACK_URL = "talkbreastcancer.com//oauth/callback?type=facebook";

	/*Local
	private static final String APP_ID = "270834789694900";
	private static final String APP_SECRET = "f9e5cd87d46afd8ff378a8aa7e2ab308";
	private static final String CALLBACK_URL = "localhost:9000/oauth/callback?type=facebook";
	*/
	
	public String getAuthURL(Session session, boolean secureRequest) {
		String authURL = null;
		String cancerType = "";
		try {
			String callbackURL = (secureRequest ? "https://" : "http://");
			cancerType = session.get("cancerType");
			if(StringUtils.isNotBlank(cancerType) && "Breast Cancer".equals(cancerType)) {
				callbackURL = callbackURL+TBC_CALLBACK_URL;
				authURL = "https://graph.facebook.com/oauth/authorize?" + "client_id="+TBC_APP_ID+"&redirect_uri="+URLEncoder.encode(callbackURL, "UTF-8") +
					"&scope=email,publish_stream,offline_access";
			} else {
				callbackURL = callbackURL+CALLBACK_URL;
				authURL = "https://graph.facebook.com/oauth/authorize?" + "client_id="+APP_ID+"&redirect_uri="+URLEncoder.encode(callbackURL, "UTF-8") +
					"&scope=email,publish_stream,offline_access";	
			}
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
		
		String cancerType = "";
		cancerType = session.get("cancerType");
		
		String accessToken = loadAccessToken(secureRequest, code, cancerType);
		session.put("token", accessToken);
		
		//load user info, parse Facebook id and email from reply
		HttpResponse res = WS.url("https://graph.facebook.com/me?access_token=%s", accessToken).get();
		String responseText = res.getString();
		Logger.info("---------- FACEBOOK INFO --------------");
		Logger.info(responseText);

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
	
	private String loadAccessToken(boolean secureRequest, String code, String cancerType) {

		String callbackURL = (secureRequest ? "https://" : "http://");
		WSRequest A = null;

		if(StringUtils.isNotBlank(cancerType) && "Breast Cancer".equals(cancerType)) {
			callbackURL = callbackURL+TBC_CALLBACK_URL;
			A = WS.url("https://graph.facebook.com/oauth/access_token?client_id=%s&redirect_uri=%s&client_secret=%s&code=%s",TBC_APP_ID, callbackURL, TBC_APP_SECRET, WS.encode(code));
		} else {
			callbackURL = callbackURL+CALLBACK_URL;
			A = WS.url("https://graph.facebook.com/oauth/access_token?client_id=%s&redirect_uri=%s&client_secret=%s&code=%s",APP_ID, callbackURL, APP_SECRET, WS.encode(code));
		}

		HttpResponse res = A.get();
		String responseText = res.getString();

		//returned string is: access_token=...token...
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
		TalkerBean anotherNewTalker = TalkerDAO.getByEmail(userEmail);
		if (anotherNewTalker != null) {
			TalkerBean checkUNameTalker = TalkerDAO.getByUserName(anotherNewTalker.getUserName());
			if(checkUNameTalker != null)
				return "/profile/notificationsettings?err=notunique&from="+ServiceType.FACEBOOK.toString();
		}
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

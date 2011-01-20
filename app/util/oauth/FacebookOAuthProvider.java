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
				"&scope=email,user_about_me,user_birthday,publish_stream,offline_access";
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return authURL;
	}

	public String handleCallback(Session session, Map<String, String> params, boolean secureRequest) throws Exception {
		String code = params.get("code");
		if (code != null) {
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
			session.put("token", accessToken);
			
			//parse Facebook id and email from reply
			res = WS.url("https://graph.facebook.com/me?access_token=%s", accessToken).get();
			responseText = res.getString();
			Logger.error("---------- FACEBOOK INFO --------------");
			Logger.error(responseText);
			
			String accountId = null;
			String userEmail = null;
			if (responseText.startsWith("{")) {
				Pattern p = Pattern.compile("\"(\\w+)\":\"([@.\\s\\w]+)\"");
				Matcher m = p.matcher(responseText);
				while (m.find()) {
					String param = m.group(1);
					String value = m.group(2);
					if (param.equals("id")) {
						accountId = value;
					}
					else if (param.equals("email")) {
						userEmail = value;
					}
				}
			}
			
			boolean isConnected = session.contains("username");
			if (isConnected) {
				//it's not login/signup - it's notifications page
				
				TalkerBean anotherTalker = TalkerDAO.getByAccount(ServiceType.FACEBOOK, accountId);
				if (anotherTalker != null) {
					//this account is already connected by another user
					return "/profile/notificationsettings?err=notunique&from="+ServiceType.FACEBOOK.toString();
				}
				
				Logger.error("Adding new Facebook account: "+userEmail);
				Logger.error(accessToken);
				
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
			else {
				//login or signup
		        TalkerBean talker = TalkerDAO.getByAccount(ServiceType.FACEBOOK, accountId);
		        Logger.error("Loading FB account: <%s>, Result: "+talker, accountId);
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
		        	
		        	// insert login record into db
					ApplicationDAO.saveLogin(talker.getId());
	
					// add TalkerBean to session
					session.put("username", talker.getUserName());
					
					return "/home";
		        }
		        else {
		        	session.put("accounttype", ServiceType.FACEBOOK);
		        	session.put("accountname", userEmail);
				    session.put("accountid", accountId);
				    
				    return "/signup?talker.email="+userEmail+"&from="+ServiceType.FACEBOOK.toString();
		        }
			}
		}
		return null;
	}

}

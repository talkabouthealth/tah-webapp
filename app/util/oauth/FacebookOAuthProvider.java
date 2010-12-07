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
	
//	private static final String APP_ID = "131545373528131";
//	private static final String APP_SECRET = "0620bead67e2ffa4e9e46f60b3376dec";
//	private static final String CALLBACK_URL =
//		"talkabouthealth.com/oauth/callback?type=facebook";
	
// Test settings	
	public static final String APP_ID = "126479497379490";
	public static final String APP_SECRET = "cd4606efec03ea8c5bd9ffb9d49000ff";
	public static final String CALLBACK_URL =
		"kan.dev.com:9000/oauth/callback?type=facebook";
	

	public String getAuthURL(Session session, boolean secureRequest) {
		String authURL = null;
		try {
			String callbackURL = (secureRequest ? "https://" : "http://");
			callbackURL = callbackURL+CALLBACK_URL;
			
			authURL = "https://graph.facebook.com/oauth/authorize?" +
				"client_id="+APP_ID+"&redirect_uri="+URLEncoder.encode(callbackURL, "UTF-8")+
				"&scope=email,user_about_me,user_birthday,publish_stream";
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return authURL;
	}

	public String handleCallback(Session session, Map<String, String> params) throws Exception {
		String code = params.get("code");
		if (code != null) {
			//user confirmed app - get access token
			String url = "https://graph.facebook.com/oauth/access_token";
			String urlParams = 
			    "client_id="+APP_ID+
//			    "&redirect_uri="+URLEncoder.encode(callbackURL, "UTF-8")+
			    "&client_secret="+APP_SECRET+
			    "&code="+URLEncoder.encode(code, "UTF-8");
			List<String> lines = CommonUtil.makeGET(url, urlParams);
			
			//returned string is:
			//access_token=...token...&expires=5745
			String accessToken = null;
			for (String line : lines) {
				if (line.startsWith("access_token")) {
					int separatorIndex = line.lastIndexOf('&');
					accessToken = line.substring(13, separatorIndex);
				}
			}
			
			session.put("token", accessToken);
			
			//parse Facebook id and email from reply
			//TODO: improve it? Use not email, but some username?
			String accountId = null;
			String userEmail = null;
			lines = CommonUtil.makeGET("https://graph.facebook.com/me", 
					"access_token="+URLEncoder.encode(accessToken, "UTF-8"));
			for (String line : lines) {
				if (line.startsWith("{")) {
					Pattern p = Pattern.compile("\"(\\w+)\":\"([@.\\s\\w]+)\"");
					Matcher m = p.matcher(line);
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
			}
			
			boolean isConnected = session.contains("username");
			if (isConnected) {
				//it's not login/signup - it's notifications page
				
				//TODO: what for facebook?
				
				TalkerBean talker = CommonUtil.loadCachedTalker(session);
				if (talker.serviceAccountByType(ServiceType.FACEBOOK) == null) {
					ServiceAccountBean fbAccount = new ServiceAccountBean(accountId, userEmail, ServiceType.FACEBOOK);
					fbAccount.setToken(accessToken);
					
					talker.getServiceAccounts().add(fbAccount);
					
					CommonUtil.updateTalker(talker, session);
				}
				else {
					//TODO: probably some error?
				}
		        
		        return "/profile/notificationsettings";
			}
			else {
				//login or signup
		        TalkerBean talker = TalkerDAO.getByAccount(ServiceType.FACEBOOK, accountId);
		        if (talker != null) {
		        	if (talker.isSuspended()) {
		        		return "/application/suspendedAccount";
		        	}
		        	
		        	if (talker.isDeactivated()) {
			    		talker.setDeactivated(false);
			    		CommonUtil.updateTalker(talker, session);
			    	}
		        	
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

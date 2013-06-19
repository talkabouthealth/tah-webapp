package util.oauth;

import static util.DBUtil.setToDB;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import logic.TalkerLogic;
import models.ServiceAccountBean;
import models.ServiceAccountBean.ServiceType;
import models.TalkerBean;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import play.Logger;
import play.cache.Cache;
import play.mvc.Scope.Session;
import util.CommonUtil;
import util.TwitterUtil;
import dao.ApplicationDAO;
import dao.TalkerDAO;

/**
 * Implemented using http://code.google.com/p/oauth-signpost/ library
 *
 */
public class TwitterOAuthProvider implements OAuthServiceProvider {

	/*Live : TalkAboutHealth.com*/
	public static final String CONSUMER_KEY = "D9iFrN4G8ObpLCtGJ9w";
	public static final String CONSUMER_SECRET = "Yy1srQbpldqjtqzzAXpJe3RzuWGxHFKPCF8FPsZKU";
	private static final String CALLBACK_URL = "talkabouthealth.com/oauth/callback?type=twitter";

	/*Live : TalkBreastCancer.com*/
	private static final String TBC_CONSUMER_KEY = "pJyZYlPoFXzJGHblwczpKg";
	private static final String TBC_CONSUMER_SECRET = "dWeC3D7rSZilfgZ5Ty1TH4UDle1Rt75KUWzz7FZeAuk";
	private static final String TBC_CALLBACK_URL = "talkbreastcancer.com/oauth/callback?type=twitter";

	private OAuthConsumer consumer;
	private OAuthProvider provider;

	private String cancerType;

	public TwitterOAuthProvider() {
		consumer = new DefaultOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
		provider = new DefaultOAuthProvider(
	            "http://twitter.com/oauth/request_token",
	            "http://twitter.com/oauth/access_token",
	            "http://twitter.com/oauth/authorize");
	}
	
	public TwitterOAuthProvider(String csrType) {
		cancerType = csrType;
		if(StringUtils.isNotBlank(cancerType) && "Breast Cancer".equals(cancerType)) {
			consumer = new DefaultOAuthConsumer(TBC_CONSUMER_KEY, TBC_CONSUMER_SECRET);
		} else {
			consumer = new DefaultOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);	
		}

			provider = 	new DefaultOAuthProvider(
                    "https://api.twitter.com/oauth/request_token",
                    "https://api.twitter.com/oauth/access_token",
                    "https://api.twitter.com/oauth/authorize");
		/*	provider = new DefaultOAuthProvider(
	            "http://twitter.com/oauth/request_token",
	            "http://twitter.com/oauth/access_token",
	            "http://twitter.com/oauth/authorize");
	    */
	}

	public String getAuthURL(Session session, boolean secureRequest) {
        String authURL = null;
        try {
        	String callbackURL = (secureRequest ? "https://" : "http://");
        	if(StringUtils.isNotBlank(cancerType) && "Breast Cancer".equals(cancerType)) {
        		callbackURL = callbackURL+TBC_CALLBACK_URL;
        	} else {
        		callbackURL = callbackURL+CALLBACK_URL;	
        	}

			authURL = provider.retrieveRequestToken(consumer, callbackURL);

			//save token and token secret for next step of OAuth
			session.put("twitter_token", consumer.getToken());
			session.put("twitter_token_secret", consumer.getTokenSecret());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return authURL;
	}

	public String handleCallback(Session session, Map<String, String> params, boolean secureRequest) throws Exception {
		retrieveTokens(session, params);
		
		URL url = new URL("http://api.twitter.com/1/account/verify_credentials.xml");
        HttpURLConnection req = (HttpURLConnection) url.openConnection();
        //sign the request
        try {
			consumer.sign(req);
		} catch (Exception e) {
			e.printStackTrace();
		}
        req.connect();
        
        BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream()));
        String line = null;
        String screenName = null;
        String accountId = null;
        while ((line = br.readLine()) != null) {
//        	System.out.println(line);
        	//For now we get only screen_name
        	//Ex: <screen_name>kankangaroo</screen_name>
        	line = line.trim();
        	System.out.println(line);
        	if (line.startsWith("<screen_name>")) {
        		screenName = line.substring(13, line.length()-14);
        	}
        	
        	//Ex: <id>23090656</id>
        	if (line.startsWith("<id>")) {
        		//check only first <id> (there is also <id> of status)
        		if (accountId == null) {
        			accountId = line.substring(4, line.length()-5);
        		}
        	}
        }
        br.close();
        
        boolean isConnected = session.contains("username");
		if (isConnected) {
			return addAccount(session, screenName, accountId);
		}
		else {
	        return loginOrSignupWithAccount(session, screenName, accountId);
		}
	}
	
	/**
	 * Retrieves tokens and updates 'consumer' object
	 * @param session
	 * @param params
	 */
	private void retrieveTokens(Session session, Map<String, String> params) {
		String oauthVerifier = params.get("oauth_verifier");
		String token = (String)session.get("twitter_token"); //oauth_token
		//String token = (String)session.get("oauth_token"); //oauth_token
		String tokenSecret = (String)session.get("twitter_token_secret");
		
		//SignPost check this flag to make access_token request
		provider.setOAuth10a(true);
		try {
			consumer.setTokenWithSecret(token, tokenSecret);
			provider.retrieveAccessToken(consumer, oauthVerifier);
			
			session.put("token", consumer.getToken());
			session.put("token_secret", consumer.getTokenSecret());
			
			Logger.info("Twitter Auth.%nToken: %s%nTokenSecret: %s", consumer.getToken(), consumer.getTokenSecret());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String addAccount(Session session, String screenName,
			String accountId) {
		TalkerBean anotherTalker = TalkerDAO.getByAccount(ServiceType.TWITTER, accountId);
		TalkerBean anotherNewTalker = TalkerDAO.getByUserName(screenName);
		
		
		if (anotherNewTalker != null) {
			return "/profile/notificationsettings?err=notunique&from="+ServiceType.TWITTER.toString();
		}
		if (anotherTalker != null) {
			//this account is already connected by another user
			return "/profile/notificationsettings?err=notunique&from="+ServiceType.TWITTER.toString();
		}
		
		Logger.info("Adding new Twitter account: "+screenName);
		Logger.info(consumer.getToken() + " : " + consumer.getTokenSecret());
		
		TwitterUtil.followUser(accountId);
		
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		if (talker.serviceAccountByType(ServiceType.TWITTER) == null) {
			ServiceAccountBean twitterAccount = new ServiceAccountBean(accountId, screenName, ServiceType.TWITTER);
			twitterAccount.setToken(consumer.getToken());
			twitterAccount.setTokenSecret(consumer.getTokenSecret());
			
			talker.getServiceAccounts().add(twitterAccount);
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

	private String loginOrSignupWithAccount(Session session, String screenName,
			String accountId) {
		TalkerBean talker = TalkerDAO.getByAccount(ServiceType.TWITTER, accountId);
		/*TalkerBean anotherNewTalker = TalkerDAO.getByUserName(screenName);
		
		if (anotherNewTalker != null) {
			return "/publicProfile/loginDetails";
		}else{*/
			if (talker != null) {
				if (talker.isSuspended()) {
					return "/application/suspendedAccount";
				}
				if (talker.isDeactivated()) {
					talker.setDeactivated(false);
					CommonUtil.updateTalker(talker, session);
				}
				
				ServiceAccountBean twitterAccount = talker.serviceAccountByType(ServiceType.TWITTER);
				twitterAccount.setToken(consumer.getToken());
				twitterAccount.setTokenSecret(consumer.getTokenSecret());
				Logger.info(twitterAccount.toString());
				CommonUtil.updateTalker(talker, session);
				
				//manual login
				ApplicationDAO.saveLogin(talker.getId(), "twitter");
				session.put("username", talker.getUserName());
				session.put("justloggedin", true);
				
				return "/home";
			}
			else {
				
				session.put("serviceType", ServiceType.TWITTER);
				session.put("screenName", screenName);
				session.put("userEmail", null);
				session.put("accountId", accountId);
				
				//redirect to TOS and PP confirmation
			    return "/application/tosAccept";
			}
		//}
	}

}


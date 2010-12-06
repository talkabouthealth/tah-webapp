package util.oauth;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
	
//	public static final String CONSUMER_KEY = "D9iFrN4G8ObpLCtGJ9w";
//	public static final String CONSUMER_SECRET = "Yy1srQbpldqjtqzzAXpJe3RzuWGxHFKPCF8FPsZKU";
//	private static final String CALLBACK_URL =
//		"http://talkabouthealth.com/oauth/callback?type=twitter";
	
//	Test values FIXME: use production settings
	public static final String CONSUMER_KEY = "7VymbW3wmOOoQ892BqIsaA";
	public static final String CONSUMER_SECRET = "s8aexaIBgMxAm4ZqQNayv5SAr6Wd1SKFVETUEPv0cmM";
	public static final String CALLBACK_URL =
		"http://kan.dev.com:9000/oauth/callback?type=twitter";
	
	private OAuthConsumer consumer;
	private OAuthProvider provider;

	public TwitterOAuthProvider() {
		consumer = new DefaultOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
		provider = new DefaultOAuthProvider(
	            "http://twitter.com/oauth/request_token",
	            "http://twitter.com/oauth/access_token",
	            "http://twitter.com/oauth/authorize");
	}
	
	public String getAuthURL(Session session) {
        String authURL = null;
        try {
			authURL = provider.retrieveRequestToken(consumer, CALLBACK_URL);
			
			//save token and token secret for next step of OAuth
			session.put("twitter_token", consumer.getToken());
			session.put("twitter_token_secret", consumer.getTokenSecret());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return authURL;
	}

	public String handleCallback(Session session, Map<String, String> params) throws Exception {
		String oauthVerifier = params.get("oauth_verifier");
		String token = (String)session.get("twitter_token");
		String tokenSecret = (String)session.get("twitter_token_secret");

		//SignPost check this flag to make access_token request
		provider.setOAuth10a(true);
		try {
			consumer.setTokenWithSecret(token, tokenSecret);
			provider.retrieveAccessToken(consumer, oauthVerifier);
			
			session.put("token", consumer.getToken());
			session.put("token_secret", consumer.getTokenSecret());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
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
        	//For now we get only screen_name
        	//Ex: <screen_name>kankangaroo</screen_name>
        	line = line.trim();			        	
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
        
        //TODO: check uniqueness
        boolean isConnected = session.contains("username");
		if (isConnected) {
			//it's not login/signup - it's adding of Twitter account for notifications!
			try {
		        TwitterUtil.followUser(accountId);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("ERROR: "+e.getMessage());
			}
			
			TalkerBean talker = CommonUtil.loadCachedTalker(session);
			if (talker.serviceAccountByType(ServiceType.TWITTER) == null) {
				ServiceAccountBean twitterAccount = new ServiceAccountBean(accountId, screenName, ServiceType.TWITTER);
				twitterAccount.setToken(consumer.getToken());
				twitterAccount.setTokenSecret(consumer.getTokenSecret());
				
				CommonUtil.updateTalker(talker, session);
			}
			else {
				//TODO: probably some error?
			}
    		
	        
	        return "/profile/notificationsettings";
		}
		else {
			//login or signup
	        TalkerBean talker = TalkerDAO.getByAccount(ServiceType.TWITTER, accountId);
	        if (talker != null) {
	        	if (talker.isSuspended()) {
	        		return "/application/suspendedAccount";
	        	}
	        	
	        	if (talker.isDeactivated()) {
		    		talker.setDeactivated(false);
		    		CommonUtil.updateTalker(talker, session);
		    	}
	        	
	        	//simple login
	        	ApplicationDAO.saveLogin(talker.getId());

				session.put("username", talker.getUserName());
				
				return "/home";
	        }
	        else {
	        	//redirect to signup
	        	TwitterUtil.followUser(accountId);
	        	
	        	session.put("accounttype", ServiceType.TWITTER);
	        	session.put("accountname", screenName);
			    session.put("accountid", accountId);
			     
			    return "/signup?talker.userName="+screenName+"&from="+ServiceType.TWITTER.toString();
	        }
		}
	}
}


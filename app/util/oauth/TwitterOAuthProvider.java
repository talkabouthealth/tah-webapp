package util.oauth;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import models.ServiceAccountBean;
import models.ServiceAccountBean.ServiceType;
import models.TalkerBean;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.signature.SignatureMethod;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import play.Logger;
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
		consumer = new DefaultOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET,SignatureMethod.HMAC_SHA1);

		provider = new DefaultOAuthProvider(consumer,
	            "http://twitter.com/oauth/request_token",
	            "http://twitter.com/oauth/access_token",
	            "http://twitter.com/oauth/authorize");
	}

	public TwitterOAuthProvider(String csrType) {
		cancerType = csrType;
		if(StringUtils.isNotBlank(cancerType) && "Breast Cancer".equals(cancerType)) {
			consumer = new DefaultOAuthConsumer(TBC_CONSUMER_KEY, TBC_CONSUMER_SECRET,SignatureMethod.HMAC_SHA1);
		} else {
			consumer = new DefaultOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET,SignatureMethod.HMAC_SHA1);	
		}
			provider = 	new DefaultOAuthProvider(consumer,
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

			authURL = provider.retrieveRequestToken(callbackURL);

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
		//URL url = new URL("http://api.twitter.com/1/account/verify_credentials.xml");
		URL url = new URL("https://api.twitter.com/1.1/account/verify_credentials.json");

        HttpURLConnection req = (HttpURLConnection) url.openConnection();
        req.setRequestMethod("GET");

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
        String jsonText = "";

        while ((line = br.readLine()) != null) {
        	jsonText = jsonText + line.trim();
        }
        
        if(StringUtils.isNotBlank(jsonText)){
        	JSONObject jsonObject = new JSONObject(jsonText);
        	screenName = jsonObject.get("screen_name").toString();
        	accountId = jsonObject.get("id_str").toString();
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
		String oauthVerifier = params.get("oauth_verifier"); //oauth_verifier
		String token = (String)session.get("twitter_token"); //oauth_token
		String tokenSecret = (String)session.get("twitter_token_secret");
		
		//SignPost check this flag to make access_token request
		//provider.setOAuth10a(true);
		try {
			consumer.setTokenWithSecret(token, tokenSecret);
			provider.retrieveAccessToken(oauthVerifier);
			
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
		} else {
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


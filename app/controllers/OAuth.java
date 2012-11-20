package controllers;

import java.util.Map;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Http.Header;
import util.oauth.FacebookOAuthProvider;
import util.oauth.OAuthServiceProvider;
import util.oauth.TwitterOAuthProvider;

public class OAuth extends Controller {
	
	/**
	 * Redirect user to Twitter or Facebook authentication page
	 * @param type 'twitter' or 'facebook'
	 * @param redirectURL URL to redirect after Twitter/Facebook authentication
	 */
	public static void getAuth(String type, String redirectURL) {
		if (redirectURL != null && !redirectURL.isEmpty()) {
    		session.put("oauth_redirect_url", redirectURL);
    	}
    	else {
    		session.remove("oauth_redirect_url");
    	}
		
		OAuthServiceProvider oauthProvider = getProvider(type);
		
		//Play! 'request.secure' is always false, so we check manually
		Header sslHeader = request.headers.get("x-forwarded-ssl");
		if (sslHeader != null && sslHeader.value().equalsIgnoreCase("on")) {
			request.secure = true;
		}
		redirect(oauthProvider.getAuthURL(session, request.secure));
	}
	
	/**
	 * Callback after Twitter/Facebook authentication
	 * @param type 'twitter' or 'facebook'
	 */
	public static void callback(String type) {
		OAuthServiceProvider oauthProvider = getProvider(type);
		
		try {
			//Play! 'request.secure' is always false, so we check manually
			Header sslHeader = request.headers.get("x-forwarded-ssl");
			if (sslHeader != null && sslHeader.value().equalsIgnoreCase("on")) {
				request.secure = true;
			}
			
			String redirectURL = oauthProvider.handleCallback(session, params.allSimple(), request.secure);
			if (redirectURL != null && redirectURL.contains("tosAccept")) {
				Application.tosAccept();
			}
			else {
				Application.redirectPage(redirectURL);
			}
		} catch (Exception e) {
			Logger.error(e, "OAuth callback exception");
		}
	}
	
	private static OAuthServiceProvider getProvider(String serviceType) {
		String cancerType = "";
		cancerType = session.get("cancerType");
		if (serviceType.equalsIgnoreCase("twitter")) {
			return new TwitterOAuthProvider(cancerType);
		}
		else if (serviceType.equalsIgnoreCase("facebook")) {
			return new FacebookOAuthProvider(cancerType);
		}
		else {
			throw new IllegalArgumentException("Bad service type");
		}
	}

}

package controllers;

import java.util.Map;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Http.Header;
import util.oauth.FacebookOAuthProvider;
import util.oauth.OAuthServiceProvider;
import util.oauth.TwitterOAuthProvider;

public class OAuth extends Controller {
	
	public static void getAuth(String type) {
		OAuthServiceProvider oauthProvider = getProvider(type);
		
		Logger.error("----------------------------");
		Map<String, Header> headers = request.headers;
		if (headers != null) {
			for (String key : headers.keySet()) {
				Logger.error(key+" : "+headers.get(key).value());
			}
		}
		Logger.error("SECURE: "+request.secure);
		
		redirect(oauthProvider.getAuthURL(session, request.secure));
	}
	
	public static void callback(String type) {
		OAuthServiceProvider oauthProvider = getProvider(type);
		
		try {
			String redirectURL = oauthProvider.handleCallback(session, params.allSimple());
			Application.redirectPage(redirectURL);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static OAuthServiceProvider getProvider(String serviceType) {
		if (serviceType.equalsIgnoreCase("twitter")) {
			return new TwitterOAuthProvider();
		}
		else if (serviceType.equalsIgnoreCase("facebook")) {
			return new FacebookOAuthProvider();
		}
		else {
			throw new IllegalArgumentException("Bad service type");
		}
	}

}

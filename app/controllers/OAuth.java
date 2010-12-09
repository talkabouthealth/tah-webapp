package controllers;

import play.Logger;
import play.mvc.Controller;
import util.oauth.FacebookOAuthProvider;
import util.oauth.OAuthServiceProvider;
import util.oauth.TwitterOAuthProvider;

public class OAuth extends Controller {
	
	public static void getAuth(String type) {
//		OAuthServiceProvider oauthProvider = getProvider(type);
//		Logger.error("SECURE: "+request.secure);
//		redirect(oauthProvider.getAuthURL(session, request.secure));
		renderText("Please check again later...");
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

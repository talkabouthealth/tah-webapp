package controllers;

import play.mvc.Controller;
import util.oauth.FacebookOAuthProvider;
import util.oauth.OAuthServiceProvider;
import util.oauth.TwitterOAuthProvider;

public class OAuth extends Controller {
	
	public static void getAuth(String type) {
		OAuthServiceProvider oauthProvider = getProvider(type);
		redirect(oauthProvider.getAuthURL(session));
	}
	
	public static void callback(String type) {
		OAuthServiceProvider oauthProvider = getProvider(type);
		
		//TODO: decouple providers from request/response?
		try {
			String redirectURL = oauthProvider.handleCallback(session, params.allSimple());
			System.out.println("!!"+redirectURL);
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
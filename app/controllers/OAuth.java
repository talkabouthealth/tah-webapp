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
		
//		Logger.error("----------------------------");
//		Map<String, Header> headers = request.headers;
//		if (headers != null) {
//			for (String key : headers.keySet()) {
//				Logger.error(key+" : "+headers.get(key).value());
//			}
//		}
//		Logger.error("SECURE: "+request.secure);
		
		//Play! 'request.secure' is always false, so we check manually
		//x-forwarded-ssl : on
		boolean isRequestSecure = false;
		Header sslHeader = request.headers.get("x-forwarded-ssl");
		if (sslHeader != null && sslHeader.value().equalsIgnoreCase("on")) {
			isRequestSecure = true;
		}
		
		redirect(oauthProvider.getAuthURL(session, isRequestSecure));
	}
	
	public static void callback(String type) {
		OAuthServiceProvider oauthProvider = getProvider(type);
		
		try {
			String redirectURL = oauthProvider.handleCallback(session, params.allSimple());
			
			Header sslHeader = request.headers.get("x-forwarded-ssl");
			if (sslHeader != null && sslHeader.value().equalsIgnoreCase("on")) {
				request.secure = true;
			}
			
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

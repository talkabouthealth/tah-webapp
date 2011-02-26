package util.oauth;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import play.mvc.Http;
import play.mvc.Scope.Session;

public interface OAuthServiceProvider {
	
	/**
	 * Returns URL for user's authentication (where we redirect user)
	 * @param session
	 * @param secureRequest
	 * @return
	 */
	public String getAuthURL(Session session, boolean secureRequest);
	
	/**
	 * Callback when user successfully authenticated in Tw/Fb.
	 * Returns URL where we should redirect user (signup, accounts page, etc.)
	 * 
	 * @param session
	 * @param params Parameters of the request
	 * @param secureRequest
	 * @return
	 * @throws Exception
	 */
	public String handleCallback(Session session, Map<String, String> params, boolean secureRequest) throws Exception;

}


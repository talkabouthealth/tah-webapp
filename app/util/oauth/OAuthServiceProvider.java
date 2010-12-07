package util.oauth;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import play.mvc.Http;
import play.mvc.Scope.Session;

public interface OAuthServiceProvider {
	
	public String getAuthURL(Session session, boolean secureRequest);
	
	public String handleCallback(Session session, Map<String, String> params) throws Exception;

}


package controllers;
 
import java.util.Calendar;
import java.util.Date;

import play.Logger;

import models.TalkerBean;
import util.CommonUtil;
import dao.ApplicationDAO;
import dao.TalkerDAO;
 
/**
 * Handles authentication  
 *
 */
public class Security extends Secure.Security {
	
    static boolean authenticate(String usernameOrEmail, String password) {
    	TalkerBean talker = 
    		TalkerDAO.getByLoginInfo(usernameOrEmail, CommonUtil.hashPassword(password));
    	return talker != null;
    }
    
    static boolean authenticate(String usernameOrEmail, String password, boolean isHash) {
    	if(!isHash)
    		return authenticate(usernameOrEmail, CommonUtil.hashPassword(password));
    	else
    		return authenticate(usernameOrEmail, password);
    }
    
    /**
     * Check if authenticated user has given profile (i.e. role)
     */
    static boolean check(String profile) {
        if("admin".equals(profile)) {
        	//talker with userName "admin" is administrator
            return connected().equals("admin");
        }
        return false;
    }

    /**
     * After successful authentication
     */
    static void onAuthenticated() {
    	//if user logged with email - change session variable "username" to username (not email)
    	String connectedUser = connected();
    	
    	TalkerBean talker = TalkerDAO.getByLoginInfo(connectedUser, null);
    	session.put("username", talker.getUserName());
    	
    	//hanlde suspended or deactivated talkers
    	if (talker.isSuspended()) {
    		session.clear();
    		response.removeCookie("rememberme");
            
            render("Application/suspendedAccount.html");
    		return;
    	}
    	if (talker.isDeactivated()) {
    		//load full talker information (to update it)
    		talker = TalkerDAO.getById(talker.getId());
    		
    		//return to original userName
    		talker.setUserName(talker.getOriginalUserName());
    		talker.setOriginalUserName(null);
    		session.put("username", talker.getUserName());
    		
    		talker.setDeactivated(false);
    		CommonUtil.updateTalker(talker, session);
    	}
    	
    	if (talker.isProf()) {
    		//used for displaying menu (different for patients/profs)
    		session.put("prof", "true");
    	}
    	
		ApplicationDAO.saveLogin(talker.getId(), "login");
		session.put("justloggedin", true);
		
		 /*
	     	Play! 1.0.3 has problems with cookies in IE8 (FLASH cookies aren't stored correctly),
	     	so we pass Original URL through hidden input on the login page
	     	http://www.pivotaltracker.com/story/show/4376939
	     */
	    String url = params.get("url");
	    if (url != null && url.trim().length() != 0) {
	    	flash.put("url", url);
	    }
	    
	    /*
	     	When user fails to login (so comes to login page from 'login' url) -
	     	after successful login he's redirected to login page again. 
	     	It's bad, better redirect to home page.
	     	Or it's my bug because of IE8 updates (see previous lines)
	     */
	    url = flash.get("url");
        if(url != null && url.trim().length() == 0) {
        	flash.put("url", "/home");
        }
    }
    
    static void onDisconnected() {
        Application.index();
    }
}

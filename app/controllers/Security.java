package controllers;
 
import java.util.Calendar;
import java.util.Date;

import models.TalkerBean;
import util.CommonUtil;
import dao.ApplicationDAO;
import dao.TalkerDAO;
 
public class Security extends Secure.Security {
	
    static boolean authenticate(String usernameOrEmail, String password) {
    	TalkerBean talker = 
    		TalkerDAO.getByLoginInfo(usernameOrEmail, CommonUtil.hashPassword(password));
    	return talker != null;
    }
    
    static boolean check(String profile) {
        if("admin".equals(profile)) {
        	//Talker with userName "admin" is administrator
            return connected().equals("admin");
        }
        return false;
    }

    static void onAuthenticated() {
    	//if user logged with email - change session "username" to username (not email)
    	String connectedUser = connected();
    	
    	TalkerBean talker = TalkerDAO.getByLoginInfo(connectedUser, null);
    	session.put("username", talker.getUserName());
    	
    	if (talker.isSuspended()) {
    		session.clear();
            response.setCookie("rememberme", "", 0);
            
            render("Application/suspendedAccount.html");
    		return;
    	}
    	
    	if (talker.isDeactivated()) {
    		//return to original userName
    		talker.setUserName(talker.getOriginalUserName());
    		talker.setOriginalUserName(null);
    		session.put("username", talker.getUserName());
    		
    		talker.setDeactivated(false);
    		CommonUtil.updateTalker(talker, session);
    	}
    	
    	if (talker.isProf()) {
    		session.put("prof", "true");
    	}
    	
		ApplicationDAO.saveLogin(talker.getId());
		
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
        	flash.put("url", "/");
        }
    }
    
    static void onDisconnected() {
        Application.index();
    }
    
}

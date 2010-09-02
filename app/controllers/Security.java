package controllers;
 
import java.util.Calendar;
import java.util.Date;

import models.TalkerBean;
import util.CommonUtil;
import dao.ApplicationDAO;
import dao.TalkerDAO;
 
public class Security extends Secure.Security {
	
    static boolean authenticate(String usernameOrEmail, String password) {
    	if ("admin".equals(usernameOrEmail) && "admin".equals(password)) {
    		return true;
    	}
    	
    	TalkerBean talker = 
    		TalkerDAO.getByLoginInfo(usernameOrEmail, CommonUtil.hashPassword(password));
    	return talker != null;
    }
    
    //TODO: test rememberme?
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
    	if (connectedUser.contains("@")) {
    		TalkerBean talker = TalkerDAO.getByEmail(connectedUser);
    		session.put("username", talker.getUserName());
    	}
    	
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	
    	if (talker.isSuspended()) {
    		session.clear();
            response.setCookie("rememberme", "", 0);
            
            render("Application/suspendedAccount.html");
    		return;
    	}
    	
    	if (talker.isDeactivated()) {
    		talker.setDeactivated(false);
    		CommonUtil.updateTalker(talker, session);
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

    }
    
    static void onDisconnected() {
        Application.index(null);
    }
    
}

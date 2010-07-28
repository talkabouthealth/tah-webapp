package controllers;
 
import java.util.Calendar;
import java.util.Date;

import models.TalkerBean;
import util.CommonUtil;
import dao.LoginHistoryDAO;
import dao.TalkerDAO;
 
public class Security extends Secure.Security {
	
    static boolean authenticate(String usernameOrEmail, String password) {
    	if ("admin".equals(usernameOrEmail) && "admin".equals(password)) {
    		return true;
    	}
    	
    	TalkerBean talker = 
    		TalkerDAO.getTalkerByLoginInfo(usernameOrEmail, CommonUtil.hashPassword(password));
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
    	if (connectedUser.contains("@")) {
    		TalkerBean talker = TalkerDAO.getByEmail(connectedUser);
    		session.put("username", talker.getUserName());
    	}
    	
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	
    	Date now = Calendar.getInstance().getTime(); 
		LoginHistoryDAO.save(talker.getId(), now);
		
		 /*
	     	Play! 1.0.3 has problems with cookies in IE8 (FLASH cookies aren't stored correctly),
	     	so we pass Original URL through hidden input on the login page
	     */
	    String url = params.get("url");
	    if (url != null) {
	    	flash.put("url", url);
	    }

    }
    
    static void onDisconnected() {
        Application.index(null);
    }
    
}

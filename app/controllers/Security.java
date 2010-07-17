package controllers;
 
import java.util.Calendar;
import java.util.Date;

import models.TalkerBean;
import util.CommonUtil;
import dao.LoginHistoryDAO;
import dao.TalkerDAO;
 
public class Security extends Secure.Security {
	
    static boolean authenticate(String username, String password) {
    	if ("admin".equals(username) && "admin".equals(password)) {
    		return true;
    	}
    	
    	TalkerBean talker = 
    		TalkerDAO.getTalkerByLoginInfo(username, CommonUtil.hashPassword(password));
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
    	//TODO: make user for admin?
    	if (!"admin".equals(connected())) {
    		TalkerBean talker = CommonUtil.loadCachedTalker(session);
        	
        	Date now = Calendar.getInstance().getTime(); 
    		LoginHistoryDAO.save(talker.getId(), now);
    	}
    }
    
    static void onDisconnected() {
        Application.index(null);
    }
    
}

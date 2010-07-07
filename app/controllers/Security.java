package controllers;
 
import java.util.Calendar;
import java.util.Date;

import models.TalkerBean;
import util.CommonUtil;
import dao.LoginHistoryDAO;
import dao.TalkerDAO;
 
public class Security extends Secure.Security {
	
    static boolean authenticate(String username, String password) {
    	TalkerBean talker = 
    		TalkerDAO.getTalkerByLoginInfo(username, CommonUtil.hashPassword(password));
    	return talker != null;
    }
    
    static void onAuthenticated() {
    	TalkerBean talker = 
    		CommonUtil.loadCachedTalker(session);
    	
    	Date now = Calendar.getInstance().getTime(); 
		LoginHistoryDAO.save(talker.getId(), now);
		
		session.put("talker", talker);
    }
    
    static void onDisconnected() {
        Application.index();
    }
    
}

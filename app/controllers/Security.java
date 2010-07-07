package controllers;
 
import util.CommonUtil;
import dao.TalkerDAO;
import models.*;
 
public class Security extends Secure.Security {
	
    static boolean authenticate(String username, String password) {
    	TalkerBean talker = 
    		TalkerDAO.getTalkerByLoginInfo(username, CommonUtil.hashPassword(password));
    	return talker != null;
    }
    
    static void onDisconnected() {
        Application.index();
    }
    
}

package controllers;

import java.net.URLEncoder;
import java.util.Date;

import models.TalkerBean;
import play.data.validation.Valid;
import play.mvc.Controller;
import util.CommonUtil;
import util.EmailUtil;
import dao.TalkerDAO;

public class Application extends Controller {

    public static void index(String newTopic) {
    	if (session.contains("username")) {
    		Home.index(newTopic);
    	}
    	else {
    		long numberOfMembers = TalkerDAO.getNumberOfTalkers();

    		render(numberOfMembers);
    	}
    }
    
    public static void signup() {
    	TalkerBean talker = new TalkerBean();
    	flash.put("talker", talker);
    	
    	String from = params.get("from");
    	if (from != null) {
    		flash.put("from", from);
    	}
    	
    	render();
    }
    
    public static void register(@Valid TalkerBean talker, String newTopic) {
		//TODO: work on this!
		//check if user signed up through Twitter or Facebook
//		accountType = (String) request.getSession().getAttribute("accounttype");
//		accountId = (String) request.getSession().getAttribute("accountid");
		
		validateTalker(talker);
				
        if(validation.hasErrors()) {
            params.flash(); // add http parameters to the flash scope
            validation.keep(); // keep the errors for the next request
            signup();
            return;
        }
        
        prepareTalker(talker);
        
        if (!TalkerDAO.save(talker)) {
        	params.flash(); // add http parameters to the flash scope
            validation.keep(); // keep the errors for the next request
        	flash.error("Sorry, unknown error.");
        	System.err.println("Error while saving talker info in DB");
        	signup();
        	return;
		}

		//Successful signup!
		CommonUtil.sendIMInvitation(talker.getUserName(), talker.getIm());
		EmailUtil.sendEmail(EmailUtil.WELCOME_TEMPLATE, talker.getEmail());

		//login
		session.put("username", talker.getUserName());

        index(newTopic);
    }

	private static void prepareTalker(TalkerBean talker) {
		//for now gender has default value
		talker.setGender("M");
		talker.setInvitations(100);
		
        String imUsername = talker.getImUsername();
        if (imUsername.trim().length() == 0) {
			//if user didn't enter it - we parse from email
			int atIndex = talker.getEmail().indexOf('@');
			imUsername = talker.getEmail().substring(0, atIndex);
			talker.setImUsername(imUsername);
		}
        
        String hashedPassword = CommonUtil.hashPassword(talker.getPassword());
        talker.setPassword(hashedPassword);
	}

	private static void validateTalker(TalkerBean talker) {
		Date dateOfBirth = CommonUtil.parseDate(talker.getDobMonth(), talker.getDobDay(), talker.getDobYear());
		if (dateOfBirth != null) {
			validation.past(dateOfBirth);
			talker.setDob(dateOfBirth);
		}
		else {
			//TODO: think about better implementation?
			validation.required(dateOfBirth).message("Date of Birth is incorrect");
		}
		
		if (!validation.hasError("talker.userName")) {
			TalkerBean tmpTalker = TalkerDAO.getByUserName(talker.getUserName());
			validation.isTrue(tmpTalker == null).message("username.exists");
		}
		if (!validation.hasError("talker.email")) {
			TalkerBean tmpTalker = TalkerDAO.getByEmail(talker.getEmail());
			validation.isTrue(tmpTalker == null).message("email.exists");
		}
	}

}
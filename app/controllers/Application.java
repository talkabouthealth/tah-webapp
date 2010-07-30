package controllers;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.TalkerBean;
import models.TalkerBean.ProfilePreference;
import play.data.validation.Email;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.mvc.Controller;
import play.mvc.Scope.Session;
import util.CommonUtil;
import util.EmailUtil;
import dao.TalkerDAO;

public class Application extends Controller {
	
	//As we have profile link as "http://talkabouthealth.com/{userName}" 
	//we need to disallow usernames equal to application routes - controllers, static files, etc
	//TODO: maybe we can user Play! configuration for this?
	private static final List<String> RESERVED_WORDS = Arrays.asList(new String[]{
		"login", "logout", "signup", "register", "forgotpassword", "sendnewpassword", 
		"home", "dashboard", "talk", "profile", "public",
		"actions", "application", "oauth", "security", "static", "topics",
		"errors", 
	});

    public static void index(String newTopic) {
    	if (Security.isConnected()) {
    		Home.index(newTopic);
    	}
    	else {
    		long numberOfMembers = TalkerDAO.getNumberOfTalkers();
    		render(numberOfMembers);
    	}
    }
    
    /* ------- Forgot Password --------- */
    public static void forgotPassword() {
    	render();
    }
    
    public static void sendNewPassword(@Email String email) {
    	TalkerBean user = null;
    	if (!validation.hasError("email")) {
    		user = TalkerDAO.getByEmail(email);
    		validation.isTrue(user != null).message("email.nosuchemail");
    	}
    	
    	if(validation.hasErrors()) {
            params.flash(); // add http parameters to the flash scope
            validation.keep();
            forgotPassword();
            return;
        }
    	
		//generate new password
		SecureRandom random = new SecureRandom();
	    String newPassword = new BigInteger(60, random).toString(32);
	    
	    //change password for this user
	    user.setPassword(CommonUtil.hashPassword(newPassword));
	    TalkerDAO.updateTalker(user);
		
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("username", user.getUserName());
		vars.put("newpassword", newPassword);
		EmailUtil.sendEmail(EmailUtil.FORGOT_PASSWORD_TEMPLATE, user.getEmail(), vars);
		
		flash.success("ok");
		forgotPassword();
    }
    
    /* ------- Sign Up --------- */
    public static void signup() {
    	params.flash();
    	render();
    }
    
    public static void register(@Valid TalkerBean talker, String newTopic) {
		validateTalker(talker);
		
//		System.out.println(validation.errorsMap());
				
        if(validation.hasErrors()) {
            params.flash(); // add http parameters to the flash scope
            validation.keep(); // keep the errors for the next request
            signup();
            return;
        }
        
        prepareTalker(talker, session);
        
        if (!TalkerDAO.save(talker)) {
        	params.flash(); // add http parameters to the flash scope
            validation.keep(); // keep the errors for the next request
        	flash.error("Sorry, unknown error.");
        	System.err.println("Error while saving talker info in DB");
        	signup();
        	return;
		}

		//Successful signup!
		CommonUtil.sendIMInvitation(talker.getIm(), talker.getUserName());
		EmailUtil.sendEmail(EmailUtil.WELCOME_TEMPLATE, talker.getEmail());

		//login
		session.put("username", talker.getUserName());

        index(newTopic);
    }
    
    private static void validateTalker(TalkerBean talker) {
		Date dateOfBirth = CommonUtil.parseDate(talker.getDobMonth(), talker.getDobDay(), talker.getDobYear());
		if (dateOfBirth != null) {
			validation.past(dateOfBirth);
			talker.setDob(dateOfBirth);
		}
		else {
			validation.required(dateOfBirth).message("Date of Birth is incorrect");
		}
		
		if (!validation.hasError("talker.userName")) {
			TalkerBean tmpTalker = TalkerDAO.getByUserName(talker.getUserName());
			validation.isTrue(tmpTalker == null).message("username.exists");

			String lowUserName = talker.getUserName().toLowerCase();
			validation.isTrue(!RESERVED_WORDS.contains(lowUserName)).message("username.reserved");
		}
		if (!validation.hasError("talker.email")) {
			TalkerBean tmpTalker = TalkerDAO.getByEmail(talker.getEmail());
			validation.isTrue(tmpTalker == null).message("email.exists");
		}
	}

	private static void prepareTalker(TalkerBean talker, Session session) {
		//for now gender has default value
		talker.setGender("M");
		talker.setInvitations(100);
		
		//check if user signed up through Twitter or Facebook
		talker.setAccountType(session.get("accounttype"));
		talker.setAccountId(session.get("accountid"));
		
		//TODO: imUsername should be without '@' symbol! && check IMService also?
        String imUsername = talker.getImUsername();
        if (imUsername.trim().length() == 0) {
			//if user didn't enter it - we parse from email
			int atIndex = talker.getEmail().indexOf('@');
			imUsername = talker.getEmail().substring(0, atIndex);
			talker.setImUsername(imUsername);
		}
        
        /*
         	By default the following sections should be unchecked:
				- Personal Info
				- Health Info
				- Basic Info
				other sections should be checked
        */
        EnumSet<ProfilePreference> defaultPreferences = EnumSet.of(
	    		ProfilePreference.ACTIVITY_STREAM, 
	    		ProfilePreference.COMMENTS, 
	    		ProfilePreference.CONVERSATIONS,
	    		ProfilePreference.FOLLOWERS, 
	    		ProfilePreference.FOLLOWING,
	    		ProfilePreference.BIO,
	    		ProfilePreference.THANKYOUS
	    	);
        talker.saveProfilePreferences(defaultPreferences);
        
        String hashedPassword = CommonUtil.hashPassword(talker.getPassword());
        talker.setPassword(hashedPassword);
	}
	
	/**
	 * Redirects top panel to given url,
	 * is used during OAuth 
	 * @param url
	 */
	public static void redirectPage(String url) {
		render(url);
	}

	
	/* ----------------- Contact Us ------------------------- */
	public static void contactus() {
    	render();
    }
    
    public static void sendContactEmail(String name, @Email String email, String subject, String message) {
    	validation.required(name).message("Name is required");
    	validation.required(email).message("Email is required");
    	validation.required(message).message("Message is required");
    	
    	if(validation.hasErrors()) {
            params.flash(); // add http parameters to the flash scope
            validation.keep();
            contactus();
            return;
        }
    	
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("name", name);
		vars.put("email", email);
		vars.put("subject", subject);
		vars.put("message", message);
		EmailUtil.sendEmail(EmailUtil.CONTACTUS_TEMPLATE, EmailUtil.SUPPORT_EMAIL, vars);
		
		flash.success("ok");
		contactus();
    }
}
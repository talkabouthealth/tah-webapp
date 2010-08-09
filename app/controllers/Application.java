package controllers;

import java.math.BigInteger;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

import models.TalkerBean;
import models.TalkerBean.ProfilePreference;
import play.Logger;
import play.Play;
import play.data.validation.Email;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.mvc.Controller;
import play.mvc.With;
import play.mvc.Scope.Session;
import util.CommonUtil;
import util.EmailUtil;
import dao.ApplicationDAO;
import dao.TalkerDAO;

public class Application extends Controller {
	
	//As we have profile link as "http://talkabouthealth.com/{userName}" 
	//we need to disallow usernames equal to application routes - controllers, static files, etc
	//TODO: maybe we can user Play! configuration for this?
	private static final List<String> RESERVED_WORDS = Arrays.asList(new String[]{
		"login", "logout", "signup", "register", "forgotpassword", "sendnewpassword", 
		"home", "dashboard", "talk", "profile", "public", "image", 
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
		boolean result = EmailUtil.sendEmail(EmailUtil.FORGOT_PASSWORD_TEMPLATE, user.getEmail(), vars);
		
		validation.isTrue(result).message("Not verified email or unknown error. " +
				"Please contact support at <a href=\"mailto:"+EmailUtil.SUPPORT_EMAIL+"\">"+EmailUtil.SUPPORT_EMAIL+"</a>");
		if(validation.hasErrors()) {
            params.flash(); // add http parameters to the flash scope
            validation.keep();
            forgotPassword();
            return;
        }
		
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
		
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("username", talker.getUserName());
		vars.put("verify_code", talker.getVerifyCode());
		EmailUtil.sendEmail(EmailUtil.VERIFICATION_TEMPLATE, talker.getEmail(), vars, null, false);
		
		vars = new HashMap<String, String>();
		vars.put("username", talker.getUserName());
		EmailUtil.sendEmail(EmailUtil.WELCOME_TEMPLATE, talker.getEmail(), vars, null, false);

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

    /**
     * Initializes talker with different default values or parsed info
     */
	private static void prepareTalker(TalkerBean talker, Session session) {
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
         	TODO: save settings as enums in App and Strings in DB?
         	Default notification settings:
			- 2 to 5 times per day
			- whenever I am online
			- types of conversations: check all
        */
        talker.setNfreq(3);
        talker.setNtime(1);
        talker.setCtype(new String[] {
        		"Informational", "Advice and opinions", "Meet new people", "Emotional support"});
        
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
        
        //code for email validation
        talker.setVerifyCode(CommonUtil.generateVerifyCode());
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
		//TODO: can we do it here? - without login!
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		render(talker);
    }
    
    public static void sendContactEmail(@Email String email, String subject, String message) {
    	validation.required(email).message("Email is required");
    	validation.required(message).message("Message is required");
    	
    	if(validation.hasErrors()) {
            params.flash(); // add http parameters to the flash scope
            validation.keep();
            contactus();
            return;
        }
    	
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("name", session.get("username") == null ? "" : session.get("username"));
		vars.put("email", email);
		vars.put("subject", subject);
		vars.put("message", message);
		EmailUtil.sendEmail(EmailUtil.CONTACTUS_TEMPLATE, EmailUtil.SUPPORT_EMAIL, vars, null, false);
		
		flash.success("ok");
		contactus();
    }
    
    
    /* -------------------- Email signup (for notifications & updates) ------------------- */
    public static void updatesEmail() {
    	render();
    }
    
    public static void saveUpdatesEmail(@Required @Email String email) {
    	if(validation.hasErrors()) {
    		validation.keep();
    		updatesEmail();
            return;
        }
    	
    	ApplicationDAO.saveEmail(email);
    	
    	flash.success("ok");
    	updatesEmail();
    }
}
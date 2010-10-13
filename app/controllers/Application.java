package controllers;

import java.math.BigInteger;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

import models.IMAccountBean;
import models.TalkerBean;
import models.TalkerBean.EmailSetting;
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
import util.EmailUtil.EmailTemplate;
import dao.ApplicationDAO;
import dao.TalkerDAO;

public class Application extends Controller {
	
	//As we have profile link as "http://talkabouthealth.com/{userName}" 
	//we need to disallow usernames equal to application routes - controllers, static files, etc
	//TODO: maybe we can user Play! configuration for this?
	public static final List<String> RESERVED_WORDS = Arrays.asList(new String[]{
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
		boolean result = EmailUtil.sendEmail(EmailTemplate.FORGOT_PASSWORD, email, vars);
		
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
    	String privacyAgreeString = params.get("privacyagree");
    	validation.isTrue("on".equalsIgnoreCase(privacyAgreeString))
    		.message("Please agree to the TalkAboutHealth Terms of Service and Privacy Policy.");
    	
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
        //Reserve this name as URL
        ApplicationDAO.createURLName(talker.getUserName());
        if (talker.getImAccounts() != null) {
        	for (IMAccountBean imAccount : talker.getImAccounts()) {
        		CommonUtil.sendIMInvitation(imAccount);
        	}
        }
		
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("username", talker.getUserName());
		vars.put("verify_code", talker.getVerifyCode());
		EmailUtil.sendEmail(EmailTemplate.VERIFICATION, talker.getEmail(), vars, null, false);
		
		vars = new HashMap<String, String>();
		vars.put("username", talker.getUserName());
		EmailUtil.sendEmail(EmailTemplate.WELCOME, talker.getEmail(), vars, null, false);

		//login
		ApplicationDAO.saveLogin(talker.getId());
		session.put("username", talker.getUserName());

		session.put("justloggedin", true);
        index(newTopic);
    }
    
    private static void validateTalker(TalkerBean talker) {
//		Date dateOfBirth = CommonUtil.parseDate(talker.getDobMonth(), talker.getDobDay(), talker.getDobYear());
//		if (dateOfBirth != null) {
//			validation.past(dateOfBirth);
//			talker.setDob(dateOfBirth);
//		}
//		else {
//			validation.required(dateOfBirth).message("Date of Birth is incorrect");
//		}
		
		if (!validation.hasError("talker.userName")) {
			boolean nameNotExists = !ApplicationDAO.isURLNameExists(talker.getUserName());
			validation.isTrue(nameNotExists).message("username.exists");
		}
		if (!validation.hasError("talker.email")) {
			TalkerBean otherTalker = TalkerDAO.getByEmail(talker.getEmail());
			validation.isTrue(otherTalker == null).message("email.exists");
		}
		
//		String imService = talker.getIm();
//        String imUsername = talker.getImUsername();
//        if (!imService.isEmpty() && !imUsername.trim().isEmpty()) {
//        	IMAccountBean imAccount = new IMAccountBean(imUsername, imService);
//        	TalkerBean otherTalker = TalkerDAO.getByIMAccount(imAccount);
//        	validation.isTrue(otherTalker == null).message("imaccount.exists");
//        }
	}

    /**
     * Initializes talker with different default values or parsed info
     */
	private static void prepareTalker(TalkerBean talker, Session session) {
		talker.setInvitations(100);
		
		//if user signed up through Twitter or Facebook
		talker.setAccountType(session.get("accounttype"));
		talker.setAccountId(session.get("accountid"));
		
//		String imService = talker.getIm();
//        String imUsername = talker.getImUsername();
//        if (!imService.isEmpty()) {
//        	//if userName empty - parse from email
//        	if (imUsername.trim().isEmpty()) {
//    			int atIndex = talker.getEmail().indexOf('@');
//    			imUsername = talker.getEmail().substring(0, atIndex);
//    		}
//        	
//        	IMAccountBean imAccount = new IMAccountBean(imUsername, imService);
//        	talker.setImAccounts(new HashSet<IMAccountBean>(Arrays.asList(imAccount)));
//        }
        
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
         	By default all sections should be checked:
        */
        EnumSet<ProfilePreference> defaultPreferences = EnumSet.allOf(ProfilePreference.class);
//        	EnumSet.complementOf(
//        		EnumSet.of(
//    				ProfilePreference.PERSONAL_INFO
//    				ProfilePreference.HEALTH_INFO,
//    				ProfilePreference.BASIC_INFO,
//    				ProfilePreference.BIO
//        		)
//        	);
        talker.saveProfilePreferences(defaultPreferences);
        
        //By default all email notifications are checked
        EnumSet<EmailSetting> emailSettings = EnumSet.allOf(EmailSetting.class);
        talker.saveEmailSettings(emailSettings);
        
        String hashedPassword = CommonUtil.hashPassword(talker.getPassword());
        talker.setPassword(hashedPassword);
        
        //code for email validation
        talker.setVerifyCode(CommonUtil.generateVerifyCode());
        
        //professional connections are unverified
        if (TalkerBean.PROFESSIONAL_CONNECTIONS_LIST.contains(talker.getConnection())) {
        	talker.setConnectionVerified(false);
        }
        else {
        	talker.setConnectionVerified(true);
        }
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
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		render(talker);
    }
    
    public static void sendContactEmail(@Email String email, String subject, String message) {
    	validation.required(email).message("Email is required");
    	validation.required(message).message("Message is required");
    	
    	if(validation.hasErrors()) {
            params.flash();
            validation.keep();
            contactus();
            return;
        }
    	
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("name", session.get("username") == null ? "" : session.get("username"));
		vars.put("email", email);
		vars.put("subject", subject);
		vars.put("message", message);
		EmailUtil.sendEmail(EmailTemplate.CONTACTUS, EmailUtil.SUPPORT_EMAIL, vars, null, false);
		
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
    
    
    public static void suspendedAccount() {
    	render();
    }
}
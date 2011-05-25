package controllers;

import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

import logic.TalkerLogic;
import models.EmailBean;
import models.IMAccountBean;
import models.PrivacySetting;
import models.ServiceAccountBean;
import models.PrivacySetting.PrivacyType;
import models.PrivacySetting.PrivacyValue;
import models.ServiceAccountBean.ServiceType;
import models.TalkerBean;
import models.TalkerBean.EmailSetting;
import play.Logger;
import play.Play;
import play.data.validation.Email;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.With;
import play.mvc.Scope.Session;
import util.CommonUtil;
import util.EmailUtil;
import util.TwitterUtil;
import util.EmailUtil.EmailTemplate;
import dao.ApplicationDAO;
import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;

/**
 * Operations for not-authenticated talkers:
 * index page, registration, forgot password, email verification, contact us.
 */
@With( LoggerController.class )
public class Application extends Controller {
	
	//As we have profile link as "http://talkabouthealth.com/{userName}" 
	//we need to disallow usernames equal to application routes - controllers, static files, etc
	public static final List<String> RESERVED_WORDS = Arrays.asList(new String[]{
		"login", "logout", "signup", "register", "forgotpassword", "sendnewpassword", 
		"contactus", "updatesemail", "verify",
		"home", "dashboard", "talk", "profile", "public", "image", 
		"actions", "application", "oauth", "security", "static", "topics",
		"errors", 
		"explore", "search", "community", "openquestions", "livechats", "conversationfeed"
	});

	/**
	 * Landing page
	 */
    public static void index() {
    	if (Security.isConnected()) {
    		//redirect to Home page if user is logged in
    		Home.index();
    	}
    	else {
    		long numberOfMembers = TalkerDAO.getNumberOfTalkers();
    		int numberOfLiveChats = ConversationDAO.getLiveConversations().size();
    		int numberOfQuestions = ConversationDAO.getNumberOfConversations();
    		int numberOfAnswers = CommentsDAO.getNumberOfAnswers();
    		
    		//future communities
    		Map<String, Integer> waitingCommunitiesInfo = ApplicationDAO.getWaitingCommunitiesInfo();
    		
    		render(waitingCommunitiesInfo, numberOfMembers, numberOfLiveChats, 
    				numberOfQuestions, numberOfAnswers);
    	}
    }
    
    /* ------- Forgot Password --------- */
    public static void forgotPassword() {
    	render();
    }
    
    /**
     * Send new password after password restoring.
     */
    public static void sendNewPassword(@Email String email) {
    	TalkerBean talker = null;
    	if (!validation.hasError("email")) {
    		talker = TalkerDAO.getByEmail(email);
    		validation.isTrue(talker != null).message("email.nosuchemail");
    	}
    	if (validation.hasErrors()) {
            params.flash();
            validation.keep();
            forgotPassword();
            return;
        }
    	
		//generate new password
	    String newPassword = CommonUtil.generateRandomPassword();
	    talker.setPassword(CommonUtil.hashPassword(newPassword));
	    TalkerDAO.updateTalker(talker);
		
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("username", talker.getUserName());
		vars.put("newpassword", newPassword);
		EmailUtil.sendEmail(EmailTemplate.FORGOT_PASSWORD, email, vars, null, false);
		
		flash.success("ok");
		forgotPassword();
    }
    
    /* ------- Sign Up --------- */
    public static void signup() {
    	params.flash();
    	
    	//prepare additional settings for FB or Twitter
    	String from = flash.get("from");
    	Map<String, String> additionalSettings = null;
    	if (from != null) {
    		ServiceType type = ServiceAccountBean.parseServiceType(from);
    		additionalSettings = ServiceAccountBean.settingsNamesByType(type);
    	}
    	
    	render(additionalSettings);
    }
    
    public static void register(@Valid TalkerBean talker) {
    	String privacyAgreeString = params.get("privacyagree");
    	validation.isTrue("on".equalsIgnoreCase(privacyAgreeString))
    		.message("Please agree to the TalkAboutHealth Terms of Service and Privacy Policy.");
    	
		validateTalker(talker);
        if (validation.hasErrors()) {
            params.flash(); // add http parameters to the flash scope
            validation.keep(); // keep the errors for the next request
            signup();
            return;
        }
        
        TalkerLogic.prepareTalkerForSignup(talker);
        
        //for these users we do not show update panels
		Set<String> hiddenHelps = talker.getHiddenHelps();
		hiddenHelps.add("updateUsername");
		hiddenHelps.add("updatePassword");
		hiddenHelps.add("updateConnection");
		hiddenHelps.add("updateTwitterSettings");
		hiddenHelps.add("updateFacebookSettings");
        
        boolean okSave = TalkerDAO.save(talker);
        if (!okSave) {
        	params.flash();
            validation.keep();
        	flash.error("Sorry, unknown error. Please contact support.");
        	Logger.error("Error during signup. User: "+talker.getEmail());
        	
        	signup();
        	return;
		}

        TalkerLogic.onSignup(talker, session);
        index();
    }
    
    /**
     * Register with Facebook or Twitter.
     * Called after accepting TOS and PP by user.
     * 
     */
    public static void createUserFromService(String email) {
    	ServiceType serviceType = ServiceType.valueOf(session.get("serviceType"));
    	String screenName = session.get("screenName");
    	String userEmail = session.get("userEmail");
    	String accountId = session.get("accountId");
    	String verifyCode = null;
    	
    	if (serviceType == ServiceType.TWITTER) {
    		//for Twitter we check email
    		validation.required(email);
    		validation.email(email);
			if (validation.hasError("email")) {
				renderText("Error: "+validation.error("email").message());
				return;
			}
			TalkerBean otherTalker = TalkerDAO.getByEmail(email);
			if (otherTalker != null) {
				renderText("Error: "+Messages.get("email.exists"));
				return;
			}
			
			userEmail = email;
			verifyCode = CommonUtil.generateVerifyCode();

			//follow this user by TAH
    		TwitterUtil.followUser(accountId);
    	}
    	TalkerLogic.signupFromService(serviceType, session, screenName, 
    			userEmail, verifyCode, accountId);
    	
    	renderText("ok");
    }

    private static void validateTalker(TalkerBean talker) {
		if (!validation.hasError("talker.userName")) {
			boolean nameNotExists = !ApplicationDAO.isURLNameExists(talker.getUserName());
			validation.isTrue(nameNotExists).message("username.exists");
		}
		if (!validation.hasError("talker.email")) {
			TalkerBean otherTalker = TalkerDAO.getByEmail(talker.getEmail());
			validation.isTrue(otherTalker == null).message("email.exists");
		}
		
		// Validation for old fields (used earlier) - might be useful later
//		Date dateOfBirth = CommonUtil.parseDate(talker.getDobMonth(), talker.getDobDay(), talker.getDobYear());
//		if (dateOfBirth != null) {
//			validation.past(dateOfBirth);
//			talker.setDob(dateOfBirth);
//		}
//		else {
//			validation.required(dateOfBirth).message("Date of Birth is incorrect");
//		}
		
//		if (!validation.hasError("talker.password")) {
//			validation.isTrue(talker.getPassword().equals(talker.getConfirmPassword())).message("password.different");
//		}
		
//		String imService = talker.getIm();
//        String imUsername = talker.getImUsername();
//        if (!imService.isEmpty() && !imUsername.trim().isEmpty()) {
//        	IMAccountBean imAccount = new IMAccountBean(imUsername, imService);
//        	TalkerBean otherTalker = TalkerDAO.getByIMAccount(imAccount);
//        	validation.isTrue(otherTalker == null).message("imaccount.exists");
//        }
	}
    public static boolean registerUser(@Valid TalkerBean talker) {
    	
		validateTalker(talker);
        if (validation.hasErrors()) {
          
            return false;
        }
        
        TalkerLogic.prepareTalkerForSignup(talker);
              
        boolean okSave = TalkerDAO.save(talker);
        if (!okSave) {
        	
        	Logger.error("Error during signup. User: "+talker.getEmail());      	
        	
        	return false;
		}
        Logger.error("successfull signup of User: "+talker.getEmail());
        return true;
     
    }
	/* ----------------- Contact Us ------------------------- */
	public static void contactus() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		render(talker);
    }
    public static void sendContactEmail(@Email String email, String subject, String message) {
    	validation.required(email).message("Email is required");
    	validation.required(message).message("Message is required");
    	if (validation.hasErrors()) {
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
    
    /* -------------------- Email saving (for notifications & updates) ------------------- */
    public static void updatesEmail() {
    	render();
    }
    public static void saveUpdatesEmail(@Required @Email String email) {
    	if (validation.hasErrors()) {
    		validation.keep();
    		updatesEmail();
            return;
        }
    	ApplicationDAO.saveEmail(email);
    	
    	flash.success("ok");
    	updatesEmail();
    }
    
    /* ---------- Other useful methods ------------- */
    /**
	 * Redirects parent page to given url, is used during OAuth 
	 * @param url
	 */
	public static void redirectPage(String url) {
		render(url);
	}
    
	public static void tosAccept() {
		render();
	}
	
    public static void suspendedAccount() {
    	render();
    }
    
    public static void verifyEmail(String verifyCode) throws Throwable {
		notFoundIfNull(verifyCode);
		TalkerBean talker = TalkerDAO.getByVerifyCode(verifyCode);
		notFoundIfNull(talker);
		
		if (verifyCode.equals(talker.getVerifyCode())) {
			//primary email
			talker.setVerifyCode(null);
		}
		else {
			//clear verify code for non-primary email
			EmailBean emailBean = talker.findNonPrimaryEmail(null, verifyCode);
			emailBean.setVerifyCode(null);
		}
		
		if (Security.isConnected()) {
			CommonUtil.updateTalker(talker, session);
			if (talker.isProf()) {
				Profile.edit(true);
			}
			else {
				Profile.healthDetails(true);
			}

		}
		else {
			TalkerDAO.updateTalker(talker);
			
			//not-authenticated users we redirect to special login page
			flash.put("verifiedEmail", true);
			Secure.login();
		}
	}
    
    public static void addToWaitingList(@Required String community, @Required @Email String email) {
    	if (validation.hasErrors()) {
    		renderText("Error: Please input correct health community and email");
            return;
        }
    	
    	ApplicationDAO.addToWaitingList(community, email);
    	
    	//send welcome email
    	Map<String, String> vars = new HashMap<String, String>();
    	String parsedUsername = email.substring(0, email.indexOf("@"));
		vars.put("username", parsedUsername);
		EmailUtil.sendEmail(EmailTemplate.WELCOME_WAITINGLIST, email, vars, null, false);
    	
    	renderText("ok");
    }
}
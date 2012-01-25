package controllers;

import java.awt.Font;
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
import org.jboss.netty.handler.codec.http.HttpHeaders;

import controllers.LoggerController;

import logic.TalkerLogic;
import models.DiseaseBean;
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
import play.cache.Cache;
import play.data.validation.Email;
import play.data.validation.Error;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.i18n.Messages;
import play.libs.Codec;
import play.libs.Images;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;
import util.EmailUtil;
import util.TwitterUtil;
import util.EmailUtil.EmailTemplate;
import dao.ApplicationDAO;
import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.DiseaseDAO;
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
    	} else {
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
    	//params.flash();
    	String remoteAddress = request.remoteAddress;
    	int duration = -6;
    	//prepare additional settings for FB or Twitter
    	String from = flash.get("from");
    	Map<String, String> additionalSettings = null;
    	List<DiseaseBean> diseaseList = DiseaseDAO.getDeiseaseList();

    	if (from != null) {
    		ServiceType type = ServiceAccountBean.parseServiceType(from);
    		additionalSettings = ServiceAccountBean.settingsNamesByType(type);
    	}
    	if(!ApplicationDAO.isIpUsed(remoteAddress,duration)){
    		flash("captcha", "true");
    	}
    	String randomID = Codec.UUID();
    	render(additionalSettings,randomID,diseaseList);
    }
    
    public static void register(@Valid TalkerBean talker,String code, String randomID) {
    	
    	/**
    	 * Added ip address verification code. 
    	 */
    	int duration = -6;
    	String remoteAddress = request.remoteAddress;
    	
    	if(!ApplicationDAO.isIpUsed(remoteAddress,duration)){
    		validation.equals( code, Cache.get(randomID)).message("Invalid code. Please type it again");
    	}
    	
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
		} else {
			/* Date : 16 Aug 2011
    		 * Added code to save user IP address for ip validation while registration
    		 * Using addUserIp method in ApplicationDAO class
    		 */
    		ApplicationDAO.addUserIp(remoteAddress);
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
    	
    	boolean nameNotExists = false;
    	if (!validation.hasError("talker.userName")) {
    		nameNotExists = !ApplicationDAO.isURLNameExists(talker.getUserName());
			validation.isTrue(nameNotExists).message("username.exists");
		}
		if (!validation.hasError("talker.email")) {
			TalkerBean otherTalker = TalkerDAO.getByEmail(talker.getEmail());
			validation.isTrue(otherTalker == null).message("email.exists");
		}
		 
    	if(talker.getCategory() == null){
			validation.required(talker.getCategory()).message("category.notselected");
		} else if(talker.getCategory().trim().equals("")) {
			validation.required(talker.getCategory()).message("category.notselected");
		} else if(talker.getCategory().trim().equals("select")) {
			nameNotExists = true;
			validation.isTrue(nameNotExists).message("category.notselected");
		}

    	if(talker.getConnection() == null){
    		validation.required(talker.getConnection()).message("connection.notselected");
    	} else if(talker.getConnection().trim().equals("")) {
			validation.required(talker.getConnection()).message("connection.notselected");
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
		
		if(talker == null){
			talker = TalkerDAO.getByOldVerifyCode(verifyCode);
			notFoundIfNull(talker);
			if (verifyCode.equals(talker.getOldVerifyCode())) {
				if (Security.isConnected()) {
					CommonUtil.updateTalker(talker, session);
					if (talker.isProf()) {
						Profile.edit(true);
					}
					else {
						Profile.healthDetails(true);
					}
				} else {
					TalkerDAO.updateTalker(talker);
					//not-authenticated users we redirect to special login page
					flash.put("verifiedEmail", true);
					Secure.login();
				}
			}else{
				notFound();
			}
		}else{
			if (verifyCode.equals(talker.getVerifyCode())) {
				//primary email
				talker.setVerifyCode(null);
				System.out.println("Setting Old: " + verifyCode);
				talker.setOldVerifyCode(verifyCode);
				System.out.println("Set Old: " + talker.getOldVerifyCode());
			} else {
				//clear verify code for non-primary email
				EmailBean emailBean = talker.findNonPrimaryEmail(null, verifyCode);
				emailBean.setVerifyCode(null);
				talker.setOldVerifyCode(verifyCode);
			}
				
			if (Security.isConnected()) {
				CommonUtil.updateTalker(talker, session);
				if (talker.isProf()) {
					Profile.edit(true);
				}
				else {
					Profile.healthDetails(true);
				}
			} else {
				TalkerDAO.updateTalker(talker);
				//not-authenticated users we redirect to special login page
				flash.put("verifiedEmail", true);
				Secure.login();
			}
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
    

    /* ----------------- Signup Newsletter ------------------------- */
    public static void newsletter_signup() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		boolean newsLetterFlag = false;
		String email = null;
		if(talker != null){
			email = talker.getEmail();
			newsLetterFlag = ApplicationDAO.isEmailExists(talker.getEmail());
		}
		render(talker,email,newsLetterFlag);
    }

    /*	Date : 24 Jun 2011
	 *	Updated signup to newsletter feature.
	 * */
    public static void subscribeNewsletter(@Email String email) {
    	validation.required(email).message("Email is required");
    	if (validation.hasErrors()) {
    	    params.flash();
            Error error = validation.errors().get(0);
			renderText("Error:" + error.message());
        }else if(ApplicationDAO.isEmailExists(email)){
        	params.flash();
        	/*Date : 24 Jun 2011
        	 * Sent success message to user if already signed up. This will not add duplicate record in database.
        	 * */
        	//renderText("Error:" + Messages.get("email.exists"));
        	/* Date : 27 Jun 2011
        	 * send welcome Newsletter email 
        	 * Also Added user name extracted from the email of user is not registred
        	 * */
        	Map<String, String> vars = new HashMap<String, String>();
        	String parsedUsername = email.substring(0, email.indexOf("@"));
    		vars.put("username", parsedUsername);
        	EmailUtil.sendEmail(EmailTemplate.WELCOME_NEWSLETTER, email, vars, null, false);
        	renderText("Thank you for subscribing!");
        }else{
        	params.flash();
	    	ApplicationDAO.addToNewsLetter(email);
	    	Map<String, String> vars = new HashMap<String, String>();
        	String parsedUsername = email.substring(0, email.indexOf("@"));
    		vars.put("username", parsedUsername);
	    	EmailUtil.sendEmail(EmailTemplate.WELCOME_NEWSLETTER, email, vars, null, false);
	    	renderText("Thank you for subscribing!");
        }
    }
    
    public static void postError(){
    	String path = request.headers.get("referer").value();
    	String remoteAddress = request.remoteAddress;
    	Date date = request.date;
    	String moredetails = "Error 404: Page not found<br/><br/> There is page not found error occure on site.<br/>Path : " 
			+ path + "<br/>Remote Address : " + remoteAddress + "<br/> Request Date : " 
			+ date.toString() + "<br/><br/>";

    	TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
    	if (currentTalker != null) {
    		moredetails += "User Online : " +  currentTalker.getUserName() + "<br/>";
    		moredetails += "User Email : " +  currentTalker.getEmail() + "<br/>";
    		moredetails += "User Url : " +  CommonUtil.generateAbsoluteURL("ViewDispatcher.view","name",currentTalker.getAnonymousName()) + "<br/>";
    	}
    	Map<String, String> vars = new HashMap<String, String>();
    	vars.put("other_talker", "admin");
    	vars.put("message_text", moredetails);
    	EmailUtil.sendEmail(EmailTemplate.NOTIFICATION_DIRECT_MESSAGE, EmailUtil.SUPPORT_EMAIL, vars, null, false);
    	renderText("We have sent this error to admin");
    }
    
    public static void captcha(String id) {
        Images.Captcha captcha = Images.captcha();
        java.awt.Font font = new Font(java.awt.Font.SERIF , 0, 40);
        List list = new ArrayList<java.awt.Font>();
        list.add(font);
        captcha.fonts = list ;
        String code = captcha.getText("#0C68B3");
        Cache.set(id, code);
        renderBinary(captcha);
    }
}

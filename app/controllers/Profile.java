package controllers;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImageFilter;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.imageio.ImageIO;

import logic.TalkerLogic;
import logic.TopicLogic;
import models.DiseaseBean;
import models.DiseaseBean.DiseaseQuestion;
import models.EmailBean;
import models.HealthItemBean;
import models.IMAccountBean;
import models.LanguageBean;
import models.PrivacySetting;
import models.PrivacySetting.PrivacyType;
import models.PrivacySetting.PrivacyValue;
import models.ServiceAccountBean;
import models.TalkerBean;
import models.ServiceAccountBean.ServiceType;
import models.TalkerBean.EmailSetting;
import models.TalkerDiseaseBean;
import models.TopicBean;
import models.actions.UpdateProfileAction;
import models.actions.Action.ActionType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import play.Logger;
import play.Play;
import play.data.validation.Error;
import play.data.validation.Valid;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Router.ActionDefinition;
import play.mvc.With;
import play.templates.JavaExtensions;
import util.CommonUtil;
import util.EmailUtil;
import util.TwitterUtil;
import util.EmailUtil.EmailTemplate;
import util.ImageUtil;
import util.NotificationUtils;
import dao.ActionDAO;
import dao.ApplicationDAO;
import dao.DiseaseDAO;
import dao.HealthItemDAO;
import dao.TalkerDAO;
import dao.TalkerDiseaseDAO;
import dao.ConversationDAO;
import dao.TopicDAO;

/**
 * Different profile related actions - profile info, health, notifications, privacy
 */
@With(Secure.class)
public class Profile extends Controller {

    public static void edit(boolean verifiedEmail) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
		TalkerLogic.preloadTalkerInfo(talker, "profile");
		
    	render(talker, verifiedEmail);
    }
	
	public static void save(@Valid TalkerBean talker) {
		TalkerBean oldTalker = CommonUtil.loadCachedTalker(session);
		
//		System.out.println("------");
//		System.out.println(talker.getLanguages());
//		System.out.println("xxxxxxx");
		
		//------- validate
		String oldUserName = oldTalker.getUserName();
		if (!oldUserName.equals(talker.getUserName())) {
			boolean nameNotExists = !ApplicationDAO.isURLNameExists(talker.getUserName());
			validation.isTrue(nameNotExists).message("username.exists");
		}
		if (validation.hasErrors()) {
			Error error = validation.errors().get(0);
			renderText("Error:"+error.message());
            return;
        }
		
		//------- parse all info
		if (talker.getKeywords() != null) {
			talker.setKeywords(CommonUtil.parseCommaSerapatedList(talker.getKeywords().get(0), "Keywords (please separate by commas)"));
		}
		//Default '-1' (because 0 is possible value)
		int childrenNum = -1;
		try {
			childrenNum = Integer.parseInt(talker.getChildrenNumStr());
		} catch (Exception e) {}
		talker.setChildrenNum(childrenNum);
		
		Date dateOfBirth = CommonUtil.parseDate(talker.getDobMonth(), talker.getDobDay(), talker.getDobYear());
		talker.setDob(dateOfBirth);
		
		//------- save actions
		if (!StringUtils.equals(oldTalker.getBio(), talker.getBio())) {
			ActionDAO.saveAction(new UpdateProfileAction(oldTalker, ActionType.UPDATE_BIO));
		}
		ActionDAO.saveAction(new UpdateProfileAction(oldTalker, ActionType.UPDATE_PERSONAL));
		
		
		//------- save updated info to the talker
		oldTalker.setUserName(talker.getUserName());
		oldTalker.setDob(dateOfBirth);
		oldTalker.setGender(talker.getGender());
		oldTalker.setWebpage(talker.getWebpage());
		oldTalker.setBio(talker.getBio());
		oldTalker.setCity(talker.getCity());
		oldTalker.setState(talker.getState());
		oldTalker.setCountry(talker.getCountry());
		oldTalker.setZip(talker.getZip());
		if (oldTalker.isProf()) {
			oldTalker.setProfStatement(talker.getProfStatement());
			
			Map<String, String> profInfo = new HashMap<String, String>();
			//parse "pr_" fields - proffesional fields
			Map<String, String> paramsMap = params.allSimple();
			for (Entry<String, String> param : paramsMap.entrySet()) {
				if (param.getKey().startsWith("pr_")) {
					//profile info parameter
					String value = param.getValue();
					if (value != null && value.equals("(separate by commas if multiple)")) {
						value = null;
					}
					
					profInfo.put(param.getKey().substring(3), value);
				}
			}
			oldTalker.setProfInfo(profInfo);
		}
		else {
			oldTalker.setMaritalStatus(talker.getMaritalStatus());
			oldTalker.setChildrenNum(talker.getChildrenNum());
			oldTalker.setChildrenAges(talker.getChildrenAges());
			oldTalker.setKeywords(talker.getKeywords());	
			
//			oldTalker.setEthnicities(talker.getEthnicities());
//			oldTalker.setReligion(talker.getReligion());
//			oldTalker.setReligionSerious(talker.getReligionSerious());
//			oldTalker.setLanguages(new LinkedHashSet<LanguageBean>(talker.getLanguagesList()));
		}
		
		CommonUtil.updateTalker(oldTalker, session);
		if (!oldUserName.equals(talker.getUserName())) {
			session.put("username", talker.getUserName());
			ApplicationDAO.createURLName(talker.getUserName(), true);
		}
		
		renderText("ok");
	}
	
	/**
	 * Back-end for updating profile on the right side of the Home page
	 * 
	 * @param name
	 * @param newValue
	 */
	public static void updateProfile(String name, String newValue) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);

		if (name.equals("userName")) {
			String oldUserName = talker.getUserName();
			if (!oldUserName.equals(newValue)) {
				boolean nameNotExists = !ApplicationDAO.isURLNameExists(newValue);
				validation.isTrue(nameNotExists).message("username.exists");
			}
			if (validation.hasErrors()) {
				Error error = validation.errors().get(0);
				renderText("Error:"+error.message());
	            return;
	        }
			
			talker.setUserName(newValue);
			CommonUtil.updateTalker(talker, session);
			if (!oldUserName.equals(newValue)) {
				session.put("username", talker.getUserName());
				ApplicationDAO.createURLName(talker.getUserName(), true);
			}
		}
		else if (name.equals("password")) {
			talker.setPassword(CommonUtil.hashPassword(newValue));
			TalkerDAO.updateTalker(talker);
		}
		else if (name.equals("email")) {
			validation.email(newValue);
			if (validation.hasError("newValue")) {
				renderText("Error:"+validation.error("newValue").message());
				return;
			}
			TalkerBean otherTalker = TalkerDAO.getByEmail(newValue);
			if (otherTalker != null) {
				renderText("Error:"+Messages.get("email.exists"));
				return;
			}
			
			talker.setEmail(newValue);
			talker.setVerifyCode(CommonUtil.generateVerifyCode());
			CommonUtil.updateTalker(talker, session);
			
			//send verification email
			Map<String, String> vars = new HashMap<String, String>();
			vars.put("username", talker.getUserName());
			vars.put("verify_code", talker.getVerifyCode());
			EmailUtil.sendEmail(EmailTemplate.VERIFICATION, talker.getEmail(), vars, null, false);
		}
		else if (name.equals("twittersettings")) {
			ServiceAccountBean twitterAccount = talker.serviceAccountByType(ServiceType.TWITTER);
			if (twitterAccount != null) {
				twitterAccount.parseSettingsFromParams(params.allSimple());
				CommonUtil.updateTalker(talker, session);
				
				if (twitterAccount.isTrue("FOLLOW")) {
					//follow TAH by this user
					TwitterUtil.followTAH(twitterAccount);
				}
			}
		}
		else if (name.equals("facebooksettings")) {
			ServiceAccountBean fbAccount = talker.serviceAccountByType(ServiceType.FACEBOOK);
			if (fbAccount != null) {
				fbAccount.parseSettingsFromParams(params.allSimple());
				CommonUtil.updateTalker(talker, session);
			}
		}
		
		renderText("ok");
	}
	
	
	/**
	 * Change connection of authenticated talker
	 * @param value
	 */
	public static void changeConnection(String value) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		talker.setConnection(value);
		talker.setConnectionVerified(false);
		CommonUtil.updateTalker(talker, session);
		
		if (talker.isProf()) {
			session.put("prof", "true");
		}
		else {
			session.remove("prof");
		}
		renderText("ok");
	}
	
	public static void changeInsuranceAccepted(List<String> insuranceAccepted) {
		flash.put("currentForm", "changeInsuranceForm");
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		talker.setInsuranceAccepted(insuranceAccepted);
		CommonUtil.updateTalker(talker, session);
		
		renderText("ok");
	}
	
	/* -------------- Image ------------------------ */
	public static void image() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		String userName = talker.getUserName();
		render(userName);
	}
	
	/**
	 * Delete current or upload new image
	 * @param submitAction 'Remove current image' or 'Upload'
	 */
	public static void uploadImage(String submitAction, File imageFile) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		if ("Remove current image".equals(submitAction)) {
			TalkerDAO.updateTalkerImage(talker, null);
		}
		else if (imageFile != null) {
			try {
				if (imageFile.length() < 10000) {
					//less then 10kb
					TalkerDAO.updateTalkerImage(talker, FileUtils.readFileToByteArray(imageFile));
				}
				else {
					BufferedImage bsrc = ImageIO.read(imageFile);
					ByteArrayOutputStream baos = ImageUtil.createThumbnail(bsrc);
		        	TalkerDAO.updateTalkerImage(talker, baos.toByteArray());
				}
			} catch (IOException e) {
				Logger.error(e, "Error converting image!");
			}
		}
		
		image();
        return;
	}
	
	/* -------------- Preferences (Privacy)  ------------------------ */
	public static void preferences() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		TalkerLogic.preloadTalkerInfo(talker, "privacy");
		
		//user just needs to view their Privacy Settings page for ProfileCompletion
		//TODO: rename 'hiddenHelps' to some common name - we can put there all actions & make tag for helps?
		if (!talker.getHiddenHelps().contains("privacyViewed")) {
			talker.getHiddenHelps().add("privacyViewed");
			CommonUtil.updateTalker(talker, session);
		}
		
		render(talker);
	}
	
	public static void preferencesSave() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		Map<String, String> paramsMap = params.allSimple();
		Set<PrivacySetting> privacySettings = new HashSet<PrivacySetting>();
		for (String paramName : paramsMap.keySet()) {
			if (paramName.startsWith("privacy_")) {
				PrivacyType type = PrivacyType.valueOf(paramName.substring(8));
				PrivacyValue value = PrivacyValue.valueOf(paramsMap.get(paramName));
				PrivacySetting privacySetting = new PrivacySetting(type, value);
				privacySettings.add(privacySetting);
			}
		}
		talker.setPrivacySettings(privacySettings);
		
		TalkerDAO.updateTalker(talker);
		renderText("ok");
	}
	
	/* -------------- IM Notifications ------------------------ */
	public static void notifications() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		TalkerLogic.preloadTalkerInfo(talker);
		
		//check possible parameters after adding Twitter/Facebook accounts
		String error = params.get("err");
		if (error != null) {
			flash.put("err", "Sorry, this account is already connected.");
		}
		flash.put("from", params.get("from"));
		
		render(talker);
	}
	
	public static void notificationsSave(TalkerBean talker, String otherCTypes) {
		TalkerBean sessionTalker = CommonUtil.loadCachedTalker(session);
		
		if (talker == null) {
			//save defaults
			talker = new TalkerBean();
		}
		sessionTalker.setNfreq(talker.getNfreq());
		sessionTalker.setNtime(talker.getNtime());
		sessionTalker.setCtype(talker.getCtype());
		sessionTalker.setOtherCtype(CommonUtil.parseCommaSerapatedList(otherCTypes, "Other (please separate by commas)"));
		
		TalkerDAO.updateTalker(sessionTalker);
		renderText("ok");
	}
	
	/**
	 * @param imUserName Username of IM account
	 * @param imService 'YahooIM'/'GoogleTalk'/'WindowLive'
	 */
	public static void addIMAccount(String imUserName, String imService) {
    	IMAccountBean imAccount = new IMAccountBean(imUserName, imService);
    	//check if such IM account exists
    	TalkerBean otherTalker = TalkerDAO.getByIMAccount(imAccount);
    	if (otherTalker != null) {
			renderText(Messages.get("imaccount.exists"));
			return;
		}
        
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		talker.getImAccounts().add(imAccount);
		CommonUtil.updateTalker(talker, session);
		
		NotificationUtils.sendIMInvitation(imAccount);
		
		IMAccountBean _imAccount = imAccount;
		render("tags/profile/profileNotificationIM.html", _imAccount);
	}
	
	/**
	 * @param imId - imUsername and imService separated by "|"
	 */
	public static void deleteIMAccount(String imId) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		String[] imArray = imId.split("\\|");
		IMAccountBean imAccount = new IMAccountBean(imArray[0], imArray[1]);
		talker.getImAccounts().remove(imAccount);
		CommonUtil.updateTalker(talker, session);
		
		renderText("Ok");
	}
	
	/* ------------------ Email notifications ------------------ */
	public static void emailSettingsSave(TalkerBean talker) {
		TalkerBean sessionTalker = CommonUtil.loadCachedTalker(session);

		Map<String, String> paramsMap = params.allSimple();
		EnumSet<EmailSetting> emailSettings = EnumSet.noneOf(EmailSetting.class);
		for (String paramName : paramsMap.keySet()) {
			//try to parse all parameters to EmailSetting enum
			try {
				EmailSetting emailSetting = EmailSetting.valueOf(paramName);
				emailSettings.add(emailSetting);
			}
			catch (IllegalArgumentException iae) {}
		}
		sessionTalker.setEmailSettings(emailSettings);
		
		if (talker == null) {
			sessionTalker.setNewsletter(false);
		}
		else {
			sessionTalker.setNewsletter(talker.isNewsletter());
		}
		
		CommonUtil.updateTalker(sessionTalker, session);
		renderText("ok");
	}
	
	/**
	 * Changes primary email of authenticated talker
	 * @param newPrimaryEmail
	 */
	public static void makePrimaryEmail(String newPrimaryEmail) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		EmailBean newPrimaryEmailBean = talker.findNonPrimaryEmail(newPrimaryEmail, null);
		notFoundIfNull(newPrimaryEmailBean);
		
		//delete this email from non-primary
		talker.getEmails().remove(newPrimaryEmailBean);
		//make old primary as non-primary
		talker.getEmails().add(new EmailBean(talker.getEmail(), talker.getVerifyCode()));
		//set new primary
		talker.setEmail(newPrimaryEmailBean.getValue());
		talker.setVerifyCode(newPrimaryEmailBean.getVerifyCode());
		
		CommonUtil.updateTalker(talker, session);
		notifications();
	}
	
	/**
	 * Adds new not-primary email for authenticated talker
	 * @param newEmail
	 */
	public static void addEmail(String newEmail) {
		validation.email(newEmail);
		if (validation.hasError("newEmail")) {
			renderText(validation.error("newEmail").message());
			return;
		}
		TalkerBean otherTalker = TalkerDAO.getByEmail(newEmail);
		if (otherTalker != null) {
			renderText(Messages.get("email.exists"));
			return;
		}
		
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		EmailBean email = new EmailBean(newEmail, CommonUtil.generateVerifyCode());
		talker.getEmails().add(email);
		CommonUtil.updateTalker(talker, session);
		
		//send verification email
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("username", talker.getUserName());
		vars.put("verify_code", email.getVerifyCode());
		EmailUtil.sendEmail(EmailTemplate.VERIFICATION, email.getValue(), vars, null, false);
		
		EmailBean _email = email;
		render("tags/profile/profileNotificationEmail.html", _email);
	}
	
	/**
	 * Deletes given non-primary email
	 * @param email
	 */
	public static void deleteEmail(String email) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		talker.getEmails().remove(new EmailBean(email, null));
		CommonUtil.updateTalker(talker, session);
		
		renderJSON("{\"result\" : \"ok\"}");
	}
	
	/* ------------- IM/Twitter/FB notifications ----------------*/
	public static void serviceSettingsSave() {
		TalkerBean sessionTalker = CommonUtil.loadCachedTalker(session);

		Set<ServiceAccountBean> serviceAccounts = sessionTalker.getServiceAccounts();
		for (ServiceAccountBean serviceAccount : serviceAccounts) {
			serviceAccount.parseSettingsFromParams(params.allSimple());
		}
		
		boolean imNotify = false;
		if (params.get("im_notify") != null) {
			imNotify = true;
		}
		sessionTalker.setImNotify(imNotify);
		
		CommonUtil.updateTalker(sessionTalker, session);
		renderText("ok");
	}
	
	/**
	 * Delete Twitter or Facebook account
	 * @param type
	 */
	public static void deleteServiceAccount(String type) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		ServiceType serviceType = ServiceAccountBean.parseServiceType(type); 
		if (serviceType != null) {
			ServiceAccountBean serviceAccount = talker.serviceAccountByType(serviceType);
			if (serviceAccount != null) {
				talker.getServiceAccounts().remove(serviceAccount);
				CommonUtil.updateTalker(talker, session);
			}
		}
		renderText("ok");
	}
	
	public static void changePassword(String curPassword, String newPassword, String confirmPassword) {
		flash.put("currentForm", "changePasswordForm");
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		String hashedPassword = CommonUtil.hashPassword(curPassword);
		validation.isTrue(talker.getPassword().equals(hashedPassword)).message("password.currentbad");
		validation.isTrue(newPassword != null && newPassword.equals(confirmPassword)).message("password.different");
		if (validation.hasErrors()) {
			flash.success("");
			validation.keep();
			notifications();
            return;
        }
		
		talker.setPassword(CommonUtil.hashPassword(newPassword));
		TalkerDAO.updateTalker(talker);
		
		flash.success("ok");
		notifications();
	}
	
	/* ------------- Health Info -------------------------- */
	public static void healthDetails() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		TalkerLogic.preloadTalkerInfo(talker, "health");
		
		//For now we have only one disease - Breast Cancer
		final String diseaseName = "Breast Cancer";
		DiseaseBean disease = DiseaseDAO.getByName(diseaseName);
		TalkerDiseaseBean talkerDisease = TalkerDiseaseDAO.getByTalkerId(talker.getId());
		
		//Load all healthItems for this disease
		Map<String, HealthItemBean> healthItemsMap = new HashMap<String, HealthItemBean>();
		for (String itemName : new String[] {"symptoms", "tests", 
				"procedures", "treatments", "sideeffects"}) {
			HealthItemBean healthItem = HealthItemDAO.getHealthItemByName(itemName, diseaseName);
			healthItemsMap.put(itemName, healthItem);
		}
		
		render(talker, talkerDisease, disease, healthItemsMap);
	}

	public static void saveHealthDetails(TalkerDiseaseBean talkerDisease, String section) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		final String diseaseName = "Breast Cancer";
		DiseaseBean disease = DiseaseDAO.getByName(diseaseName);
		
		parseHealthQuestions(disease, talkerDisease);
		parseHealthItems(talkerDisease);
		talkerDisease.setUid(talker.getId());
		
		Date symptomDate = CommonUtil.parseDate(talkerDisease.getSymptomMonth(), 1, talkerDisease.getSymptomYear());
		talkerDisease.setSymptomDate(symptomDate);
		Date diagnoseDate = CommonUtil.parseDate(talkerDisease.getDiagnoseMonth(), 1, talkerDisease.getDiagnoseYear());
		talkerDisease.setDiagnoseDate(diagnoseDate);
		
		//Automatically follow topics based on HealthInfo
		List<TopicBean> recommendedTopics = TalkerLogic.getTopicsByHealthInfo(talkerDisease);
		if (!recommendedTopics.isEmpty()) {
			for (TopicBean topic : recommendedTopics) {
				talker.getFollowingTopicsList().add(topic);
			}
			CommonUtil.updateTalker(talker, session);
		}
		
		//Save or update
		TalkerDiseaseDAO.saveTalkerDisease(talkerDisease);
		
		ActionDAO.saveAction(new UpdateProfileAction(talker, ActionType.UPDATE_HEALTH));
		renderText("ok");
	}
	
	/**
	 * Parse answers to given disease questions from params
	 * @param disease Disease which questions should be parsed
	 * @param talkerDisease Object to save parsed health info
	 */
	private static void parseHealthQuestions(DiseaseBean disease, TalkerDiseaseBean talkerDisease) {
		Map<String, String[]> paramsMap = params.all();
		
		Map<String, List<String>> healthInfo = new HashMap<String, List<String>>();
		for (DiseaseQuestion question : disease.getQuestions()) {
			String[] values = paramsMap.get(question.getName());
			if (values != null && values[0].length() != 0) {
				healthInfo.put(question.getName(), Arrays.asList(values));
			}
		}
		talkerDisease.setHealthInfo(healthInfo);
	}
	
	/**
	 * Get selected health items from params
	 * @param talkerDisease Object to save parsed health items
	 */
	private static void parseHealthItems(TalkerDiseaseBean talkerDisease) {
		Map<String, String> paramsMap = params.allSimple();
		
		Set<String> healthItems = new HashSet<String>();
		Map<String, List<String>> otherHealthItems = new HashMap<String, List<String>>();
		for (String paramName : paramsMap.keySet()) {
			//Health item parameter name: 'healthitemID'
			if (paramName.startsWith("healthitem")) {
				String id = paramName.substring(10);
				healthItems.add(id);
			}
			
			//Other health items
			if (paramName.startsWith("other")) {
				String id = paramName.substring(5);
				String otherValue = paramsMap.get(paramName);
				
				List<String> otherItems = CommonUtil.parseCommaSerapatedList(otherValue, "Other (please separate by commas)");
				if (!otherItems.isEmpty()) {
					otherHealthItems.put(id, otherItems);
				}
			}
		}
		
		talkerDisease.setHealthItems(healthItems);
		talkerDisease.setOtherHealthItems(otherHealthItems);
	}
	
	public static void resendVerificationEmail(String email) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		String verifyCode = talker.getVerifyCode();
		if (!talker.getEmail().equals(email)) {
			//resend for non-primary email
			EmailBean nonPrimaryEmail = talker.findNonPrimaryEmail(email, null);
			notFoundIfNull(nonPrimaryEmail);
			
			verifyCode = nonPrimaryEmail.getVerifyCode();
		}
		
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("username", talker.getUserName());
		vars.put("verify_code", verifyCode);
		EmailUtil.sendEmail(EmailTemplate.VERIFICATION, email, vars, null, false);
		
		renderText("Ok! Email sent.");
	}
	
	public static void deactivateAccount() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		talker.setOriginalUserName(talker.getUserName());
		talker.setUserName(talker.getAnonymousName());
		talker.setDeactivated(true);
		TalkerDAO.updateTalker(talker);
		
		try {
			Secure.logout();
		} catch (Throwable e) {
			Logger.error("Logout error", e);
		}
	}
	
	public static void hideHelpInfo(String type) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		talker.getHiddenHelps().add(type);
		CommonUtil.updateTalker(talker, session);
		renderText("ok");
	}
}

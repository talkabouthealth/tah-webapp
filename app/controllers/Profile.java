package controllers;

import java.awt.Graphics2D;
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
import models.TalkerBean;
import models.TalkerBean.EmailSetting;
import models.TalkerBean.ProfilePreference;
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
import play.data.validation.Valid;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Router.ActionDefinition;
import play.mvc.With;
import play.ns.nl.captcha.util.ImageUtil;
import play.templates.JavaExtensions;
import util.CommonUtil;
import util.EmailUtil;
import util.EmailUtil.EmailTemplate;
import dao.ActionDAO;
import dao.ApplicationDAO;
import dao.DiseaseDAO;
import dao.HealthItemDAO;
import dao.TalkerDAO;
import dao.TalkerDiseaseDAO;
import dao.ConversationDAO;
import dao.TopicDAO;

@With(Secure.class)
public class Profile extends Controller {

    public static void edit(boolean verifiedEmail) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	
    	//TODO move to one logic method - info is the same
    	talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
    	talker.setActivityList(ActionDAO.load(talker.getId()));
		TalkerLogic.calculateProfileCompletion(talker);
		
    	render(talker, verifiedEmail);
    }
	
	public static void save(@Valid TalkerBean talker) {
		flash.put("currentForm", "editForm");
		TalkerBean oldTalker = CommonUtil.loadCachedTalker(session);
		
		//to have default value we use string param
		String childrenNumStr = talker.getChildrenNumStr();
		if (childrenNumStr == null || childrenNumStr.trim().length() == 0) {
			//default value
			talker.setChildrenNum(-1);
		}
		else {
			int childrenNum = -1;
			try {
				childrenNum = Integer.parseInt(childrenNumStr);
			} catch (NumberFormatException nfe) {}
			talker.setChildrenNum(childrenNum);
		}
		
		String oldUserName = oldTalker.getUserName();
		if (!oldUserName.equals(talker.getUserName())) {
			boolean nameNotExists = !ApplicationDAO.isURLNameExists(talker.getUserName());
			validation.isTrue(nameNotExists).message("username.exists");
		}
		
		if (talker.getKeywords() != null) {
			talker.setKeywords(CommonUtil.parseCommaSerapatedList(talker.getKeywords().get(0), "Keywords (please separate by commas)"));
		}
		
		Date dateOfBirth = CommonUtil.parseDate(talker.getDobMonth(), talker.getDobDay(), talker.getDobYear());
		talker.setDob(dateOfBirth);
//		validation.required(dateOfBirth).message("Please input correct Birth Date");
		
		if(validation.hasErrors()) {
			//prepare info for displaying page
			//TODO: it's not good
			talker.setProfInfo(oldTalker.getProfInfo());
			talker.setHiddenHelps(oldTalker.getHiddenHelps());
			talker.saveProfilePreferences(oldTalker.loadProfilePreferences());
			talker.setFollowingTopicsList(oldTalker.getFollowingTopicsList());
			talker.setThankYouList(oldTalker.getThankYouList());
			talker.setFollowingList(oldTalker.getFollowingList());
			talker.setFollowerList(TalkerDAO.loadFollowers(oldTalker.getId()));
	    	talker.setActivityList(ActionDAO.load(oldTalker.getId()));
			TalkerLogic.calculateProfileCompletion(talker);
			
			flash.success("");
			render("@edit", talker);
            return;
        }
		
		if (TalkerBean.PROFESSIONAL_CONNECTIONS_LIST.contains(oldTalker.getConnection())) {
			//TODO: username, birthday, etc.
			
			Map<String, String> profInfo = new HashMap<String, String>();
			
			//parse "pr_"
			Map<String, String> paramsMap = params.allSimple();
			for (Entry<String, String> param : paramsMap.entrySet()) {
				if (param.getKey().startsWith("pr_")) {
					//profile info parameter
					String value = param.getValue();
					if (value != null && value.equals("(separate by commas if multiple)")) {
						value = "";
					}
					profInfo.put(param.getKey().substring(3), value);
				}
			}
			oldTalker.setProfInfo(profInfo);
			
			CommonUtil.updateTalker(oldTalker, session);
		}
		else {
			if (!StringUtils.equals(oldTalker.getBio(), talker.getBio())) {
				ActionDAO.saveAction(new UpdateProfileAction(oldTalker, ActionType.UPDATE_BIO));
			}
			
			//check if any fields were changed
			if ( !(
					StringUtils.equals(oldTalker.getGender(), talker.getGender()) &&
					StringUtils.equals(oldTalker.getCountry(), talker.getCountry()) &&
					StringUtils.equals(oldTalker.getState(), talker.getState()) &&
					StringUtils.equals(oldTalker.getCity(), talker.getCity()) &&
					StringUtils.equals(oldTalker.getMaritalStatus(), talker.getMaritalStatus()) &&
					oldTalker.getChildrenNum() == oldTalker.getChildrenNum() &&
					oldTalker.getChildrenAges().equals(talker.getChildrenAges()) &&
					StringUtils.equals(oldTalker.getWebpage(), talker.getWebpage()) &&
					oldTalker.getKeywords().equals(talker.getKeywords())
						)) {
				ActionDAO.saveAction(new UpdateProfileAction(oldTalker, ActionType.UPDATE_PERSONAL));
			}
			
			oldTalker.setUserName(talker.getUserName());
//			oldTalker.setEmail(talker.getEmail());
//			oldTalker.setFirstName(talker.getFirstName());
//			oldTalker.setLastName(talker.getLastName());
			oldTalker.setDob(dateOfBirth);
			oldTalker.setGender(talker.getGender());
			oldTalker.setMaritalStatus(talker.getMaritalStatus());
			oldTalker.setConnection(talker.getConnection());
			oldTalker.setCity(talker.getCity());
			oldTalker.setState(talker.getState());
			oldTalker.setCountry(talker.getCountry());
			oldTalker.setChildrenNum(talker.getChildrenNum());
			oldTalker.setZip(talker.getZip());
			oldTalker.setWebpage(talker.getWebpage());
			oldTalker.setBio(talker.getBio());
			oldTalker.setChildrenAges(talker.getChildrenAges());
			oldTalker.setKeywords(talker.getKeywords());
			
//			if (!oldEmail.equals(talker.getEmail())) {
//				//send verification email
//				oldTalker.setVerifyCode(CommonUtil.generateVerifyCode());
//				
//				Map<String, String> vars = new HashMap<String, String>();
//				vars.put("username", oldTalker.getUserName());
//				vars.put("verify_code", oldTalker.getVerifyCode());
//				EmailUtil.sendEmail(EmailTemplate.VERIFICATION, oldTalker.getEmail(), vars, null, false);
//			}
			
			CommonUtil.updateTalker(oldTalker, session);
			
			if (!oldUserName.equals(talker.getUserName())) {
				ApplicationDAO.createURLName(talker.getUserName());
			}
		}
		
//		if (!StringUtils.equals(oldTalker.getConnection(), talker.getConnection())) {
//			//check if new connection is professional and unverify it
//			oldTalker.setConnectionVerified(false);
//		}
		
		
		
		flash.success("ok");
		talker = oldTalker;
		talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
    	talker.setActivityList(ActionDAO.load(talker.getId()));
		TalkerLogic.calculateProfileCompletion(talker);
		render("@edit", talker);
	}
	
	public static void changePassword(String curPassword, String newPassword, String confirmPassword) {
		flash.put("currentForm", "changePasswordForm");
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		String hashedPassword = CommonUtil.hashPassword(curPassword);
		
		validation.isTrue(talker.getPassword().equals(hashedPassword)).message("password.currentbad");
		validation.isTrue(newPassword != null && newPassword.equals(confirmPassword)).message("password.different");
		
		if(validation.hasErrors()) {
			flash.success("");
			render("@edit", talker);
			
            return;
        }
		
		talker.setPassword(CommonUtil.hashPassword(newPassword));
		TalkerDAO.updateTalker(talker);
		
		flash.success("ok");
		render("@edit", talker);
	}
	
	/* -------------- Image ------------------------ */
	public static void image() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		String userName = talker.getUserName();
		
		render(userName);
	}
	
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
					
					int width = 100;
					int height = 100;
					BufferedImage bdest =
					      new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
					Graphics2D g = bdest.createGraphics();
					AffineTransform at =
					      AffineTransform.getScaleInstance((double)width/bsrc.getWidth(),
					          (double)height/bsrc.getHeight());
					g.drawRenderedImage(bsrc,at);
					
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
		        
		        	ImageIO.write(bdest, "GIF", baos);
		        	TalkerDAO.updateTalkerImage(talker, baos.toByteArray());
				}
			} catch (IOException e) {
				Logger.error(e, "Error converting image!");
			}
		}
		
		image();
        return;
	}
	
	/* -------------- Preferences ------------------------ */
	public static void preferences() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		EnumSet<ProfilePreference> profilePreferences = talker.loadProfilePreferences();
		
		//user just needs to view their Privacy Settings page for ProfileCompletion
		//TODO: rename 'hiddenHelps' to some common name - we can put there all actions
		if (!talker.getHiddenHelps().contains("privacyViewed")) {
			talker.getHiddenHelps().add("privacyViewed");
			CommonUtil.updateTalker(talker, session);
		}
		
		talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
    	talker.setActivityList(ActionDAO.load(talker.getId()));
		TalkerLogic.calculateProfileCompletion(talker);
		
		render(talker, profilePreferences);
	}
	
	public static void preferencesSave() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		Map<String, String> paramsMap = params.allSimple();
		EnumSet<ProfilePreference> preferencesSet = EnumSet.noneOf(ProfilePreference.class);
		for (String paramName : paramsMap.keySet()) {
			//try to parse all parameters to ProfilePreference enum
			try {
				ProfilePreference preference = ProfilePreference.valueOf(paramName);
				preferencesSet.add(preference);
			}
			catch (IllegalArgumentException iae) {}
		}
		
		talker.saveProfilePreferences(preferencesSet);
		TalkerDAO.updateTalker(talker);
		
		flash.success("ok");
		preferences();
	}
	
	/* -------------- Notifications ------------------------ */
	public static void notifications() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
    	talker.setActivityList(ActionDAO.load(talker.getId()));
		TalkerLogic.calculateProfileCompletion(talker);
		
		render(talker);
	}
	
	public static void notificationsSave(TalkerBean talker, String otherCTypes) {
		flash.put("currentForm", "notificationsForm");
		TalkerBean sessionTalker = CommonUtil.loadCachedTalker(session);
		
		if (talker == null) {
			//save defaults
			talker = new TalkerBean();
		}
		
		sessionTalker.setNfreq(talker.getNfreq());
		sessionTalker.setNtime(talker.getNtime());
		sessionTalker.setCtype(talker.getCtype());
		
		//parse other convo types field
		otherCTypes = otherCTypes.trim();
		if (!otherCTypes.equals("Other (please separate by commas)")) {
			String[] otherCTypesArray = otherCTypes.split(",");
			//validate and add other ctypes
			List<String> cTypeList = new ArrayList<String>();
			for (String cType : otherCTypesArray) {
				cType = cType.trim();
				if (cType.length() != 0) {
					cTypeList.add(cType);
				}
			}
			
			//add standard ctypes to the list
			if (talker.getCtype() != null) {
				cTypeList.addAll(Arrays.asList(talker.getCtype()));
			}
			
			sessionTalker.setCtype(cTypeList.toArray(new String[]{}));
		}
		
		TalkerDAO.updateTalker(sessionTalker);
		flash.success("ok");
		notifications();
	}
	
	public static void emailSettingsSave(TalkerBean talker) {
		flash.put("currentForm", "emailsettingsForm");
		
		TalkerBean sessionTalker = CommonUtil.loadCachedTalker(session);

		Map<String, String> paramsMap = params.allSimple();
		EnumSet<EmailSetting> emailSettings = EnumSet.noneOf(EmailSetting.class);
		for (String paramName : paramsMap.keySet()) {
			//try to parse all parameters to ProfilePreference enum
			try {
				EmailSetting emailSetting = EmailSetting.valueOf(paramName);
				emailSettings.add(emailSetting);
			}
			catch (IllegalArgumentException iae) {}
		}
		sessionTalker.saveEmailSettings(emailSettings);
		
		if (talker == null) {
			sessionTalker.setNewsletter(false);
		}
		else {
			sessionTalker.setNewsletter(talker.isNewsletter());
		}
		
		CommonUtil.updateTalker(sessionTalker, session);
		flash.success("ok");
		notifications();
	}
	
	public static void makePrimaryEmail(String newPrimaryEmail) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		EmailBean newPrimaryEmailBean = talker.findNonPrimaryEmail(newPrimaryEmail, null);
		notFoundIfNull(newPrimaryEmailBean);
		
		//delete this email from non-primary
		talker.getEmails().remove(newPrimaryEmailBean);
		//make old primary non-primary
		talker.getEmails().add(new EmailBean(talker.getEmail(), talker.getVerifyCode()));
		//set new primary
		talker.setEmail(newPrimaryEmailBean.getValue());
		talker.setVerifyCode(newPrimaryEmailBean.getVerifyCode());
		
		CommonUtil.updateTalker(talker, session);
		notifications();
	}
	
	//ajax methods
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
		
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("username", talker.getUserName());
		vars.put("verify_code", email.getVerifyCode());
		EmailUtil.sendEmail(EmailTemplate.VERIFICATION, email.getValue(), vars, null, false);
		
		
		EmailBean _email = email;
		render("tags/profileNotificationEmail.html", _email);
	}
	
	public static void deleteEmail(String email) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		talker.getEmails().remove(new EmailBean(email, null));
		
		CommonUtil.updateTalker(talker, session);
		
		renderJSON("{\"result\" : \"ok\"}");
	}
	
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
		
		CommonUtil.sendIMInvitation(imAccount);
		
		IMAccountBean _imAccount = imAccount;
		render("tags/profileNotificationIM.html", _imAccount);
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
	
	/* ------------- Health Info -------------------------- */
	public static void healthDetails() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
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
		
		talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
    	talker.setActivityList(ActionDAO.load(talker.getId()));
		TalkerLogic.calculateProfileCompletion(talker);
		
		render(talker, talkerDisease, disease, healthItemsMap);
	}
	
	public static void saveHealthDetails(TalkerDiseaseBean talkerDisease, String section) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
//		for (String key : params.all().keySet()) {
//			System.out.println(key+" : "+Arrays.toString(params.all().get(key)));
//		}
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
		//Let's only have this happen the first time they save their Health Info.
		if (TalkerDiseaseDAO.getByTalkerId(talker.getId()) == null) {
			
			List<TopicBean> recommendedTopics = TalkerLogic.getRecommendedTopics(talkerDisease);
			if (!recommendedTopics.isEmpty()) {
				for (TopicBean topic : recommendedTopics) {
					talker.getFollowingTopicsList().add(topic);
				}
				CommonUtil.updateTalker(talker, session);
			}
		}
		
		//Save or update
		TalkerDiseaseDAO.saveTalkerDisease(talkerDisease);
		
		ActionDAO.saveAction(new UpdateProfileAction(talker, ActionType.UPDATE_HEALTH));
		
		flash.success("ok");
		
		//Strange fix for redirect with "#reference". Discussed here:
		//http://groups.google.com/group/play-framework/browse_thread/thread/93c2ec3e34c20f4e/bf94f63fcb2e529d?lnk=gst&q=addRef#bf94f63fcb2e529d
//		ActionDefinition action = reverse(); {
//			//render("@edit", talker);
//			edit();
//        }
//		redirect(action.addRef("changePasswordForm").toString());
		
		ActionDefinition action = reverse(); 
		{
			healthDetails();
        }
		if (section == null) {
			redirect(action.toString()); 
		}
		else {
			flash.put(section, "Changes saved!");
			
			redirect(action.addRef(section).toString()); 
		}
	}
	
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
	
	public static void verifyEmail(String verifyCode) {
		notFoundIfNull(verifyCode);
		
		TalkerBean talker = TalkerDAO.getByVerifyCode(verifyCode);
		//TODO: better error reply?
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
		CommonUtil.updateTalker(talker, session);
		
		edit(true);
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
		
		//TODO: on registration check 'member' uniqueness?
		String newUserName = CommonUtil.generateDeactivatedUserName(true);
		talker.setOriginalUserName(talker.getUserName());
		talker.setUserName(newUserName);
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

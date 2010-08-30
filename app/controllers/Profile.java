package controllers;

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
import java.util.Set;

import models.DiseaseBean;
import models.DiseaseBean.DiseaseQuestion;
import models.EmailBean;
import models.HealthItemBean;
import models.IMAccountBean;
import models.TalkerBean;
import models.TalkerBean.EmailSetting;
import models.TalkerBean.ProfilePreference;
import models.TalkerDiseaseBean;
import models.actions.UpdateProfileAction;

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
import util.CommonUtil;
import util.EmailUtil;
import util.EmailUtil.EmailTemplate;
import dao.ActivityDAO;
import dao.DiseaseDAO;
import dao.HealthItemDAO;
import dao.TalkerDAO;
import dao.TalkerDiseaseDAO;
import dao.ConversationDAO;

@With(Secure.class)
public class Profile extends Controller {

    public static void edit() {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	render(talker);
    }
	
	public static void save(@Valid TalkerBean talker) {
		flash.put("currentForm", "editForm");
		TalkerBean oldTalker = CommonUtil.loadCachedTalker(session);
		
		String oldUserName = oldTalker.getUserName();
		String oldEmail = oldTalker.getEmail();
		if (!oldUserName.equals(talker.getUserName())) {
			TalkerBean tmpTalker = TalkerDAO.getByUserName(talker.getUserName());
			validation.isTrue(tmpTalker == null).message("username.exists");
		}
		if (!oldEmail.equals(talker.getEmail())) {
			TalkerBean tmpTalker = TalkerDAO.getByEmail(talker.getEmail());
			validation.isTrue(tmpTalker == null).message("email.exists");
		}
		
		talker.setKeywords(CommonUtil.parseCommaSerapatedList(talker.getKeywords().get(0)));
		
		Date dateOfBirth = CommonUtil.parseDate(talker.getDobMonth(), talker.getDobDay(), talker.getDobYear());
		talker.setDob(dateOfBirth);
		validation.required(dateOfBirth).message("Please input correct Birth Date");
		
		if(validation.hasErrors()) {
			flash.success("");
			render("@edit", talker);
            return;
        }
		
		if (!StringUtils.equals(oldTalker.getConnection(), talker.getConnection())) {
			//check if new connection professional and unverify it
			if (TalkerBean.PROFESSIONAL_CONNECTIONS_LIST.contains(talker.getConnection())) {
				oldTalker.setConnectionVerified(false);
			}
		}
		
		if (!StringUtils.equals(oldTalker.getBio(), talker.getBio())) {
			ActivityDAO.saveActivity(new UpdateProfileAction(oldTalker, "BIO"));
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
			ActivityDAO.saveActivity(new UpdateProfileAction(oldTalker, "PERSONAL"));
		}
		
		oldTalker.setUserName(talker.getUserName());
		oldTalker.setEmail(talker.getEmail());
		oldTalker.setDob(dateOfBirth);
		oldTalker.setGender(talker.getGender());
		oldTalker.setMaritalStatus(talker.getMaritalStatus());
		oldTalker.setConnection(talker.getConnection());
		oldTalker.setCity(talker.getCity());
		oldTalker.setState(talker.getState());
		oldTalker.setCountry(talker.getCountry());
		oldTalker.setChildrenNum(talker.getChildrenNum());
		
		oldTalker.setFirstName(talker.getFirstName());
		oldTalker.setLastName(talker.getLastName());
		oldTalker.setZip(talker.getZip());
		oldTalker.setWebpage(talker.getWebpage());
		oldTalker.setBio(talker.getBio());
		oldTalker.setChildrenAges(talker.getChildrenAges());
		oldTalker.setKeywords(talker.getKeywords());
		
		//TODO duplicates functionality? - move email creation to EmailUtil?
		if (!oldEmail.equals(talker.getEmail())) {
			//send verification email
			oldTalker.setVerifyCode(CommonUtil.generateVerifyCode());
			
			Map<String, String> vars = new HashMap<String, String>();
			vars.put("username", oldTalker.getUserName());
			vars.put("verify_code", oldTalker.getVerifyCode());
			EmailUtil.sendEmail(EmailTemplate.VERIFICATION, oldTalker.getEmail(), vars, null, false);
		}
		
		TalkerDAO.updateTalker(oldTalker);
		CommonUtil.updateCachedTalker(session);
		
		flash.success("ok");
		talker = oldTalker;
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
		
		render("@image", userName);
	}
	
	public static void uploadImage(String submitAction, File imageFile) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		if ("Remove current image".equals(submitAction)) {
			TalkerDAO.updateTalkerImage(talker, null);
		}
		else {
	        try {
				TalkerDAO.updateTalkerImage(talker, FileUtils.readFileToByteArray(imageFile));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		image();
        return;
	}
	
	/* -------------- Preferences ------------------------ */
	public static void preferences() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		EnumSet<ProfilePreference> profilePreferences = talker.loadProfilePreferences();
		
		render(profilePreferences);
	}
	
	public static void savePreferences() {
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
		
		render(talker);
	}
	
	public static void saveNotifications(TalkerBean talker, String otherCTypes) {
		flash.put("currentForm", "notificationsForm");
		TalkerBean sessionTalker = CommonUtil.loadCachedTalker(session);
		
		if (talker == null) {
			//save defaults
			talker = new TalkerBean();
		}
		
		//TODO: save as strings in db?
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
	
	public static void saveEmailSettings(TalkerBean talker) {
		flash.put("currentForm", "emailsettingsForm");
		
		TalkerBean sessionTalker = CommonUtil.loadCachedTalker(session);

		//TODO: duplicate? easier?
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
		
		
		TalkerDAO.updateTalker(sessionTalker);
		CommonUtil.updateCachedTalker(session);
		
		flash.success("ok");
		notifications();
	}
	
	public static void makePrimaryEmail(String newPrimaryEmail) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		EmailBean newPrimaryEmailBean = talker.findNonPrimaryEmail(newPrimaryEmail, null);
		notFoundIfNull(newPrimaryEmailBean);
		
		//delete this email from non-primary
		TalkerDAO.deleteEmail(talker, newPrimaryEmailBean);
		
		//make old primary non-primary
		TalkerDAO.saveEmail(talker, new EmailBean(talker.getEmail(), talker.getVerifyCode()));
		
		//set new primary
		talker.setEmail(newPrimaryEmailBean.getValue());
		talker.setVerifyCode(newPrimaryEmailBean.getVerifyCode());
		TalkerDAO.updateTalker(talker);
		
		CommonUtil.updateCachedTalker(session);
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
		TalkerDAO.saveEmail(talker, email);
		
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("username", talker.getUserName());
		vars.put("verify_code", email.getVerifyCode());
		EmailUtil.sendEmail(EmailTemplate.VERIFICATION, email.getValue(), vars, null, false);
		
		CommonUtil.updateCachedTalker(session);
		EmailBean _email = email;
		render("tags/profileNotificationEmail.html", _email);
	}
	
	public static void deleteEmail(String email) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		TalkerDAO.deleteEmail(talker, new EmailBean(email, null));
		
		CommonUtil.updateCachedTalker(session);
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
		
		TalkerDAO.updateTalker(talker);
		CommonUtil.updateCachedTalker(session);
		
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
		
		TalkerDAO.updateTalker(talker);
		CommonUtil.updateCachedTalker(session);
		
		renderText("Ok");
	}
	
	/* ------------- Health Info -------------------------- */
	public static void healthDetails() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		//For now we have only one disease - Breast Cancer
		final String diseaseName = "Breast Cancer";
		DiseaseBean disease = DiseaseDAO.getByName(diseaseName);
//		List<String> typesList = DiseaseDAO.getValuesByDisease("types", diseaseName);
//		List<String> stagesList = DiseaseDAO.getValuesByDisease("stages", diseaseName);

		TalkerDiseaseBean talkerDisease = TalkerDiseaseDAO.getByTalkerId(talker.getId());
		System.out.println(talkerDisease.getOtherHealthItems().get("symptoms"));
		
		//Load all healthItems for this disease
		Map<String, HealthItemBean> healthItemsMap = new HashMap<String, HealthItemBean>();
		for (String itemName : new String[] {"symptoms", "tests", 
				"procedures", "treatments", "sideeffects"}) {
			HealthItemBean healthItem = HealthItemDAO.getHealthItemByName(itemName, diseaseName);
			healthItemsMap.put(itemName, healthItem);
		}
		
		render(talkerDisease, disease, healthItemsMap);
//		render(talkerDisease, typesList, stagesList, healthItemsMap);
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
		
		//Save or update
		TalkerDiseaseDAO.saveTalkerDisease(talkerDisease);
		
		ActivityDAO.saveActivity(new UpdateProfileAction(talker, "HEALTH"));
		
		flash.success("ok");
		
		//TODO strange fix for redirect with "#reference". Discussed here:
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
			flash.put(section, "Changes saved");
			
			redirect(action.addRef(section).toString()); 
		}
	}
	
	private static void parseHealthQuestions(DiseaseBean disease, TalkerDiseaseBean talkerDisease) {
		Map<String, String[]> paramsMap = params.all();
		
		Map<String, List<String>> healthInfo = new HashMap<String, List<String>>();
		for (DiseaseQuestion question : disease.getQuestions()) {
			String[] values = paramsMap.get(question.getName());
			if (values != null) {
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
				if (!otherValue.equals("Other (please separate by commas)")) {
					List<String> otherItems = CommonUtil.parseCommaSerapatedList(otherValue);
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
			TalkerDAO.updateTalker(talker);
		}
		else {
			//non-primary email
			EmailBean emailBean = talker.findNonPrimaryEmail(null, verifyCode);
			
			//clear verify code in db
			TalkerDAO.deleteEmail(talker, emailBean);
			emailBean.setVerifyCode(null);
			TalkerDAO.saveEmail(talker, emailBean);
		}
		
		CommonUtil.updateCachedTalker(session);
		render();
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
		
		talker.setDeactivated(true);
		TalkerDAO.updateTalker(talker);
		
		try {
			Secure.logout();
		} catch (Throwable e) {
			Logger.error("Logout error", e);
		}
	}
}

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

import models.HealthItemBean;
import models.TalkerBean;
import models.TalkerBean.ProfilePreference;
import models.TalkerDiseaseBean;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import play.Play;
import play.data.validation.Valid;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;
import util.EmailUtil;
import dao.ActivityDAO;
import dao.DiseaseDAO;
import dao.HealthItemDAO;
import dao.TalkerDAO;
import dao.TalkerDiseaseDAO;
import dao.TopicDAO;

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
		
		//TODO duplicates functionality?
		if (!oldEmail.equals(talker.getEmail())) {
			//send verification email
			oldTalker.setVerifyCode(CommonUtil.generateVerifyCode());
			
			Map<String, String> vars = new HashMap<String, String>();
			vars.put("username", oldTalker.getUserName());
			vars.put("verify_code", oldTalker.getVerifyCode());
			EmailUtil.sendEmail(EmailUtil.VERIFICATION_TEMPLATE, oldTalker.getEmail(), vars, null, false);
		}
		
		TalkerDAO.updateTalker(oldTalker);
		
		CommonUtil.updateCachedTalker(session);
		
		flash.success("ok");
		render("@edit", oldTalker);
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
			
			//TODO strange fix for redirect with "#reference". Discussed here:
			//http://groups.google.com/group/play-framework/browse_thread/thread/93c2ec3e34c20f4e/bf94f63fcb2e529d?lnk=gst&q=addRef#bf94f63fcb2e529d
//			ActionDefinition action = reverse(); {
//				//render("@edit", talker);
//				edit();
//	        }
//			redirect(action.addRef("changePasswordForm").toString()); 
			
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
		
		ProfilePreference[] profilePreferencesArr = ProfilePreference.values();
		render(profilePreferencesArr, profilePreferences);
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
	
	public static void saveAccounts(TalkerBean talker) {
		flash.put("currentForm", "accountsForm");
		
		TalkerBean sessionTalker = CommonUtil.loadCachedTalker(session);
		String oldEmail = sessionTalker.getEmail();
		
		//if something was changed - send new invitation
		if (!sessionTalker.getIm().equals(talker.getIm()) 
				|| !sessionTalker.getImUsername().equals(talker.getImUsername())) {
			CommonUtil.sendIMInvitation(talker.getIm(), talker.getImUsername());
		}
		
		//TODO: what email here? check for duplicate email?
		sessionTalker.setEmail(talker.getEmail());
		sessionTalker.setImUsername(talker.getImUsername());
		sessionTalker.setIm(talker.getIm());
		
		if (!oldEmail.equals(talker.getEmail())) {
			//send verification email
			sessionTalker.setVerifyCode(CommonUtil.generateVerifyCode());
			
			Map<String, String> vars = new HashMap<String, String>();
			vars.put("username", sessionTalker.getUserName());
			vars.put("verify_code", sessionTalker.getVerifyCode());
			EmailUtil.sendEmail(EmailUtil.VERIFICATION_TEMPLATE, sessionTalker.getEmail(), vars, null, false);
		}
		
		TalkerDAO.updateTalker(sessionTalker);
		
		CommonUtil.updateCachedTalker(session);
		
		flash.success("ok");
		notifications();
	}
	
	/* ------------- Health Info -------------------------- */
	public static void healthDetails() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		//For now we have only one disease - Breast Cancer
		final String diseaseName = "Breast Cancer";

		//Load data for selects
		List<String> stagesList = DiseaseDAO.getValuesByDisease("stages", diseaseName);
		List<String> typesList = DiseaseDAO.getValuesByDisease("types", diseaseName);
		
		TalkerDiseaseBean talkerDisease = TalkerDiseaseDAO.getByTalkerId(talker.getId());
		
		//Load all healthItems for this disease
		Map<String, HealthItemBean> healthItemsMap = new HashMap<String, HealthItemBean>();
		for (String itemName : new String[] {"symptoms", "tests", 
				"procedures", "treatments", "sideeffects"}) {
			HealthItemBean healthItem = HealthItemDAO.getHealthItemByName(itemName, diseaseName);
			healthItemsMap.put(itemName, healthItem);
		}
		
		render(talkerDisease, stagesList, typesList, healthItemsMap);
	}
	
	public static void saveHealthDetails(TalkerDiseaseBean talkerDisease) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		parseHealthItems(talkerDisease);
		talkerDisease.setUid(talker.getId());
		
		Date symptomDate = CommonUtil.parseDate(talkerDisease.getSymptomMonth(), 1, talkerDisease.getSymptomYear());
		talkerDisease.setSymptomDate(symptomDate);
		Date diagnoseDate = CommonUtil.parseDate(talkerDisease.getDiagnoseMonth(), 1, talkerDisease.getDiagnoseYear());
		talkerDisease.setDiagnoseDate(diagnoseDate);
		
		//Save or update
		TalkerDiseaseDAO.saveTalkerDisease(talkerDisease);
		
		flash.success("ok");
		healthDetails();
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
	
	/* ---------------- Public Profile ------------------------ */
	public static void view(String userName) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		
		TalkerBean talker = TalkerDAO.getByUserName(userName);
		
		notFoundIfNull(talker);
		
		// Health info
		//For now we have only one disease - Breast Cancer
		final String diseaseName = "Breast Cancer";
		TalkerDiseaseBean talkerDisease = TalkerDiseaseDAO.getByTalkerId(talker.getId());
		if (talkerDisease != null) {
			talkerDisease.setName(diseaseName);
		}
		
		//Load all healthItems for this disease
		//TODO: duplicate code?
		Map<String, HealthItemBean> healthItemsMap = new HashMap<String, HealthItemBean>();
		for (String itemName : new String[] {"symptoms", "tests", 
				"procedures", "treatments", "sideeffects"}) {
			HealthItemBean healthItem = HealthItemDAO.getHealthItemByName(itemName, diseaseName);
			healthItemsMap.put(itemName, healthItem);
		}
		
		talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
		talker.setActivityList(ActivityDAO.load(talker.getId()));
		talker.setProfileCommentsList(TalkerDAO.loadProfileComments(talker.getId()));
		//TODO: Temporarily - later we'll load all list of topics
		talker.setNumberOfTopics(TopicDAO.getNumberOfTopics(talker.getId()));
		
		render(talker, talkerDisease, healthItemsMap, currentTalker);
	}
}

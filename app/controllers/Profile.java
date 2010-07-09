package controllers;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;

import models.TalkerBean;
import models.TalkerBean.ProfilePreference;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import play.Play;
import play.data.validation.Valid;
import play.mvc.Controller;
import play.mvc.Router.ActionDefinition;
import play.mvc.With;
import util.CommonUtil;
import dao.TalkerDAO;

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
		
		//TODO incorrect date handling and error message?
		
		if(validation.hasErrors()) {
			flash.success("");
			render("@edit", talker);
            return;
        }
		
		oldTalker.setUserName(talker.getUserName());
		oldTalker.setEmail(talker.getEmail());
		oldTalker.setDob(talker.getDob());
		oldTalker.setGender(talker.getGender());
		oldTalker.setMaritalStatus(talker.getMaritalStatus());
		oldTalker.setCity(talker.getCity());
		oldTalker.setState(talker.getState());
		oldTalker.setCountry(talker.getCountry());
		oldTalker.setChildrenNum(talker.getChildrenNum());
		
		TalkerDAO.updateTalker(oldTalker);
		
		flash.success("ok");
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
			
			//TODO strange fix. Discussed here:
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
		String imagePath = talker.getImagePath();
		
		render("@image", imagePath);
	}
	
	public static void uploadImage(String submitAction, File imageFile) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		System.out.println(submitAction);
		
		if ("Remove current image".equals(submitAction)) {
			talker.setImagePath(null);
			TalkerDAO.updateTalker(talker);
			
			image();
			return;
		}
		else {
			//TODO: move it to separate directory or db?
			String extension = FilenameUtils.getExtension(imageFile.getName());
	    	if (extension == null) {
	    		//default
	    		extension = ".gif";
	    	}
	    	String fileName = talker.getId()+extension;
	        
	    	File destFile = new File(Play.getFile("public/images/pictures/"), fileName);
	        try {
				IOUtils.copy(new FileInputStream(imageFile), new FileOutputStream(destFile));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} 
	        
	        talker.setImagePath("/public/images/pictures/"+fileName);
	        TalkerDAO.updateTalker(talker);
	        
	        image();
	        return;
		}
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
	
	public static void saveNotifications(TalkerBean talker) {
		flash.put("currentForm", "notificationsForm");
		
		TalkerBean sessionTalker = CommonUtil.loadCachedTalker(session);
		
		sessionTalker.setNfreq(talker.getNfreq());
		sessionTalker.setNtime(talker.getNtime());
		sessionTalker.setCtype(talker.getCtype());
		
		TalkerDAO.updateTalker(sessionTalker);
		flash.success("ok");
		notifications();
	}
	
	/* ------------- Health Info -------------------------- */
	
	public static void healthinfo() {
		
	}
}

package util;

import java.util.Map;

import models.EmailBean;
import models.TalkerBean;

import com.sailthru.TriggerMailClient;

import dao.TalkerDAO;

/**
 * Uses Sailthru API, http://sailthru.com/
 * @author kindcoder
 */
public class EmailUtil {
	//TODO: move to Play! configuration?
	private static final String SAILTHRU_APIKEY = "4007bc4d4b48586353eb44012172eaf3";
	private static final String SAILTHRU_SECRET = "4ba0a437f0f138fceba76dac5c33e567";
	
	public static final String SUPPORT_EMAIL = "support@talkabouthealth.com";
	public static final String MURRAY_EMAIL = "murrayjones@gmail.com";
	
	public static final String WELCOME_TEMPLATE = "welcome";
	public static final String FORGOT_PASSWORD_TEMPLATE = "forgotpassword";
	public static final String INVITATION_TEMPLATE = "invitation";
	public static final String CONTACTUS_TEMPLATE = "contactus";
	public static final String VERIFICATION_TEMPLATE = "verificate";
	public static final String NOTIFICATION_TEMPLATE = "notification";
	
	public static boolean sendEmail(String templateName, String toEmail) {
		return sendEmail(templateName, toEmail, null, null, true);
	}
	
	public static boolean sendEmail(String templateName, String toEmail, Map<String, String> vars) {
		return sendEmail(templateName, toEmail, vars, null, true);
	}
	
	public static boolean sendEmail(String templateName, String toEmail, 
			Map<String, String> vars, Map<String, String> options, boolean verify) {
		if (verify && !isVerifiedEmail(toEmail)) {
			return false;
		}
		
		TriggerMailClient client;
		try {
			client = new TriggerMailClient(SAILTHRU_APIKEY, SAILTHRU_SECRET);
			//TODO: other for DEV?
			client.send(templateName, toEmail, vars, options);
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Couldn't send email: "+e.getMessage());
			return false;
		}
		
		return true;
	}

	private static boolean isVerifiedEmail(String email) {
		TalkerBean talker = TalkerDAO.getByEmail(email);
		
		if (talker == null) {
			return false;
		}
		
		//verified talker has empty Verify Code
		if (talker.getEmail().equals(email)) {
			//primary email
			return talker.getVerifyCode() == null;
		}
		else {
			//non-primary
			EmailBean emailBean = talker.findNonPrimaryEmail(email, null);
			return emailBean.getVerifyCode() == null;
		}
	}
}


package util;

import java.util.Map;

import com.sailthru.TriggerMailClient;

/**
 * Uses Sailthru API, http://sailthru.com/
 * @author kindcoder
 */
public class EmailUtil {
	
	private static final String SAILTHRU_APIKEY = "4007bc4d4b48586353eb44012172eaf3";
	private static final String SAILTHRU_SECRET = "4ba0a437f0f138fceba76dac5c33e567";
	
	public static final String SUPPORT_EMAIL = "support@talkabouthealth.com";
	
	public static final String WELCOME_TEMPLATE = "welcome";
	public static final String FORGOT_PASSWORD_TEMPLATE = "forgotpassword";
	public static final String INVITATION_TEMPLATE = "invitation";
	public static final String CONTACTUS_TEMPLATE = "contactus";
	
	public static void sendEmail(String templateName, String toEmail) {
		sendEmail(templateName, toEmail, null, null);
	}
	
	public static void sendEmail(String templateName, String toEmail, Map<String, String> vars) {
		sendEmail(templateName, toEmail, vars, null);
	}
	
	public static void sendEmail(String templateName, String toEmail, 
			Map<String, String> vars, Map<String, String> options) {
		TriggerMailClient client;
		try {
			client = new TriggerMailClient(SAILTHRU_APIKEY, SAILTHRU_SECRET);
			//TODO: replace to real "toEmail" later
			client.send(templateName, "support@talkabouthealth.com", vars, options);
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Couldn't send email: "+e.getMessage());
		}
	}
}


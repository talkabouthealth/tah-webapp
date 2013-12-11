package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import play.Logger;

import models.ConversationBean;
import models.EmailBean;
import models.EmailListBean;
import models.TalkerBean;

import com.sailthru.EmailStatus;
import com.sailthru.TriggerMailClient;

import dao.TalkerDAO;

/**
 * Uses Sailthru API, http://sailthru.com/
 */
public class EmailUtil {
	private static final String SAILTHRU_APIKEY = "4007bc4d4b48586353eb44012172eaf3";
	private static final String SAILTHRU_SECRET = "4ba0a437f0f138fceba76dac5c33e567";
	
	public static final String SUPPORT_EMAIL = "support@talkabouthealth.com";
	public static final String MURRAY_EMAIL = "murrayjones@gmail.com";
	public static final String ADMIN_EMAIL = "admin@talkabouthealth.com";
	
	/**
	 * Names are the same (only uppercase) as templates' names on http://sailthru.com/
	 */
	public enum EmailTemplate {
		WELCOME,
		WELCOME_WAITINGLIST,
		FORGOT_PASSWORD,
		INVITATION,
		SHARE,
		CONTACTUS,
		FLAGGED,
		VERIFICATION,
		WELCOME_NEWSLETTER,
		
		NOTIFICATION_THANKYOU,
		NOTIFICATION_FOLLOWER,
		NOTIFICATION_PROFILE_COMMENT,
		NOTIFICATION_REPLY_TO_COMMENT_IN_JOURNAL,
		NOTIFICATION_DIRECT_MESSAGE,
		
		NOTIFICATION_CONVO_RESTART,
		NOTIFICATION_CONVO_ANSWER,
		NOTIFICATION_CONVO_REPLY_TO_ANSWER,
		NOTIFICATION_CONVO_REPLY,
		NOTIFICATION_CONVO_SUMMARY,
		NOTIFICATION_PERSONAL_QUESTION,
		NOTIFICATION_PERSONAL_QUESTION_MODERATED,
		NOTIFICATION_REPLY_TO_THANKYOU,

		NOTIFICATION_OF_THOUGHT_MENTION,
		NOTIFICATION_OF_ANSWER_MENTION,
		
		DAY_EMAIL_VERIFICATION_FOLLOWUP,
		WEEK_EMAIL_VERIFICATION_FOLLOWUP
		;
	}
	
	public static boolean sendEmail(EmailTemplate emailTemplate, String toEmail) {
		return sendEmail(emailTemplate, toEmail, null, null, true);
	}
	
	public static boolean sendEmail(EmailTemplate emailTemplate, String toEmail, Map<String, String> vars) {
		return sendEmail(emailTemplate, toEmail, vars, null, true);
	}
	
	/**
	 * Send email through Sailthru
	 * @param emailTemplate Template to use
	 * @param toEmail Recipient's email
	 * @param vars Parameters for templates
	 * @param options Additional Sailthru options, usually 'null'
	 * @param verify Should recipient's email be verified?
	 * @return
	 */
	public static boolean sendEmail(EmailTemplate emailTemplate, String toEmail, 
			Map<String, String> vars, Map<String, String> options, boolean verify) {
		if (toEmail == null) {
			return false;
		}

		/*
		Date : 29-Jun-2011
		Removed the email verification code and sending email's to all user email's
		*/
		/*
		if (verify && !isVerifiedEmail(toEmail)) {
			return false;
		}
		*/

		TriggerMailClient client;
		try {
			client = new TriggerMailClient(SAILTHRU_APIKEY, SAILTHRU_SECRET);
			client.send(emailTemplate.toString().toLowerCase(), toEmail, vars, options);
		} catch (Exception e) {
			Logger.error(e, "EmailUtil.java : sendEmail");
			return false;
		}

		return true;
	}

	/**
	 * 
	 * @param listName
	 * @param emailList
	 * @return
	 */
	public static boolean setEmail(ArrayList<EmailListBean> emailList){
		boolean returnFlag = true;
		TriggerMailClient client;
		try {
			client = new TriggerMailClient(SAILTHRU_APIKEY, SAILTHRU_SECRET);
			EmailListBean email;
			Map<String, Boolean> lists;
			if(emailList != null && emailList.size() > 0){
				for (Iterator iterator = emailList.iterator(); iterator.hasNext();) {
					email = (EmailListBean) iterator.next();
					lists = new HashMap<String, Boolean>();
					lists.put(email.getListName(), email.isListFlag());
					try {
						client.setEmail(email.getEmail(), false, false, false, null, lists, null);
					} catch (Exception e) {
						Logger.error(e, "EmailUtil.java: setEmail");
					}
				}
			}else{
				System.out.println("List is empty");
			}
		} catch (Exception e) {
			Logger.error(e, "EmailUtil.java: sentMail");
			return false;
		}
		emailList.clear();
		return returnFlag;
	}
	

	/**
	 * 
	 * @param listName
	 * @param emailList
	 * @return
	 */
	public static boolean setEmail(Map<String, Boolean> lists,String email){
		boolean returnFlag = true;
		TriggerMailClient client;
		try {
			client = new TriggerMailClient(SAILTHRU_APIKEY, SAILTHRU_SECRET);
			client.setEmail(email, false, false, false, null, lists, null);
		} catch (Exception e) {
			Logger.error(e, "EmailUtil.java: sentMail");
			return false;
		}
		return returnFlag;
	}
	/**
	 * Checks if given email was verified by talker
	 * @param email
	 * @return
	 */
	private static boolean isVerifiedEmail(String email) {
		TalkerBean talker = TalkerDAO.getByEmail(email);
		if (talker == null) {
			return false;
		}

		//verified talker has empty Verify Code
		if (talker.getEmail().equals(email)) {
			//primary email
			return talker.getVerifyCode() == null;
		} else {
			//non-primary
			EmailBean emailBean = talker.findNonPrimaryEmail(email, null);
			return emailBean.getVerifyCode() == null;
		}
	}
}
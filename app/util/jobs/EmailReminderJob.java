package util.jobs;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.ConversationBean;
import models.TalkerBean;
import play.jobs.Job;
import play.jobs.On;
import play.jobs.OnApplicationStart;
import util.EmailUtil;
import util.EmailUtil.EmailTemplate;
import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;
 
/**
 * Reminds users to verify email after 1 day or 1 week after signup.
 * Runs every day at noon.
 *
 */

public class EmailReminderJob{
		
	public static void main(String[] args) throws Throwable {
		System.out.println("EmailReminderJob Started::::"+ new Date());
		List<TalkerBean> talkersList = TalkerDAO.loadAllTalkers(true);
		for (TalkerBean talker : talkersList) {
			if (talker.getVerifyCode() != null) {
				Calendar now = Calendar.getInstance();
				Calendar signupDate = Calendar.getInstance();
				signupDate.setTime(talker.getRegDate());

				boolean sameYear = (now.get(Calendar.YEAR) == signupDate.get(Calendar.YEAR));
				int daysDiff = now.get(Calendar.DAY_OF_YEAR) - signupDate.get(Calendar.DAY_OF_YEAR);
				
				if (sameYear && (daysDiff == 1 || daysDiff == 7)) {
					//load different talkers info
					int numOfFollowers = TalkerDAO.getFollowersCount(talker.getId());
					//number of answers to the questions started by talker
					int numOfAnswers = 0;
					List<String> startedConvos = ConversationDAO.getStartedConvosForEmailReminderJob(talker.getId());
					for (String convoId : startedConvos) {
						numOfAnswers += CommentsDAO.getConvoAnswersCount(convoId);
					}
					
					Map<String, String> vars = new HashMap<String, String>();
					String subjectInfo = "You have "+numOfAnswers+" answer(s) and "+numOfFollowers+" follower(s).";
					vars.put("subject_info", subjectInfo);
					vars.put("username", talker.getUserName());
					vars.put("verify_code", talker.getVerifyCode()+"");
					if (daysDiff == 1) {
						try{
							EmailUtil.sendEmail(EmailTemplate.DAY_EMAIL_VERIFICATION_FOLLOWUP, talker.getEmail(), vars, null, false);
						}catch (Exception e) {
							e.printStackTrace();
						}
						System.out.println("Send day email verification followup to "+talker.getEmail());
					}
					else {
						try{
							EmailUtil.sendEmail(EmailTemplate.WEEK_EMAIL_VERIFICATION_FOLLOWUP, talker.getEmail(), vars, null, false);
						}catch (Exception e) {
							e.printStackTrace();
						}
						System.out.println("Send week email verification followup to "+talker.getEmail());
					}
					
				}
			}
		}
		System.out.println("EmailReminderJob Completed::::"+ new Date());
		EmailReminderJob emailJob = new EmailReminderJob();
		emailJob.finalize();
	}
	
	protected void finalize() throws Throwable {
		super.finalize();
	}
	
}

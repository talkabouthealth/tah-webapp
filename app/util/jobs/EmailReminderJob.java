package util.jobs;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dao.ApplicationDAO;
import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;
import models.ConversationBean;
import models.TalkerBean;
import play.Logger;
import play.jobs.Job;
import play.jobs.On;
import util.CommonUtil;
import util.EmailUtil;
import util.EmailUtil.EmailTemplate;
 
/**
 * Reminds users to verify email after 1 day or 1 week after signup.
 * Runs every day at noon.
 *
 */
@On("0 0 12 * * ?")
public class EmailReminderJob extends Job {
		
	@Override
	public void doJob() throws Exception {
		/*
		Logger.info("Strating EmailReminderJob");
		for (TalkerBean talker : TalkerDAO.loadAllTalkers()) {
			if (talker.getVerifyCode() != null) {
				Calendar now = Calendar.getInstance();
				Calendar signupDate = Calendar.getInstance();
				signupDate.setTime(talker.getRegDate());

				boolean sameYear = (now.get(Calendar.YEAR) == signupDate.get(Calendar.YEAR));
				int daysDiff = now.get(Calendar.DAY_OF_YEAR) - signupDate.get(Calendar.DAY_OF_YEAR);
				
				if (sameYear && (daysDiff == 1 || daysDiff == 7)) {
					//load different talkers info
					int numOfFollowers = TalkerDAO.loadFollowers(talker.getId()).size();
					//number of answers to the questions started by talker
					int numOfAnswers = 0;
					List<ConversationBean> startedConvos = ConversationDAO.getStartedConvos(talker.getId(), null, -1);
					for (ConversationBean convo : startedConvos) {
						numOfAnswers += CommentsDAO.loadConvoAnswers(convo.getId()).size();
					}
					
					Map<String, String> vars = new HashMap<String, String>();
					String subjectInfo = "You have "+numOfAnswers+" answer(s) and "+numOfFollowers+" follower(s).";
					vars.put("subject_info", subjectInfo);
					vars.put("username", talker.getUserName());
					vars.put("verify_code", talker.getVerifyCode()+"");
					if (daysDiff == 1) {
						EmailUtil.sendEmail(EmailTemplate.DAY_EMAIL_VERIFICATION_FOLLOWUP, talker.getEmail(), vars, null, false);
					}
					else {
						EmailUtil.sendEmail(EmailTemplate.WEEK_EMAIL_VERIFICATION_FOLLOWUP, talker.getEmail(), vars, null, false);
					}
				}
			}
		}
		Logger.info("Completing EmailReminderJob");
		*/
	}
}
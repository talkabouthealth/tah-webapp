package util.jobs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;
import models.ConversationBean;
import models.TalkerBean;
import play.jobs.Job;
import util.CommonUtil;
import util.EmailUtil;
import util.EmailUtil.EmailTemplate;

/** Fire at 12pm (noon) every day **/ 
//@On("0 0 12 * * ?")
public class EmailReminderJob {
//	public class ThoughtsFromServicesJob extends Job {
		
//	@Override
	public static void doJob() throws Exception {
		TalkerBean talker = TalkerDAO.getByUserName("kangaroo");
		
		int numOfFollowers = TalkerDAO.loadFollowers(talker.getId()).size();
		//number of answers to the questions started by talker
		int numOfAnswers = 0;
		List<ConversationBean> startedConvos = ConversationDAO.getStartedConvos(talker.getId());
		for (ConversationBean convo : startedConvos) {
			numOfAnswers += CommentsDAO.loadConvoAnswers(convo.getId()).size();
		}
		
		//1 day later email template in Sailthru: 1day_email_verification_followup
		Map<String, String> vars = new HashMap<String, String>();
		String subjectInfo = "You have "+numOfAnswers+" answer(s) and "+numOfFollowers+" follower(s).";
		vars.put("subject_info", subjectInfo);
		vars.put("username", talker.getUserName());
		vars.put("verify_code", talker.getVerifyCode()+"");
		EmailUtil.sendEmail(EmailTemplate.DAY_EMAIL_VERIFICATION_FOLLOWUP, talker.getEmail(), vars, null, false);
	}

}

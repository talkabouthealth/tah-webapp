package util.jobs;

import models.IMAccountBean;
import models.TalkerBean;

import dao.TalkerDAO;

import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

/**
 * Job is used for different updates in db when deploying new features.
 */
@OnApplicationStart
public class ApplicationUpdatesJob extends Job {
	
	public void doJob() {
    	//Move old IM account info to new format ('im_accounts' array)
		for (TalkerBean talker : TalkerDAO.loadAllTalkers()) {
//			System.out.println(talker.getIm()+" : "+talker.getImUsername());
			if (talker.getIm() != null && talker.getImUsername() != null) {
				if (!talker.getIm().isEmpty() && !talker.getImUsername().isEmpty()) {
					IMAccountBean imAccount = 
						new IMAccountBean(talker.getImUsername(), talker.getIm());
					talker.getImAccounts().add(imAccount);
					TalkerDAO.updateTalker(talker);
				}
			}
		}
    }
	
}

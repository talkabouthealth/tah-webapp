package util.jobs;

import play.jobs.Job;
import play.jobs.OnApplicationStart;

@OnApplicationStart
public class OnlineUsersJob extends Job {
 
    public void doJob() {
    	//TODO: output from IM app and Dashboard?
    	System.out.println("***Starting Onlin User Singleton Thread!!!");
		Thread tRunTalkmi = new Thread(new OnlineUsersSingleton(), "OnlineUsersSingleton");
    	tRunTalkmi.start();
    }
 
}

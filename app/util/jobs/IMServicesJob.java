package util.jobs;

import com.tah.im.IMNotifier;

import improject.LoginInfo;
import improject.IMSession.IMService;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

/**
 * Job for running IM services on startup
 *
 */
@OnApplicationStart
public class IMServicesJob extends Job {
	
	private static final LoginInfo[] PROD_LOGIN_ARRAY = new LoginInfo[] {
		new LoginInfo(IMService.GOOGLE, "talkabouthealth.com@gmail.com", "CarrotCake917"),
		new LoginInfo(IMService.MSN, "talkabouthealth.com@live.com", "CarrotCake917"),
		new LoginInfo(IMService.YAHOO, "talkabouthealth@ymail.com", "CarrotCake917"),
	};
	private static final LoginInfo[] DEV_LOGIN_ARRAY = new LoginInfo[] {
		new LoginInfo(IMService.GOOGLE, "talkabouthealth.com.test@gmail.com", "CarrotCake917"),
		new LoginInfo(IMService.MSN, "talkabouthealth.com.test@hotmail.com", "CarrotCake917"),
		new LoginInfo(IMService.YAHOO, "talkabouthealthtest@ymail.com", "CarrotCake917"),
	};
 
    public void doJob() {
    	String appMode = (String)Play.configuration.get("application.mode");
    	if (appMode != null && appMode.equalsIgnoreCase("prod")) {
    		IMNotifier.init(PROD_LOGIN_ARRAY);
    	}
    	else {
    		IMNotifier.init(DEV_LOGIN_ARRAY);
    	}
    }
 
}

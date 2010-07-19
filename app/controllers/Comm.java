package controllers;

import play.mvc.Controller;

public class Comm extends Controller {
	
	public static void commApp(String userid, String username, String topicid, String topic) {
		render(userid, username, topicid, topic);
	}

}

package controllers;

import dao.TalkerDAO;
import play.mvc.*;

public class Application extends Controller {

    public static void index() {
    	if (session.contains("username")) {
    		Home.index(params.get("newtopic"));
    	}
    	else {
    		long numberOfMembers = TalkerDAO.getNumberOfTalkers();
    		
    		render(numberOfMembers);
    	}
    }

}
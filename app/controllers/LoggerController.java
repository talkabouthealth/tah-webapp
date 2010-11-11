package controllers;

import play.Logger;
import play.mvc.After;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Finally;

public class LoggerController extends Controller {

	@Before
    static void before() {
		Logger.error("Before "+request.toString()+", "+session.get("username"));
    }
	
	@After
    static void after() {
		Logger.error("After "+request.toString()+", "+session.get("username"));
    }
	
	@Finally
    static void fin() {
		Logger.error("Finally "+request.toString()+", "+session.get("username"));
    }

}

package controllers;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import play.data.validation.Email;
import play.mvc.Controller;
import play.mvc.With;
import util.EmailUtil;
import util.EmailUtil.EmailTemplate;

@With( LoggerController.class )
public class NewHome extends Controller {

	 public static void index() {
		 String def = session.get("newhome");
		 if(StringUtils.isBlank(def)) {
			 render();
		 } else if (def.equalsIgnoreCase("1")) {
			 Application.index();
		 }
	 }

	 public static void start() {
		 String def = session.get("newhome");
		 if(StringUtils.isBlank(def)) {
			 def = "1";
		 }
		 session.put("newhome", def);
		 Application.index();
	 }
	 
	 public static void sendContactEmail(@Email String email, String subject, String message) {
		validation.clear();
    	validation.required(email).message("Email is required");
    	validation.required(message).message("Message is required");
    	if (validation.hasErrors()) {
            params.flash();
            validation.keep();
            renderText("Error:Please correct your form");
        } else {
        	params.flash();
			Map<String, String> vars = new HashMap<String, String>();
			vars.put("name", session.get("username") == null ? "" : session.get("username"));
			vars.put("email", email);
			vars.put("subject", subject);
			vars.put("message", message);
			EmailUtil.sendEmail(EmailTemplate.CONTACTUS, EmailUtil.SUPPORT_EMAIL, vars, null, false);
			renderText("Thank you");
        }
    }
}
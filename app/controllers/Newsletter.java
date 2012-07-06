package controllers;

import models.NewsLetterBean;
import models.TalkerBean;
import play.data.validation.Email;
import play.data.validation.Error;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;
import dao.NewsLetterDAO;

@With( LoggerController.class )
public class Newsletter extends Controller {

	public static void subscribeWorkShop(NewsLetterBean newsletter){
    	String []types = newsletter.getNewsLetterType();
    	String email = newsletter.getEmail();
    	validation.required(email).message("Email is required");
    	validation.email(email);
    	if (validation.hasErrors()) {
    		params.flash();
            Error error = validation.errors().get(0);
			renderText("Error:" + error.message());
    	} else if(types == null) {
    		renderText("Error: Please select workshop types.");
    	} else {
    		TalkerBean talker = CommonUtil.loadCachedTalker(session);
    		NewsLetterDAO.saveOrUpdateNewsletter(newsletter,talker);
    		renderText("Thank you for subscribing!");
    	}
    }
	
	public static void subscribeTopic(@Email String email, String topicId){
		validation.required(email).message("Email is required");
    	validation.email(email);
		if (validation.hasErrors()) {
    		params.flash();
            Error error = validation.errors().get(0);
			renderText("Error:" + error.message());
    	} else if(topicId == null) {
    		renderText("Error: Please try again.");
    	} else {
    		NewsLetterDAO.saveOrUpdateTopicNewsletter(email,topicId);
    		renderText("Thank you for subscribing!");
    	}
	}
	
	public static void subscribeTalker(@Email String email, String talkerId){
		validation.required(email).message("Email is required");
    	validation.email(email);
		if (validation.hasErrors()) {
    		params.flash();
            Error error = validation.errors().get(0);
			renderText("Error:" + error.message());
    	} else if(talkerId == null) {
    		renderText("Error: Please try again.");
    	} else {
    		NewsLetterDAO.saveOrUpdateTalkerNewsletter(email,talkerId);
    		renderText("Thank you for subscribing!");
    	}
	}
}
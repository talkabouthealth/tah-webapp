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
    	boolean isNewsletter = NewsLetterDAO.isnewsLetterSubscribe(email);
    	validation.required(email).message("Email is required");
    	validation.email(email);
    	String msg;
    	if (validation.hasErrors()) {
    		params.flash();
            Error error = validation.errors().get(0);
			msg = "Error:" + error.message();
    	} else if(types == null) {
    		msg = "Error: Please select workshop types.";
    	} else {
    		TalkerBean talker = CommonUtil.loadCachedTalker(session);
    		NewsLetterDAO.saveOrUpdateNewsletter(newsletter,talker);
    		if(isNewsletter)
    			msg = "Thank you for subscribing!";
    		else
    			msg = "NEW:Thank you for subscribing!";
    	}
    	renderText(msg);
    }
	
	public static void subscribeTopic(@Email String email, String topicId){
		validation.required(email).message("Email is required");
    	validation.email(email);
    	boolean isNewsletter = NewsLetterDAO.isnewsLetterSubscribe(email);
    	String msg;
		if (validation.hasErrors()) {
    		params.flash();
            Error error = validation.errors().get(0);
            msg = "Error:" + error.message();
    	} else if(topicId == null) {
    		msg = "Error: Please try again.";
    	} else {
    		NewsLetterDAO.saveOrUpdateTopicNewsletter(email,topicId);
    		if(isNewsletter)
    			msg ="Thank you for subscribing!";
    		else
    			msg ="NEW:Thank you for subscribing!";
    	}
		renderText(msg);
	}
	
	public static void subscribeTalker(@Email String email, String talkerId){
		validation.required(email).message("Email is required");
    	validation.email(email);
    	boolean isNewsletter = NewsLetterDAO.isnewsLetterSubscribe(email);
    	String msg;
		if (validation.hasErrors()) {
    		params.flash();
            Error error = validation.errors().get(0);
            msg = "Error:" + error.message();
    	} else if(talkerId == null) {
    		msg = "Error: Please try again.";
    	} else {
    		NewsLetterDAO.saveOrUpdateTalkerNewsletter(email,talkerId);
    		if(isNewsletter)
    			msg = "Thank you for subscribing!";
    		else
    			msg = "NEW:Thank you for subscribing!";
    	}
		renderText(msg);
	}
	
	public static void subscribeMoreNewsLetter(NewsLetterBean newsletter) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		String email = newsletter.getEmail();
		validation.required(email).message("Email is required");
		validation.email(email.trim());
		if (validation.hasErrors()) {
			renderText("Error:" + validation.errors().get(0));
		}
		if(newsletter != null && newsletter.getNewsLetterType() != null && newsletter.getNewsLetterType().length > 0){
			NewsLetterDAO.saveOrUpdateNewsletter(newsletter,talker);
			renderText("Ok");
		}else{
			renderText("Please select on of the option");
		}
	}
}
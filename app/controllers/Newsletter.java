package controllers;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;

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
    		//NewsLetterDAO.saveOrUpdateTopicNewsletter(email,topicId);
    		NewsLetterDAO.saveOrUpdateTopicNewsletterNew(email,topicId);
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
    		//NewsLetterDAO.saveOrUpdateTalkerNewsletter(email,talkerId);
    		NewsLetterDAO.saveOrUpdateTalkerNewsletterNew(email,talkerId);
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
		validation.email(email);
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

	public static void subscribeCancerNews(NewsLetterBean newsletter) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		String highrisk = params.get("highrisk");
		String day = params.get("day")==null?"":params.get("day");
		String month = params.get("month")==null?"":params.get("month");
		String year = params.get("year")==null?"":params.get("year");
		String guidance = params.get("guidance");
		boolean isNewsletterSaved = false;
		
		
		String email = newsletter.getEmail();
		validation.required(email).message("Email is required");
		validation.email(email.trim());
		if (validation.hasErrors()) {
			renderText("Error:" + validation.errors().get(0));
		}

		if(StringUtils.isNotEmpty(guidance)) {
			boolean highriskFlag = StringUtils.isNotBlank(highrisk);
			NewsLetterDAO.saveOrUpdateGuidanceNewsletter(email,guidance,highriskFlag,day,month,year);
			isNewsletterSaved = true;
		}
		
		if(newsletter != null && newsletter.getNewsLetterType() != null && newsletter.getNewsLetterType().length > 0) {
			NewsLetterBean newsletterNew = NewsLetterDAO.getNewsLetterInfo(email);
			int i = 0;
			String types[]= null;
			if(newsletterNew!=null) {
				String[] newLetterTypes = newsletterNew.getNewsLetterType();
				int len = newLetterTypes==null?0:newLetterTypes.length;
				types =new String[len+1];
	    		if(len != 0) {
	    			for(i=0;i<len;i++) {
	    				types[i]=newLetterTypes[i];
	    			}
	    		}
			} else {
				types =new String[1];
			}
			types[i]="Breast Cancer Update";
    		newsletter.setNewsLetterType(types);
			NewsLetterDAO.saveOrUpdateNewsletter(newsletter,talker);
			isNewsletterSaved = true;
		}
		if(isNewsletterSaved) {
			renderText("Ok");
		} else {
			renderText("Please select one of the option");
		}
	}
}
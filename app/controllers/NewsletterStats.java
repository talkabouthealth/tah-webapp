package controllers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.util.Util;

import models.DiseaseBean;
import models.NewsLetterBean;
import play.mvc.Controller;
import play.mvc.With;
import dao.DiseaseDAO;
import dao.NewsLetterDAO;


/**
 * Admin Dashboard : NewsletterStats
 *
 */
@Check("admin")
@With(Secure.class)
public class NewsletterStats extends Controller {
	
	public static void index(String letterType,String fromDate,String toDate) {
		
		String errorMsg = "";
		DateFormat dateFormat = new SimpleDateFormat("yyyy-dd-MM");//09/04/2012
		Date fromDt = new Date();
		Date toDt = new Date();
		boolean dateError = false;
		if(fromDate != null && !"".equals(fromDate)) {
			try {
				fromDt = dateFormat.parse(fromDate);
				toDt = dateFormat.parse(toDate);
				if(toDt.before(fromDt)){
					dateError = true;
				}
			} catch(Exception e) {
				e.printStackTrace();
				dateError = true;
			}
			if(dateError) {
				errorMsg = "Wrong dates selected";
			}
		} else {
			dateError = true;
		}

		if(letterType == null){
			letterType = "2";
		}
		
		Map<String, String> emailList = new HashMap<String, String>();
		long total = 0;
		long dTotal = 0;
		if("0".equals(letterType)) {
			List<DiseaseBean> diseaseList = DiseaseDAO.getCatchedDiseasesList(session);
			for (DiseaseBean diseaseBean : diseaseList) {
				if(!dateError) {
					dTotal = NewsLetterDAO.getNewsletterCount(diseaseBean.getName(),fromDt,toDt);
				}else {
					dTotal = NewsLetterDAO.getNewsletterCount(diseaseBean.getName());	
				}
				total = total + dTotal;
				emailList.put(diseaseBean.getName(), Long.toString(dTotal));
			}
		} else if("1".equals(letterType)) {
			List<String> rewardList = new ArrayList<String>();
			rewardList.add("TalkAboutHealth Rewards");
			rewardList.add("Diet, Nutrition, Food, and Recipe Rewards");
			rewardList.add("Fitness and Exercise Rewards");
			rewardList.add("Skin and Beauty Rewards");
			rewardList.add("Pharmaceutical Rewards");
			rewardList.add("Family Rewards");
			for (String string : rewardList) {
				if(!dateError){
					dTotal = NewsLetterDAO.getNewsletterCount(string,fromDt,toDt);
				}else{
					dTotal = NewsLetterDAO.getNewsletterCount(string);	
				}
				//dTotal = NewsLetterDAO.getNewsletterCount(string);
				total = total + dTotal;
				emailList.put(string, Long.toString(dTotal));
			}
		} else if("2".equals(letterType)) {
			List<String> rewardList = new ArrayList<String>();
			//rewardList.add("workshop");
			//rewardList.add("Workshop summery");
			rewardList.add("Best of TalkAboutHealth");
			rewardList.add("TAH Workshop Notification");
			rewardList.add("TAH Workshop Summary");
			//rewardList.add("other");
			for (String string : rewardList) {
				if(!dateError) {
					dTotal = NewsLetterDAO.getNewsletterCount(string,fromDt,toDt);
				}else {
					dTotal = NewsLetterDAO.getNewsletterCount(string);	
				}
				//dTotal = NewsLetterDAO.getNewsletterCount(string);
				total = total + dTotal;
				emailList.put(string, Long.toString(dTotal));
			}
		} else if("3".equals(letterType)) {
			if(!dateError){
				emailList = NewsLetterDAO.getTalkerNewsletterCount(fromDt,toDt);
			}else{
				emailList = NewsLetterDAO.getTalkerNewsletterCount(null,null);
			}
			//emailList = NewsLetterDAO.getTalkerNewsletterCount();
			total = Long.parseLong(emailList.get("all"));
			emailList.remove("all");
		} else if("4".equals(letterType)) {
			if(!dateError){
				emailList = NewsLetterDAO.getTopicNewsletterCount(fromDt,toDt);
			}else{
				emailList = NewsLetterDAO.getTopicNewsletterCount(null,null);
			}
			//emailList = NewsLetterDAO.getTopicNewsletterCount();
			total = Long.parseLong(emailList.get("all"));
			emailList.remove("all");
		} else if("5".equals(letterType)) {
			List<String> rewardList = new ArrayList<String>();
			rewardList.add("Beauty and Skin Care");
			rewardList.add("Bone Health");
			rewardList.add("Cancer Genetics");
			rewardList.add("Cancer Medications");
			rewardList.add("Cancer Prevention");
			rewardList.add("Cancer Screening");
			rewardList.add("Cancer Treatments");
			rewardList.add("Depression");
			rewardList.add("Emotional Health");
			rewardList.add("Fertility");
			rewardList.add("Fitness");
			rewardList.add("Food and Nutrition");
			rewardList.add("Integrative Medicine");
			rewardList.add("Joint Health");
			rewardList.add("Mens Health");
			rewardList.add("Menopause");
			rewardList.add("Mood and Stress");
			rewardList.add("Oral Health");
			rewardList.add("Pain Management");
			rewardList.add("Parenting");
			rewardList.add("Pregnancy");
			rewardList.add("Sex and Relationships");
			rewardList.add("Sleep Management");
			rewardList.add("Vitamins and Supplements");
			rewardList.add("Weight Loss and Diet");
			rewardList.add("Womens Health");
			for (String string : rewardList) {
				if(!dateError){
					dTotal = NewsLetterDAO.getNewsletterCount(string,fromDt,toDt);
				}else{
					dTotal = NewsLetterDAO.getNewsletterCount(string);	
				}
				//dTotal = NewsLetterDAO.getNewsletterCount(string);
				total = total + dTotal;
				emailList.put(string, Long.toString(dTotal));
			}	
		}
		
		render(emailList,total,letterType,fromDate,toDate,errorMsg);
	}

	public static void emailList(String emailList) {
		List<String> emailLIst = NewsLetterDAO.getNewsletterEmail(emailList);
		render(emailLIst);
	}
	
	public static void moveEmail(String type) {
		if("topic".equals(type)) {
			NewsLetterDAO.moveNewsletters();
		} else if ("expert".equals(type)) {
			NewsLetterDAO.moveNewslettersExpert();
		}
		renderText("OK");
	}
	
	public static void emailLookup(String email) {
		validation.email(email);
		String errorMsg = "";
		NewsLetterBean letterBean = null;
		if(validation.hasErrors()){
			errorMsg = "Wrong Email";
		} else {
			letterBean = NewsLetterDAO.getNewsLetterInfo(email);
		}
		render(email,errorMsg,letterBean);
	}
	
	public static void changeNewsletterName(String oldName,String newName) {
		List<String> emailList = NewsLetterDAO.getNewsletterEmail(oldName);
		NewsLetterBean letterBean = null;
		int count = 0;
		if(!emailList.isEmpty()) {
			for (String email : emailList) {
				letterBean = NewsLetterDAO.getNewsLetterInfo(email);
				String[] newLetterTypes = letterBean.getNewsLetterType();
				for (int i = 0; i < newLetterTypes.length; i++) {
					if(newLetterTypes[i].equals(oldName)){
						newLetterTypes[i] = newLetterTypes[i].replaceAll(oldName, newName);
						//System.out.println(letterBean.getEmail() + " : Name" + newLetterTypes[i]);
					}
				}
				letterBean.setNewsLetterType(newLetterTypes);
				NewsLetterDAO.saveOrUpdateNewsletter(letterBean, null);
				
				count++;
			}					
		}
		renderText("Done: " + count);
	}
}
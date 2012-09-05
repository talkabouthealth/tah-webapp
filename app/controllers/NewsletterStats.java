package controllers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import models.DiseaseBean;
import dao.DiseaseDAO;
import dao.NewsLetterDAO;
import play.mvc.Controller;
import play.mvc.With;


/**
 * Admin Dashboard : NewsletterStats
 *
 */
@Check("admin")
@With(Secure.class)
public class NewsletterStats extends Controller {
	
	public static void index(String letterType,String fromDate,String toDate) {
		
		String errorMsg = "";
		DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
		boolean dateError = false;
		if(fromDate != null && !"".equals(fromDate)){
			try{
				Date fromDt = dateFormat.parse(fromDate);
				Date toDt = dateFormat.parse(toDate);
				if(toDt.before(fromDt)){
					dateError = true;
				}
			}catch(Exception e){
				//e.printStackTrace();
				dateError = true;
			}
		}

		if(letterType == null){
			letterType = "2";
		}
		
		Map<String, String> emailList = new HashMap<String, String>();
		long total = 0;
		long dTotal;
		if("0".equals(letterType)) {
			List<DiseaseBean> diseaseList = DiseaseDAO.getCatchedDiseasesList(session);
			for (DiseaseBean diseaseBean : diseaseList) {
				dTotal = NewsLetterDAO.getNewsletterCount(diseaseBean.getName());
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
				dTotal = NewsLetterDAO.getNewsletterCount(string);
				total = total + dTotal;
				emailList.put(string, Long.toString(dTotal));
			}
		} else if("2".equals(letterType)) {
			List<String> rewardList = new ArrayList<String>();
			rewardList.add("workshop");
			rewardList.add("Workshop summery");
			rewardList.add("Best of TalkAboutHealth");
			for (String string : rewardList) {
				dTotal = NewsLetterDAO.getNewsletterCount(string);
				total = total + dTotal;
				emailList.put(string, Long.toString(dTotal));
			}
		} else if("3".equals(letterType)) {
			emailList = NewsLetterDAO.getTalkerNewsletterCount();
			total = Long.parseLong(emailList.get("all"));
			emailList.remove("all");
		} else if("4".equals(letterType)) {
			emailList = NewsLetterDAO.getTopicNewsletterCount();
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
				dTotal = NewsLetterDAO.getNewsletterCount(string);
				total = total + dTotal;
				emailList.put(string, Long.toString(dTotal));
			}	
		}
		if(dateError){
			errorMsg = "Wrong dates selected";
		}
		render(emailList,total,letterType,fromDate,toDate,errorMsg);
	}

	public static void emailList(String emailList) {
		List<String> emailLIst = NewsLetterDAO.getNewsletterEmail(emailList);
		render(emailLIst);
	}
}
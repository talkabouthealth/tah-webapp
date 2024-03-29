package controllers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.AdvertisementBean;
import models.TalkerBean;

import org.apache.commons.lang.StringUtils;

import dao.AdvertisementDAO;
import dao.NewsLetterDAO;
import play.mvc.Controller;
import util.CommonUtil;

public class Advertisement extends Controller {

	public static void index(String addId) {
		String oldSessionId = session.get("thisSessionId");
		String sessionId = session.getId();
		String clickCount = session.get("click" + addId);
		if(StringUtils.isNotEmpty(oldSessionId) && oldSessionId.equals(sessionId)) {
			if(StringUtils.isNotBlank(clickCount)) {
				//session.remove("click" + addId);
			} else {
				AdvertisementDAO.populateStats(addId, "click",  true);
				session.put("click" + addId,"1");
			}
		} else {
			AdvertisementDAO.populateStats(addId, "click",  true);
		}
		//redirect("https://attendee.gotowebinar.com/register/6386058441856173568");
		redirect("http://www.fifthseasonfinancial.com/");
	}
	
	public static void logReport(String fromDate,String toDate,boolean group) {
		
		if (Security.isConnected()) {
			TalkerBean talker = CommonUtil.loadCachedTalker(session);
			if(talker.isAdmin()) {
				String errorMsg = "No/Wrong date range will display complete stats";
				List<AdvertisementBean> emailList;// = new HashMap<String, String>();
				DateFormat dateFormat = new SimpleDateFormat("yyyy-dd-MM");//09/04/2012
				Date fromDt = new Date();
				Date toDt = new Date();
				int totalImp = 0, totalClick = 0;
				
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
					if(dateError){
						errorMsg = "Wrong dates selected";
					}
				}else{
					dateError = true;
				}
				if(dateError) {
					emailList = AdvertisementDAO.getAdvertisementCount(null,null,group);
				} else {
					emailList = AdvertisementDAO.getAdvertisementCount(fromDt,toDt,group);
				}
				if(!group) {
					AdvertisementBean bean = emailList.get(emailList.size() - 1);
					totalClick = bean.getAdCount();
					emailList.remove(bean);	
					
					bean = emailList.get(emailList.size() - 1);
					totalImp = bean.getAdCount();
					emailList.remove(bean);
				}
				render(emailList,totalImp,totalClick,fromDate,toDate,errorMsg,group);
			} else {
				forbidden();
			}
		} else {
			forbidden();
		}
	}
}

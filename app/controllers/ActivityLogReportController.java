package controllers;

import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.codec.StringDecoder;
import org.apache.commons.lang.StringUtils;

import dao.ActivityLogDAO;

import models.ActivityLogBean;
import models.TalkerBean;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;
import play.mvc.Http.Header;
import play.mvc.Http.Request;
import util.CommonUtil;

@Check("admin")
@With(Secure.class)
public class ActivityLogReportController  extends Controller {

	public static void logReport() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		
		ArrayList<ActivityLogBean> logList = ActivityLogDAO.getLogList(calendar.getTime());
		boolean group = false;
		render("Dashboard/activitylogReport.html",logList,group);
	}
	
	public static void logSortedReport(boolean group,String dateString) {
		
		System.out.println("Group : " + group);
		
		Date date = new Date();
		Calendar calendar = Calendar.getInstance();
		if(StringUtils.isNotBlank(dateString)) {
			try { //10/31/2012 : To get date check exact date format on view side script
				date = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH).parse(dateString);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		calendar.setTime(date);
		calendar.set(Calendar.HOUR, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		if(group){
			Map<String, Integer> logList = ActivityLogDAO.getLogListGrouped(calendar.getTime());
			render("Dashboard/activitylogReport.html",logList,dateString,group);
		} else {
			ArrayList<ActivityLogBean> logList = ActivityLogDAO.getLogList(calendar.getTime());	
			render("Dashboard/activitylogReport.html",logList,dateString,group);
		}
	}
}
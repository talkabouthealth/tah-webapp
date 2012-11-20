package controllers;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.StringDecoder;
import org.apache.commons.lang.StringUtils;

import dao.ActivityLogDAO;

import models.ActivityLogBean;
import models.TalkerBean;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Header;
import play.mvc.Http.Request;
import util.CommonUtil;

public class ActivityLogController  extends Controller{

	// ActivityLogController
	public static void log() {

		if(Security.isConnected() && Security.connected().equals("admin")) {
			renderText("Admin");
			return;
		}

		//Page Type
		String pageType = params.get("pageType");
		if(StringUtils.isEmpty(pageType))
			pageType = "Other";

		//Remote IP address
		String remoteIp = request.remoteAddress;
		if(request.headers.containsKey("X-Forwarded-For")) {
			remoteIp = request.headers.get("X-Forwarded-For").value();
			remoteIp = remoteIp.substring(0, remoteIp.indexOf(",")); 
		}

		//Header Values
		String userAgent = "";
		String userLanguage = "";
		String userCookie = "";
		String cancerSite = "";
		Set<String> heads =  request.headers.keySet();
		for (String string : heads) {
			if(string.equals("user-agent"))
				userAgent = request.headers.get("user-agent").value();
			if(string.equals("accept-language"))
				userLanguage = request.headers.get("accept-language").value();
			if(string.equals("cookie"))
				userCookie = request.headers.get("cookie").value();
			if(StringUtils.isNotBlank(session.get("cancerType")))
				cancerSite = session.get("cancerType");
			// accept-language, cookie
			// System.out.println(string + " : " + request.headers.get(string).value());
			// TAH Or TalkBreastCancer
		}

		//Location
		String userLocationCode = params.get("geoLcode");
		String userLocationCountry = params.get("geoLcountry");
		String userLocationState = params.get("geoLstate");
		String userLocationCity = params.get("geoLcity");
		String userLocationLatitude = params.get("geoLat");
		String userLocationLongitude = params.get("geoLong");

		/*System.out.println("userLocationCode: " + userLocationCode);
		System.out.println("userLocationCountry: " + userLocationCountry);
		System.out.println("userLocationState: " + userLocationState);
		System.out.println("userLocationCity: " + userLocationCity);
		System.out.println("userLocationLatitude: " + userLocationLatitude);
		System.out.println("userLocationLongitude: " + userLocationLongitude);
*/
		ActivityLogBean logBean = new ActivityLogBean(
							remoteIp,
							pageType,
							URLDecoder.decode(params.get("page")),
							URLDecoder.decode(params.get("ref")),
							userAgent,
							session.getId(),
							"",
							"");

		logBean.setUserLanguage(userLanguage);
		logBean.setUserCookie(userCookie);
		logBean.setCancerSite(cancerSite);
		
		//Location details
		logBean.setUserLocationCode(userLocationCode);
		logBean.setUserLocationCountry(userLocationCountry);
		logBean.setUserLocationState(userLocationState);
		logBean.setUserLocationCity(userLocationCity);
		logBean.setUserLocationLatitude(userLocationLatitude);
		logBean.setUserLocationLongitude(userLocationLongitude);
		if(Security.isConnected()) {
			TalkerBean talker = CommonUtil.loadCachedTalker(session);
			logBean.setUserEmail(talker.getEmail());
			logBean.setUserName(talker.getName());
		}
		renderText(ActivityLogDAO.logRequest(logBean));
		//renderText("OK");
	}

	/*
	public static void logouttime() {
		String logId = params.get("logId");
		System.out.println("Log Id: " + logId);
		ActivityLogDAO.logOutTime(logId);
		renderText("Done");
	}
	*/
}
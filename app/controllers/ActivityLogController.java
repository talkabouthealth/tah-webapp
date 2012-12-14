package controllers;

import java.net.URLDecoder;
import java.util.Set;

import models.ActivityLogBean;
import models.TalkerBean;

import org.apache.commons.lang.StringUtils;

import play.mvc.Controller;
import util.CommonUtil;
import dao.ActivityLogDAO;
import dao.AdvertisementDAO;

public class ActivityLogController  extends Controller{

	// ActivityLogController
	public static void log() {

		if(Security.isConnected() && Security.connected().equals("admin")) {
			renderText("Admin");
			return;
		}

		String oldSessionId = session.get("thisSessionId");
		String sessionId = session.getId();
		session.put("thisSessionId", sessionId);

		//Page Type
		String pageType = params.get("pageType");
		if(StringUtils.isEmpty(pageType))
			pageType = "Other";

		//Remote IP address
		String remoteIp = request.remoteAddress;
		if(request.headers.containsKey("x-forwarded-for")) { //x-forwarded-for: Getting remote address if not in request. Nginx manage this
			remoteIp = request.headers.get("x-forwarded-for").value();
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
		}

		String addrArray = null;

		//Location
		String userLocationCode = params.get("geoLcode");
		String userLocationCountry = params.get("geoLcountry");
		String userLocationState = params.get("geoLstate");
		String userLocationCity = params.get("geoLcity");
		String userLocationLatitude = params.get("geoLat");
		String userLocationLongitude = params.get("geoLong");

		if(!userLocationCode.equals("unknown")) {
			addrArray = userLocationCode + "," + userLocationCountry + "," + userLocationState + "," + userLocationCity + "," + userLocationLatitude + "," + userLocationLongitude;
			session.put("address", addrArray);
		}
		if(StringUtils.isNotEmpty(oldSessionId) && oldSessionId.equals(sessionId)) {
			addrArray =  session.get("address");
			String addressArray[] = addrArray.split(",");
			userLocationCode = addressArray[0];
			userLocationCountry = addressArray[1];
			userLocationState = addressArray[2];
			userLocationCity = addressArray[3];
			userLocationLatitude = addressArray[4];
			userLocationLongitude = addressArray[5];
			addrArray = userLocationCode + "," + userLocationCountry + "," + userLocationState + "," + userLocationCity + "," + userLocationLatitude + "," + userLocationLongitude;
			session.put("address", addrArray);
		} else {
			addrArray = userLocationCode + "," + userLocationCountry + "," + userLocationState + "," + userLocationCity + "," + userLocationLatitude + "," + userLocationLongitude;
			session.put("address", addrArray);
		}

		ActivityLogBean logBean = new ActivityLogBean(
							remoteIp,
							pageType,
							URLDecoder.decode(params.get("page")),
							URLDecoder.decode(params.get("ref")),
							userAgent,
							sessionId,
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
		
		if(isAdvertisementPage(pageType)) {
			AdvertisementDAO.populateStats("1", "impression",  true);
		}
		
		renderText(ActivityLogDAO.logRequest(logBean));
	}
	
	private static boolean isAdvertisementPage(String pageName) {
		//conversationSummary, 
		boolean returnFlag = false;
		if(StringUtils.isNotBlank(pageName) && ("conversationSummary".equals(pageName) || "topicPage".equals(pageName) || "cancerPage".equals(pageName) )) {
			returnFlag = true;
		}
		return returnFlag;
	}
}

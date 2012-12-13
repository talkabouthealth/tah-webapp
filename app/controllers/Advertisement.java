package controllers;

import org.apache.commons.lang.StringUtils;

import dao.AdvertisementDAO;
import play.mvc.Controller;

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
		redirect("http://www.fifthseasonfinancial.com/");
	}
}
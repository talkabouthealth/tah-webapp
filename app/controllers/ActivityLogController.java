package controllers;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

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
		/* 
		System.out.println("IP: " + request.remoteAddress);
		System.out.println("Page: " + URLDecoder.decode(params.get("page")));
		System.out.println("pageType: " + params.get("pageType"));
		System.out.println("Referer: " + URLDecoder.decode(params.get("ref")));
		System.out.println("Session: "+ session.getId());

		HashMap<String, Http.Header> head = (HashMap<String, Header>) request.headers;
		Iterator<Entry<String, Header>> it = head.entrySet().iterator();

		while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        System.out.println(pairs.getKey() + " = " + pairs.getValue());
	        it.remove();
	    }
		*/
		if(Security.isConnected() && Security.connected().equals("admin")) {
			renderText("Admin");
			return;
		}
		String pageType = params.get("pageType");
		if(StringUtils.isEmpty(pageType))
			pageType = "Other";

		ActivityLogBean logBean = new ActivityLogBean(
									request.remoteAddress,
									pageType,
									URLDecoder.decode(params.get("page")),
									URLDecoder.decode(params.get("ref")),
									request.headers.get("user-agent").value(),
									session.getId(),
									"",
									"");
		if(Security.isConnected()) {
			TalkerBean talker = CommonUtil.loadCachedTalker(session);
			/*
			System.out.println("e-Mail: " + talker.getEmail());
			System.out.println("User Name: " + talker.getName());
			*/
			logBean.setUserEmail(talker.getEmail());
			logBean.setUserName(talker.getName());
		}
		if(ActivityLogDAO.logRequest(logBean))
			renderText("DONE");
		else
			renderText("ERROR");
	}
}
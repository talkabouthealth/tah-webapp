package controllers;

import java.util.Date;

import play.mvc.Controller;

import util.CommonUtil;

import models.TalkerBean;
import models.ThankYouBean;
import dao.TalkerDAO;

public class Actions extends Controller {
	
	public static void createThankYou(String toTalker, String note) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		ThankYouBean thankYouBean = new ThankYouBean();
		thankYouBean.setTime(new Date());
		thankYouBean.setNote(note);
		thankYouBean.setFrom(talker.getId());
		thankYouBean.setTo(toTalker);
		
		TalkerDAO.saveThankYou(thankYouBean);
	}

}

package controllers;

import org.apache.commons.lang.StringUtils;

import antlr.collections.List;
import models.ConversationBean;
import models.NewsLetterBean;
import models.TalkerBean;
import models.VideoBean;
import play.data.validation.Email;
import play.data.validation.Error;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;
import dao.ConversationDAO;
import dao.NewsLetterDAO;
import dao.TalkerDAO;
import dao.VideoDAO;

@With( LoggerController.class )
public class MicroSite extends Controller {

	public static void video(String name) {
		String cancerType = session.get("cancerType");
		render("/MicroSite/video.html",cancerType);
	}

	public static void medical_professionals(String name) {
		String cancerType = session.get("cancerType");
		render(cancerType);
	}

	public static void workshops(String name) {
		String cancerType = session.get("cancerType");
		render(cancerType);
   }
}
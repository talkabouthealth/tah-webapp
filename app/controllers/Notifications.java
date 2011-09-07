package controllers;

import static util.DBUtil.getCollection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.DBCollection;

import logic.TalkerLogic;
import models.NotificationBean;
import models.TalkerBean.EmailSetting;

import dao.ConversationDAO;
import dao.QuestionDAO;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;
import util.EmailUtil;
import util.NotificationUtils;
import util.EmailUtil.EmailTemplate;

@With( { Secure.class, LoggerController.class } )
public class Notifications extends Controller{
	
	public static final String ADMIN = "admin";
	
	public static void index(String id, String details) {
		
		//check admin user 
		if(!session.get("username").equalsIgnoreCase(ADMIN)){
			redirect("/home");
		}
		
		//topicsList
		if(id == null){
			List<NotificationBean> list = QuestionDAO.loadAllQuestions();
			render(list);
		}else if(details != null){
			NotificationBean bean = QuestionDAO.loadNotification(id);
			bean.setFlag(true);
			String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", bean.getConvos().getMainURL());
			Map<String, String> vars = new HashMap<String, String>();

			if(!"REMOVE".equals(details)){
				vars.put("other_talker", bean.getTalker().getUserName());
				vars.put("question", bean.getConvos().getTopic());
				vars.put("details", details);
				vars.put("convo_url",convoURL);

				EmailUtil.sendEmail(EmailTemplate.NOTIFICATION_PERSONAL_QUESTION_MODERATED, bean.getTalker().getEmail(), vars, null, true); 
			}
			QuestionDAO.updateNotification(bean);

			//After all
			List<NotificationBean> list = QuestionDAO.loadAllQuestions();
			render(list);

		} else {
			NotificationBean bean = QuestionDAO.loadNotification(id);
			render(bean);
		}
	}
}
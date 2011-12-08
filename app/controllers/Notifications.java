package controllers;

import static util.DBUtil.getCollection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.DBCollection;

import logic.TalkerLogic;
import models.ConversationBean;
import models.NotificationBean;
import models.TalkerBean.EmailSetting;
import models.actions.StartConvoAction;
import models.actions.Action.ActionType;

import dao.ActionDAO;
import dao.ConversationDAO;
import dao.QuestionDAO;
import dao.TalkerDAO;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;
import util.EmailUtil;
import util.NotificationUtils;
import util.EmailUtil.EmailTemplate;

@With( { Secure.class, LoggerController.class } )
public class Notifications extends Controller{
	
	public static final String ADMIN = "admin";
	public static final String WAITING = "Waiting";
	public static final String HIDDEN = "Hidden";
	public static final String ACTIVE = "Active";
	
	public static void index(String id, String details, String expertName, String note, String state) {
		
		//check admin user 
		if(!session.get("username").equalsIgnoreCase(ADMIN)) {
			redirect("/home");
		}
		
		//topicsList
		if(id == null) {
			
			List<ConversationBean> list = QuestionDAO.loadAllQuestion();
			render(list);
			
		} else if(details != null) {
			
			ConversationBean bean = ConversationDAO.getById(id);
			details = details.trim();
			if("NOTIFY".equals(details)){
				String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", bean.getMainURL());
				String expertEmail = TalkerDAO.getByUserName(expertName) == null ? "" : TalkerDAO.getByUserName(expertName).getEmail();
				Map<String, String> vars = new HashMap<String, String>();
				if("".equals(note)) {
					 //other_talker * convo  * convo_url  
					//vars.put("other_talker", bean.getTalker().getUserName());
					vars.put("other_talker", bean.getTalker().getUserName());
					vars.put("convo", bean.getTopic());
					//vars.put("details", bean.getDetails());
					vars.put("convo_url",convoURL);
					EmailUtil.sendEmail(EmailTemplate.NOTIFICATION_PERSONAL_QUESTION, expertEmail, vars, null, true);					
				} else {
					//vars.put("other_talker", bean.getTalker().getUserName());
					vars.put("other_talker", bean.getTalker().getUserName());
					vars.put("question", bean.getTopic());
					vars.put("details", bean.getDetails());
					vars.put("note", note);
					vars.put("convo_url",convoURL);
					EmailUtil.sendEmail(EmailTemplate.NOTIFICATION_PERSONAL_QUESTION_MODERATED, expertEmail, vars, null, true);	
				}
				
			}else if("CHANGE_STATE".equals(details)){
				
				ConversationBean conversationBean = new ConversationBean();
				conversationBean = ConversationDAO.getById(id);
				conversationBean.setQuestionState(state);			
				if(state.equals(ACTIVE)){
					String actionID = ActionDAO.saveActionGetId(new StartConvoAction(conversationBean.getTalker(), conversationBean, ActionType.START_CONVO));
					conversationBean.setActionID(actionID);
					conversationBean.setDeleted(false);
				}else if(state.equals(HIDDEN)){
					conversationBean.setDeleted(true);
					//remove related actions
			    	ActionDAO.deleteActionsByConvo(conversationBean);
				}else{
					conversationBean.setDeleted(false);
					//remove related actions
			    	ActionDAO.deleteActionsByConvo(conversationBean);
				}
				
				ConversationDAO.updateConvo(conversationBean);
					
			}else if(!"REMOVE".equals(details)) {
				
				bean.setAdminComments(details);
				QuestionDAO.updateNotification(bean);
				renderText("OK");
				
			} else if("REMOVE".equals(details)) {
				
				bean.setRemovedByadmin(true);
				QuestionDAO.updateNotification(bean);
				
			}
			//After all
			List<ConversationBean> list = QuestionDAO.loadAllQuestion();
			render(list);

		} else {
			NotificationBean bean = QuestionDAO.loadNotification(id);
			render(bean);
		}
	}
}
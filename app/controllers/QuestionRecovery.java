package controllers;

import java.util.List;

import models.ConversationBean;
import models.actions.StartConvoAction;
import models.actions.Action.ActionType;
import play.mvc.Controller;
import play.mvc.With;
import dao.ActionDAO;
import dao.ConversationDAO;
import dao.QuestionRecoveryDAO;

@With( { Secure.class, LoggerController.class } )
public class QuestionRecovery extends Controller{
	
	public static final String ADMIN = "admin";
	public static final String ACTIVE = "Active";
	public static final String WAITING = "Waiting";
	public static final String HIDDEN = "Hidden";
	
	public static void index(String id, String action, String state){
		//check admin user 
		if(!session.get("username").equalsIgnoreCase(ADMIN)){
			redirect("/home");
		}
		
		if(action != null && action.equalsIgnoreCase("changeState")){
			ConversationBean conversationBean = ConversationDAO.getById(id);
			conversationBean.setQuestionState(state);
			
			
			if(state.equalsIgnoreCase(ACTIVE)){
				String actionID = ActionDAO.saveActionGetId(new StartConvoAction(conversationBean.getTalker(), conversationBean, ActionType.START_CONVO));
				conversationBean.setActionID(actionID);
				conversationBean.setDeleted(false);
			}else if(state.equalsIgnoreCase(HIDDEN)){
				conversationBean.setDeleted(true);
				//remove related actions
		    	ActionDAO.deleteActionsByConvo(conversationBean);
			}else{
				conversationBean.setDeleted(false);
				//remove related actions
		    	ActionDAO.deleteActionsByConvo(conversationBean);
			}
			
			ConversationDAO.updateConvo(conversationBean);
		}
			
		List<ConversationBean> list = QuestionRecoveryDAO.getConvos();
		
		render(list);
	}

}

package controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.CommentBean;
import models.ConversationBean;
import models.TalkerBean;
import models.actions.AnswerConvoAction;
import models.actions.Action.ActionType;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;
import util.EmailUtil;
import util.EmailUtil.EmailTemplate;
import controllers.LoggerController;
import controllers.Secure;
import dao.ActionDAO;
import dao.AnswerNotificationDAO;
import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;
import dao.UserListDAO;

@With( { Secure.class, LoggerController.class } )
public class AnswerNotification extends Controller {
	
	public static final String ADMIN = "admin";
	public static final String NOT_HELPFUL = "Not Helpful";
	public static final String APPROVE_ANSWER = "Approve Answer";
	public static final String DELETE_ANSWER = "Delete Answer";
	
	public static void index(String id, String action, String moderate) {
		
		//check admin user 
		if(!session.get("username").equalsIgnoreCase(ADMIN)){
			redirect("/home");
		}
		String question = "";
		
		if(action == null){
			List<ConversationBean> listTemp = AnswerNotificationDAO.getConvos();
			List<ConversationBean> list = new ArrayList();
			for(int index = 0; index < listTemp.size(); index++){
				List<CommentBean> commentList= AnswerNotificationDAO.getConvoComments(listTemp.get(index).getId());
				if(commentList.size() > 0){
					list.add(listTemp.get(index));
				}
			}
			render(list);
		}else if(action != null && action.equalsIgnoreCase("getAnswers")){
			List<CommentBean> answerList = CommentsDAO.loadConvoAnswersTree(id);
			List<CommentBean> commentList= answerList;
			if(commentList != null && commentList.size()>0){
				ConversationBean convo = ConversationDAO.getById(commentList.get(0).getConvoId());
				question = convo.getTopic();
			}
			render(commentList,question);
		}else if(action != null && action.equalsIgnoreCase("moderateAnswer")){
			CommentBean comment = CommentsDAO.getConvoCommentById(id);
			if(moderate != null && !moderate.equalsIgnoreCase("select")){
				comment.setModerate(moderate);
				if(moderate.equalsIgnoreCase(APPROVE_ANSWER)){
					AnswerNotificationDAO.sendMailToFollowers(comment);
				}
				if(moderate.equalsIgnoreCase(NOT_HELPFUL)){
					comment.setNotHelpful(true);
				}else{
					comment.setNotHelpful(false);
				}
			}
			CommentsDAO.updateConvoComment(comment);
			List<CommentBean> answerList = CommentsDAO.loadConvoAnswersTree(comment.getConvoId());
			List<CommentBean> commentList= answerList;
			if(commentList != null && commentList.size()>0){
				ConversationBean convo = ConversationDAO.getById(commentList.get(0).getConvoId());
				question = convo.getTopic();
				//insert answer details in activity
				if(!moderate.equalsIgnoreCase(DELETE_ANSWER))
					ActionDAO.saveAction(new AnswerConvoAction(comment.getFromTalker(), convo, comment, null, ActionType.ANSWER_CONVO));
			}
			render(commentList,question);
		}
	}

}

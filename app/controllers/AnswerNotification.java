package controllers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import logic.FeedsLogic;
import logic.FeedsLogic.FeedType;
import models.CommentBean;
import models.ConversationBean;
import models.TalkerBean;
import models.actions.Action;
import models.actions.AnswerConvoAction;
import models.actions.PersonalProfileCommentAction;
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
	
	public static void index(String id, String action, String moderate,String page) {
		
		//check admin user 
		if(!session.get("username").equalsIgnoreCase(ADMIN)){
			redirect("/home");
		}
		String question = "";
		if(action == null){
			
			page = page==null?"1":page;
			int pageNo = Integer.parseInt(page) - 1;
			int convoCount= session.get("convoCount") == null ? 0 : Integer.parseInt(session.get("convoCount").toString());
			if(convoCount == 0){
				convoCount = AnswerNotificationDAO.getAllConvoCount();
				if (convoCount > 0)
					session.put("convoCount", convoCount);
			}
			
			//check if list is available in session then get it from session otherwise from db
			List<ConversationBean> tempList = Cache.get("questionList") == null ? AnswerNotificationDAO.getConvos() : (List<ConversationBean>) Cache.get("questionList");
			if(tempList.size()>0)
				Cache.set("questionList", tempList);
			List<ConversationBean> list = new ArrayList<ConversationBean>();
			
			int count = AnswerNotificationDAO.CONVO_PER_PAGE * (pageNo+1);
			
			int prevCount = AnswerNotificationDAO.CONVO_PER_PAGE * pageNo;
			if(pageNo >1 && tempList.size() > count){
				count = tempList.size();
			}
			int condition = (count > prevCount+20 ? prevCount+20 : count);
			if(condition > tempList.size())
				condition = tempList.size();
			
			System.out.println(prevCount + " to " + condition);
			for(int index = prevCount ; index < condition; index++){
				ConversationBean convo = tempList.get(index);
				list.add(convo);
			}
						
			int flt = convoCount % AnswerNotificationDAO.CONVO_PER_PAGE;
			convoCount = convoCount / AnswerNotificationDAO.CONVO_PER_PAGE;
			if(flt > 0)
				convoCount++;
			
			render(list,convoCount,page);
			
		}else if(action != null && action.equalsIgnoreCase("getAnswers")){
			List<CommentBean> answerList = CommentsDAO.loadAllConvoAnswers(id);
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
			List<CommentBean> answerList = CommentsDAO.loadAllConvoAnswers(comment.getConvoId());
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
	
	/**
	 * 
	 * @param feedType
	 * @param beforeActionId
	 * @param talkerName
	 * @param isheader
	 */
	public static void feedAjaxUpdate(String feedType,String beforeActionId,String talkerName,String isheader) {
    	int counter = 0;
    	//Get records added in last minute
    	Calendar date = Calendar.getInstance();
		date.setTime(new Date());
		date.set(Calendar.SECOND, -60);
		
    	List<CommentBean> answerList = CommentsDAO.loadAllConvoAnswers(date.getTime());
    	if(answerList != null){
	    	counter = answerList.size();
    	}
    	
    	if(counter > 0){
    		render("tags/feed/answerFeedCounter.html",counter);
    	}
	}
}

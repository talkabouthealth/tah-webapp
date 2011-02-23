package controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import logic.ConversationLogic;
import models.CommentBean;
import models.ConversationBean;
import models.TalkerBean;
import models.TopicBean;
import models.ConversationBean.ConvoType;
import models.actions.FollowConvoAction;
import models.actions.StartConvoAction;
import models.actions.Action.ActionType;
import dao.ActionDAO;
import dao.ApplicationDAO;
import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;
import dao.TopicDAO;
import play.mvc.Before;
import play.mvc.Controller;
import util.CommonUtil;
import util.NotificationUtils;

/**
 * API for IM bot
 *
 */
public class API extends Controller {
	
	private static final String API_PASS = "b6-$2NNs7dq1!p";
	
	@Before(unless={})
    static void checkAccess() throws Throwable {
        String passParam = params.get("pass");
        if (passParam == null || !passParam.equals(API_PASS)) {
        	forbidden();
        }
    }
	
	/**
	 * Create answer or reply to a conversation.
	 * @param convoId
	 * @param talkerId
	 * @param parentId id of parent answer (for replies)
	 * @param text
	 */
	public static void createAnswer(String convoId, String talkerId, String parentId, String text) {
		TalkerBean authorTalker = TalkerDAO.getById(talkerId);
		notFoundIfNull(authorTalker);
		
		ConversationBean convo = null;
		if(convoId == null || convoId.length() == 0) {
			//try to find convo by parent answer
			CommentBean answer = CommentsDAO.getConvoCommentById(parentId);
			if (answer != null) {
				convo = ConversationDAO.getById(answer.getConvoId());
			}
		}
		else {
			convo = ConversationDAO.getById(convoId);
		}
		notFoundIfNull(convo);
		
		CommentBean comment = ConversationLogic.createAnswerOrReply(convo, authorTalker, parentId, text);
		renderText(comment.getId());
	}
	
	/**
	 * 
	 * @param type String 'Question' or 'Conversation'
	 * @param title
	 * @param talkerId
	 */
	public static void createConvo(String type, String title, String talkerId) {
    	TalkerBean talker = TalkerDAO.getById(talkerId);
    	notFoundIfNull(talker);
    	
    	ConvoType convoType = ConvoType.CONVERSATION;
    	if ("Question".equalsIgnoreCase(type)) {
    		convoType = ConvoType.QUESTION;
    	}
    	ConversationBean convo = 
    		ConversationLogic.createConvo(convoType, title, talker, null, null, true, null, null, null);
		renderText(convo.getTid());
    }
	
}

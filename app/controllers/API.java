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

public class API extends Controller {
	
	private static final String API_PASS = "TestPass";
	
	@Before(unless={})
    static void checkAccess() throws Throwable {
        String passParam = params.get("pass");
        if (passParam == null || !passParam.equals(API_PASS)) {
        	forbidden();
        }
    }
	
	public static void createAnswer(String convoId, String talkerId, String parentId, String text) {
		TalkerBean authorTalker = TalkerDAO.getById(talkerId);
		System.out.println("TALKER: "+authorTalker+"<");
		notFoundIfNull(authorTalker);
		
		ConversationBean convo = null;
		System.out.println("CONVOID: "+convoId+"<");
		if(convoId == null || convoId.length() == 0) {
			//try to find convo by parent answer
			CommentBean answer = CommentsDAO.getConvoAnswerById(parentId);
			System.out.println("ANSWER: "+answer);
			if (answer != null) {
				convo = ConversationDAO.getByConvoId(answer.getTopicId());
			}
		}
		else {
			convo = ConversationDAO.getByConvoId(convoId);
		}
		notFoundIfNull(convo);
		
		CommentBean comment = ConversationLogic.createAnswer(convo, authorTalker, parentId, text);
		renderText(comment.getId());
	}
	
	
	public static void createConvo(String title, String talkerId) {
    	TalkerBean talker = TalkerDAO.getById(talkerId);
    	notFoundIfNull(talker);
    	
    	//FIXME: make type
    	ConversationBean convo = ConversationLogic.createConvo(ConvoType.CONVERSATION, title, talker, null, null);
    	
		renderText(convo.getTid());
    }
	
}

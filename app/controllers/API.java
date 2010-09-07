package controllers;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import models.CommentBean;
import models.ConversationBean;
import models.TalkerBean;
import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;
import play.mvc.Before;
import play.mvc.Controller;
import util.CommonUtil;

public class API extends Controller {
	
	public static final String API_PASS = "TestPass";
	
	@Before(unless={})
    static void checkAccess() throws Throwable {
        String passParam = params.get("pass");
        if (passParam == null || !passParam.equals(API_PASS)) {
        	forbidden();
        }
    }
	
	public static void createAnswer(String convoId, String authorId, String parentId, String text) {
		TalkerBean authorTalker = TalkerDAO.getById(authorId);
		notFoundIfNull(authorTalker);
		ConversationBean convo = ConversationDAO.getByConvoId(convoId);
		notFoundIfNull(convo);
		
		CommentBean comment = new CommentBean();
		comment.setParentId(parentId.trim().length() == 0 ? null : parentId);
		comment.setTopicId(convoId);
		comment.setFromTalker(authorTalker);
		comment.setText(text);
		comment.setTime(new Date());
		
		String id = CommentsDAO.saveConvoComment(comment);
		comment.setId(id);
		
//		@mnjones provided an Answer to your question:
//			"You will need lots of pillows."
//			(To reply to @mnjones, just reply with your message.)
//
//			@mnj5 replied to your answer:
//			"What are the pillows for?'"
//			(To reply to mnj5, just reply to this message.)
		
		
	}
	
}

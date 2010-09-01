package controllers;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import models.CommentBean;
import models.ConversationBean;
import models.TalkerBean;
import dao.CommentsDAO;
import dao.ConversationDAO;
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
	
	
	public static void createAnswer(String convoId, String parentId, String text) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		ConversationBean convo = ConversationDAO.getByConvoId(convoId);
		notFoundIfNull(convo);
		
		CommentBean comment = new CommentBean();
		comment.setParentId(parentId.trim().length() == 0 ? null : parentId);
		comment.setTopicId(convoId);
		comment.setFromTalker(talker);
		comment.setText(text);
		comment.setTime(new Date());
		
		String id = CommentsDAO.saveTopicComment(comment);
		comment.setId(id);
		
		//render html of new comment using tag
		List<CommentBean> _commentsList = Arrays.asList(comment);
		int _level = (comment.getParentId() == null ? 1 : 2);
		render("tags/topicCommentsTree.html", _commentsList, _level);
	}
	
}

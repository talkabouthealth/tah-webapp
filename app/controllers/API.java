package controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import models.CommentBean;
import models.ConversationBean;
import models.TalkerBean;
import models.TopicBean;
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
	
	
	public static void createConvo(String title) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	
		ConversationBean convo = new ConversationBean();
		convo.setTopic(title);
		convo.setUid(talker.getId());
		Date currentDate = Calendar.getInstance().getTime();
		convo.setCreationDate(currentDate);
		convo.setDisplayTime(currentDate);
		convo.setTopics(new ArrayList<TopicBean>());

		String topicURL = ApplicationDAO.createURLName(title);
		convo.setMainURL(topicURL);
		
		// insert new topic into database
		ConversationDAO.save(convo);
		ActionDAO.saveAction(new StartConvoAction(talker, convo, ActionType.START_CONVO));
		
		//send notifications if Automatic Notifications is On
		NotificationUtils.sendAutomaticNotifications(convo.getId());
		
		//automatically follow started topic
		talker.getFollowingConvosList().add(convo.getId());
		CommonUtil.updateTalker(talker, session);
		
		ActionDAO.saveAction(new FollowConvoAction(talker, convo));

		renderText("ok");
    }
	
}

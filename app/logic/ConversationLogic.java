package logic;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tah.im.IMNotifier;

import util.CommonUtil;
import util.NotificationUtils;
import dao.ActionDAO;
import dao.ApplicationDAO;
import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;
import models.CommentBean;
import models.ConversationBean;
import models.TalkerBean;
import models.TopicBean;
import models.TalkerBean.EmailSetting;
import models.actions.AnswerConvoAction;
import models.actions.FollowConvoAction;
import models.actions.StartConvoAction;
import models.actions.Action.ActionType;

public class ConversationLogic {
	
	public static ConversationBean createConvo(String title, TalkerBean talker) {
		ConversationBean convo = new ConversationBean();
		convo.setTopic(title);
		convo.setUid(talker.getId());
		Date currentDate = Calendar.getInstance().getTime();
		convo.setCreationDate(currentDate);
		convo.setDisplayTime(currentDate);
		convo.setTopics(new ArrayList<TopicBean>());

		String topicURL = ApplicationDAO.createURLName(title);
		convo.setMainURL(topicURL);
		
		//insert new topic into database
		ConversationDAO.save(convo);
		ActionDAO.saveAction(new StartConvoAction(talker, convo, ActionType.START_CONVO));
		
		//send notifications if Automatic Notifications is On
		NotificationUtils.sendAutomaticNotifications(convo.getId());
		
		//automatically follow started topic
		talker.getFollowingConvosList().add(convo.getId());
		TalkerDAO.updateTalker(talker);

		ActionDAO.saveAction(new FollowConvoAction(talker, convo));
		
		return convo;
	}
	
	public static CommentBean createAnswer(ConversationBean convo, TalkerBean talker, String parentId, String text) {
		//TODO: if answers - automatically follow topic?
		talker.getFollowingConvosList().add(convo.getId());
		ActionDAO.saveAction(new FollowConvoAction(talker, convo));
		convo.getFollowers().add(talker);
		
		CommentBean comment = new CommentBean();
		parentId = parentId.trim().length() == 0 ? null : parentId;
		comment.setParentId(parentId);
		comment.setTopicId(convo.getId());
		comment.setFromTalker(talker);
		comment.setText(text);
		comment.setTime(new Date());
		
		String id = CommentsDAO.saveConvoComment(comment);
		comment.setId(id);
		
		if (convo.isOpened()) {
			convo.setOpened(false);
			ConversationDAO.updateConvo(convo);
		}
		
		//actions
		if (parentId == null) {
			ActionDAO.saveAction(new AnswerConvoAction(talker, convo, comment, null, ActionType.ANSWER_CONVO));
		}
		else {
			CommentBean parentAnswer = new CommentBean(parentId);
			ActionDAO.saveAction(new AnswerConvoAction(talker, convo, parentAnswer, comment, ActionType.REPLY_CONVO));
		}
		
		//Email and IM notifications
		Set<String> talkersForNotification = new HashSet<String>();
		
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("convo", convo.getTopic());
		vars.put("other_talker", talker.getUserName());
		vars.put("answer_text", comment.getText());
		vars.put("convo_type", convo.getConvoType().stringValue());
		String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL());
		vars.put("convo_url", convoURL);
    	for (TalkerBean follower : convo.getFollowers()) {
    		if (!talker.equals(follower)) { //do not send notification to himself
    			talkersForNotification.add(follower.getId());
        		NotificationUtils.sendEmailNotification(EmailSetting.CONVO_COMMENT, follower, vars);
    		}
    	}
    	
    	if (!talkersForNotification.isEmpty()) {
			IMNotifier imNotifier = IMNotifier.getInstance();
			try {
				String[] uidArray = talkersForNotification.toArray(new String[talkersForNotification.size()]);
				imNotifier.answerNotify(uidArray, talker.getUserName(), convo.getId(), 
						comment.getParentId(), comment.getId(), comment.getText());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
    	
//		@mnjones provided an Answer to your question:
//		"You will need lots of pillows."
//		(To reply to @mnjones, just reply with your message.)
//
//		@mnj5 replied to your answer:
//		"What are the pillows for?'"
//		(To reply to mnj5, just reply to this message.)
    	
    	return comment;
	}

}
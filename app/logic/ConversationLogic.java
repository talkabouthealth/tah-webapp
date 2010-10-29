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
import dao.TopicDAO;
import models.CommentBean;
import models.ConversationBean;
import models.ConversationBean.ConvoType;
import models.TalkerBean;
import models.TopicBean;
import models.TalkerBean.EmailSetting;
import models.actions.AnswerConvoAction;
import models.actions.FollowConvoAction;
import models.actions.StartConvoAction;
import models.actions.Action.ActionType;

public class ConversationLogic {
	
	public static ConversationBean createConvo(ConvoType type, String title, 
			TalkerBean talker, String details, Set<TopicBean> topicsSet, boolean notifyTalkers) {
		
		//when a new topic is created, it automatically has a parent topic of "Unorganized"
		TopicBean topic = TopicDAO.getByTitle(TopicLogic.DEFAULT_TOPIC);
		if (topic != null) {
			topicsSet.add(topic);
		}
		
		ConversationBean convo = new ConversationBean();
		convo.setConvoType(type);
		convo.setTopic(title);
		convo.setUid(talker.getId());
		Date currentDate = Calendar.getInstance().getTime();
		convo.setCreationDate(currentDate);
		convo.setDetails(details);
		convo.setTopics(topicsSet);

		String topicURL = ApplicationDAO.createURLName(title);
		convo.setMainURL(topicURL);
		
		//insert new topic into database
		ConversationDAO.save(convo);
		ActionDAO.saveAction(new StartConvoAction(talker, convo, ActionType.START_CONVO));
		
		if (notifyTalkers) {
			//send notifications if Automatic Notifications is On
			NotificationUtils.sendAutomaticNotifications(convo.getId(), null);
		}
		
		//automatically follow started topic
		talker.getFollowingConvosList().add(convo.getId());
		TalkerDAO.updateTalker(talker);

		return convo;
	}
	
	public static CommentBean createAnswer(ConversationBean convo, TalkerBean talker, String parentId, String text) {
		//Follow convo automatically on answer?
//		if (!talker.getFollowingConvosList().contains(convo.getId())) {
//			talker.getFollowingConvosList().add(convo.getId());
//			TalkerDAO.updateTalker(talker);
//			ActionDAO.saveAction(new FollowConvoAction(talker, convo));
//			convo.getFollowers().add(talker);
//		}
		
		CommentBean comment = new CommentBean();
		parentId = parentId.trim().length() == 0 ? null : parentId;
		comment.setParentId(parentId);
		comment.setConvoId(convo.getId());
		comment.setFromTalker(talker);
		comment.setText(text);
		comment.setTime(new Date());
		
		if (parentId == null) {
			//it's an answer (not reply)
			comment.setAnswer(true);
		}
		
		String id = CommentsDAO.saveConvoComment(comment);
		comment.setId(id);
		
		if (convo.isOpened()) {
			convo.setOpened(false);
			ConversationDAO.updateConvo(convo);
		}
		
		//actions
		CommentBean answer = null;
		if (parentId == null) {
			ActionDAO.saveAction(new AnswerConvoAction(talker, convo, comment, null, ActionType.ANSWER_CONVO));
		}
		else {
			answer = CommentsDAO.getConvoAnswerById(parentId);
			ActionDAO.saveAction(new AnswerConvoAction(talker, convo, answer, comment, ActionType.REPLY_CONVO));
		}
		
		//Email and IM notifications
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("convo", convo.getTopic());
		vars.put("other_talker", talker.getUserName());
		if (comment.isAnswer()) {
			vars.put("answer_text", comment.getText());
		}
		else {
			vars.put("reply_text", comment.getText());
			vars.put("answer_text", answer.getText());
		}
		vars.put("convo_type", convo.getConvoType().stringValue());
		String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", convo.getMainURL());
		vars.put("convo_url", convoURL);
    	for (TalkerBean follower : convo.getFollowers()) {
    		if (!talker.equals(follower)) { //do not send notification to himself
        		NotificationUtils.sendEmailNotification(EmailSetting.CONVO_COMMENT, follower, vars);
    		}
    	}
    	
//    	The exception is users will receive IM notifications of answers 
//    	if they started the conversation/question or if they answered a question and 
//    	the user who started the question wants to follow up.
    	Set<String> talkersForNotification = new HashSet<String>();
    	talkersForNotification.add(convo.getTalker().getId());
    	if (answer != null) {
    		talkersForNotification.add(answer.getFromTalker().getId());
    	}
    	//do not send notification to himself
    	talkersForNotification.remove(talker.getId());
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
    	
    	return comment;
	}

}

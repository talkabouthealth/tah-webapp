package dao;

import static util.DBUtil.createRef;
import static util.DBUtil.getCollection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.CommentBean;
import models.ConversationBean;
import models.TalkerBean;
import models.TalkerBean.EmailSetting;
import models.actions.AnswerConvoAction;
import models.actions.Action.ActionType;

import org.bson.types.ObjectId;

import play.Logger;

import util.CommonUtil;
import util.NotificationUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

public class AnswerNotificationDAO {

	public static final String CONVO_COMMENTS_COLLECTION = "convocomments";
	public static final String CONVOS_COLLECTION = "convos";
	public static final int CONVO_PER_PAGE = 20;
	
	public static int getAllConvoCount(){
		int convoCount = 0;
		DBCollection convoColl = getCollection(CONVOS_COLLECTION);
		DBCollection commentColl = getCollection(CONVO_COMMENTS_COLLECTION);
		DBObject query = BasicDBObjectBuilder.start().get();
		DBCursor convo = convoColl.find(query);
		while(convo.hasNext()) {
			ConversationBean conversationBean = new ConversationBean();
			conversationBean.parseFromDB(convo.next());
			DBRef convoRef = createRef(CONVOS_COLLECTION, conversationBean.getId());
			DBObject query1 = BasicDBObjectBuilder.start()
				.add("convo", convoRef)
				.get();
			DBCursor name = commentColl.find(query1);
			if(name.size()>0)
				convoCount++;
		}
		CommonUtil.log("AnswerNotificationDAO.getAllConvoCount()", ""+convo.size());
		//Logger.info("getAllConvoCount : "+convo.size());
		return convoCount; 
		
	}
	/**
	 * Retrieve all questions
	 * @return List<ConversationBean> list of replies in ConversationBean form
	 */
	public static List<ConversationBean> getConvos() {//int convoCount,int pageNo
		DBCollection convoColl = getCollection(CONVOS_COLLECTION);
		
		DBObject query = BasicDBObjectBuilder.start().get();
		
		DBCursor cur = convoColl.find(query).sort(new BasicDBObject("cr_date", -1));
		List<ConversationBean> result = new ArrayList<ConversationBean>();
		while(cur.hasNext()) {
			ConversationBean conversationBean = new ConversationBean();
			conversationBean.parseFromDB(cur.next());
			
			List<CommentBean> commentList = getConvoComments(conversationBean.getId());
			if(commentList.size() > 0)
				result.add(conversationBean);
		}
		CommonUtil.log("AnswerNotificationDAO.getConvos", ""+cur.size());
		//Logger.info("getConvos : "+cur.size());
		return result;
	}
	
	/**
	 * Retrieve a set of comments by a list of convo id
	 * @param List<String> convoId
	 * @return List<CommentBean> list of replies in CommentBean form
	 */
	public static List<CommentBean> getConvoComments(String convoId) {
		DBCollection commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		DBRef convoRef = createRef(CONVOS_COLLECTION, convoId);
		
		DBObject query = BasicDBObjectBuilder.start()
			.add("convo", convoRef)
			.get();
		DBCursor cur = commentsColl.find(query);
		
		List<CommentBean> result = new ArrayList<CommentBean>();
		
		while(cur.hasNext()) {
			CommentBean comment = new CommentBean();
			comment.parseFromDB(cur.next());
			ConversationBean convo = ConversationDAO.getById(convoId);
			comment.setQuestion(convo.getTopic());
			result.add(comment);
		}
		CommonUtil.log("AnswerNOtificationDAO.getConvoComments", ""+cur.size());
		//Logger.info("getConvoComments : "+cur.size());
		return result;
	}
	
	/**
	 * Send Mail to question followers.
	 * @param commentBean
	 */
	public static void sendMailToFollowers(CommentBean commentBean){
		ConversationBean conversationBean = ConversationDAO.getById(commentBean.getConvoId());
		List<TalkerBean> followers = conversationBean.getFollowers();
		
		//Email notifications
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("convo", conversationBean.getTopic());
		vars.put("other_talker", commentBean.getFromTalker().getUserName());
		vars.put("answer_text", CommonUtil.commentToHTML(commentBean));
		vars.put("convo_type", conversationBean.getConvoType().stringValue());
		String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", conversationBean.getMainURL());
		vars.put("convo_url", convoURL);
		
		for (int index = 0; index < followers.size(); index++) {
			TalkerBean mailSendtalker = TalkerDAO.getByEmail(followers.get(index).getEmail());
			if(mailSendtalker.getEmailSettings().toString().contains("CONVO_COMMENT"))
				NotificationUtils.sendEmailNotification(EmailSetting.CONVO_COMMENT, followers.get(index), vars);
		}
	}
}

package dao;

import static util.DBUtil.getCollection;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import models.CommentBean;
import models.ConversationBean;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;

import controllers.QuestionRecovery;

public class QuestionRecoveryDAO {
	
	public static final String CONVOS_COLLECTION = "convos";

	/**
	 * Retrieve all questions
	 * @return List<ConversationBean> list of rconversations
	 */
	public static List<ConversationBean> getConvos() {//int convoCount,int pageNo
		DBCollection convoColl = getCollection(CONVOS_COLLECTION);
		
		DBObject query = BasicDBObjectBuilder.start()
		.add("question_state", new BasicDBObject("$ne", Arrays.asList(null,QuestionRecovery.ACTIVE) ))
		.get();
		QueryBuilder qb = QueryBuilder.start(); 
		qb.or(query);
		qb.or(BasicDBObjectBuilder.start().add("deleted", true).get());
		DBCursor cur = convoColl.find(qb.get()).sort(new BasicDBObject("cr_date", -1)).limit(20);
		
		List<ConversationBean> result = new ArrayList<ConversationBean>();
		
		while(cur.hasNext()) {
			ConversationBean conversationBean = new ConversationBean();
			conversationBean.parseFromDB(cur.next());
			if(conversationBean.isDeleted()){
				conversationBean.setQuestionState(QuestionRecovery.HIDDEN);
				
			}
			SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy, HH:MM");
			Date date = null;
			if(conversationBean.getModifiedDate() != null)
				date = conversationBean.getModifiedDate();
			else
				date = conversationBean.getCreationDate();
			String newDate = dateFormat.format(date);
			conversationBean.setDisplayDate(newDate);
			
			if(conversationBean.getQuestionState() != null && !conversationBean.getQuestionState().equals("") && !conversationBean.getQuestionState().equals(QuestionRecovery.ACTIVE)){
				result.add(conversationBean);
			}
		}
		return result;
	}
}

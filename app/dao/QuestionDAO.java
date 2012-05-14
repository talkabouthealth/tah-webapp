package dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import logic.TalkerLogic;
import models.ConversationBean;
import models.NotificationBean;
import models.TalkerBean;
import models.actions.Action.ActionType;

import org.bson.types.ObjectId;

import util.DBUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.tah.im.model.Notification;

import controllers.Notifications;

import static util.DBUtil.*;

public class QuestionDAO {
	
	public static final String QUESTION_COLLECTION = "question";
	public static final String TYPE = "QUESTION";
	
	public static void saveQuestionNotification(String talkerId, String convoId) {
		DBCollection notificationsColl = getCollection(QUESTION_COLLECTION);
		
		DBRef talkerRef = new DBRef(getDB(), "talkers", new ObjectId(talkerId));
		DBRef convoRef = new DBRef(getDB(), "convos", new ObjectId(convoId));
		DBObject notificationDBObject = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("convoid", convoRef)
			.add("type", TYPE)
			.add("flag", "false")
			.add("time", new Date())
			.get();
		notificationsColl.save(notificationDBObject);
	}
	
	public static List<NotificationBean> loadAllQuestions() {
		DBCollection notificationsColl = getCollection(QUESTION_COLLECTION);
		DBObject query = BasicDBObjectBuilder.start().add("flag", "false").get();
		
		
		List<DBObject> topicsDBList = new ArrayList<DBObject>();// notificationsColl.find(query).toArray();
		DBCursor questionCur=notificationsColl.find(query);
		while(questionCur.hasNext()){
			topicsDBList.add(questionCur.next());
		}
		
		List<NotificationBean> list = new ArrayList<NotificationBean>();
		for (DBObject questionDBObject : topicsDBList) {
			NotificationBean notificationBean = new NotificationBean();
			notificationBean.parseDBObject(questionDBObject);
			list.add(notificationBean);
		}
		return list;
	}
	
	public static NotificationBean loadNotification(String id){
		DBCollection notificationsColl = getCollection(QUESTION_COLLECTION);
		NotificationBean notificationBean = new NotificationBean();
		DBObject dbObject = notificationsColl.findOne(new BasicDBObject("_id", new ObjectId(id)));
		notificationBean.parseDBObject(dbObject);
		return notificationBean;
	}
	
	public static boolean updateNotification(ConversationBean convo){
		boolean updateFlag = true;
		/*DBCollection notificationsColl = getCollection(QUESTION_COLLECTION);
		DBObject query = new BasicDBObject("_id", new ObjectId(notificationBean.getId()));
		notificationsColl.update(query,new BasicDBObject("$set", new BasicDBObject("flag", "true")), false, true);*/
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
			 
		DBObject convoObject = BasicDBObjectBuilder.start()
			.add("removedByadmin", convo.isRemovedByadmin())
			.add("adminComments", convo.getAdminComments())
			.get();
			
		DBObject convoId = new BasicDBObject("_id", new ObjectId(convo.getId()));
		convosColl.update(convoId, new BasicDBObject("$set", convoObject));
		return updateFlag;
	}
	
	public static List<ConversationBean> loadAllQuestion() {
		List<ConversationBean> list = new ArrayList<ConversationBean>();
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);

		List<DBObject> convosDBList = null;

		DBObject query = BasicDBObjectBuilder.start().add("$or", 
			Arrays.asList(
					new BasicDBObject("removedByadmin", null),
					new BasicDBObject("removedByadmin", false)
				)
		)
		.get();
		
		DBObject fields = ConversationDAO.getBasicConversationFields();
		convosDBList = convosColl.find(query, fields).sort(new BasicDBObject("cr_date", -1)).limit(10).toArray();
		ConversationBean conversationBean = null;
		for (DBObject convoDBObject : convosDBList) {
			conversationBean = ConversationDAO.parseConvoFromDBObject(convoDBObject, true);
			conversationBean.setNotifedTalker(getNotification(conversationBean.getId()));
			conversationBean.setAdminComments(convoDBObject.get("adminComments")==null?"":convoDBObject.get("adminComments").toString());
			if(conversationBean.isDeleted())
				conversationBean.setQuestionState(Notifications.HIDDEN);
			list.add(conversationBean);
		}
		return list;
	}
	
	public static TalkerBean getNotification(String convoId) {
		TalkerBean bean = null;
		DBRef convoRef = new DBRef(getDB(), "convos", new ObjectId(convoId));
		DBObject query = new BasicDBObject("convoid", convoRef);
		DBCollection notificationsColl = getCollection(QUESTION_COLLECTION);
		NotificationBean notificationBean = new NotificationBean();
		DBObject dbObject = notificationsColl.findOne(query);
		if(dbObject != null){
			notificationBean.parseDBObject(dbObject);
			bean = TalkerLogic.loadTalkerFromCache(dbObject, "uid");
		}
		return bean;
	}
	
	public static final String CONVERSATIONS_COLLECTION = "convos";
}

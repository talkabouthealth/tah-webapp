package dao;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static util.DBUtil.createRef;
import static util.DBUtil.getCollection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;

import play.Logger;

import models.MessageBean;
import models.TalkerBean;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.MongoException;

import controllers.Messaging;

public class MessagingDAO {

	public static final String MESSAGES_COLLECTION = "messages";
	/**
	 * Save messages
	 * @param messageBean
	 */
	public static String saveMessage(MessageBean messageBean){
		
		DBCollection messagesColl = getCollection(MESSAGES_COLLECTION);
		DBRef fromtalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, messageBean.getFromTalkerId());
		DBRef toTalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, messageBean.getToTalkerId());
		
		System.out.println("----------replied flag----------"+messageBean.isReplied());
		
		DBObject messageDBObject = BasicDBObjectBuilder.start()
				.add("fromTalker", fromtalkerRef)
				.add("toTalker", toTalkerRef)
				.add("dummyid",messageBean.getDummyId())
				.add("subject", messageBean.getSubject())
				.add("message", messageBean.getText())
				.add("time", new Date())
				.add("rootid", messageBean.getRootId())
				.add("read_flag",messageBean.isReadFlag())
				.add("delete_flag", messageBean.isDeleteFlag())
				.add("archieve_flag", messageBean.isArchieveFlag())
				.add("read_flag_sender",messageBean.isReadFlagSender())
				.add("delete_flag_sender", messageBean.isDeleteFlagSender())
				.add("archieve_flag_sender", messageBean.isArchieveFlagSender())
				.add("replied", messageBean.isReplied())
				.add("modified_time",messageBean.getTime())
				.get();
		try {
			messagesColl.save(messageDBObject);
			String id=messageDBObject.get("_id").toString();
			return id;
		}
		catch (MongoException me) {
			//E11000 duplicate key error index
			if (me.getCode() == 11000) {
				Logger.error("Duplicate key error while saving message");
			}
			me.printStackTrace();
			return null;
		}		
	}
	
	/**
	 * Method use for getting all inbox messages  
	 * @param id
	 * @return List<MessageBean>
	 */
	
	public static List<MessageBean> getInboxMessagesById(String id, int pageNo){
				
		System.out.println("-----------id-------------"+id);
		DBCollection messagesColl = getCollection(MESSAGES_COLLECTION);		
		DBRef totalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, id);	
		
		DBObject query = BasicDBObjectBuilder.start()
		.add("toTalker", totalkerRef)
		.add("rootid", "")
		.add("delete_flag", new BasicDBObject("$ne", true))
		.add("archieve_flag", new BasicDBObject("$ne", true))
		.get();		
				
		DBObject query1 = BasicDBObjectBuilder.start()
		.add("fromTalker", totalkerRef)
		.add("replied",true)
		.add("rootid", "")
		.add("delete_flag", new BasicDBObject("$ne", true))
		.add("archieve_flag", new BasicDBObject("$ne", true))
		.get();
				
		//System.out.println("\nQuery1---------------------"+query1);
				
		DBObject querylist = new BasicDBObject("$or", 		//"OR" CONDITION FOR query and query1
				Arrays.asList(query1,query)
			);
		
		pageNo = pageNo * Messaging.CONVO_PER_PAGE;
		
		List<DBObject> messageDBObjectList = messagesColl.find(querylist).skip(pageNo).limit(Messaging.CONVO_PER_PAGE).sort(new BasicDBObject("time", -1)).toArray();
		//System.out.println("messagedbobjlist:::-----"+messageDBObjectList);
	    
		List<MessageBean> messageList = new ArrayList<MessageBean>();
		for (DBObject messageDBObject : messageDBObjectList) {
			MessageBean message = new MessageBean();
			message.parseFromDB(messageDBObject);
			messageList.add(message);
		}		
		//System.out.println("\n\n MessageList----->>>>>>"+messageList);		
		return messageList;		
	}	
	/**
	 * Method use for getting all sent mail messages  
	 * @param id
	 * @return List<MessageBean>
	 */
	public static List<MessageBean> getSentMailMessagesById(String id, int pageNo){
		
		DBCollection messagesColl = getCollection(MESSAGES_COLLECTION);
		
		DBRef fromtalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, id);
		
		DBObject query = BasicDBObjectBuilder.start()
		.add("fromTalker", fromtalkerRef)
		.add("rootid", "")
		.add("dummyid", "")
		.add("delete_flag_sender", new BasicDBObject("$ne", true))
		.add("archieve_flag_sender", new BasicDBObject("$ne", true))
		.get();
		
		DBObject query1 = BasicDBObjectBuilder.start()
		.add("toTalker", fromtalkerRef)
		.add("replied", true)
		.add("rootid", "")
		.add("delete_flag_sender", new BasicDBObject("$ne", true))
		.add("archieve_flag_sender", new BasicDBObject("$ne", true))
		.get();
		
		
		DBObject querylist = new BasicDBObject("$or", 		//"OR" CONDITION FOR query and query1
				Arrays.asList(query1,query)
			);
		List<DBObject> messageDBObjectList1 = messagesColl.find(querylist).toArray();
		System.out.println("total sent messages ..............................................:"+messageDBObjectList1.size());
		pageNo = pageNo * Messaging.CONVO_PER_PAGE;
		List<DBObject> messageDBObjectList = messagesColl.find(querylist).skip(pageNo).limit(Messaging.CONVO_PER_PAGE).sort(new BasicDBObject("time", -1)).toArray();
		
		List<MessageBean> messageList = new ArrayList<MessageBean>();
		for (DBObject messageDBObject : messageDBObjectList) {
			MessageBean message = new MessageBean();
			message.parseFromDB(messageDBObject);
			messageList.add(message);
		}		
		return messageList;
	}
	
	public static List<String> getToTalkerNamesByMessageId(String messageId){
		DBCollection messagesColl = getCollection(MESSAGES_COLLECTION);

		DBObject query = BasicDBObjectBuilder.start()
		.add("rootid", "")
		.add("dummyid",messageId)
		.get();
		
		//DBObject query = new BasicDBObject("_id", new ObjectId(messageId));
		
		List<DBObject> messageDBObjectList = messagesColl.find(query).toArray();
		System.out.println("Db object lis size  ..................................:"+messageDBObjectList.size());
		List<String> toTalkerList = new ArrayList<String>();
		for (DBObject messageDBObject : messageDBObjectList) {
			MessageBean message = new MessageBean();
			message.parseFromDB(messageDBObject);
			toTalkerList.add((message.getToTalker()).getUserName());
		}
		//System.out.println("in get Talker nams ..................................:"+toTalkerList.size());
		return toTalkerList;
	}
	
	
	/**
	 * Method use for getting all inbox messages of particular user
	 * @param String, String
	 * @return List<MessageBean>
	 */
	public static List<MessageBean> getInboxMessagesByUser(String id,String fromTalker){
		DBCollection messagesColl = getCollection(MESSAGES_COLLECTION);
		
		DBRef toTalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, id);
		DBRef fromTalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, fromTalker);
		
		DBObject query = BasicDBObjectBuilder.start()
			.add("toTalker", toTalkerRef)
			.add("fromTalker", fromTalkerRef)
			.get();
		
		List<DBObject> messageDBObjectList = messagesColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		List<MessageBean> messageList = new ArrayList<MessageBean>();
		for (DBObject messageDBObject : messageDBObjectList) {
			MessageBean message = new MessageBean();
			message.parseFromDB(messageDBObject);
			messageList.add(message);
		}		
		return messageList;		
	}
	
	/**
	 * Method use for getting message by message id
	 * @param id
	 * @return MessageBean
	 */
	public static MessageBean getMessageById(String id){
		DBCollection messagesColl = getCollection(MESSAGES_COLLECTION);
		DBObject query = new BasicDBObject("_id", new ObjectId(id));
		
		DBObject messageDBObject = messagesColl.findOne(query);
				
		if (messageDBObject == null) {
			return null;
		}
		else {
			MessageBean messBean = new MessageBean();
			messBean.parseFromDB(messageDBObject);

			return messBean;
		}
	}
	
	/**
	 * Method used for getting all replies of message
	 * @param id
	 * @return List<MessageBean>
	 */
	public static List<MessageBean> getMessageReplies(String id){
		
		//System.out.println("rootid>>>>>>>>>>>>"+id);
		
		DBCollection messagesColl = getCollection(MESSAGES_COLLECTION);
		DBObject query = new BasicDBObject("rootid",id);
		
		/*DBObject replyQuery = BasicDBObjectBuilder.start()
		.add("uname", usernameOrEmailRegex)
		.add("pass", passwordRegex)
		.get();
		
		List<DBObject> replyMsg=messagesColl.find(replyQuery).toArray();*/
						
		List<DBObject> messagesDBList = messagesColl.find(query).toArray();
		List<MessageBean> replyList = new ArrayList<MessageBean>();
		
		for(DBObject messageDBObject : messagesDBList){
			MessageBean message = new MessageBean();
			message.parseFromDB(messageDBObject);
			replyList.add(message);
		}
		return replyList;
	}
	
	/**
	 * Method use for getting all inbox messages  
	 * @param id
	 * @return List<MessageBean>
	 */					
	public static int getInboxMessagesCount(String id){
		
		int convoCount = 0;
		DBCollection messagesColl = getCollection(MESSAGES_COLLECTION);
		
		DBRef totalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, id);

		/*DBObject query = BasicDBObjectBuilder.start()
			.add("toTalker", totalkerRef)			
			.add("delete_flag", new BasicDBObject("$ne", true))
			.add("archieve_flag", new BasicDBObject("$ne", true))
			.get();*/
		DBObject query = BasicDBObjectBuilder.start()
		.add("toTalker", totalkerRef)
		.add("rootid", "")
		.add("delete_flag", new BasicDBObject("$ne", true))
		.add("archieve_flag", new BasicDBObject("$ne", true))
		.get();		
				
		DBObject query1 = BasicDBObjectBuilder.start()
		.add("fromTalker", totalkerRef)
		.add("replied",true)
		.add("rootid", "")
		.add("delete_flag", new BasicDBObject("$ne", true))
		.add("archieve_flag", new BasicDBObject("$ne", true))
		.get();
				
		//System.out.println("\nQuery1---------------------"+query1);
				
		DBObject querylist = new BasicDBObject("$or", 		//"OR" CONDITION FOR query and query1
				Arrays.asList(query,query1)
			);
		
		convoCount = messagesColl.find(querylist).count();		
		
		return convoCount; 
	}
	
	public static int getAllSentMessageCount(String id){
		
		int convoCount = 0;
		DBCollection messagesColl = getCollection(MESSAGES_COLLECTION);		
		DBRef fromtalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, id);
				
		DBObject query = BasicDBObjectBuilder.start()
		.add("fromTalker", fromtalkerRef)
		.add("rootid", "")
		.add("dummyid", "")
		.add("delete_flag_sender", false)
		.add("archieve_flag_sender", false)
			.get();
		
		DBObject query1 = BasicDBObjectBuilder.start()
		.add("toTalker", fromtalkerRef)
		.add("replied", true)
		.add("rootid", "")
		.add("delete_flag", new BasicDBObject("$ne", true))
		.add("archieve_flag", new BasicDBObject("$ne", true))
		.get();
		
		
		DBObject querylist = new BasicDBObject("$or", //"OR" CONDITION FOR query and query1
				Arrays.asList(query1,query)
			);
		
		convoCount = messagesColl.find(querylist).count();
		return convoCount; 
	}
	
	/**
	 * Method use for load all messages.
	 * @return List<MessageBean>
	 */
	public static List<MessageBean> loadAllMessages() {
		
		DBCollection messagesColl = getCollection(MESSAGES_COLLECTION);

		List<DBObject> messageDBObjectList = null;
		DBObject query = BasicDBObjectBuilder.start()
			.add("rootid", "")
			.get();
		messageDBObjectList = messagesColl.find().sort(new BasicDBObject("time", -1)).toArray();
		List<MessageBean> messageList = new ArrayList<MessageBean>();
		for (DBObject messageDBObject : messageDBObjectList) {
			MessageBean messageBean = new MessageBean();
			messageBean.parseFromDB(messageDBObject);
			messageList.add(messageBean);
		}
		return messageList;
	}
	
	/**
	 * Update messages
	 * @param messageBean
	 */
	public static void updateMessage(MessageBean messageBean){
		DBCollection messagesColl = getCollection(MESSAGES_COLLECTION);
		
		DBRef fromtalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, messageBean.getFromTalkerId());
		DBRef toTalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, messageBean.getToTalkerId());
		
		DBObject messageDBObject = BasicDBObjectBuilder.start()
				.add("fromTalker", fromtalkerRef)
				.add("toTalker", toTalkerRef)
				.add("dummyid", messageBean.getDummyId())
				.add("subject", messageBean.getSubject())
				.add("message", messageBean.getText())
				.add("time", messageBean.getTime())
				.add("rootid", messageBean.getRootId())
				.add("read_flag",messageBean.isReadFlag())
				.add("delete_flag", messageBean.isDeleteFlag())
				.add("archieve_flag", messageBean.isArchieveFlag())
				.add("read_flag_sender",messageBean.isReadFlagSender())
				.add("delete_flag_sender", messageBean.isDeleteFlagSender())
				.add("archieve_flag_sender", messageBean.isArchieveFlagSender())
				.add("replied",messageBean.isReplied())
				
				.get();

		try {
			DBObject msgId = new BasicDBObject("_id", new ObjectId(messageBean.getId()));
			//"$set" is used for updating fields
			messagesColl.update(msgId,messageDBObject);
		}
		catch (MongoException me) {
			//E11000 duplicate key error index
			if (me.getCode() == 11000) {
				Logger.error("Duplicate key error while saving message");
			}
			me.printStackTrace();
		}
		
	}
	
	/**
	 * Method use for getting all archive messages  
	 * @param id
	 * @return List<MessageBean>
	 */
	public static List<MessageBean> getArchiveMessagesById(String id, int pageNo){
		
		DBCollection messagesColl = getCollection(MESSAGES_COLLECTION);
		
		DBRef totalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, id);
		
		
		DBObject query = BasicDBObjectBuilder.start()
		.add("toTalker", totalkerRef)
		.add("rootid", "")
		.add("delete_flag", new BasicDBObject("$ne", true))
		.add("archieve_flag", true)
		.get();
		
		
		DBObject query1 = BasicDBObjectBuilder.start()
		.add("fromTalker", totalkerRef)
		//.add("replied",true)
		.add("rootid", "")
		.add("delete_flag_sender", new BasicDBObject("$ne", true))
		.add("archieve_flag_sender",true)
		.get();
		
		
		DBObject querylist = new BasicDBObject("$or", 		//"OR" CONDITION FOR query and query1
				Arrays.asList(query,query1)
			);
		
		pageNo = pageNo * Messaging.CONVO_PER_PAGE;
		List<DBObject> messageDBObjectList = messagesColl.find(querylist).skip(pageNo).limit(Messaging.CONVO_PER_PAGE).sort(new BasicDBObject("time", -1)).toArray();
		
		List<MessageBean> messageList = new ArrayList<MessageBean>();
		for (DBObject messageDBObject : messageDBObjectList) {
			MessageBean message = new MessageBean();
			message.parseFromDB(messageDBObject);
			messageList.add(message);
		}
		
		return messageList;
		
	}
	
	public static int getArchiveMessageCount(String id){
		int convoCount = 0;
		DBCollection messagesColl = getCollection(MESSAGES_COLLECTION);
		
		DBRef totalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, id);

		DBObject query = BasicDBObjectBuilder.start()
		.add("toTalker", totalkerRef)
		.add("rootid", "")
		.add("delete_flag", new BasicDBObject("$ne", true))
		.add("archieve_flag", true)
		.get();
		
		
		DBObject query1 = BasicDBObjectBuilder.start()
		.add("fromTalker", totalkerRef)
		//.add("replied",true)
		.add("rootid", "")
		.add("delete_flag_sender", new BasicDBObject("$ne", true))
		.add("archieve_flag_sender",true)
		.get();
		
		
		DBObject querylist = new BasicDBObject("$or", 		//"OR" CONDITION FOR query and query1
				Arrays.asList(query,query1)
			);
		
		
		convoCount = messagesColl.find(querylist).count();
		return convoCount; 
	}
	
	/**
	 * Method use for getting message by message subject
	 * @param id
	 * @return MessageBean
	 */
	public static MessageBean getMessageBySubject(String messageSubject){
		DBCollection messagesColl = getCollection(MESSAGES_COLLECTION);
		DBObject query = null;
		try{
			query = new BasicDBObject("subject", Pattern.compile(messageSubject , Pattern.CASE_INSENSITIVE));
		}catch(Exception e){
			e.printStackTrace();
		}
		DBObject messageDBObject = messagesColl.findOne(query);
				
		if (messageDBObject == null) {
			return null;
		}
		else {
			MessageBean messBean = new MessageBean();
			messBean.parseFromDB(messageDBObject);
			return messBean;
		}
		
	}
}

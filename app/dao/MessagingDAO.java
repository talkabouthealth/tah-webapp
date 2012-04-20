package dao;
import static util.DBUtil.createRef;
import static util.DBUtil.getCollection;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import models.MessageBean;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.bson.types.ObjectId;

import play.Logger;
import util.SearchUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.MongoException;

import controllers.Messaging;

public class MessagingDAO {

	public static final String MESSAGES_COLLECTION = "messages";
	private static final boolean IndexDeletionPolicy = false;
	/**
	 * Save messages
	 * @param messageBean
	 */
	public static String saveMessage(MessageBean messageBean){
		
		DBCollection messagesColl = getCollection(MESSAGES_COLLECTION);
		DBRef fromtalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, messageBean.getFromTalkerId());
		DBRef toTalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, messageBean.getToTalkerId());
		
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
			Logger.error(me,"MessagingDAO.java : saveMessage ");
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
		.add("delete_flag_sender", new BasicDBObject("$ne", true))
		.add("archieve_flag_sender", new BasicDBObject("$ne", true))
		.get();
				
		DBObject querylist = new BasicDBObject("$or", 		//"OR" CONDITION FOR query and query1
				Arrays.asList(query1,query)
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
	
	/**
	 * Method use for displaying multiple talkers name in sentmail list
	 * @param messageId
	 * @return List<String>
	 */
	public static List<String> getToTalkerNamesByMessageId(String messageId){
		DBCollection messagesColl = getCollection(MESSAGES_COLLECTION);

		DBObject query = BasicDBObjectBuilder.start()
		.add("rootid", "")
		.add("dummyid",messageId)
		.get();
		
		//DBObject query = new BasicDBObject("_id", new ObjectId(messageId));
		
		List<DBObject> messageDBObjectList = messagesColl.find(query).toArray();
		List<String> toTalkerList = new ArrayList<String>();
		for (DBObject messageDBObject : messageDBObjectList) {
			MessageBean message = new MessageBean();
			message.parseFromDB(messageDBObject);
			toTalkerList.add((message.getToTalker()).getUserName());
		}
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
		
		DBCollection messagesColl = getCollection(MESSAGES_COLLECTION);
		DBObject query = new BasicDBObject("rootid",id);
		
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
		.add("delete_flag_sender", new BasicDBObject("$ne", true))
		.add("archieve_flag_sender", new BasicDBObject("$ne", true))
		.get();
				
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
		.add("delete_flag_sender", new BasicDBObject("$ne", true))
		.add("archieve_flag_sender", new BasicDBObject("$ne", true))
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
			Logger.error(me, "MessagingDAO.java : updateMessage");
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
			query = new BasicDBObject("subject",messageSubject);
		}catch(Exception e){
			Logger.error(e, "MessagingDAO.java : getMessageBySubject");
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
	
	/**
	 * Method used for populating indexes for auto-complete
	 * @param id
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	public static void populateMessageIndex(String id) throws Exception{
		File messageAutocompleteIndexerFile = new File(SearchUtil.SEARCH_INDEX_PATH+"messageAutocomplete");
 		Directory messageAutocompleteIndexDir = FSDirectory.open(messageAutocompleteIndexerFile);
		IndexWriter autocompleteMessageIndexWriter = null;
		if(IndexReader.indexExists(messageAutocompleteIndexDir)){
			autocompleteMessageIndexWriter = new IndexWriter(messageAutocompleteIndexDir, new StandardAnalyzer(Version.LUCENE_36), false,MaxFieldLength.UNLIMITED) ; 
		}else{
			autocompleteMessageIndexWriter = new IndexWriter(messageAutocompleteIndexDir, new StandardAnalyzer(Version.LUCENE_36), true,MaxFieldLength.UNLIMITED) ; 
		}
		
		MessageBean message = null;
		message = getMessageById(id);
		if(message != null){
			if (message.isDeleteFlag() != true || !message.getRootId().equals(null)) {
				Document doc = new Document();
				doc.add(new Field("id", message.getId(), Field.Store.YES, Field.Index.ANALYZED));
				doc.add(new Field("title", message.getSubject(), Field.Store.YES, Field.Index.ANALYZED));
				doc.add(new Field("type", "Message", Field.Store.YES, Field.Index.NO));
				doc.add(new Field("rootid",message.getRootId(), Field.Store.YES, Field.Index.NO));
				doc.add(new Field("fromTalker", message.getFromTalkerId(), Field.Store.YES, Field.Index.NO));
				doc.add(new Field("toTalker", message.getToTalkerId(), Field.Store.YES, Field.Index.NO));
				autocompleteMessageIndexWriter.addDocument(doc);
			}
		}
		autocompleteMessageIndexWriter.close();
	}
	
	/**
	 * Method used for deleting index from auto-complete
	 * @param id
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	public static void deleteMessageIndex(String id) throws Exception{
		File messageAutocompleteIndexerFile = new File(SearchUtil.SEARCH_INDEX_PATH+"messageAutocomplete");
 		Directory messageAutocompleteIndexDir = FSDirectory.open(messageAutocompleteIndexerFile);
		IndexReader autocompleteMessageIndexReader = IndexReader.open(messageAutocompleteIndexDir, false);
		Term term = new Term("id",id);
		try{
			autocompleteMessageIndexReader.deleteDocuments(term);
			
		}catch(Exception e){
			Logger.error(e,"MessagingDAO : deleteMessageIndex ");
		}
		autocompleteMessageIndexReader.close();
	}
	
	/**
	 * Method use for getting message by message id
	 * @param id
	 * @return List<MessageBean>
	 */
	public static List<MessageBean> getMessageByRootId(String id){
		DBCollection messagesColl = getCollection(MESSAGES_COLLECTION);
		DBObject query = new BasicDBObject("rootid", id);
		
		List<MessageBean> messageList = new ArrayList<MessageBean>();
		List<DBObject> messageDBObjectList = messagesColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		for (DBObject messageDBObject : messageDBObjectList) {
			MessageBean messageBean = new MessageBean();
			messageBean.parseFromDB(messageDBObject);
			messageList.add(messageBean);
		}
		return messageList;
	}
	
	/**
	 * Return the No of unread Message in Inbox 
	 * @param talkerId
	 * @return messageCount
	 */
	public static int getUnreadMessageCount(String talkerId){
		
		int messageCount;
		DBCollection messagesColl = getCollection(MESSAGES_COLLECTION);
		DBRef totalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talkerId);

		DBObject query = BasicDBObjectBuilder.start()
			.add("toTalker", totalkerRef)
			.add("rootid", "")
			.add("delete_flag", new BasicDBObject("$ne", true))
			.add("archieve_flag", new BasicDBObject("$ne", true))
			.add("read_flag", new BasicDBObject("$ne", true))
			.get();		
	
		DBObject query1 = BasicDBObjectBuilder.start()
			.add("fromTalker", totalkerRef)
			.add("replied",true)
			.add("rootid", "")
			.add("delete_flag_sender", new BasicDBObject("$ne", true))
			.add("archieve_flag_sender", new BasicDBObject("$ne", true))
			.add("read_flag_sender", new BasicDBObject("$ne", true))
			.get();
				
		DBObject querylist = new BasicDBObject("$or", 		//"OR" CONDITION FOR query and query1
				Arrays.asList(query1,query)
			);
		
		messageCount = messagesColl.find(querylist).count();		
		
		return messageCount;
	}

	
}

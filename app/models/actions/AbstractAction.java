package models.actions;

import static util.DBUtil.getString;

import java.util.Date;

import org.bson.types.ObjectId;

import util.DBUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import dao.CommentsDAO;
import dao.TalkerDAO;
import dao.ConversationDAO;

import models.CommentBean;
import models.TalkerBean;
import models.ConversationBean;

public abstract class AbstractAction implements Action {
	
	protected String id;
	protected TalkerBean talker;
	protected Date time;
	
	protected ActionType type;
	
	//other possible data
	protected ConversationBean convo;
	protected TalkerBean otherTalker;
	protected CommentBean answer;
	protected CommentBean reply;
	
	public AbstractAction(ActionType type, TalkerBean talker) {
		this.type = type;
		this.talker = talker;
		time = new Date();
	}
	
	public AbstractAction(DBObject dbObject) {
		setId(dbObject.get("_id").toString());
		
		DBObject talkerDBObject = ((DBRef)dbObject.get("uid")).fetch();
		String talkerId = talkerDBObject.get("_id").toString();
		String talkerName = (String)talkerDBObject.get("uname");
		setTalker(new TalkerBean(talkerId, talkerName));
		
		setTime((Date)dbObject.get("time"));
		setType(ActionType.valueOf((String)dbObject.get("type")));
		
		if (hasConvo()) {
			convo = parseConvo(dbObject);
		}
		if (hasOtherTalker()) {
			otherTalker = parseOtherTalker(dbObject);
		}
		if (hasAnswer()) {
			answer = parseAnswerOrReply(dbObject, "answer");
		}
		if (hasReply()) {
			answer = parseAnswerOrReply(dbObject, "reply");
		}
	}
	
	protected boolean hasConvo() { return false; }
	protected boolean hasOtherTalker() { return false; }
	protected boolean hasAnswer() { return false; }
	protected boolean hasReply() { return false; }

	protected String userName() {
		return "<b>"+talker.getUserName()+"</b>";
	}
	
	public DBObject toDBObject() {
		DBRef talkerRef = new DBRef(DBUtil.getDB(), TalkerDAO.TALKERS_COLLECTION, new ObjectId(talker.getId()));
		DBObject dbObject = BasicDBObjectBuilder.start()
				.add("uid", talkerRef)
				.add("type", type)
				.add("time", time)
				.get();
		
		if (hasConvo()) {
			addTopic(dbObject, convo);
		}
		if (hasOtherTalker()) {
			addOtherTalker(dbObject, otherTalker);
		}
		if (hasAnswer()) {
			addAnswerOrReply(dbObject, "answer", answer);
		}
		if (hasReply()) {
			addAnswerOrReply(dbObject, "reply", reply);
		}
	
		return dbObject;
	}
	
	// Topic connected actions
	protected ConversationBean parseConvo(DBObject dbObject) {
		DBObject topicDBObject = ((DBRef)dbObject.get("topicId")).fetch();
		ConversationBean topic = new ConversationBean();
    	topic.setId(topicDBObject.get("_id").toString());
    	topic.setTid((Integer)topicDBObject.get("tid"));
    	topic.setTopic((String)topicDBObject.get("topic"));
    	topic.setMainURL((String)topicDBObject.get("main_url"));
    	
    	return topic;
	}
	
	protected void addTopic(DBObject dbObject, ConversationBean topic) {
		DBRef topicRef = new DBRef(DBUtil.getDB(), ConversationDAO.CONVERSATIONS_COLLECTION, new ObjectId(topic.getId()));
		dbObject.put("topicId", topicRef);
	}
	
	protected Object topicLink() {
		return convo.getTopic();
	}
	
	// Other talker connected actions
	protected TalkerBean parseOtherTalker(DBObject dbObject) {
		DBObject talkerDBObject = ((DBRef)dbObject.get("otherTalker")).fetch();
		String talkerId = talkerDBObject.get("_id").toString();
		String talkerName = (String)talkerDBObject.get("uname");
    	
    	return new TalkerBean(talkerId, talkerName);
	}
	
	protected void addOtherTalker(DBObject dbObject, TalkerBean talker) {
		DBRef talkerRef = new DBRef(DBUtil.getDB(), TalkerDAO.TALKERS_COLLECTION, new ObjectId(talker.getId()));
		dbObject.put("otherTalker", talkerRef);
	}
	
	// Answer & Reply actions
	protected CommentBean parseAnswerOrReply(DBObject dbObject, String name) {
		DBRef answerDBRef = (DBRef)dbObject.get(name);
		if (answerDBRef == null) {
			return null;
		}
		
		DBObject answerDBObject = answerDBRef.fetch();
		CommentBean answer = new CommentBean();
		answer.setId(getString(answerDBObject, "_id"));
		answer.setText((String)answerDBObject.get("text"));
		answer.setTime((Date)answerDBObject.get("time"));
		
		//TODO: the same as thankyou222?
		DBObject fromTalkerDBObject = ((DBRef)answerDBObject.get("from")).fetch();
		TalkerBean fromTalker = new TalkerBean();
		fromTalker.parseBasicFromDB(fromTalkerDBObject);
		answer.setFromTalker(fromTalker);
		
    	return answer;
	}
	
	protected void addAnswerOrReply(DBObject dbObject, String name, CommentBean answer) {
		if (answer != null) {
			DBRef answerRef = new DBRef(DBUtil.getDB(), 
					CommentsDAO.CONVO_COMMENTS_COLLECTION, new ObjectId(answer.getId()));
			dbObject.put(name, answerRef);
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public TalkerBean getTalker() {
		return talker;
	}

	public void setTalker(TalkerBean talker) {
		this.talker = talker;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public ActionType getType() {
		return type;
	}

	public void setType(ActionType type) {
		this.type = type;
	}

}

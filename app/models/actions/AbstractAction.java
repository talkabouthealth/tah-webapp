package models.actions;

import java.util.Date;

import org.bson.types.ObjectId;

import util.DBUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import dao.TalkerDAO;
import dao.ConversationDAO;

import models.TalkerBean;
import models.ConversationBean;

public abstract class AbstractAction implements Action {
	
	protected String id;
	protected TalkerBean talker;
	protected Date time;
	
	protected ActionType type;
	
	//other possible data
	protected ConversationBean topic;
	protected TalkerBean otherTalker;
	
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
		
		if (hasTopic()) {
			topic = parseTopic(dbObject);
		}
		if (hasOtherTalker()) {
			otherTalker = parseOtherTalker(dbObject);
		}
	}
	
	protected boolean hasTopic() { return false; }
	protected boolean hasOtherTalker() { return false; }

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
		
		if (hasTopic()) {
			addTopic(dbObject, topic);
		}
		if (hasOtherTalker()) {
			addOtherTalker(dbObject, otherTalker);
		}
	
		return dbObject;
	}
	
	// Topic connected actions
	protected ConversationBean parseTopic(DBObject dbObject) {
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
		return topic.getTopic();
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

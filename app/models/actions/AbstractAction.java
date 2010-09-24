package models.actions;

import static util.DBUtil.*;

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
import models.TopicBean;

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
	protected CommentBean profileComment;
	protected CommentBean profileReply;
	
	public AbstractAction(ActionType type, TalkerBean talker) {
		this.type = type;
		this.talker = talker;
		time = new Date();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof AbstractAction)) {
			return false;
		}
		
		AbstractAction other = (AbstractAction)obj;
		return id.equals(other.id);
	}
	
	@Override
	public int hashCode() {
		if (id == null) {
			return 47;
		}
		return id.hashCode();
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
			reply = parseAnswerOrReply(dbObject, "reply");
		}
		if (hasProfileComment()) {
			profileComment = parseProfileCommentOrReply(dbObject, "profile_comment");
		}
		if (hasProfileReply()) {
			profileReply = parseProfileCommentOrReply(dbObject, "profile_reply");
		}
	}
	
	protected boolean hasConvo() { return false; }
	protected boolean hasOtherTalker() { return false; }
	protected boolean hasAnswer() { return false; }
	protected boolean hasReply() { return false; }
	protected boolean hasProfileComment() { return false; }
	protected boolean hasProfileReply() { return false; }
	
	protected String userName() {
		return "<b>"+talker.getUserName()+"</b>";
	}
	
	public DBObject toDBObject() {
		DBRef talkerRef = new DBRef(DBUtil.getDB(), TalkerDAO.TALKERS_COLLECTION, new ObjectId(talker.getId()));
		DBObject dbObject = BasicDBObjectBuilder.start()
				.add("uid", talkerRef)
				.add("type", type.toString())
				.add("time", time)
				.get();
		
		if (hasConvo()) {
			addConvo(dbObject, convo);
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
		if (hasProfileComment()) {
			addProfileCommentOrReply(dbObject, "profile_comment", profileComment);
		}
		if (hasProfileReply()) {
			addProfileCommentOrReply(dbObject, "profile_reply", profileReply);
		}
	
		return dbObject;
	}
	
	// Topic connected actions
	protected ConversationBean parseConvo(DBObject dbObject) {
		DBObject convoDBObject = ((DBRef)dbObject.get("topicId")).fetch();
		ConversationBean convo = new ConversationBean();
		convo.parseBasicFromDB(convoDBObject);
    	
    	return convo;
	}
	
	protected void addConvo(DBObject dbObject, ConversationBean topic) {
		DBRef topicRef = new DBRef(DBUtil.getDB(), ConversationDAO.CONVERSATIONS_COLLECTION, new ObjectId(topic.getId()));
		dbObject.put("topicId", topicRef);
	}
	
	protected Object convoLink() {
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
		
		answer.setFromTalker(parseTalker(answerDBObject, "from"));
		
    	return answer;
	}
	
	protected void addAnswerOrReply(DBObject dbObject, String name, CommentBean answer) {
		if (answer != null) {
			DBRef answerRef = new DBRef(DBUtil.getDB(), 
					CommentsDAO.CONVO_COMMENTS_COLLECTION, new ObjectId(answer.getId()));
			dbObject.put(name, answerRef);
		}
	}
	
	//Comments/Replies on Profiles
	protected CommentBean parseProfileCommentOrReply(DBObject dbObject, String name) {
		DBRef commentDBRef = (DBRef)dbObject.get(name);
		if (commentDBRef == null) {
			return null;
		}
		
		DBObject commentDBObject = commentDBRef.fetch();
		CommentBean comment = new CommentBean();
		comment.setId(getString(commentDBObject, "_id"));
		comment.setText((String)commentDBObject.get("text"));
		comment.setTime((Date)commentDBObject.get("time"));
		
		comment.setFromTalker(parseTalker(commentDBObject, "from"));
		
    	return comment;
	}
	
	protected void addProfileCommentOrReply(DBObject dbObject, String name, CommentBean comment) {
		if (comment != null) {
			DBRef commentRef = new DBRef(DBUtil.getDB(), 
					CommentsDAO.PROFILE_COMMENTS_COLLECTION, new ObjectId(comment.getId()));
			dbObject.put(name, commentRef);
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

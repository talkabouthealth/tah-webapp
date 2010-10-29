package models.actions;

import static util.DBUtil.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;

import util.CommonUtil;
import util.DBUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import dao.CommentsDAO;
import dao.TalkerDAO;
import dao.ConversationDAO;
import dao.TopicDAO;

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
	protected TopicBean topic;
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
		String connection = (String)talkerDBObject.get("connection");
		setTalker(new TalkerBean(talkerId, talkerName, connection));
		
		setTime((Date)dbObject.get("time"));
		setType(ActionType.valueOf((String)dbObject.get("type")));
		
		if (hasConvo()) {
			convo = parseConvo(dbObject);
		}
		if (hasTopic()) {
			topic = parseTopic(dbObject);
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
		if (hasTopic()) {
			addTopic(dbObject, topic);
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
	
	protected boolean hasConvo() { return false; }
	protected boolean hasTopic() { return false; }
	protected boolean hasOtherTalker() { return false; }
	protected boolean hasAnswer() { return false; }
	protected boolean hasReply() { return false; }
	protected boolean hasProfileComment() { return false; }
	protected boolean hasProfileReply() { return false; }
	
	protected String userName() {
		return "<b>"+talker.getUserName()+"</b>";
	}
	
	protected String fullUserName(TalkerBean user, boolean authenticated) {
		return CommonUtil.talkerToHTML(user, authenticated);
	}
	
	protected String convoTopics() {
		return CommonUtil.topicsToHTML(convo);
	}
	
	protected String topic() {
		String topicURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", topic.getMainURL());
		String topicLink = "<a href='"+topicURL+"'>"+topic.getTitle()+"</a>";
		return topicLink;
	}
	
	protected void addConvo(DBObject dbObject, ConversationBean convo) {
		DBRef topicRef = new DBRef(DBUtil.getDB(), ConversationDAO.CONVERSATIONS_COLLECTION, new ObjectId(convo.getId()));
		dbObject.put("convoId", topicRef);
	}
	
	// Convo connected actions
	protected ConversationBean parseConvo(DBObject dbObject) {
		DBObject convoDBObject = ((DBRef)dbObject.get("convoId")).fetch();
		
		ConversationBean convo = new ConversationBean();
		convo.parseBasicFromDB(convoDBObject);
		convo.setComments(CommentsDAO.loadConvoAnswers(convo.getId()));
    	
    	return convo;
	}
	
	protected void addTopic(DBObject dbObject, TopicBean topic) {
		DBRef topicRef = new DBRef(DBUtil.getDB(), TopicDAO.TOPICS_COLLECTION, new ObjectId(topic.getId()));
		dbObject.put("topicId", topicRef);
	}
	
	protected TopicBean parseTopic(DBObject dbObject) {
		DBObject topicDBObject = ((DBRef)dbObject.get("topicId")).fetch();
		
		TopicBean topic = new TopicBean();
		topic.parseBasicFromDB(topicDBObject);
    	return topic;
	}
	
	// Other talker connected actions
	protected TalkerBean parseOtherTalker(DBObject dbObject) {
		DBRef otherTalkerDBRef = (DBRef)dbObject.get("otherTalker");
		if (otherTalkerDBRef != null) {
			DBObject talkerDBObject = otherTalkerDBRef.fetch();
			String talkerId = talkerDBObject.get("_id").toString();
			String talkerName = (String)talkerDBObject.get("uname");
			String connection = (String)talkerDBObject.get("connection");
	    	
	    	return new TalkerBean(talkerId, talkerName, connection);
		}
		return null;
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
		
		if (name.equalsIgnoreCase("profile_comment")) {
			List<CommentBean> childrenList = new ArrayList<CommentBean>();
			List<String> childrenIdsList = getStringList(commentDBObject, "children");
			for (String childId : childrenIdsList) {
//				CommentBean child = CommentsDAO.getProfileCommentById(childId);
//				childrenList.add(child);
				
				DBCollection commentsColl = getCollection(CommentsDAO.PROFILE_COMMENTS_COLLECTION);
				
				DBObject query = new BasicDBObject("_id", new ObjectId(childId));
				DBObject answerDBObject = commentsColl.findOne(query);
				
				CommentBean child = new CommentBean();
				child.setId(getString(answerDBObject, "_id"));
				child.setText((String)answerDBObject.get("text"));
				child.setTime((Date)answerDBObject.get("time"));
				child.setFromTalker(parseTalker(answerDBObject, "from"));
				child.setDeleted(getBoolean(answerDBObject, "deleted"));
				childrenList.add(child);
			}
			comment.setChildren(childrenList);
		}
		
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
	
	public ConversationBean getConvo() {
		return convo;
	}

	public void setConvo(ConversationBean convo) {
		this.convo = convo;
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

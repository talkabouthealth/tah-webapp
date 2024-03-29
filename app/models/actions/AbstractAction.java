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

import logic.TalkerLogic;
import models.CommentBean;
import models.PrivacySetting;
import models.TalkerBean;
import models.ConversationBean;
import models.TopicBean;

//Every action has different data - convo, topics, answer or all this data together...
public abstract class AbstractAction implements Action {
	
	protected String id;
	//author of the activity
	protected TalkerBean talker;
	protected Date time;
	protected ActionType type;
	
	//other possible data connected with action
	protected ConversationBean convo;
	protected TopicBean topic;
	protected TalkerBean otherTalker;
	//conversation answer/reply
	protected CommentBean answer;
	protected CommentBean reply;
	//thought/reply
	protected CommentBean profileComment;
	protected CommentBean profileReply;
	
	public AbstractAction(ActionType type, TalkerBean talker) {
		this.type = type;
		this.talker = talker;
		time = new Date();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
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
		setTalker(TalkerLogic.loadTalkerFromCache(dbObject, "uid"));
		
		setTime((Date)dbObject.get("time"));
		setType(ActionType.valueOf((String)dbObject.get("type")));
		
		//parse additional info if it exists
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
							.add("time", time).get();

		if (hasConvo()) {
			addConvo(dbObject, convo);
			dbObject.put("other_disease_categories", convo.getOtherDiseaseCategories());
			dbObject.put("category", convo.getCategory());
		}else{
			dbObject.put("category", talker.getCategory());
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
	
	//default - everything false
	//Child actions override these methods
	protected boolean hasConvo() { return false; }
	protected boolean hasTopic() { return false; }
	protected boolean hasOtherTalker() { return false; }
	protected boolean hasAnswer() { return false; }
	protected boolean hasReply() { return false; }
	protected boolean hasProfileComment() { return false; }
	protected boolean hasProfileReply() { return false; }
	
	
	/* --------------- Methods for parsing/saving info from/to db --------------- */
	
	protected void addConvo(DBObject dbObject, ConversationBean convo) {
		DBRef topicRef = new DBRef(DBUtil.getDB(), ConversationDAO.CONVERSATIONS_COLLECTION, new ObjectId(convo.getId()));
		dbObject.put("convoId", topicRef);
	}
	protected ConversationBean parseConvo(DBObject dbObject) {
		String convoId = ((DBRef)dbObject.get("convoId")).getId().toString();
		
		ConversationBean convo = TalkerLogic.loadConvoFromCache(convoId);
		convo.setComments(CommentsDAO.loadConvoAnswers(convo.getId()));
    	return convo;
	}
	
	protected void addTopic(DBObject dbObject, TopicBean topic) {
		DBRef topicRef = new DBRef(DBUtil.getDB(), TopicDAO.TOPICS_COLLECTION, new ObjectId(topic.getId()));
		dbObject.put("topicId", topicRef);
	}
	protected TopicBean parseTopic(DBObject dbObject) {
		String topicId = ((DBRef)dbObject.get("topicId")).getId().toString();
		
		TopicBean topic = TopicDAO.getByIdBasic(topicId);
    	return topic;
	}
	
	//Other talker connected with this action (i.e. followed talker)
	protected void addOtherTalker(DBObject dbObject, TalkerBean talker) {
		DBRef talkerRef = new DBRef(DBUtil.getDB(), TalkerDAO.TALKERS_COLLECTION, new ObjectId(talker.getId()));
		dbObject.put("otherTalker", talkerRef);
	}
	protected TalkerBean parseOtherTalker(DBObject dbObject) {
		TalkerBean talker = TalkerLogic.loadTalkerFromCache(dbObject, "otherTalker");
		return talker;
	}
	
	//conversation answer/reply
	protected void addAnswerOrReply(DBObject dbObject, String name, CommentBean answer) {
		if (answer != null) {
			DBRef answerRef = new DBRef(DBUtil.getDB(), 
					CommentsDAO.CONVO_COMMENTS_COLLECTION, new ObjectId(answer.getId()));
			dbObject.put(name, answerRef);
		}
	}
	protected CommentBean parseAnswerOrReply(DBObject dbObject, String name) {
		DBRef answerDBRef = (DBRef)dbObject.get(name);
		if (answerDBRef == null) {
			return null;
		}
		
		DBObject answerDBObject = answerDBRef.fetch();
		CommentBean answer = new CommentBean();
		answer.parseFromDB(answerDBObject);
    	return answer;
	}
	
	//thought/reply
	protected void addProfileCommentOrReply(DBObject dbObject, String name, CommentBean comment) {
		if (comment != null) {
			DBRef commentRef = new DBRef(DBUtil.getDB(), 
					CommentsDAO.PROFILE_COMMENTS_COLLECTION, new ObjectId(comment.getId()));
			dbObject.put(name, commentRef);
		}
	}
	protected CommentBean parseProfileCommentOrReply(DBObject dbObject, String name) {
		DBRef commentDBRef = (DBRef)dbObject.get(name);
		if (commentDBRef == null) {
			return null;
		}
		
		DBObject commentDBObject = commentDBRef.fetch();
		CommentBean comment = new CommentBean();
		comment.parseFromDB(commentDBObject);
		
		if (name.equalsIgnoreCase("profile_comment")) {
			//for thought we also load replies
			List<CommentBean> childrenList = new ArrayList<CommentBean>();
			List<String> childrenIdsList = getStringList(commentDBObject, "children");
			for (String childId : childrenIdsList) {
				DBCollection commentsColl = getCollection(CommentsDAO.PROFILE_COMMENTS_COLLECTION);
				
				DBObject query = new BasicDBObject("_id", new ObjectId(childId));
				DBObject answerDBObject = commentsColl.findOne(query);
				
				CommentBean child = new CommentBean();
				child.parseFromDB(answerDBObject);
				if (!child.isDeleted()) {
					childrenList.add(child);
				}
			}
			comment.setChildren(childrenList);
			childrenIdsList.clear();
		}
    	return comment;
	}
	
	
	/*------------ Methods for display ----------------- */
	/**
	 * Returns user info as html string (username, connection, etc.)
	 * @param talker
	 * @param authenticated Is current user logged in?
	 * @return
	 */
	protected String fullUserName(TalkerBean talker, boolean authenticated) {
		return CommonUtil.talkerToHTML(talker, authenticated);
	}
	
	/**
	 * Returns convo topics as html string
	 * @return
	 */
	protected String convoTopics() {
		return CommonUtil.topicsToHTML(convo);
	}
	
	protected String topic() {
		String topicURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", topic.getMainURL());
		String topicLink = "<a href='"+topicURL+"'>"+topic.getTitle()+"</a>";
		return topicLink;
	}
	
	
	public void setID(String id) { this.id=id; }
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	
	public ConversationBean getConvo() { return convo; }
	public void setConvo(ConversationBean convo) { this.convo = convo; }

	public TalkerBean getTalker() { return talker; }
	public void setTalker(TalkerBean talker) { this.talker = talker; }

	public Date getTime() { return time; }
	public void setTime(Date time) { this.time = time; }

	public ActionType getType() { return type; }
	public void setType(ActionType type) { this.type = type; }
}

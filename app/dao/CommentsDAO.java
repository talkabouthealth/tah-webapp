package dao;

import java.util.ArrayList;

import static util.DBUtil.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.CommentBean;
import models.CommentBean.Vote;
import models.TalkerBean;
import models.TopicBean;

import org.bson.types.ObjectId;

import util.DBUtil;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

/*
 	We store profile comments in separate collection, as "child lists" tree.
 	Example "child lists" structure:
		{"_id": "A", "children": ["B", "C"]}
		{"_id": "B", "children": ["D"]}
		{"_id": "C"}
		{"_id": "D"} 
 */
public class CommentsDAO {
	
	public static final String PROFILE_COMMENTS_COLLECTION = "profilecomments";
	public static final String CONVO_COMMENTS_COLLECTION = "topiccomments";
	
	// ---------------- Profile comments --------------------------
	public static String saveProfileComment(CommentBean comment) {
		DBCollection commentsColl = getCollection(PROFILE_COMMENTS_COLLECTION);
		
		DBRef profileTalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, comment.getProfileTalkerId());
		DBRef fromTalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, comment.getFromTalker().getId());
		DBObject commentObject = BasicDBObjectBuilder.start()
			.add("profile", profileTalkerRef)
			.add("from", fromTalkerRef)
			.add("text", comment.getText())
			.add("time", comment.getTime())
			.get();
		commentsColl.save(commentObject);
		
		updateParent(commentsColl, comment.getParentId(), getString(commentObject, "_id"));
		
		return getString(commentObject, "_id");
	}
	
	public static List<CommentBean> loadProfileComments(String talkerId) {
		DBCollection commentsColl = getCollection(PROFILE_COMMENTS_COLLECTION);
		
		DBRef profileTalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talkerId);
		DBObject query = BasicDBObjectBuilder.start()
			.add("profile", profileTalkerRef)
			.get();
		List<DBObject> commentsList = commentsColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		//comments without parent (top in hierarchy)
		List<CommentBean> topCommentsList = parseCommentsTree(commentsList);
		return topCommentsList;
	}

	// -------------- Convo comments -----------------
	public static String saveConvoComment(CommentBean comment) {
		DBCollection commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		
		DBRef topicRef = createRef(ConversationDAO.CONVERSATIONS_COLLECTION, comment.getTopicId());
		DBRef fromTalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, comment.getFromTalker().getId());
		DBObject commentObject = BasicDBObjectBuilder.start()
			.add("topic", topicRef)
			.add("from", fromTalkerRef)
			.add("text", comment.getText())
			.add("time", comment.getTime())
			.get();
		commentsColl.save(commentObject);
		
		updateParent(commentsColl, comment.getParentId(), getString(commentObject, "_id")); 
		
		return getString(commentObject, "_id");
	}
	
	public static List<CommentBean> loadConvoAnswers(String topicId) {
		DBCollection commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		
		DBRef convoRef = createRef(ConversationDAO.CONVERSATIONS_COLLECTION, topicId);
		DBObject query = BasicDBObjectBuilder.start()
			.add("topic", convoRef)
			.get();
		List<DBObject> commentsList = commentsColl.find(query).sort(new BasicDBObject("vote_score", -1)).toArray();
		
		//comments without parent (top in hierarchy)
		List<CommentBean> topCommentsList = parseCommentsTree(commentsList);
		return topCommentsList;
	}
	
	public static void updateConvoAnswer(CommentBean answer) {
		DBCollection commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		
		DBObject answerObject = BasicDBObjectBuilder.start()
			.add("vote_score", answer.getVoteScore())
			.add("votes", setToDB(answer.getVotes()))
			.get();
		
		DBObject answerId = new BasicDBObject("_id", new ObjectId(answer.getId()));
		commentsColl.update(answerId, new BasicDBObject("$set", answerObject));
	}
	
	public static CommentBean getConvoAnswerById(String answerId) {
		DBCollection commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		
		DBObject query = new BasicDBObject("_id", new ObjectId(answerId));
		DBObject answerDBObject = commentsColl.findOne(query);
		
		CommentBean answer = new CommentBean();
		answer.parseBasicFromDB(answerDBObject);
		return answer;
	}
	
	
	//update parent - add new comment "_id" to children array
	private static void updateParent(DBCollection commentsColl, String parentCommentId, String commentId) {
		if (parentCommentId != null) {
			DBObject parentIdDBObject = new BasicDBObject("_id", new ObjectId(parentCommentId));
			commentsColl.update(parentIdDBObject, 
					new BasicDBObject("$push", new BasicDBObject("children", commentId)));
		}
	}
	
	private static List<CommentBean> parseCommentsTree(List<DBObject> commentsList) {
		List<CommentBean> topCommentsList = new ArrayList<CommentBean>();
		//temp map for resolving children
		Map<String, CommentBean> commentsCacheMap = new HashMap<String, CommentBean>();
		for (DBObject commentDBObject : commentsList) {
			String commentId = getString(commentDBObject, "_id");
			
			CommentBean commentBean = commentsCacheMap.get(commentId);
			if (commentBean == null) {
				commentBean = new CommentBean(commentId);
				commentsCacheMap.put(commentId, commentBean);
				
				topCommentsList.add(commentBean);
			}
			
			commentBean.parseBasicFromDB(commentDBObject);
			
			//save children
			List<CommentBean> childrenList = new ArrayList<CommentBean>();
			List<String> childrenIdsList = getStringList(commentDBObject, "children");
			for (String childId : childrenIdsList) {
				//try to get cached instance
				CommentBean childrenCommentBean = commentsCacheMap.get(childId);
				if (childrenCommentBean == null) {
					childrenCommentBean = new CommentBean(childId);
					commentsCacheMap.put(childId, childrenCommentBean);
				}
				
				childrenList.add(childrenCommentBean);
				//remove child comments from top list
				topCommentsList.remove(childrenCommentBean);
			}
			commentBean.setChildren(childrenList);
		}
		
		return topCommentsList;
	}

}

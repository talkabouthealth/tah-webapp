package dao;

import java.util.ArrayList;

import static util.DBUtil.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	public static final String CONVO_COMMENTS_COLLECTION = "convocomments";
	
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
	
	public static void updateProfileComment(CommentBean comment) {
		DBCollection commentsColl = getCollection(PROFILE_COMMENTS_COLLECTION);
		
		DBObject commentObject = BasicDBObjectBuilder.start()
			.add("deleted", comment.isDeleted())
			.get();
		
		DBObject commentId = new BasicDBObject("_id", new ObjectId(comment.getId()));
		commentsColl.update(commentId, new BasicDBObject("$set", commentObject));
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
		
//		for (CommentBean cb : topCommentsList) {
//			System.out.println("!"+cb.getFromTalker());
//			if (cb.getChildren() != null) {
//				for (CommentBean c : cb.getChildren()) {
//					System.out.println("!!!!!"+c.getFromTalker()+" : "+c.getText());
//				}
//			}
//		}
		return topCommentsList;
	}

	// -------------- Convo comments -----------------
	public static String saveConvoComment(CommentBean comment) {
		DBCollection commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		
		DBRef convoRef = createRef(ConversationDAO.CONVERSATIONS_COLLECTION, comment.getConvoId());
		DBRef fromTalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, comment.getFromTalker().getId());
		DBObject commentObject = BasicDBObjectBuilder.start()
			.add("convo", convoRef)
			.add("from", fromTalkerRef)
			.add("text", comment.getText())
			.add("time", comment.getTime())
			.add("answer", comment.isAnswer())
			.get();
		commentsColl.save(commentObject);
		
		updateParent(commentsColl, comment.getParentId(), getString(commentObject, "_id")); 
		
		return getString(commentObject, "_id");
	}
	
	public static void updateConvoAnswer(CommentBean answer) {
		DBCollection commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		
		DBObject answerObject = BasicDBObjectBuilder.start()
			.add("vote_score", answer.getVoteScore())
			.add("votes", setToDB(answer.getVotes()))
			
			.add("text", answer.getText())
			.add("old_texts", answer.getOldTexts())
			
			.add("deleted", answer.isDeleted())
			.add("answer", answer.isAnswer())
			
			.add("not_helpful", answer.isNotHelpful())
			.add("not_helpful_votes", setToDB(answer.getNotHelpfulVotes()))
			
			.get();
		
		DBObject answerId = new BasicDBObject("_id", new ObjectId(answer.getId()));
		commentsColl.update(answerId, new BasicDBObject("$set", answerObject));
	}
	
	public static List<CommentBean> loadConvoAnswers(String convoId) {
		DBCollection commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		
		DBRef convoRef = createRef(ConversationDAO.CONVERSATIONS_COLLECTION, convoId);
		DBObject query = BasicDBObjectBuilder.start()
			.add("convo", convoRef)
			.get();
		List<DBObject> commentsList = commentsColl.find(query).sort(new BasicDBObject("vote_score", -1)).toArray();
		
		//comments without parent (top in hierarchy)
		List<CommentBean> topCommentsList = parseCommentsTree(commentsList);
		return topCommentsList;
	}
	
	public static List<CommentBean> loadAllAnswers() {
		DBCollection commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		
		List<DBObject> commentsList = commentsColl.find().sort(new BasicDBObject("vote_score", -1)).toArray();
		
		//comments without parent (top in hierarchy)
		List<CommentBean> topCommentsList = parseCommentsTree(commentsList);
		return topCommentsList;
	}
	
	//by topic or all answers
	public static List<CommentBean> getTalkerAnswers(String talkerId, TopicBean topic) {
		DBCollection commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		
		DBRef fromTalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talkerId);
		BasicDBObjectBuilder queryBuilder = BasicDBObjectBuilder.start()
			.add("from", fromTalkerRef)
			.add("deleted", new BasicDBObject("$ne", true))
			.add("answer", true);
		if (topic != null) {
			Set<DBRef> convosDBSet = ConversationDAO.getConversationsByTopic(topic);
			queryBuilder.add("convo", new BasicDBObject("$in", convosDBSet));
		}
		
		DBObject query = queryBuilder.get();
		List<DBObject> commentsList = commentsColl.find(query).sort(new BasicDBObject("vote_score", -1)).toArray();
		
		List<CommentBean> answersList = new ArrayList<CommentBean>();
		for (DBObject answerDBObject : commentsList) {
			CommentBean answer = new CommentBean();
			answer.parseBasicFromDB(answerDBObject);
			answersList.add(answer);
		}
		return answersList;
	}
	
	public static CommentBean getConvoAnswerById(String answerId) {
		DBCollection commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		
		DBObject query = new BasicDBObject("_id", new ObjectId(answerId));
		DBObject answerDBObject = commentsColl.findOne(query);
		
		CommentBean answer = new CommentBean();
		answer.parseBasicFromDB(answerDBObject);
		return answer;
	}
	
	public static CommentBean getProfileCommentById(String commentId) {
		DBCollection commentsColl = getCollection(PROFILE_COMMENTS_COLLECTION);
		
		DBObject query = new BasicDBObject("_id", new ObjectId(commentId));
		DBObject answerDBObject = commentsColl.findOne(query);
		
		CommentBean comment = new CommentBean();
		comment.parseBasicFromDB(answerDBObject);
		return comment;
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

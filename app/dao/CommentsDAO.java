package dao;

import java.util.ArrayList;

import static util.DBUtil.*;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
	
	/**
	 * Get thought/reply by id.
	 */
	public static CommentBean getProfileCommentById(String commentId) {
		DBCollection commentsColl = getCollection(PROFILE_COMMENTS_COLLECTION);
		
		DBObject query = new BasicDBObject("_id", new ObjectId(commentId));
		DBObject answerDBObject = commentsColl.findOne(query);
		
		CommentBean comment = new CommentBean();
		comment.parseFromDB(answerDBObject);
		return comment;
	}
	
	/**
	 * Save thought/reply.
	 * Set newly created id to given comment object.
	 */
	public static void saveProfileComment(CommentBean comment) {
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
		
		String id = getString(commentObject, "_id");
		comment.setId(id);
	}
	
	/**
	 * Update thought/reply
	 */
	public static void updateProfileComment(CommentBean comment) {
		DBCollection commentsColl = getCollection(PROFILE_COMMENTS_COLLECTION);
		
		DBObject commentObject = BasicDBObjectBuilder.start()
			.add("text", comment.getText())
			.add("old_texts", comment.getOldTexts())
			.add("deleted", comment.isDeleted())
			.get();
		
		DBObject commentId = new BasicDBObject("_id", new ObjectId(comment.getId()));
		commentsColl.update(commentId, new BasicDBObject("$set", commentObject));
	}
	
	/**
	 * Load tree of all thoughts and replies for given talker
	 */
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
	
	/**
	 * Get conversation answer/reply by id.
	 */
	public static CommentBean getConvoAnswerById(String answerId) {
		DBCollection commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		
		DBObject query = new BasicDBObject("_id", new ObjectId(answerId));
		DBObject answerDBObject = commentsColl.findOne(query);
		
		CommentBean answer = new CommentBean();
		answer.parseFromDB(answerDBObject);
		return answer;
	}
	
	/**
	 * Save answer/reply
	 */
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
	
	/**
	 * Loads full tree of answers and replies for given conversation.
	 */
	public static List<CommentBean> loadConvoAnswersTree(String convoId) {
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
	
	/**
	 * Load all not-deleted answers for given conversation,
	 * answers have only id.
	 */
	public static List<CommentBean> loadConvoAnswers(String convoId) {
		DBCollection commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		
		DBRef convoRef = createRef(ConversationDAO.CONVERSATIONS_COLLECTION, convoId);
		DBObject query = BasicDBObjectBuilder.start()
			.add("convo", convoRef)
			.add("deleted", new BasicDBObject("$ne", true))
			.add("answer", true)
			.get();
		List<DBObject> commentsList = commentsColl.find(query).sort(new BasicDBObject("vote_score", -1)).toArray();
		
		List<CommentBean> answersList = new ArrayList<CommentBean>();
		for (DBObject answerDBObject : commentsList) {
			CommentBean answer = new CommentBean();
			answer.setId(answerDBObject.get("_id").toString());
			answersList.add(answer);
		}
		return answersList;
	}
	
	/**
	 * Returns number of all answers in this community
	 */
	public static int getNumberOfAnswers() {
		DBCollection commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		
		int numberOfAnswers = commentsColl.find(new BasicDBObject("answer", true)).size();
		return numberOfAnswers;
	}
	
	/**
	 * Returns talker answers, all or filtered by given topic
	 */
	public static List<CommentBean> getTalkerAnswers(String talkerId, TopicBean topic) {
		DBCollection commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		
		DBRef fromTalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talkerId);
		BasicDBObjectBuilder queryBuilder = BasicDBObjectBuilder.start()
			.add("from", fromTalkerRef)
			.add("deleted", new BasicDBObject("$ne", true))
			.add("answer", true);
		if (topic != null) {
			Set<DBRef> convosDBSet = 
				ConversationDAO.getConversationsByTopics(new HashSet<TopicBean>(Arrays.asList(topic)));
			queryBuilder.add("convo", new BasicDBObject("$in", convosDBSet));
		}
		
		DBObject query = queryBuilder.get();
		List<DBObject> commentsList = commentsColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		List<CommentBean> answersList = new ArrayList<CommentBean>();
		for (DBObject answerDBObject : commentsList) {
			CommentBean answer = new CommentBean();
			answer.parseFromDB(answerDBObject);
			answersList.add(answer);
		}
		return answersList;
	}
	
	
	
	/*---------------- Common --------------------*/
	
	/**
	 * Adds newly created comment to given parent.
	 */
	private static void updateParent(DBCollection commentsColl, String parentCommentId, String commentId) {
		if (parentCommentId != null) {
			DBObject parentIdDBObject = new BasicDBObject("_id", new ObjectId(parentCommentId));
			commentsColl.update(parentIdDBObject, 
					new BasicDBObject("$push", new BasicDBObject("children", commentId)));
		}
	}
	
	/**
	 * Transforms list of comments/replies to tree of comments/replies
	 */
	private static List<CommentBean> parseCommentsTree(List<DBObject> commentsList) {
		List<CommentBean> topCommentsList = new ArrayList<CommentBean>();
		
		//temp map for resolving children
		Map<String, CommentBean> commentsCacheMap = new HashMap<String, CommentBean>();
		for (DBObject commentDBObject : commentsList) {
			CommentBean commentBean = new CommentBean();
			commentBean.parseFromDB(commentDBObject);
			commentsCacheMap.put(commentBean.getId(), commentBean);
			
			if (!commentBean.isDeleted()) {
				topCommentsList.add(commentBean);
			}
		}
		
		for (DBObject commentDBObject : commentsList) {
			String commentId = getString(commentDBObject, "_id");
			CommentBean commentBean = commentsCacheMap.get(commentId);
			
			//save children
			List<CommentBean> childrenList = new ArrayList<CommentBean>();
			List<String> childrenIdsList = getStringList(commentDBObject, "children");
			for (String childId : childrenIdsList) {
				CommentBean childrenCommentBean = commentsCacheMap.get(childId);
				if (!childrenCommentBean.isDeleted()) {
					childrenList.add(childrenCommentBean);
				}
				
				//remove replies from the list of answers/thoughts
				topCommentsList.remove(childrenCommentBean);
			}
			commentBean.setChildren(childrenList);
		}
		
		return topCommentsList;
	}
	
}

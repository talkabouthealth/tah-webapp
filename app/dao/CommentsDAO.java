package dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.CommentBean;
import models.TalkerBean;

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
	public static final String TOPIC_COMMENTS_COLLECTION = "topiccomments";
	
	// ---------------- Profile comments --------------------------
	public static String saveProfileComment(CommentBean comment) {
		DBCollection commentsColl = DBUtil.getCollection(PROFILE_COMMENTS_COLLECTION);
		
		DBRef profileTalkerRef = new DBRef(DBUtil.getDB(), 
				TalkerDAO.TALKERS_COLLECTION, new ObjectId(comment.getProfileTalkerId()));
		DBRef fromTalkerRef = new DBRef(DBUtil.getDB(), 
				TalkerDAO.TALKERS_COLLECTION, new ObjectId(comment.getFromTalker().getId()));
		DBObject commentObject = BasicDBObjectBuilder.start()
			.add("profile", profileTalkerRef)
			.add("from", fromTalkerRef)
			.add("text", comment.getText())
			.add("time", comment.getTime())
			.get();
		
		commentsColl.save(commentObject);
		
		updateParent(commentsColl, comment.getParentId(), commentObject.get("_id").toString());
		
		return commentObject.get("_id").toString();
	}
	
	public static List<CommentBean> loadProfileComments(String talkerId) {
		DBCollection commentsColl = DBUtil.getCollection(PROFILE_COMMENTS_COLLECTION);
		
		DBRef profileTalkerRef = new DBRef(DBUtil.getDB(), TalkerDAO.TALKERS_COLLECTION, new ObjectId(talkerId));
		DBObject query = BasicDBObjectBuilder.start()
			.add("profile", profileTalkerRef)
			.get();
		
		List<DBObject> commentsList = commentsColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		//comments without parent (top in hierarchy)
		List<CommentBean> topCommentsList = parseCommentsTree(commentsList);
		return topCommentsList;
	}

	// -------------- Topics comments -----------------
	public static String saveTopicComment(CommentBean comment) {
		DBCollection commentsColl = DBUtil.getCollection(TOPIC_COMMENTS_COLLECTION);
		
		DBRef topicRef = new DBRef(DBUtil.getDB(), 
				TopicDAO.TOPICS_COLLECTION, new ObjectId(comment.getTopicId()));
		DBRef fromTalkerRef = new DBRef(DBUtil.getDB(), 
				TalkerDAO.TALKERS_COLLECTION, new ObjectId(comment.getFromTalker().getId()));
		DBObject commentObject = BasicDBObjectBuilder.start()
			.add("topic", topicRef)
			.add("from", fromTalkerRef)
			.add("text", comment.getText())
			.add("time", comment.getTime())
			.get();
		
		commentsColl.save(commentObject);
		
		updateParent(commentsColl, comment.getParentId(), commentObject.get("_id").toString()); 
		
		return commentObject.get("_id").toString();
	}
	
	public static List<CommentBean> loadTopicComments(String topicId) {
		DBCollection commentsColl = DBUtil.getCollection(TOPIC_COMMENTS_COLLECTION);
		
		DBRef topicRef = new DBRef(DBUtil.getDB(), 
				TopicDAO.TOPICS_COLLECTION, new ObjectId(topicId));
		DBObject query = BasicDBObjectBuilder.start()
			.add("topic", topicRef)
			.get();
		
		List<DBObject> commentsList = commentsColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		
		//comments without parent (top in hierarchy)
		List<CommentBean> topCommentsList = parseCommentsTree(commentsList);
		return topCommentsList;
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
		//TODO: we don't need cache map because of sorting? (children comments are always older)
		Map<String, CommentBean> commentsCacheMap = new HashMap<String, CommentBean>();
		for (DBObject commentDBObject : commentsList) {
			String commentId = commentDBObject.get("_id").toString();
			
			CommentBean commentBean = commentsCacheMap.get(commentId);
			if (commentBean == null) {
				commentBean = new CommentBean(commentId);
				commentsCacheMap.put(commentId, commentBean);
			}
			topCommentsList.add(commentBean);
			
			commentBean.setText((String)commentDBObject.get("text"));
			commentBean.setTime((Date)commentDBObject.get("time"));
			
			//TODO: the same as thankyou?
			DBObject fromTalkerDBObject = ((DBRef)commentDBObject.get("from")).fetch();
			TalkerBean fromTalker = new TalkerBean();
			fromTalker.setUserName((String)fromTalkerDBObject.get("uname"));
			commentBean.setFromTalker(fromTalker);
			
			//save children
			List<CommentBean> childrenList = new ArrayList<CommentBean>();
			BasicDBList childrenDBList = (BasicDBList)commentDBObject.get("children");
			if (childrenDBList != null) {
				for (Object childIdObject : childrenDBList) {
					String childId = (String)childIdObject;
					
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
			}
			commentBean.setChildren(childrenList);
		}
		
		return topCommentsList;
	}
}
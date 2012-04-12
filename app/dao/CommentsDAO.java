package dao;

import java.text.SimpleDateFormat;
import static util.DBUtil.createRef;
import static util.DBUtil.getCollection;
import static util.DBUtil.getString;
import static util.DBUtil.getStringList;
import static util.DBUtil.setToDB;
import java.util.ArrayList;

import static util.DBUtil.*;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import logic.TalkerLogic;
import models.CommentBean;
import models.CommentBean.Vote;
import models.actions.AbstractAction;
import models.actions.Action;
import models.actions.AnswerDisplayAction;
import models.actions.PersonalProfileCommentAction;
import models.actions.PreloadAction;
import models.actions.Action.ActionType;
import models.ConversationBean;
import models.TalkerBean;
import models.TopicBean;

import org.bson.types.ObjectId;

import play.Logger;

import util.DBUtil;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import controllers.AnswerNotification;

/*
 	We store profile comments in separate collection, as "child lists" tree.
 	Example "child lists" structure:
		{"_id": "A", "children": ["B", "C"]}
		{"_id": "B", "children": ["D"]}
		{"_id": "C"}
		{"_id": "D"} 
 */
public class CommentsDAO {
	
	public static final String CONVERSATIONS_COLLECTION = "convos";	
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
			
			.add("from_service", comment.getFrom())
			.add("from_service_id", comment.getFromId())
			
			.add("rootid",comment.getRootId())
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

		commentsColl.ensureIndex(new BasicDBObject("time", 1));
		//Added from_Service to hide thank you from the thoughts page. #21378031
		DBRef profileTalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talkerId);
		DBObject query = BasicDBObjectBuilder.start()
			.add("profile", profileTalkerRef)
			.add("from_service", new BasicDBObject("$ne", "thankyou"))
			.get();
		List<DBObject> commentsList = commentsColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		//comments without parent (top in hierarchy)
		List<CommentBean> topCommentsList = parseCommentsTree(commentsList);
		return topCommentsList;
	}
	
	public static CommentBean getThoughtByFromInfo(String from, String fromId) {
		DBCollection convosColl = getCollection(PROFILE_COMMENTS_COLLECTION);
		
		DBObject query = BasicDBObjectBuilder.start()
			.add("from_service", from)
			.add("from_service_id", fromId)
			.get();
		DBObject commentDBObject = convosColl.findOne(query);
		
		if (commentDBObject == null) {
			return null;
		}
		
		CommentBean thought = new CommentBean();
		thought.parseFromDB(commentDBObject);
		return thought;
	}
	
	public static boolean getThoughtDuplicates(String fromId,String text,int lookupDepth) {
		DBCollection convosColl = getCollection(PROFILE_COMMENTS_COLLECTION);
		
		DBRef fromTalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, fromId);		
		
		// three days back
		Date now = new Date();
		Date start = new Date(now.getTime()-lookupDepth*24*3600000);
		//BasicDBObject time = new BasicDBObject("ts", start);
		BasicDBObject time = new BasicDBObject("$gt", start);
		
		DBObject query = BasicDBObjectBuilder.start()
		.add("text",text)
		.add("from",fromTalkerRef)
		.add("time", time)
		.get();
		
		DBCursor cur = convosColl.find(query);
		return cur.hasNext();
	}
	
	/**
	 * Return list of replies with given root id
	 * 
	 * @param rootId
	 * @return
	 */
	public static List<CommentBean> getThoughtByRootId(String rootId) {
		DBCollection convosColl = getCollection(PROFILE_COMMENTS_COLLECTION);
		
		DBObject query = BasicDBObjectBuilder.start()
		.add("rootid",rootId)
		.get();
		
		DBCursor cur = convosColl.find(query);
		
		if(!cur.hasNext()) return null; 
		List<CommentBean> result = new ArrayList<CommentBean>();
        while(cur.hasNext()) {
        	CommentBean m = new CommentBean();   
        	m.parseFromDB(cur.next());
        	result.add(m);        	
        }		
		
		return result;
	}
	

	// -------------- Convo comments -----------------
	
	/**
	 * Get conversation answer/reply/convoreply by id.
	 */
	public static CommentBean getConvoCommentById(String commentId) {
		DBCollection commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		
		DBObject query = new BasicDBObject("_id", new ObjectId(commentId));
		DBObject answerDBObject = commentsColl.findOne(query);
		
		CommentBean comment = new CommentBean();
		comment.parseFromDB(answerDBObject);
		return comment;
	}
	
	/**
	 * Get list of replies stored with conversation comment
	 * @param String parentId
	 * @return List<String> of reply-ids in string form
	 */
	public static List<String> getConvoReplies(String parentId) {
		return CommentsDAO.getConvoReplies(getCollection(CONVO_COMMENTS_COLLECTION),parentId);
	}
	
	/**
	 * Get list of replies stored with conversation comment
	 * @param String parentId
	 * @return List<String> of reply-ids in string form
	 */
	public static List<String> getConvoReplies(DBCollection coll, String parentId) {
		List<String> result = new ArrayList<String>();
		if(parentId == null) return result;
		
		DBObject query = new BasicDBObject("_id", new ObjectId(parentId));
		DBObject answerDBObject = coll.findOne(query);
				
		BasicDBList replies = (BasicDBList) answerDBObject.get("children");
		if(replies.size()>0) for(Object reply : replies) result.add((String)reply);

		return result;
	}	
	
	/**
	 * Retrieve a set of comments by a list of their ids
	 * @param List<String> commentIds
	 * @return List<CommentBean> list of replies in CommentBean form
	 */
	public static List<CommentBean> getConvoCommentsByIds(List<String> commentIds) {
		if(commentIds.size()==0) return new ArrayList<CommentBean>();
		
		DBCollection commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		
		List<ObjectId> objects = new ArrayList<ObjectId>();
		for(String commentID : commentIds) if(commentID != null) objects.add(new ObjectId(commentID));
		
		DBObject query = BasicDBObjectBuilder.start()
		.add("_id", new BasicDBObject("$in",objects))
		.get();
		
		DBCursor cur = commentsColl.find(query);

		List<CommentBean> result = new ArrayList<CommentBean>();
		
		while(cur.hasNext()) {
			CommentBean comment = new CommentBean();
			comment.parseFromDB(cur.next());
			result.add(comment);
		}
		
		return result;
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
			.add("convoreply", comment.isConvoReply())
			.get();
		commentsColl.save(commentObject);
		
		// update [children ...] repo on target answer/comment
		updateParent(commentsColl, comment.getParentId(), getString(commentObject, "_id"));
		
		if(comment.getParentId() == null) {
			// if nothing, then save this at the [children ...] of the conversation root
			DBCollection convoColl = getCollection(CONVERSATIONS_COLLECTION);
			updateParent(convoColl, comment.getConvoId(), getString(commentObject, "_id"));
		}
		return getString(commentObject, "_id");
	}
	
	public static void updateConvoComment(CommentBean answer) {
		DBCollection commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		
		DBRef convoRef = createRef(ConversationDAO.CONVERSATIONS_COLLECTION, answer.getConvoId());
		DBObject answerObject = BasicDBObjectBuilder.start()
			.add("convo", convoRef)
			
			.add("vote_score", answer.getVoteScore())
			.add("votes", setToDB(answer.getVotes()))

			.add("text", answer.getText())
			.add("old_texts", answer.getOldTexts())
			
			.add("deleted", answer.isDeleted())
			.add("answer", answer.isAnswer())
			.add("convoreply", answer.isConvoReply())
			
			.add("not_helpful", answer.isNotHelpful())
			.add("not_helpful_votes", setToDB(answer.getNotHelpfulVotes()))
			
			.add("moderate", answer.getModerate())
			
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
			.add("convoreply", new BasicDBObject("$ne", true))
			.get();
		List<DBObject> commentsList = commentsColl.find(query).sort(new BasicDBObject("vote_score", -1)).toArray();
		
		//comments without parent (top in hierarchy)
		List<CommentBean> topCommentsList = parseCommentsTree(commentsList);
		return topCommentsList;
	}
	
	public static List<CommentBean> loadConvoReplies(String convoId) {
		DBCollection commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		
		DBRef convoRef = createRef(ConversationDAO.CONVERSATIONS_COLLECTION, convoId);
		DBObject query = BasicDBObjectBuilder.start()
			.add("convo", convoRef)
			.add("convoreply", true)
			.add("deleted", new BasicDBObject("$ne", true))
			.get();
		List<DBObject> commentsDBList = commentsColl.find(query).sort(new BasicDBObject("time", 1)).toArray();
		
		List<CommentBean> convoReplies = new ArrayList<CommentBean>();
		for (DBObject commentDBObject : commentsDBList) {
			CommentBean commentBean = new CommentBean();
			commentBean.parseFromDB(commentDBObject);
			convoReplies.add(commentBean);
		}
		return convoReplies;
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
		List<DBObject> commentsList = commentsColl.find(query, new BasicDBObject("_id", 1)).toArray();
		
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
	
	/**
	 * Returns talker number of answers, all or filtered by given topic
	 */
	public static int getTalkerNumberOfAnswers(String talkerId, TopicBean topic) {
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
		
		int numOfAnswers = commentsColl.find(query).count();
		return numOfAnswers;
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
			
			//if (!commentBean.isDeleted()) {
				topCommentsList.add(commentBean);
			//}
		}
		
		for (DBObject commentDBObject : commentsList) {
			String commentId = getString(commentDBObject, "_id");
			CommentBean commentBean = commentsCacheMap.get(commentId);
			
			//save children
			List<CommentBean> childrenList = new ArrayList<CommentBean>();
			List<String> childrenIdsList = getStringList(commentDBObject, "children");
			for (String childId : childrenIdsList) {
				CommentBean childrenCommentBean = commentsCacheMap.get(childId);
				//if (!childrenCommentBean.isDeleted()) {
					childrenList.add(childrenCommentBean);
				//}
				
				//remove replies from the list of answers/thoughts
				topCommentsList.remove(childrenCommentBean);
			}
			commentBean.setChildren(childrenList);
		}
		
		return topCommentsList;
	}
	
	/**
	 * Returns talker's mentions ('@username') in thoughts and answers
	 * @param talker
	 * @return
	 */
	public static List<Action> getTalkerMentions(TalkerBean talker, String nextActionId) {
		Date firstActionTime = null;
		if (nextActionId != null) {
			firstActionTime = getProfileCommentTime(nextActionId);
		}
		
		DBCollection commentsColl = getCollection(PROFILE_COMMENTS_COLLECTION);
		
		Pattern mentionRegex = Pattern.compile("@"+talker.getUserName()+"[^\\w]*", Pattern.CASE_INSENSITIVE);
		BasicDBObjectBuilder queryBuilder =  BasicDBObjectBuilder.start()
			.add("text", mentionRegex)
			.add("deleted", new BasicDBObject("$ne", true));
		if (firstActionTime != null) {
			queryBuilder.add("time", new BasicDBObject("$gt", firstActionTime));
		}
		DBObject query = queryBuilder.get();
		List<DBObject> commentsList = null;
		commentsList = commentsColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		List<Action> talkerMentions = new ArrayList<Action>();
		for (DBObject commentDBObject : commentsList) {
			CommentBean commentBean = new CommentBean();
			commentBean.parseFromDB(commentDBObject);
			PersonalProfileCommentAction thoughtAction = new PersonalProfileCommentAction(
					talker, talker, commentBean, null, ActionType.PERSONAL_PROFILE_COMMENT);
			thoughtAction.setID(commentBean.getId());
			thoughtAction.setTime(commentBean.getTime());
			talkerMentions.add(thoughtAction);
		}
		
		//add answers also
		commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		queryBuilder =  BasicDBObjectBuilder.start()
			.add("text", mentionRegex)
			.add("answer", true)
			.add("deleted", new BasicDBObject("$ne", true));
		if (firstActionTime != null) {
			queryBuilder.add("time", new BasicDBObject("$gt", firstActionTime));
		}
		
		query = queryBuilder.get();
		commentsList = commentsColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		for (DBObject commentDBObject : commentsList) {
			CommentBean answer = new CommentBean();
			answer.parseFromDB(commentDBObject);
			
			ConversationBean convo = TalkerLogic.loadConvoFromCache(answer.getConvoId());
			
			AnswerDisplayAction answerAction = new AnswerDisplayAction(answer.getFromTalker(), convo,
					answer, ActionType.ANSWER_CONVO, "Answer");
			answerAction.setTime(answer.getTime());
			talkerMentions.add(answerAction);
		}
		
		Collections.sort(talkerMentions, new Comparator<Action>() {
			@Override
			public int compare(Action o1, Action o2) {
				return o2.getTime().compareTo(o1.getTime());
			}
		});
		
		return talkerMentions;
	}

	public static List<Action> getTopicMentions(TopicBean topic) {
		DBCollection commentsColl = getCollection(PROFILE_COMMENTS_COLLECTION);
		
		String topicTitle = topic.getTitle().replaceAll(" ", "");
		Pattern mentionRegex = Pattern.compile("#"+topicTitle+"[^\\w]*", Pattern.CASE_INSENSITIVE);
		DBObject query = BasicDBObjectBuilder.start()
			.add("text", mentionRegex)
			.add("deleted", new BasicDBObject("$ne", true))
			.get();
		List<DBObject> commentsList = commentsColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		List<Action> topicMentions = new ArrayList<Action>();
		for (DBObject commentDBObject : commentsList) {
			CommentBean commentBean = new CommentBean();
			commentBean.parseFromDB(commentDBObject);
			
			Action thoughtAction = new PersonalProfileCommentAction(commentBean.getFromTalker(),
					commentBean.getFromTalker(), commentBean, null, ActionType.PERSONAL_PROFILE_COMMENT);
			thoughtAction.setID(commentBean.getId());
			topicMentions.add(thoughtAction);
		}
		return topicMentions;
	}
	
	public static Date getProfileCommentTime(String id) {
		DBCollection commentsColl = getCollection(PROFILE_COMMENTS_COLLECTION);

		DBObject query = new BasicDBObject("_id", new ObjectId(id));
		DBObject actionDBObject = commentsColl.findOne(query);
		if (actionDBObject != null) {
			return (Date)actionDBObject.get("time");
		}
		return null;
	}
	
	public static List<Action> getProfileComments(String id, TalkerBean profile){
		DBCollection commentsColl = getCollection(PROFILE_COMMENTS_COLLECTION);
		
		DBRef fromTalkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, profile.getId());
		
		DBObject query = BasicDBObjectBuilder.start()
		//.add("profile", fromTalkerRef)
		.add("rootid", id)
		.add("deleted", new BasicDBObject("$ne", true))
		.get();
		
		List<Action> personalProfileList = new ArrayList<Action>();
		List<DBObject> commentsList = commentsColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		for (DBObject commentDBObject : commentsList) {
			CommentBean commentBean = new CommentBean();
			commentBean.parseFromDB(commentDBObject);
			Action thankYouAction = new PersonalProfileCommentAction(commentBean.getFromTalker(),
					commentBean.getFromTalker(), commentBean, null, ActionType.PERSONAL_PROFILE_COMMENT);
			thankYouAction.setID(commentBean.getId());
			personalProfileList.add(thankYouAction);
		}
		return personalProfileList;
	}
	
	/**
	 * Load all not-deleted answers for given conversation,
	 * answers have only id.
	 */
	public static List<CommentBean> loadAllConvoAnswers(Date date) {
		DBCollection commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		
		DBObject query = BasicDBObjectBuilder.start()
			.add("time", new BasicDBObject("$gt", date))
			.add("deleted", new BasicDBObject("$ne", true))
			.add("answer", true)
			.get();
		List<DBObject> commentsList = commentsColl.find(query).toArray();
		
		List<CommentBean> answersList = new ArrayList<CommentBean>();
		
		for (DBObject answerDBObject : commentsList) {
			CommentBean answer = new CommentBean();
			answer.parseFromDB(answerDBObject);
			answersList.add(answer);
		}
		return answersList;
	}
	
	/**
	 * Loads all answers and replies for given conversation.
	 */
	public static List<CommentBean> loadAllConvoAnswers(String convoId) {
		DBCollection commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		
		DBRef convoRef = createRef(ConversationDAO.CONVERSATIONS_COLLECTION, convoId);
		DBObject query = BasicDBObjectBuilder.start()
			.add("convo", convoRef)
			.get();
		List<DBObject> commentsList = commentsColl.find(query).toArray();
		
		List<CommentBean> answersList = new ArrayList<CommentBean>();
		//comments without parent (top in hierarchy)
		for (DBObject answerDBObject : commentsList) {
			CommentBean answer = new CommentBean();
			answer.parseFromDB(answerDBObject);
			if(answer.isDeleted() == true){
				answer.setModerate(AnswerNotification.DELETE_ANSWER);
			}
			answersList.add(answer);
		}
		
		return answersList;
	}
	/**
	 * Load no of answers for conversation.
	 * @param convoId
	 * @return int
	 */
	public static int getConvoAnswersCount(String convoId){
		DBCollection commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		DBRef convoRef = createRef(ConversationDAO.CONVERSATIONS_COLLECTION, convoId);
		DBObject query = BasicDBObjectBuilder.start()
			.add("convo", convoRef)
			.add("deleted", new BasicDBObject("$ne", true))
			.add("answer", true)
			.get();
		
		BasicDBObject bd=new BasicDBObject();
		bd.append("-id",1);
		bd.append("from", 1);
		bd.append("text", 1);
		
		return commentsColl.find(query, bd).count();
	}
	
	/**
	 * Loads full tree of answers and replies for given conversation.
	 */
	public static List<CommentBean> loadConvoAnswersTreeForScheduler(String convoId) {
		DBCollection commentsColl = getCollection(CONVO_COMMENTS_COLLECTION);
		
		DBRef convoRef = createRef(ConversationDAO.CONVERSATIONS_COLLECTION, convoId);
		DBObject query = BasicDBObjectBuilder.start()
			.add("convo", convoRef)
			.add("convoreply", new BasicDBObject("$ne", true))
			.add("vote_score", new BasicDBObject("$ne", 0))
			.get();
		
		BasicDBObject orderby=new BasicDBObject();
		orderby.put("vote_score", -1);
		orderby.put("time", 1);
		
		List<DBObject> commentsList = commentsColl.find(query).sort(orderby).toArray();
		
		
		query=BasicDBObjectBuilder.start()
		.add("convo", convoRef)
		.add("convoreply", new BasicDBObject("$ne", true))
		.add("vote_score", 0)
		.get();
		commentsList.addAll(commentsColl.find(query).sort(new BasicDBObject("time", -1)).toArray());
		
		List<CommentBean> topCommentsList = parseCommentsTreeForScheduler(commentsList);
		return topCommentsList;
	}
	
	
	/**
	 * Transforms list of comments/replies to tree of comments/replies
	 */
	private static List<CommentBean> parseCommentsTreeForScheduler(List<DBObject> commentsList) {
		List<CommentBean> topCommentsList = new ArrayList<CommentBean>();
		
		//temp map for resolving children
		Map<String, CommentBean> commentsCacheMap = new HashMap<String, CommentBean>();
		for (DBObject commentDBObject : commentsList) {
			CommentBean commentBean = new CommentBean();
			commentBean.parseBasicFromDB(commentDBObject);
			commentsCacheMap.put(commentBean.getId(), commentBean);
			
			//if (!commentBean.isDeleted()) {
				topCommentsList.add(commentBean);
			//}
			
			CommonUtil.commentToHTML(commentBean);
		}
		
		for (DBObject commentDBObject : commentsList) {
			String commentId = getString(commentDBObject, "_id");
			CommentBean commentBean = commentsCacheMap.get(commentId);
			
			//save children
			List<CommentBean> childrenList = new ArrayList<CommentBean>();
			List<String> childrenIdsList = getStringList(commentDBObject, "children");
			for (String childId : childrenIdsList) {
				CommentBean childrenCommentBean = commentsCacheMap.get(childId);
				//if (!childrenCommentBean.isDeleted()) {
					childrenList.add(childrenCommentBean);
				//}
				
				//remove replies from the list of answers/thoughts
				topCommentsList.remove(childrenCommentBean);
			}
			commentBean.setChildren(childrenList);
			childrenList.clear();
			CommonUtil.commentToHTML(commentBean);
		}
		
		return topCommentsList;
	}

}

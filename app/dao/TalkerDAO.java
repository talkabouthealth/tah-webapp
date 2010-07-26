package dao;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.CommentBean;
import models.TalkerBean;
import models.ThankYouBean;

import org.apache.lucene.analysis.cn.ChineseTokenizer;
import org.bson.types.ObjectId;

import util.DBUtil;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

public class TalkerDAO {
	
	public static final String TALKERS_COLLECTION = "talkers";
	public static final String PROFILE_COMMENTS_COLLECTION = "profilecomments";
	
	public static boolean save(TalkerBean talker) {
		DBCollection talkersColl = DBUtil.getCollection(TALKERS_COLLECTION);

		DBObject talkerDBObject = BasicDBObjectBuilder.start()
				.add("uname", talker.getUserName())
				.add("pass", talker.getPassword())
				.add("email", talker.getEmail())
				.add("dob", talker.getDob())
				.add("gender", talker.getGender())
				.add("timestamp",  Calendar.getInstance().getTime())
				.add("im", talker.getIm())
				.add("im_uname", talker.getImUsername())
				.add("connection", talker.getConnection())
				.add("newsletter", talker.isNewsletter())
				.add("act_type", talker.getAccountType())
				.add("act_id", talker.getAccountId())
				.add("invites", 100)
				.get();

		talkersColl.save(talkerDBObject);
		return true;
	}
	
	public static void updateTalker(TalkerBean talker) {
		DBCollection talkersColl = DBUtil.getCollection(TALKERS_COLLECTION);
		
		DBObject talkerObject = BasicDBObjectBuilder.start()
			.add("uname", talker.getUserName())
			.add("pass", talker.getPassword())
			.add("email", talker.getEmail())
			.add("dob", talker.getDob())
			.add("gender", talker.getGender())
			.add("mar_status", talker.getMaritalStatus())
			.add("city", talker.getCity())
			.add("state", talker.getState())
			.add("country", talker.getCountry())
			.add("category", talker.getCategory())
			.add("connection", talker.getConnection())
			.add("ch_num", talker.getChildrenNum())
			.add("invites", talker.getInvitations())
			.add("prefs", talker.profilePreferencesToInt())
			.add("im", talker.getIm())
			.add("im_uname", talker.getImUsername())
			.add("img", talker.getImagePath())
			.add("nfreq", talker.getNfreq())
			.add("ntime", talker.getNtime())
			.add("ctype", talker.getCtype())
			.get();
		
		DBObject talkerId = new BasicDBObject("_id", new ObjectId(talker.getId()));
		//"$set" is used for updating fields
		talkersColl.update(talkerId, new BasicDBObject("$set", talkerObject));
	}
	
	public static TalkerBean getByUserName(String userName) {
		return getByField("uname", userName);
	}
	
	public static TalkerBean getByEmail(String email) {
		return getByField("email", email);
	}
	
	/**
	 * For now, there will only be the breast cancer community,
	 * so this method returns total number of users (talkers) in the DB.
	 */
	public static long getNumberOfTalkers() {
		DBCollection talkersColl = DBUtil.getCollection(TALKERS_COLLECTION);
		return talkersColl.count();
	}
	
	public static TalkerBean getTalkerByLoginInfo(String usernameOrEmail, String password) {
		DBCollection talkersColl = DBUtil.getCollection(TALKERS_COLLECTION);
		
		DBObject query = new BasicDBObject();
		query.put("uname", usernameOrEmail);
		query.put("pass", password);
		
		DBObject talkerDBObject = talkersColl.findOne(query);
		
		/*
		 	Login by Username or Email
		 	Current version (1.4.4) doesn't support "$or" query (will be supported in 1.5.3),
		 	so we make second query
		 	TODO: Later use "$or" operator
		*/
		if (talkerDBObject == null) {
			query = new BasicDBObject();
			query.put("email", usernameOrEmail);
			query.put("pass", password);
			
			talkerDBObject = talkersColl.findOne(query);
		}
		
		TalkerBean talker = null;
		if (talkerDBObject != null) {
			talker = new TalkerBean();
			talker.parseFromDB(talkerDBObject);
		}
		return talker;
	}
	
	public static TalkerBean getTalkerByAccount(String accountType, String accountId) {
		DBCollection talkersColl = DBUtil.getCollection(TALKERS_COLLECTION);
		
		DBObject query = new BasicDBObject();
		query.put("act_type", accountType);
		query.put("act_id", accountId);
		
		DBObject talkerDBObject = talkersColl.findOne(query);
		if (talkerDBObject == null) {
			return null;
		}
		else {
			TalkerBean talker = new TalkerBean();
			talker.parseFromDB(talkerDBObject);
			return talker;
		}
	}
	
	/*
	 * Loads talker bean by custom field
	 */
	private static TalkerBean getByField(String fieldName, Object fieldValue) {
		DBCollection talkersColl = DBUtil.getCollection(TALKERS_COLLECTION);
		
		DBObject query = new BasicDBObject(fieldName, fieldValue);
		DBObject talkerDBObject = talkersColl.findOne(query);
		
		if (talkerDBObject == null) {
			return null;
		}
		else {
			TalkerBean talker = new TalkerBean();
			talker.parseFromDB(talkerDBObject);
			return talker;
		}
	}
	
	public static List<Map<String, String>> loadTalkersForDashboard() {
		DBCollection talkersColl = DBUtil.getDB().getCollection(TALKERS_COLLECTION);
		DateFormat dateFormat = new SimpleDateFormat("MM.dd.yyyy HH:mm:ss");
		
		//TODO: sort by last notification!
		List<DBObject> talkersDBList = talkersColl.find().toArray();
		
		List<Map<String, String>> talkersInfoList = new ArrayList<Map<String,String>>();
		for (DBObject talkerDBObject : talkersDBList) {
			Map<String, String> talkerInfoMap = new HashMap<String, String>();
			
			talkerInfoMap.put("id", talkerDBObject.get("_id").toString());
			talkerInfoMap.put("uname", talkerDBObject.get("uname").toString());
			talkerInfoMap.put("email", talkerDBObject.get("email").toString());
			talkerInfoMap.put("imService", talkerDBObject.get("im").toString());
			talkerInfoMap.put("imUsername", talkerDBObject.get("im_uname").toString());
			
			Date latestNotification = NotificationDAO.getLatestNotification(talkerInfoMap.get("id"));
			if (latestNotification != null) {
				talkerInfoMap.put("latestNotification", dateFormat.format(latestNotification));
			}
			long numOfNotifications = NotificationDAO.getNumOfNotifications(talkerInfoMap.get("id"));
			talkerInfoMap.put("numOfNotifications", ""+numOfNotifications);
			
			talkersInfoList.add(talkerInfoMap);
		}
		
		return talkersInfoList;
	}
	
	
	/* --------------------- "Thank you" feature ----------------------------- */
	public static void saveThankYou(ThankYouBean thankYouBean) {
		DBCollection talkersColl = DBUtil.getCollection(TALKERS_COLLECTION);
		
		DBRef fromTalkerRef = new DBRef(DBUtil.getDB(), TALKERS_COLLECTION, new ObjectId(thankYouBean.getFrom()));
		DBObject thankYouObject = BasicDBObjectBuilder.start()
			.add("time", thankYouBean.getTime())
			.add("note", thankYouBean.getNote())
			.add("from", fromTalkerRef)
			.get();
		
		DBObject talkerId = new BasicDBObject("_id", new ObjectId(thankYouBean.getTo()));
		//For creating/adding to array: { $push : { field : value } }
		talkersColl.update(talkerId, new BasicDBObject("$push", new BasicDBObject("thankyous", thankYouObject)));
	}
	
	/* ---------------------- Following/Followers feature --------------------------- */
	
	/**
	 * Follows or unfollows depending on third parameter
	 */
	public static void followAction(String followerId, String followingId, boolean follow) {
		DBCollection talkersColl = DBUtil.getCollection(TALKERS_COLLECTION);
		
		DBRef followingTalkerRef = new DBRef(DBUtil.getDB(), 
				TALKERS_COLLECTION, new ObjectId(followingId));
		DBObject talkerId = new BasicDBObject("_id", new ObjectId(followerId));
		
		String action = null;
		if (follow) {
			//add to array
			action = "$push";
		}
		else {
			//remove from array
			action = "$pull";
		}
		DBObject updateQuery = new BasicDBObject(action, new BasicDBObject("following", followingTalkerRef));
		talkersColl.update(talkerId, updateQuery);
	}
	
	public static List<TalkerBean> loadFollowers(String talkerId) {
		DBCollection talkersColl = DBUtil.getCollection(TALKERS_COLLECTION);

		DBRef followingTalkerRef = new DBRef(DBUtil.getDB(), 
				TALKERS_COLLECTION, new ObjectId(talkerId));
		BasicDBObject query = new BasicDBObject("following", followingTalkerRef);
		List<DBObject> followerDBList = talkersColl.find(query).toArray();
		
		List<TalkerBean> followerList = new ArrayList<TalkerBean>();
		for (DBObject followerDBObject : followerDBList) {
			//TODO: same as following?
			TalkerBean followerTalker = new TalkerBean();
			followerTalker.setId(followerDBObject.get("_id").toString());
			followerTalker.setUserName(followerDBObject.get("uname").toString());
			//Test?
			Object imgObject = followerDBObject.get("img");
			if (imgObject != null) {
				followerTalker.setImagePath((String)imgObject);
			}
			
			followerList.add(followerTalker);
		}
		
		return followerList;
	}
	
	
	/* ---------------- Profile comments -------------------------- 
	 	We store profile comments in separate collection, as "child lists" tree.
	 	Example "child lists" structure:
			{"_id": "A", "children": ["B", "C"]}
			{"_id": "B", "children": ["D"]}
			{"_id": "C"}
			{"_id": "D"}
	 */
	public static void saveProfileComment(CommentBean comment) {
		DBCollection commentsColl = DBUtil.getCollection(PROFILE_COMMENTS_COLLECTION);
		
		DBRef profileTalkerRef = new DBRef(DBUtil.getDB(), 
				TALKERS_COLLECTION, new ObjectId(comment.getProfileTalkerId()));
		DBRef fromTalkerRef = new DBRef(DBUtil.getDB(), 
				TALKERS_COLLECTION, new ObjectId(comment.getFromTalker().getId()));
		DBObject commentObject = BasicDBObjectBuilder.start()
			.add("profile", profileTalkerRef)
			.add("from", fromTalkerRef)
			.add("text", comment.getText())
			.add("time", comment.getTime())
			.get();
		
		commentsColl.save(commentObject);
		
		//update parent - add new comment "_id" to children array
		String parentCommentId = comment.getParentId();
		if (parentCommentId != null) {
			DBObject parentIdDBObject = new BasicDBObject("_id", new ObjectId(parentCommentId));
			commentsColl.update(parentIdDBObject, 
					new BasicDBObject("$push", new BasicDBObject("children", commentObject.get("_id").toString())));
		}
	}
	
	public static List<CommentBean> loadProfileComments(String talkerId) {
		DBCollection talkersColl = DBUtil.getCollection(PROFILE_COMMENTS_COLLECTION);
		
		DBRef profileTalkerRef = new DBRef(DBUtil.getDB(), TALKERS_COLLECTION, new ObjectId(talkerId));
		DBObject query = BasicDBObjectBuilder.start()
			.add("profile", profileTalkerRef)
			.get();
		
		List<DBObject> commentsList = talkersColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		//comments without parent (top in hierarchy)
		List<CommentBean> topCommentsList = new ArrayList<CommentBean>();
		//temp map for resolving children
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
			fromTalker.setImagePath((String)fromTalkerDBObject.get("img"));
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
		}
		
		return topCommentsList;
	}
	
	public static void main(String[] args) {
//		TalkerDAO.follow("4c2cb43160adf3055c97d061", "4c35dbeb5165f305eebfc5f2", true);
//		System.out.println(loadFollowers("4c35dbeb5165f305eebfc5f2"));
		
//		TalkerDAO.saveProfileComment("4c35dbeb5165f305eebfc5f2", "4c2cb43160adf3055c97d061", "Teeeext");
//		TalkerDAO.loadProfileComments("4c35dbeb5165f305eebfc5f2");
//		TalkerDAO.saveProfileReply("4c35dbeb5165f305eebfc5f2", "4c35dbeb5165f305eebfc5f2", "Reply2222");
		
		System.out.println("cool");
	}

}


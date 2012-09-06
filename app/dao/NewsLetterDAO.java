package dao;

import static util.DBUtil.createRef;
import static util.DBUtil.getCollection;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.time.DateUtils;

import play.Logger;

import util.DBUtil;

import models.NewsLetterBean;
import models.TalkerBean;
import models.TopicBean;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

public class NewsLetterDAO {
	
	public static final String NEWSLETTER_COLLECTION = "newsletter";
	
	/**
	 * Method used for save or update newsletter information.
	 * @param newsLetterBean
	 */
	public static boolean saveOrUpdateNewsletter(NewsLetterBean newsLetterBean,TalkerBean talker){
		DBCollection newsLetterColl = getCollection(NEWSLETTER_COLLECTION);
		boolean isExist = ApplicationDAO.isEmailExists(newsLetterBean.getEmail());
		String []types = newsLetterBean.getNewsLetterType();
		if(isExist) {
			DBObject email = new BasicDBObject("email", newsLetterBean.getEmail());
			if(talker == null) {
				DBObject obj=newsLetterColl.findOne(email);
				Collection<String> newLetterTypes = (Collection<String>)obj.get("newsletter_type");
				if(newLetterTypes != null && !newLetterTypes.isEmpty()) {
					for(String type:types) {
						if(!newLetterTypes.contains(type)) {
							newLetterTypes.add(type);
							populateStats(type,true);
						}
					}
				}
				types = newLetterTypes.toArray(new String[]{});
			} else {
				DBObject obj=newsLetterColl.findOne(email);
				Collection<String> newLetterTypes = (Collection<String>)obj.get("newsletter_type");
				if(newLetterTypes != null && !newLetterTypes.isEmpty()) {
					for(String type:types) {
						if(!newLetterTypes.contains(type)) {
							populateStats(type,true);
						}
					}
				}
			}
			
			DBObject newsLetterDBObject = BasicDBObjectBuilder.start()
				.add("email", newsLetterBean.getEmail())
				.add("newsletter_type", types)
				.get();
			newsLetterColl.update(email,newsLetterDBObject);
		} else {
			for(String type:types) {
				populateStats(type,true);
			}
			DBObject newsLetterDBObject = BasicDBObjectBuilder.start()
				.add("email", newsLetterBean.getEmail())
				.add("newsletter_type", types)
				.get();
			newsLetterColl.save(newsLetterDBObject);
		}
		return isExist;
	}
	
	public static void populateStats(String newsLetter, boolean addFlag){
		DBCollection newsLetterColl = getCollection("newsletterStats");
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-dd-MM");
		Date dt = Calendar.getInstance().getTime();
		Logger.info(dateFormat.format(dt));
		DBObject query = new BasicDBObject("newsletter_type", newsLetter).append("timestamp",dateFormat.format(dt));
		DBObject obj = newsLetterColl.findOne(query);
		if(obj != null) {
			DBObject updateObj;
			if(addFlag)
				updateObj = new BasicDBObject("letterCount", 1);
			else
				updateObj = new BasicDBObject("letterCount", -1);
			newsLetterColl.update(query,new BasicDBObject("$inc", updateObj));
		} else {
			DBObject newsLetterDBObject = BasicDBObjectBuilder.start()
					.add("newsletter_type", newsLetter)
					.add("timestamp", dateFormat.format(dt))
					.add("letterCount", 1)
					.get();
			newsLetterColl.save(newsLetterDBObject);
		}
		Logger.info("Done");
		
	}
	public static void main(String [] args){
		populateStats("workshop",true);
	}
	/**
	 * Method used for get newsletter information by user email.
	 * @param email
	 * @return NewsLetterBean
	 */
	public static NewsLetterBean getNewsLetterInfo(String email){
		DBCollection newsLetterColl = getCollection(NEWSLETTER_COLLECTION);
		NewsLetterBean newsLetterBean = null;
		DBObject query = new BasicDBObject("email", email);
		DBObject newsLetterDBObject = newsLetterColl.findOne(query);
		if(newsLetterDBObject != null){
			newsLetterBean = new NewsLetterBean();
			newsLetterBean.parseBasicFromDB(newsLetterDBObject);
		}
		return newsLetterBean;
	}
	
	
	public static boolean saveOrUpdateTopicNewsletter(String email, String topicId) {
		String NEWSLETTER_COLLECTION = "topicNewsletter";
		DBCollection newsLetterColl = getCollection(NEWSLETTER_COLLECTION);
		DBRef topicRef = createRef(TopicDAO.TOPICS_COLLECTION, topicId);
		DBObject topicIdObj = new BasicDBObject("topicId", topicRef);
		DBObject obj = newsLetterColl.findOne(topicIdObj);
		Set<String> emailList = null;
		DBObject newsLetterDBObject;
		if(obj == null) {
			emailList = new HashSet<String>();
			emailList.add(email);
			newsLetterDBObject = BasicDBObjectBuilder.start().add("topicId", topicRef).add("email", emailList).get();
			newsLetterColl.save(newsLetterDBObject);
		} else {
			emailList = DBUtil.getStringSet(obj,"email");
			emailList.add(email);
			newsLetterDBObject = BasicDBObjectBuilder.start().add("topicId", topicRef).add("email", emailList).get();
			newsLetterColl.update(topicIdObj,newsLetterDBObject);
		}
		return true;
	}
	
	public static boolean saveOrUpdateTalkerNewsletter(String email, String talkerId) {
		String NEWSLETTER_COLLECTION = "talkerNewsletter";
		DBCollection newsLetterColl = getCollection(NEWSLETTER_COLLECTION);
		DBRef talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talkerId);
		DBObject talkerIdObj = new BasicDBObject("talkerId", talkerRef);
		DBObject obj = newsLetterColl.findOne(talkerIdObj);
		Set<String> emailList = null;
		DBObject newsLetterDBObject;
		if(obj == null) {
			emailList = new HashSet<String>();
			emailList.add(email);
			newsLetterDBObject = BasicDBObjectBuilder.start().add("talkerId", talkerRef).add("email", emailList).get();
			newsLetterColl.save(newsLetterDBObject);
		} else {
			emailList = DBUtil.getStringSet(obj,"email");
			emailList.add(email);
			newsLetterDBObject = BasicDBObjectBuilder.start().add("talkerId", talkerRef).add("email", emailList).get();
			newsLetterColl.update(talkerIdObj,newsLetterDBObject);
		}
		return true;
	}
	
	public static boolean isSubscribeTalker(String email, String talkerId) {
		String NEWSLETTER_COLLECTION = "talkerNewsletter";
		DBCollection newsLetterColl = getCollection(NEWSLETTER_COLLECTION);
		DBRef talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, talkerId);
		DBObject talkerIdObj = new BasicDBObject("talkerId", talkerRef);
		DBObject obj = newsLetterColl.findOne(talkerIdObj);
		boolean returnFlag = false;
		if(obj == null) {
			returnFlag = false;
		} else {
			Set<String> emailList = DBUtil.getStringSet(obj,"email");
			if(emailList.contains(email)) {
				returnFlag = true;
			} else {
				returnFlag = false;
			}
		}
		return returnFlag;
	}
	
	/**
	 * Used for checking newsletter subscribe or not.
	 * @param email
	 * @param newsLetter
	 * @return
	 */
	public static boolean isnewsLetterSubscribe(String email){
		NewsLetterBean newsletter = NewsLetterDAO.getNewsLetterInfo(email);
		if(newsletter != null)
			return true;
		else
			return false;
	}
	
	/*Methods for statistics*/
	
	public static long getNewsletterCount(String newsletterType) {
		DBCollection newsLetterColl = getCollection(NEWSLETTER_COLLECTION);
		List<String> nType = new ArrayList<String>();
		nType.add(newsletterType);
		DBObject query = new BasicDBObject("newsletter_type", new BasicDBObject("$in", nType));
		long newsLetterDBObject = newsLetterColl.count(query);
		return newsLetterDBObject;
	}
	
	public static long getNewsletterCount(String newsletterType,Date fromDt, Date toDt) {
		DBCollection newsLetterColl = getCollection("newsletterStats");
		long count=0;
		DateFormat dateFormat = new SimpleDateFormat("yyyy-dd-MM");
		toDt = DateUtils.addDays(toDt, 1);
		do {
			DBObject usernameQuery = BasicDBObjectBuilder.start()
					.add("newsletter_type", newsletterType)
					.add("timestamp", dateFormat.format(fromDt))
					.get();
			DBObject newsLetterDBObject = newsLetterColl.findOne(usernameQuery);
			if(newsLetterDBObject!=null) {
				count = DBUtil.getInt(newsLetterDBObject, "letterCount");
			}
			fromDt = DateUtils.addDays(fromDt, 1);
		}while(toDt.after(fromDt));
		return count;
	}
	
	public static List<String> getNewsletterEmail(String newsletterType) {
		DBCollection newsLetterColl = getCollection(NEWSLETTER_COLLECTION);
		List<String> nType = new ArrayList<String>();
		nType.add(newsletterType);
		DBObject query = new BasicDBObject("newsletter_type", new BasicDBObject("$in", nType));
		DBCursor dbObject = newsLetterColl.find(query);
		List<String> emaiList = new ArrayList<String>();
		for (DBObject talkerDBObject : dbObject) {
			emaiList.add(DBUtil.getString(talkerDBObject, "email"));
		}
		return emaiList;
	}
	
	public static Map<String, String> getTalkerNewsletterCount() {
		Map<String, String> emaiList = new HashMap<String, String>();
		String NEWSLETTER_COLLECTION = "talkerNewsletter";
		DBCollection newsLetterColl = getCollection(NEWSLETTER_COLLECTION);
		DBCursor dbObject = newsLetterColl.find();
		TalkerBean talkerBean;
		Set<String> emailList = null;
		long total = 0;
		for (DBObject talkerDBObject : dbObject) {
			DBRef talkerRef = (DBRef)talkerDBObject.get("talkerId");
			emailList = DBUtil.getStringSet(talkerDBObject,"email");
			talkerBean = TalkerDAO.getById(talkerRef.getId().toString());
			total = total + emailList.size();
			emaiList.put(talkerBean.getName(), Long.toString(emailList.size())); 
		}
		emaiList.put("all", Long.toString(total)); 
		return emaiList;
	}
	
	public static Map<String, String> getTopicNewsletterCount() { 
		Map<String, String> emaiList = new HashMap<String, String>();
		String NEWSLETTER_COLLECTION = "topicNewsletter";
		DBCollection newsLetterColl = getCollection(NEWSLETTER_COLLECTION);
		DBCursor dbObject = newsLetterColl.find();
		TopicBean topicBean;
		Set<String> emailList = null;
		long total = 0;
		for (DBObject talkerDBObject : dbObject) {
			DBRef topicRef = (DBRef)talkerDBObject.get("topicId");
			topicBean = TopicDAO.getById(topicRef.getId().toString());
			emailList = DBUtil.getStringSet(talkerDBObject,"email");
			total = total + emailList.size();
			emaiList.put(topicBean.getTitle(), Long.toString(emailList.size())); 
		}
		emaiList.put("all", Long.toString(total)); 
		return emaiList;
	}
}

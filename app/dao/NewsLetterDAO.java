package dao;

import static util.DBUtil.createRef;
import static util.DBUtil.getCollection;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import util.DBUtil;

import models.NewsLetterBean;
import models.TalkerBean;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

public class NewsLetterDAO {
	
	public static final String NEWSLETTER_COLLECTION = "newsletter";
	
	/**
	 * Method used for save or update newsletter information.
	 * @param newsLetterBean
	 */
	public static void saveOrUpdateNewsletter(NewsLetterBean newsLetterBean,TalkerBean talker){
		DBCollection newsLetterColl = getCollection(NEWSLETTER_COLLECTION);
		boolean isExist = ApplicationDAO.isEmailExists(newsLetterBean.getEmail());
		if(isExist){
			DBObject email = new BasicDBObject("email", newsLetterBean.getEmail());
			String []types = newsLetterBean.getNewsLetterType();
			if(talker == null) {
				DBObject obj=newsLetterColl.findOne(email);
				Collection<String> newLetterTypes = (Collection<String>)obj.get("newsletter_type");
				if(newLetterTypes != null && !newLetterTypes.isEmpty()) {
					for(String type:types) {
						if(!newLetterTypes.contains(type)) {
							newLetterTypes.add(type);
						}
					}
				}
				types = newLetterTypes.toArray(new String[]{});
			}
			DBObject newsLetterDBObject = BasicDBObjectBuilder.start()
				.add("email", newsLetterBean.getEmail())
				.add("newsletter_type", types)
				.get();
			newsLetterColl.update(email,newsLetterDBObject);
		}else{

			DBObject newsLetterDBObject = BasicDBObjectBuilder.start()
				.add("email", newsLetterBean.getEmail())
				.add("newsletter_type", newsLetterBean.getNewsLetterType())
				.get();
			newsLetterColl.save(newsLetterDBObject);
		}
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
		DBObject topicIdObj = new BasicDBObject("talkerId", talkerRef);
		DBObject obj = newsLetterColl.findOne(topicIdObj);
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
			newsLetterColl.update(topicIdObj,newsLetterDBObject);
		}
		return true;
	}
}

package dao;

import static util.DBUtil.getCollection;

import java.util.Collection;

import models.NewsLetterBean;
import models.TalkerBean;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

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
}

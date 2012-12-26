package dao;

import static util.DBUtil.getCollection;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.AdvertisementBean;
import models.TopicBean;

import play.Logger;
import util.DBUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

public class AdvertisementDAO {

	public static void populateStats(String adId, String recordType,  boolean addFlag) {
		DBCollection newsLetterColl = getCollection("advertisement");
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-dd-MM");
		Date dt = Calendar.getInstance().getTime();
		DBObject query = new BasicDBObject("adId", adId).append("recordType",recordType).append("timestamp",dateFormat.format(dt));
		DBObject obj = newsLetterColl.findOne(query);
		if(obj != null) {
			DBObject updateObj;
			if(addFlag)
				updateObj = new BasicDBObject("adCount", 1);
			else
				updateObj = new BasicDBObject("adCount", -1);
			newsLetterColl.update(query,new BasicDBObject("$inc", updateObj));
		} else {
			DBObject newsLetterDBObject = BasicDBObjectBuilder.start()
					.add("adId", adId)
					.add("timestamp", dateFormat.format(dt))
					.add("recordType", recordType)
					.add("adCount", 1)
					.get();
			newsLetterColl.save(newsLetterDBObject);
		}
	}
	
	public static List<AdvertisementBean> getAdvertisementCount(Date fromDt,Date toDt, boolean group) { 
		//Map<String, String> emaiList = new HashMap<String, String>();
		
		List<AdvertisementBean> advertisementBeans = new ArrayList<AdvertisementBean>(); 
		AdvertisementBean bean;
		DateFormat dateFormat = new SimpleDateFormat("yyyy-dd-MM");
		DBCollection newsLetterColl = getCollection("advertisement");
		DBCursor dbObject = null;
		if(fromDt != null) {
			BasicDBObject query = new BasicDBObject();
			query.put("timestamp", BasicDBObjectBuilder.start("$gte", dateFormat.format(fromDt)).add("$lte", dateFormat.format(toDt)).get());
			dbObject = newsLetterColl.find(query);
		} else {
			dbObject = newsLetterColl.find();
		}
		int clickCount=0;
		int impCount=0;
		String type;
		for (DBObject talkerDBObject : dbObject) {
			type = DBUtil.getString(talkerDBObject, "recordType");
			if("click".equals(type)){
				clickCount = clickCount + DBUtil.getInt(talkerDBObject, "adCount");
			} else {
				impCount = impCount +  + DBUtil.getInt(talkerDBObject, "adCount");
			}
			if(!group) {
				bean = new AdvertisementBean(DBUtil.getString(talkerDBObject, "timestamp"),DBUtil.getString(talkerDBObject, "recordType"),DBUtil.getInt(talkerDBObject, "adCount"));
				advertisementBeans.add(bean);
			}
		}
		
		if(group) {
			bean = new AdvertisementBean("","click",clickCount);
			advertisementBeans.add(bean);
			bean = new AdvertisementBean("","impression",impCount);
			advertisementBeans.add(bean);
		} else {
			bean = new AdvertisementBean("","impression",impCount);
			advertisementBeans.add(bean);
			
			bean = new AdvertisementBean("","click",clickCount);
			advertisementBeans.add(bean);
		}
		
		
		return advertisementBeans;
	}
}
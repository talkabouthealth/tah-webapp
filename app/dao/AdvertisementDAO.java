package dao;

import static util.DBUtil.getCollection;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
		Logger.info(dateFormat.format(dt));
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
	
	public static Map<String, String> getAdvertisementCount(Date fromDt,Date toDt) { 
		Map<String, String> emaiList = new HashMap<String, String>();
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
		long clickCount=0;
		long impCount=0;
		String type;
		for (DBObject talkerDBObject : dbObject) {
			type = DBUtil.getString(talkerDBObject, "recordType");
			if("click".equals(type)){
				clickCount = clickCount + DBUtil.getInt(talkerDBObject, "adCount");
			} else {
				impCount = impCount +  + DBUtil.getInt(talkerDBObject, "adCount");
			}
		}
		emaiList.put("click", Long.toString(clickCount)); 
		emaiList.put("impression", Long.toString(impCount));
		emaiList.put("all", Long.toString(impCount)); 
		return emaiList;
	}
}
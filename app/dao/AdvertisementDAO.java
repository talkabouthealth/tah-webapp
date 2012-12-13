package dao;

import static util.DBUtil.getCollection;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import play.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

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
		Logger.info("Done");
	}
}
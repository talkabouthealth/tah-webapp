package dao;

import static util.DBUtil.getCollection;
import static util.DBUtil.getString;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import models.ActivityLogBean;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class ActivityLogDAO {

	public static final String ACTIVITYLOG_COLLECTION = "activitylog";
	
	public static String logRequest(ActivityLogBean logBean) {
		String result = "0";
		try {
			DBCollection activityLogColl = getCollection(ACTIVITYLOG_COLLECTION);
			DBObject activityLogDBObject = BasicDBObjectBuilder.start()
					.add("ipAddress", logBean.getIpAddress())
					.add("pageName", logBean.getPageName())
					.add("pageURL", logBean.getPageURL())
					.add("referer", logBean.getReferer())
					.add("userAgent", logBean.getUserAgent())
					.add("userLanguage", logBean.getUserLanguage())
					.add("userCookie", logBean.getUserCookie())
					.add("cancerSite", logBean.getCancerSite())
					.add("sessionId", logBean.getSessionId())
					.add("userEmail", logBean.getUserEmail())
					.add("userName", logBean.getUserName())
					.add("userLocationCode", logBean.getUserLocationCode())
					.add("userLocationCountry", logBean.getUserLocationCountry())
					.add("userLocationState", logBean.getUserLocationState())
					.add("userLocationCity", logBean.getUserLocationCity())
					.add("userLocationLatitude", logBean.getUserLocationLatitude())
					.add("userLocationLongitude", logBean.getUserLocationLongitude())
					.add("timestamp",  Calendar.getInstance().getTime())
					.get();
			activityLogColl.save(activityLogDBObject);
			result = getString(activityLogDBObject, "_id");;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Deprecated
	public static ArrayList<ActivityLogBean> getLogList() {
		ArrayList<ActivityLogBean> logList = null;
		DBCollection activityLogColl = getCollection(ACTIVITYLOG_COLLECTION);
		DBCursor dbCursor = activityLogColl.find().sort(new BasicDBObject("timestamp", -1));
		ActivityLogBean logBean;
		if(dbCursor != null) {
			logList = new ArrayList<ActivityLogBean>();
			for (DBObject dbObject : dbCursor) {
				logBean = new ActivityLogBean();
				logBean.parseFromDB(dbObject);
				logList.add(logBean);
			}
		}
		return logList;
	}

	public static ArrayList<ActivityLogBean> getLogList(Date date) {

		ArrayList<ActivityLogBean> logList = null;
		DBCollection activityLogColl = getCollection(ACTIVITYLOG_COLLECTION);

		Calendar startDate = Calendar.getInstance();
		startDate.setTime(date);

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_MONTH, 1);

		DBObject query = new BasicDBObject();
		query.put("timestamp", BasicDBObjectBuilder.start("$gte", startDate.getTime()).add("$lte", calendar.getTime()).get());
		DBCursor dbCursor = activityLogColl.find(query).sort(new BasicDBObject("timestamp", -1));
		ActivityLogBean logBean;
		if(dbCursor != null) {
			logList = new ArrayList<ActivityLogBean>();
			for (DBObject dbObject : dbCursor) {
				logBean = new ActivityLogBean();
				logBean.parseFromDB(dbObject);
				logList.add(logBean);
			}
		}
		return logList;
	}

	/* 
	 public static void logOutTime(String logId) {
		DBCollection activityLogColl = getCollection(ACTIVITYLOG_COLLECTION);
		DBObject logIdRef = new BasicDBObject("_id", new ObjectId(logId));
		DBObject logOutObject = BasicDBObjectBuilder.start().add("outTimeStamp", Calendar.getInstance().getTime()).get();
		activityLogColl.update(logIdRef, new BasicDBObject("$set", logOutObject));
	}
	*/
}
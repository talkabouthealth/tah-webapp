package dao;

import static util.DBUtil.getCollection;
import static util.DBUtil.getString;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.time.DateUtils;
import org.bson.types.ObjectId;

import play.Logger;
import util.DBUtil;

import models.ActivityLogBean;
import models.DiseaseBean;

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
	
	public static Map<String, Integer> getLogListGrouped(Date date) {

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

		Map<String, Integer> logList = new HashMap<String, Integer>();

		if(dbCursor != null) {
			for (DBObject dbObject : dbCursor) {
				logBean = new ActivityLogBean();
				logBean.parseFromDB(dbObject);
				Integer intCount = logList.get(logBean.getPageName());
				if(intCount != null ){
					intCount = intCount + 1;
					logList.put(logBean.getPageName(), intCount);
				} else {
					logList.put(logBean.getPageName(), new Integer(1));
				}
			}
		}
		return logList;
	}

	public static void logDisease(Set<DiseaseBean> diseaseList) {
		if(diseaseList != null && !diseaseList.isEmpty()) {
			for (DiseaseBean diseaseBean : diseaseList) {
				DBCollection newsLetterColl = getCollection("diseaseStats");
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-dd-MM");
				Date dt = Calendar.getInstance().getTime();
				DBObject disease = new BasicDBObject("_id", new ObjectId(diseaseBean.getId()));
				DBObject query = new BasicDBObject("disease", disease).append("timestamp",dateFormat.format(dt));
				DBObject obj = newsLetterColl.findOne(query);
				if(obj != null) {
					DBObject updateObj;
						updateObj = new BasicDBObject("viewCount", 1);
					newsLetterColl.update(query,new BasicDBObject("$inc", updateObj));
				} else {
					DBObject newsLetterDBObject = BasicDBObjectBuilder.start()
							.add("disease", disease)
							.add("timestamp", dateFormat.format(dt))
							.add("viewCount", 1)
							.get();
					newsLetterColl.save(newsLetterDBObject);
				}
			}
		}
	}

	public static void logSingleDisease(DiseaseBean diseaseBean) {
		if(diseaseBean != null) {
				DBCollection newsLetterColl = getCollection("diseaseStats");
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-dd-MM");
				Date dt = Calendar.getInstance().getTime();
				DBObject disease = new BasicDBObject("_id", new ObjectId(diseaseBean.getId()));
				DBObject query = new BasicDBObject("disease", disease).append("timestamp",dateFormat.format(dt));
				DBObject obj = newsLetterColl.findOne(query);
				if(obj != null) {
					DBObject updateObj;
						updateObj = new BasicDBObject("viewCount", 1);
					newsLetterColl.update(query,new BasicDBObject("$inc", updateObj));
				} else {
					DBObject newsLetterDBObject = BasicDBObjectBuilder.start()
							.add("disease", disease)
							.add("timestamp", dateFormat.format(dt))
							.add("viewCount", 1)
							.get();
					newsLetterColl.save(newsLetterDBObject);
				}
			}
	}
	public static String getDiseaseLogList(String diseaseId,Date fromDt, Date toDt) {

		DBCollection activityLogColl = getCollection("diseaseStats");
		DateFormat dateFormat = new SimpleDateFormat("yyyy-dd-MM");

		int count=0;
		toDt = DateUtils.addDays(toDt, 1);
		DBObject usernameQuery = null;
		DBObject disease = new BasicDBObject("_id", new ObjectId(diseaseId));
		do {
			usernameQuery = BasicDBObjectBuilder.start()
					.add("disease", disease)
					.add("timestamp", dateFormat.format(fromDt))
					.get();
			System.out.println(usernameQuery);
			DBObject newsLetterDBObject = activityLogColl.findOne(usernameQuery);
			if(newsLetterDBObject!=null) {
				count += DBUtil.getInt(newsLetterDBObject, "viewCount");
			}
			fromDt = DateUtils.addDays(fromDt, 1);
		} while(toDt.after(fromDt));
		return Integer.toString(count);
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
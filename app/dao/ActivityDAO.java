package dao;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import util.DBUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import models.ActivityBean;
import models.TalkerBean;

public class ActivityDAO {
	
	public static final String ACTIVITIES_COLLECTION = "activities";
	
	public static void save(ActivityBean activity) {
		DBCollection activitiesColl = DBUtil.getCollection(ACTIVITIES_COLLECTION);

		DBRef talkerRef = new DBRef(DBUtil.getDB(), 
				TalkerDAO.TALKERS_COLLECTION, activity.getTalker().getId());
		DBObject activityDBObject = BasicDBObjectBuilder.start()
				.add("uid", talkerRef)
				.add("time", new Date())
				.add("text", activity.getText())
				.get();

		activitiesColl.save(activityDBObject);
	}
	
	public static void createActivity(String talkerId, String text) {
		ActivityBean activity = new ActivityBean();
		activity.setTalker(new TalkerBean(talkerId));
		activity.setText(text);
		ActivityDAO.save(activity);
	}
	
	public static List<ActivityBean> load(String talkerId) {
		DBCollection activitiesColl = DBUtil.getCollection(ACTIVITIES_COLLECTION);
		
		DBRef talkerRef = new DBRef(DBUtil.getDB(), TalkerDAO.TALKERS_COLLECTION, talkerId);
		DBObject query = new BasicDBObject("uid", talkerRef);
		List<DBObject> activitiesDBList = 
			activitiesColl.find(query).sort(new BasicDBObject("time", -1)).toArray();
		
		List<ActivityBean> activitiesList = new ArrayList<ActivityBean>();
		for (DBObject activityDBObject : activitiesDBList) {
			ActivityBean activity = new ActivityBean();
			activity.setId(activityDBObject.get("_id").toString());
			activity.setTalker(new TalkerBean(talkerId));
			activity.setTime((Date)activityDBObject.get("time"));
			activity.setText(activityDBObject.get("text").toString());
			
			activitiesList.add(activity);
		}
		return activitiesList;
	}

	public static void main(String[] args) {
//		ActivityBean activity = new ActivityBean();
//		activity.setTalker(new TalkerBean("4c2cb43160adf3055c97d061"));
//		activity.setText("First activity by kangaroo!!");
//		ActivityDAO.save(activity);
		
		System.out.println(ActivityDAO.load("4c2cb43160adf3055c97d061"));
	}
}

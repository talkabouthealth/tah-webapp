package dao;

import java.util.Date;

import models.ActivityBean;
import util.DBUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

public class ConfigDAO {
	
	public static final String CONFIGS_COLLECTION = "configs";
	
	public static void saveConfig (String name, Object value) {
		DBCollection configsColl = DBUtil.getCollection(CONFIGS_COLLECTION);

		DBObject query = new BasicDBObject("name", name);
		DBObject configDBObject = BasicDBObjectBuilder.start()
				.add("name", name)
				.add("value", value)
				.get();

		//update or save
		configsColl.update(query, configDBObject, true, false);
	}
	
	public static boolean getBooleanConfig(String name) {
		DBCollection configsColl = DBUtil.getCollection(CONFIGS_COLLECTION);

		DBObject query = new BasicDBObject("name", name);

		DBObject configDBObject = configsColl.findOne(query);
		if (configDBObject == null) {
			//TODO: if config not found? return false ?
			return false;
		}
		else {
			return (Boolean)configDBObject.get("value");
		}
	}

}
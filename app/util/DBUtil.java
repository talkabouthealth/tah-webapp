package util;

import static util.DBUtil.getCollection;

import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import models.DBModel;
import models.TalkerBean;
import models.ServiceAccountBean.ServiceType;

import org.bson.types.ObjectId;

import play.Logger;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

import play.Play;
import dao.TalkerDAO;


public class DBUtil {
	
	private static Mongo mongo;
	
	private static String HOST_NAME = Play.configuration.getProperty("db.hostname");
	//private static String HOST_NAME = null;
	
	static {
		try {
			if(HOST_NAME==null) {
				System.out.println("Connecting to DB on localhost...");
				mongo = new Mongo("localhost", 27017);
				System.out.println("Connected, proceeding!");
			}
			else {
				System.out.println("Connecting to DB on " + HOST_NAME + "...");
				mongo = new Mongo(HOST_NAME, 27017);
				System.out.println("Connected, proceeding!");
			}
		} catch (UnknownHostException e) {
			Logger.error(e, "DB connection");
		} catch (MongoException e) {
			Logger.error(e, "DB connection");
		}
	}
	
	public static DB getDB() {
		//boolean auth = db.authenticate(myUserName, myPassword);
		return mongo.getDB("tahdb");
	}

	public static DBCollection getCollection(String collectionName) {
		return getDB().getCollection(collectionName);
	}
	
	public static boolean isCollectionEmpty(String collectionName) {
		DBCollection coll = getCollection(collectionName);
		return coll.count() == 0;
	}
	
	/* -------- Different utility methods to use in DAO layer ------------ */
	
	/**
	 * Create DB reference for given id and collection
	 */
	public static DBRef createRef(String collectionName, String objectId) {
		DBRef ref = new DBRef(getDB(), collectionName, new ObjectId(objectId));
		return ref;
	}
	
	//Get String, int or boolean value from given DBObject
	public static String getString(DBObject dbObject, String name) {
		Object value = dbObject.get(name);
		if (value == null) {
			return null;
		}
		return value.toString();
	}
	public static int getInt(DBObject dbObject, String name) {
		Integer value = (Integer)dbObject.get(name);
		if (value == null) {
			return 0;
		}
		return value.intValue();
	}
	public static boolean getBoolean(DBObject dbObject, String name) {
		Boolean value = (Boolean)dbObject.get(name);
		if (value == null) {
			return false;
		}
		return value.booleanValue();
	}
	
	//Useful methods for loading string list/set/map
	public static List<String> getStringList(DBObject dbObject, String name) {
		Set<String> stringSet = getSet(dbObject, name);
		return new ArrayList<String>(stringSet);
	}
	public static Set<String> getStringSet(DBObject dbObject, String name) {
		return getSet(dbObject, name);
	}
	
	public static <T> Set<T> getSet(DBObject dbObject, String name) {
		Object value = dbObject.get(name);
		if (value == null) {
			return new HashSet<T>();
		}
		Set<T> set = new LinkedHashSet<T>((Collection<T>)value);
		return set;
	}
	
	/* ----------------- Use for DBModel instances ---------------- */
	
	/**
	 * Parse set of objects from DB
	 * 
	 * @param <T> Any class that implements DBModel interface
	 * @param clazz Class of objects to load
	 * @param dbObject Source DBObject
	 * @param name Name of collection in the DB
	 */
	public static <T extends DBModel> Set<T> parseSet(Class<T> clazz, DBObject dbObject, String name) {
		Collection<DBObject> value = (Collection<DBObject>)dbObject.get(name);
		if (value == null) {
			return new HashSet<T>();
		}
		
		Set<T> valueSet = new HashSet<T>();
		for (DBObject valueDBObject : value) {
			T t = null;
			try {
				t = clazz.newInstance();
			} catch (InstantiationException e) {
				Logger.error(e, "");
			} catch (IllegalAccessException e) {
				Logger.error(e, "");
			}
			if (t != null) {
				t.parseDBObject(valueDBObject);
				valueSet.add(t);
			}
		}
		return valueSet;
	}
	
	/**
	 * Returns set of objects as set of DBObjects to save in DB
	 * @param <T>
	 * @param valueSet
	 * @return
	 */
	public static <T extends DBModel> Set<DBObject> setToDB(Set<T> valueSet) {
		Set<DBObject> dbSet = new HashSet<DBObject>();
		if (valueSet != null) {
			for (T t : valueSet) {
				dbSet.add(t.toDBObject());
			}
		}
		return dbSet;
	}
	
	
//	public static void main(String[] args) {
//		Set<String> colls = getDB().getCollectionNames();
//
//		for (String s : colls) {
//		    System.out.println(s);
//		}
//	}
}

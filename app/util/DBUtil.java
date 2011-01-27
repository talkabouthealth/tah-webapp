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

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

import dao.TalkerDAO;


public class DBUtil {
	
	private static Mongo mongo;
	
	static {
		try {
			mongo = new Mongo("localhost", 27017);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (MongoException e) {
			e.printStackTrace();
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
	
	
	//-------- Different utility methods to use in DAO ------------
	public static DBRef createRef(String collectionName, String objectId) {
		DBRef ref = new DBRef(getDB(), collectionName, new ObjectId(objectId));
		return ref;
	}
	
	public static TalkerBean parseTalker(DBObject dbObject, String name) {
		DBRef talkerRef = (DBRef)dbObject.get(name);
		if (talkerRef == null) {
			return null;
		}
		
		return parseTalker(talkerRef);
	}
	
	public static TalkerBean parseTalker(DBRef talkerRef) {
		DBObject talkerDBObject = talkerRef.fetch();
		TalkerBean talker = new TalkerBean();
		talker.parseBasicFromDB(talkerDBObject);
		return talker;
	}
	
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
	
	//generic?
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
	
	public static Map<String, String> getStringMap(DBObject dbObject, String name) {
		DBObject value = (DBObject)dbObject.get(name);
		if (value == null) {
			return Collections.emptyMap();
		}
		Map<String, String> map = value.toMap();
		return map;
	}
	
	//--------- Using DBModel -----------
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
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			if (t != null) {
				t.parseDBObject(valueDBObject);
				valueSet.add(t);
			}
		}
		return valueSet;
	}
	
	public static <T extends DBModel> Set<DBObject> setToDB(Set<T> valueSet) {
		Set<DBObject> dbSet = new HashSet<DBObject>();
		if (valueSet != null) {
			for (T t : valueSet) {
				dbSet.add(t.toDBObject());
			}
		}
		return dbSet;
	}
	
	
	public static void main(String[] args) {
		Set<String> colls = getDB().getCollectionNames();

		for (String s : colls) {
		    System.out.println(s);
		}
		
		//FIXME: add indexes later? for now we have not many users
//		activities
//		configs
//		convocomments
//		convos
//		diseases
//		healthitems
//		logins
//		names
//		notifications
//		profilecomments
//		system.indexes
//		talkers
//		topics
	}
}

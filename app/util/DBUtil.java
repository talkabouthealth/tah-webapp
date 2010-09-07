package util;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

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
		//TODO: load name from app configuration? handle non-Play runs!
		return mongo.getDB("tahdb");
	}

	public static DBCollection getCollection(String collectionName) {
		return getDB().getCollection(collectionName);
	}
	
	//-------- Different utility methods to use in DAO ------------
	public static DBRef createRef(String collectionName, String objectId) {
		DBRef ref = new DBRef(getDB(), collectionName, new ObjectId(objectId));
		return ref;
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

	public static List<String> getStringList(DBObject dbObject, String name) {
		Object value = dbObject.get(name);
		if (value == null) {
			return Collections.emptyList();
		}
		List<String> list = new ArrayList<String>((Collection<String>)value);
		return list;
	}
	
	public static Set<String> getStringSet(DBObject dbObject, String name) {
		Object value = dbObject.get(name);
		if (value == null) {
			return Collections.emptySet();
		}
		Set<String> set = new LinkedHashSet<String>((Collection<String>)value);
		return set;
	}
	
	
	
	
	
	
	
	public static void main(String[] args) {
		Set<String> colls = getDB().getCollectionNames();

		for (String s : colls) {
		    System.out.println(s);
		}
	}
}

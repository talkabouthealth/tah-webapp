package util;

import java.net.UnknownHostException;
import java.util.Set;

import org.bson.types.ObjectId;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoException;


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
	
	public static void main(String[] args) {
		Set<String> colls = getDB().getCollectionNames();

		for (String s : colls) {
		    System.out.println(s);
		}
	}
}

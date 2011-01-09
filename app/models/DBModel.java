package models;

import com.mongodb.DBObject;

/**
 * Allows common DB save/load of simple objects.
 *
 */
public interface DBModel {

	public DBObject toDBObject();
	
	public void parseDBObject(DBObject dbObject);
	
}

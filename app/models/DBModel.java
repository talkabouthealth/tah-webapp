package models;

import com.mongodb.DBObject;

public interface DBModel {

	public DBObject toDBObject();
	
	public void parseDBObject(DBObject dbObject);
	
}

package models.actions;

import java.util.Date;

import com.mongodb.DBObject;

public interface Action {
	
	public DBObject toDBObject();
	
	public Date getTime();

}

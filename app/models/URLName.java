package models;

import static util.DBUtil.createRef;
import static util.DBUtil.getBoolean;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import dao.TalkerDAO;

/**
 * Class for conversations and topics old URLs/names  
 *
 */
public class URLName implements DBModel {
	private String title;
	private String url;
	
	public URLName() {}
	
	public URLName(String title, String url) {
		this.title = title;
		this.url = url;
	}
	
	@Override
	public DBObject toDBObject() {
		DBObject dbObject = BasicDBObjectBuilder.start()
			.add("title", getTitle())
			.add("url", getUrl())
			.get();
		return dbObject;
	}
	
	@Override
	public void parseDBObject(DBObject dbObject) {
		setTitle((String)dbObject.get("title"));
		setUrl((String)dbObject.get("url"));
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof URLName)) {
			return false;
		}
		
		URLName other = (URLName)obj;
		return title.equals(other.title);
	}
	
	@Override
	public int hashCode() {
		if (title == null) {
			return 47;
		}
		return title.hashCode();
	}
	
	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	
	public String getUrl() { return url; }
	public void setUrl(String url) { this.url = url; }
}

package models;

import static util.DBUtil.createRef;
import static util.DBUtil.getBoolean;
import static util.DBUtil.getString;

import java.util.Date;

import util.DBUtil;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.DBRef;



/**
 * Used for storing not-primary emails.
 *
 */
public class NewsLetterBean implements DBModel {
	
	public NewsLetterBean() {
	}

	private String email;
	
	public NewsLetterBean(String value) {
		this.email = value;
	}
	
	@Override
	public DBObject toDBObject() {
		DBObject emailDBObject = BasicDBObjectBuilder.start()
			.add("email", getEmail()).get();
		return emailDBObject;
	}
	
	@Override
	public void parseDBObject(DBObject dbObject) {
		String emailValue = (String)dbObject.get("email");
		setEmail(emailValue);
	}
	
	@Override
	public String toString() {
		return email;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof NewsLetterBean)) {
			return false;
		}
		
		NewsLetterBean other = (NewsLetterBean)obj;
		return email.equals(other.email);
	}
	
	@Override
	public int hashCode() {
		if (email == null) {
			return 47;
		}
		return email.hashCode();
	}

	public String getEmail() { return email; }

	public void setEmail(String email) { this.email = email; }

	public void parseBasicFromDB(DBObject newsLetterDBObject) {
		if (newsLetterDBObject == null) {
			return;
		}
		setEmail(getString(newsLetterDBObject, "email"));
	}
}
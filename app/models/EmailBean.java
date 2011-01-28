package models;

import static util.DBUtil.createRef;
import static util.DBUtil.getBoolean;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import dao.TalkerDAO;

/**
 * Used for storing not-primary emails.
 *
 */
public class EmailBean implements DBModel {
	
	private String value;
	//code used for email verification
	//(or 'null' if email is already verified)
	private String verifyCode;
	
	public EmailBean(String value, String verifyCode) {
		this.value = value;
		this.verifyCode = verifyCode;
	}
	
	@Override
	public DBObject toDBObject() {
		DBObject emailDBObject = BasicDBObjectBuilder.start()
			.add("value", getValue())
			.add("verify_code", getVerifyCode())
			.get();
		return emailDBObject;
	}
	
	@Override
	public void parseDBObject(DBObject dbObject) {
		String emailValue = (String)dbObject.get("value");
		String verifyCode = (String)dbObject.get("verify_code");
		
		setValue(emailValue);
		setVerifyCode(verifyCode);
	}
	
	@Override
	public String toString() {
		return value;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof EmailBean)) {
			return false;
		}
		
		EmailBean other = (EmailBean)obj;
		return value.equals(other.value);
	}
	
	@Override
	public int hashCode() {
		if (value == null) {
			return 47;
		}
		return value.hashCode();
	}
	
	
	public String getValue() { return value; }
	public void setValue(String value) { this.value = value; }
	
	public String getVerifyCode() { return verifyCode; }
	public void setVerifyCode(String verifyCode) { this.verifyCode = verifyCode; }
}

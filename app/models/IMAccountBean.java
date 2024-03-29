package models;

import java.util.HashMap;
import java.util.Map;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

public class IMAccountBean implements DBModel {
	
	public static final Map<String, String> IM_SERVICES_MAP = new HashMap<String, String>();
	static {
		IM_SERVICES_MAP.put("YahooIM", "Yahoo IM / Yahoo Messenger");
		IM_SERVICES_MAP.put("GoogleTalk", "GoogleTalk / Gmail Chat / Gchat");
		IM_SERVICES_MAP.put("WindowLive", "Windows Live Messenger");
	}
	
	private String userName;
	private String service;
	
	public IMAccountBean(){
		
	}
	public IMAccountBean(String userName, String service) {
		this.userName = userName;
		this.service = service;
	}
	
	@Override
	public DBObject toDBObject() {
		DBObject imAccountDBObject = BasicDBObjectBuilder.start()
			.add("uname", getUserName())
			.add("service", getService())
			.get();
		return imAccountDBObject;
	}
	
	@Override
	public void parseDBObject(DBObject dbObject) {
		String userName = (String)dbObject.get("uname");
		String service = (String)dbObject.get("service");
		
		setUserName(userName);
		setService(service);
	}
	
	@Override
	public String toString() {
		return userName+" ("+service+")";
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof IMAccountBean)) {
			return false;
		}
		
		IMAccountBean other = (IMAccountBean)obj;
		return userName.equals(other.userName) && service.equals(other.service);
	}
	
	@Override
	public int hashCode() {
		if (userName == null) {
			return 47;
		}
		return userName.hashCode();
	}

	
	public String getUserName() { return userName; }
	public void setUserName(String userName) { this.userName = userName; }

	public String getService() { return service; }
	public void setService(String service) { this.service = service; }
}

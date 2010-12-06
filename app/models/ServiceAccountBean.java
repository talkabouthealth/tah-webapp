package models;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import models.ServiceAccountBean.ServiceType;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

//Facebook or Twitter info
public class ServiceAccountBean implements DBModel {
	
	private static final Map<String, String> TWITTER_SETTINGS_NAMES;
	private static final Map<String, String> FACEBOOK_SETTINGS_NAMES; 
	
	static {
		TWITTER_SETTINGS_NAMES = new LinkedHashMap<String, String>();
		TWITTER_SETTINGS_NAMES.put("notify", "Notify me of relevant questions and live chats via Twitter direct message.");
		TWITTER_SETTINGS_NAMES.put("shareToThoughts", "Share my Twitter posts in my TalkAboutHealth Thoughts Feed.");
		TWITTER_SETTINGS_NAMES.put("shareFromThoughts", "Share my TalkAboutHealth Thoughts Feed posts in my Twitter feed.");
		TWITTER_SETTINGS_NAMES.put("postOnAnswer", "Post on Twitter when I answer a question.");
		TWITTER_SETTINGS_NAMES.put("postOnCovo", "Post on Twitter when I ask a question or start a live chat.");
		TWITTER_SETTINGS_NAMES.put("follow", "Follow TalkAboutHealth on Twitter.");
		
		FACEBOOK_SETTINGS_NAMES = new LinkedHashMap<String, String>();
		FACEBOOK_SETTINGS_NAMES.put("notify", "Notify me of relevant questions and live chats via Facebook chat.");
		FACEBOOK_SETTINGS_NAMES.put("shareToThoughts", "Share my Facebook posts in my TalkAboutHealth Thoughts Feed.");
		FACEBOOK_SETTINGS_NAMES.put("shareFromThoughts", "Share my TalkAboutHealth Thoughts Feed posts in my Facebook feed.");
		FACEBOOK_SETTINGS_NAMES.put("postOnAnswer", "Post on Facebook when I answer a question.");
		FACEBOOK_SETTINGS_NAMES.put("postOnCovo", "Post on Facebook when I ask a question or start a live chat.");
	}
	
	public enum ServiceType { TWITTER, FACEBOOK }
	
	private String id;
	private String userName;
	private ServiceType type;
	
	private String token;
	private String tokenSecret;
	
	private Map<String, String> settings;
	
	
	public ServiceAccountBean(String id, String userName, ServiceType type) {
		this.id = id;
		this.userName = userName;
		this.type = type;
	}

	@Override
	public DBObject toDBObject() {
		DBObject accountDBObject = BasicDBObjectBuilder.start()
			.add("id", getId())
			.add("uname", getUserName())
			.add("type", getType().toString())
			
			.add("token", getToken())
			.add("token_secret", getTokenSecret())
			
			.add("settings", getSettings())
			.get();
		
		return accountDBObject;
	}
	
	@Override
	public void parseDBObject(DBObject dbObject) {
		setId((String)dbObject.get("id"));
		setUserName((String)dbObject.get("uname"));
		setType(parseServiceType((String)dbObject.get("type")));

		setToken((String)dbObject.get("token"));
		setTokenSecret((String)dbObject.get("token_secret"));
		
		setSettings((Map)dbObject.get("settings"));
	}
	
	@Override
	public String toString() {
		return userName+" ("+type+")";
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ServiceAccountBean)) {
			return false;
		}
		
		ServiceAccountBean other = (ServiceAccountBean)obj;
		return userName.equals(other.userName) && type == other.type;
	}
	
	@Override
	public int hashCode() {
		if (userName == null) {
			return 47;
		}
		return userName.hashCode();
	}
	
	
	public static Map<String, String> settingsNamesByType(ServiceType serviceType) {
		if (serviceType == ServiceType.TWITTER) {
			return TWITTER_SETTINGS_NAMES;
		}
		else if (serviceType == ServiceType.FACEBOOK) {
			return FACEBOOK_SETTINGS_NAMES;
		}
		else {
			return null;
		}
	}
	
	public static ServiceType parseServiceType(String typeStr) {
		ServiceType type = null;
		try {
			type = ServiceType.valueOf(typeStr);
		}
		catch (Exception e) { 
			//nothing to do - default is null
		}
		return type;
	}
	
	public void parseSettingsFromParams(Map<String, String> paramsMap) {
		Map<String, String> settings = new HashMap<String, String>();
		for (Entry<String, String> param : paramsMap.entrySet()) {
			String typeStr = type.toString();
			if (param.getKey().startsWith(typeStr+"_")) {
				settings.put(param.getKey().substring(typeStr.length()+1), param.getValue());
			}
		}
		setSettings(settings);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public ServiceType getType() {
		return type;
	}

	public void setType(ServiceType type) {
		this.type = type;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getTokenSecret() {
		return tokenSecret;
	}

	public void setTokenSecret(String tokenSecret) {
		this.tokenSecret = tokenSecret;
	}

	public Map<String, String> getSettings() {
		return settings;
	}

	public void setSettings(Map<String, String> settings) {
		this.settings = settings;
	}

}

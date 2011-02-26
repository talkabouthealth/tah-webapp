package models;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;


public class PrivacySetting implements DBModel {
	
	public enum PrivacyType {
		USERNAME("Username"),
		PROFILE_IMAGE("Profile Image"),
		PROFILE_INFO("Personal Info"),
		//only for Prof users
		PROFESSIONAL_INFO("Professional Info"),
		//only for non-Prof users
		HEALTH_INFO("Health Info"),
		ACTIVITY_STREAM("Activity Stream"),
		THOUGHTS("Thoughts"),
		ANSWERS("Answers"),
		FOLLOWERS("Followers"),
		FOLLOWING("Following"),
		THANKYOUS("Thank you's"),
		QUESTIONS_STARTED("Questions Asked"),
		CHATS_JOINED("Chats Joined"),
		QUESTIONS_FOLLOWING("Questions Following"),
		TOPICS_FOLLOWING("Topics Following");
		
		private final String description;
		
		private PrivacyType(String description) {
			this.description = description;
		}

		public String getDescription() { return description; }
	}

	public enum PrivacyValue {
		PRIVATE("No one (private)"),
		COMMUNITY("Community (members)"),
		PUBLIC("Public (everyone)");
		
		private final String description;
		
		private PrivacyValue(String description) {
			this.description = description;
		}

		public String getDescription() { return description; }
	}
	
	private PrivacyType type;
	private PrivacyValue value;
	
	public PrivacySetting(PrivacyType type, PrivacyValue value) {
		this.type = type;
		this.value = value;
	}

	@Override
	public DBObject toDBObject() {
		DBObject privacyDBObject = BasicDBObjectBuilder.start()
			.add("type", type.toString())
			.add("value", value.toString())
			.get();
		return privacyDBObject;
	}
	
	@Override
	public void parseDBObject(DBObject dbObject) {
		String typeStr = (String)dbObject.get("type");
		setType(PrivacyType.valueOf(typeStr));
		
		String valueStr = (String)dbObject.get("value");
		setValue(PrivacyValue.valueOf(valueStr));
	}
	
	@Override
	public String toString() {
		return type.toString()+ " : " + value.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof PrivacySetting)) {
			return false;
		}
		
		PrivacySetting other = (PrivacySetting)obj;
		return type == other.type;
	}
	
	@Override
	public int hashCode() {
		if (type == null) {
			return 47;
		}
		return type.hashCode();
	}

	
	public PrivacyType getType() { return type; }
	public void setType(PrivacyType type) { this.type = type; }

	public PrivacyValue getValue() { return value; }
	public void setValue(PrivacyValue value) { this.value = value; }
}

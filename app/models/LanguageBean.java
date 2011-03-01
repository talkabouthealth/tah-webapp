package models;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

public class LanguageBean implements DBModel {
	
	private String name;
	private String proficiency;
	
	public LanguageBean(){}
	public LanguageBean(String name, String proficiency) {
		this.name = name;
		this.proficiency = proficiency;
	}

	@Override
	public DBObject toDBObject() {
		DBObject langDBObject = BasicDBObjectBuilder.start()
			.add("name", getName())
			.add("proficiency", getProficiency())
			.get();
		return langDBObject;
	}
	
	@Override
	public void parseDBObject(DBObject dbObject) {
		String langName = (String)dbObject.get("name");
		String langProficiency = (String)dbObject.get("proficiency");
		
		setName(langName);
		setProficiency(langProficiency);
	}
	
	@Override
	public String toString() {
		String languageString = getName();
		if (languageString == null || languageString.length() == 0) {
			return "";
		}
		if (getProficiency() != null && getProficiency().length() > 0) {
			languageString = languageString+" ("+getProficiency()+")";
		}
		return languageString;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof LanguageBean)) {
			return false;
		}
		
		LanguageBean other = (LanguageBean)obj;
		return name.equals(other.name);
	}
	
	@Override
	public int hashCode() {
		if (name == null) {
			return 47;
		}
		return name.hashCode();
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getProficiency() {
		return proficiency;
	}
	public void setProficiency(String proficiency) {
		this.proficiency = proficiency;
	}
}
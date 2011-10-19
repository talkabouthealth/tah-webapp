package models;

import static util.DBUtil.getString;

import java.util.List;
import java.util.Set;

import com.mongodb.DBObject;


public class DiseaseBean {
	
	private String id;
	private String name;
	private List<DiseaseQuestion> questions;
	
	/**
	 * Represent question related to some disease.
	 *
	 */
	public static class DiseaseQuestion {
		public enum DiseaseQuestionType {
			TEXT,
			SELECT,
			MULTI_SELECT
		}
		
		private DiseaseQuestionType type;
		//used internally
		private String name;
		//used for displaying info
		private String displayName;
		//used on the HealthInfo form
		private String text;
		
		//possible choice if question is select/multiselect
		private Set<String> choices;
		
		public DiseaseQuestionType getType() { return type; }
		public void setType(DiseaseQuestionType type) { this.type = type; }
		
		public String getName() { return name; }
		public void setName(String name) { this.name = name; }
		
		public String getDisplayName() { return displayName; }
		public void setDisplayName(String displayName) { this.displayName = displayName; }
		
		public String getText() { return text; }
		public void setText(String text) { this.text = text; }
		
		public Set<String> getChoices() { return choices; }
		public void setChoices(Set<String> choices) { this.choices = choices; }
	}
	
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	
	public List<DiseaseQuestion> getQuestions() { return questions; }
	public void setQuestions(List<DiseaseQuestion> questions) { this.questions = questions; }
	
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public void parseBasicFromDB(DBObject diseaseDBObject) {
		if (diseaseDBObject == null) {
			return;
		}
		setId(getString(diseaseDBObject, "_id"));
		setName((String)diseaseDBObject.get("name"));
	}
}

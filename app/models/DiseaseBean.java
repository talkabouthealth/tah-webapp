package models;

import java.util.List;
import java.util.Set;


public class DiseaseBean {
	
	private String name;
	private List<DiseaseQuestion> questions;
	
	public static class DiseaseQuestion {
		public enum DiseaseQuestionType {
			TEXT,
			SELECT,
			MULTI_SELECT
		}
		
		private DiseaseQuestionType type;
		private String name;
		private String displayName;
		private String text;
		private Set<String> choices;
		
		public DiseaseQuestionType getType() {
			return type;
		}
		public void setType(DiseaseQuestionType type) {
			this.type = type;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getDisplayName() {
			return displayName;
		}
		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}
		public String getText() {
			return text;
		}
		public void setText(String text) {
			this.text = text;
		}
		public Set<String> getChoices() {
			return choices;
		}
		public void setChoices(Set<String> choices) {
			this.choices = choices;
		}
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<DiseaseQuestion> getQuestions() {
		return questions;
	}
	public void setQuestions(List<DiseaseQuestion> questions) {
		this.questions = questions;
	}

}

package models;

import java.util.Date;
import java.util.Set;

public class TalkerDiseaseBean {
	
	private String uid;
	private String stage;
	private String type;
	private boolean recurrent;
	
	private Date symptomDate;
	private Date diagnoseDate;
	
	private Set<String> healthItems;
	
	public boolean isRecurrent() {
		return recurrent;
	}
	public void setRecurrent(boolean recurrent) {
		this.recurrent = recurrent;
	}
	public Date getSymptomDate() {
		return symptomDate;
	}
	public void setSymptomDate(Date symptomDate) {
		this.symptomDate = symptomDate;
	}
	public Date getDiagnoseDate() {
		return diagnoseDate;
	}
	public void setDiagnoseDate(Date diagnoseDate) {
		this.diagnoseDate = diagnoseDate;
	}
	public String getUid() {
		return uid;
	}
	public void setUid(String uid) {
		this.uid = uid;
	}
	public Set<String> getHealthItems() {
		return healthItems;
	}
	public void setHealthItems(Set<String> healthItems) {
		this.healthItems = healthItems;
	}
	public String getStage() {
		return stage;
	}
	public void setStage(String stage) {
		this.stage = stage;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
}


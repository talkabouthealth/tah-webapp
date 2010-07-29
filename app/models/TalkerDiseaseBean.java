package models;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TalkerDiseaseBean {
	
	private String uid;
	private String name;
	private String stage;
	private String type;
	private boolean recurrent;
	
	private Date symptomDate;
	private int symptomMonth;
	private int symptomYear;
	
	private Date diagnoseDate;
	private int diagnoseMonth;
	private int diagnoseYear;
	
	private Set<String> healthItems;
	//Map for "Other" fields
	private Map<String, List<String>> otherHealthItems;
	
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
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
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
	public int getSymptomMonth() {
		return symptomMonth;
	}
	public void setSymptomMonth(int symptomMonth) {
		this.symptomMonth = symptomMonth;
	}
	public int getSymptomYear() {
		return symptomYear;
	}
	public void setSymptomYear(int symptomYear) {
		this.symptomYear = symptomYear;
	}
	public int getDiagnoseMonth() {
		return diagnoseMonth;
	}
	public void setDiagnoseMonth(int diagnoseMonth) {
		this.diagnoseMonth = diagnoseMonth;
	}
	public int getDiagnoseYear() {
		return diagnoseYear;
	}
	public void setDiagnoseYear(int diagnoseYear) {
		this.diagnoseYear = diagnoseYear;
	}
	public Map<String, List<String>> getOtherHealthItems() {
		return otherHealthItems;
	}
	public void setOtherHealthItems(Map<String, List<String>> otherHealthItems) {
		this.otherHealthItems = otherHealthItems;
	}
}


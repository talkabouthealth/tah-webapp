package models;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.DiseaseBean.DiseaseQuestion;

/**
 * Stores talker's Health Info related to particular disease
 *
 */
public class TalkerDiseaseBean {
	
	private String uid;
	//disease name
	private String name;
	//done as string to handle "user-not-entered-info" case 
	//(i.e. we don't know if user selected 'false' or selected nothing)
	private String recurrent;
	
	private String healthBio;
	
	private String diseaseName;
	private boolean isDefault;
	
	private Date symptomDate;
	private int symptomMonth;
	private int symptomYear;
	private Date diagnoseDate;
	private int diagnoseMonth;
	private int diagnoseYear;
	
	//information for particular disease (e.g. for Breast Cancer - stage, cancer hormones, etc.)
	private Map<String, List<String>> healthInfo;
	
	private Set<String> healthItems;
	//Map for "Other" fields - not selected, but entered by user
	private Map<String, List<String>> otherHealthItems;
	
	//Map of health items
	private Map<String, HealthItemBean> healthItemsMap = null;
	
	//Map of disease questions
	private DiseaseBean disease = null;
	
	public void setHealthItemsMap(Map<String, HealthItemBean> map) { this.healthItemsMap=map; }
	
	public void setDiseaseQuestions(DiseaseBean questions) { this.disease = questions; }
	
	public String getRecurrent() { return recurrent; }
	public void setRecurrent(String recurrent) { this.recurrent = recurrent; }
	
	public Date getSymptomDate() { return symptomDate; }
	public void setSymptomDate(Date symptomDate) { this.symptomDate = symptomDate; }
	
	public Date getDiagnoseDate() { return diagnoseDate; }
	public void setDiagnoseDate(Date diagnoseDate) { this.diagnoseDate = diagnoseDate; }
	
	public String getUid() { return uid; }
	public void setUid(String uid) { this.uid = uid; }
	
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	
	public Set<String> getHealthItems() { return healthItems; }
	public void setHealthItems(Set<String> healthItems) { this.healthItems = healthItems; }
	
	public int getSymptomMonth() { return symptomMonth; }
	public void setSymptomMonth(int symptomMonth) { this.symptomMonth = symptomMonth; }
	
	public int getSymptomYear() { return symptomYear; }
	public void setSymptomYear(int symptomYear) { this.symptomYear = symptomYear; }
	
	public int getDiagnoseMonth() { return diagnoseMonth; }
	public void setDiagnoseMonth(int diagnoseMonth) { this.diagnoseMonth = diagnoseMonth; }
	
	public int getDiagnoseYear() { return diagnoseYear; }
	public void setDiagnoseYear(int diagnoseYear) { this.diagnoseYear = diagnoseYear; }
	
	public Map<String, List<String>> getOtherHealthItems() { return otherHealthItems; }
	public void setOtherHealthItems(Map<String, List<String>> otherHealthItems) { this.otherHealthItems = otherHealthItems; }
	
	public Map<String, List<String>> getHealthInfo() { return healthInfo; }
	public void setHealthInfo(Map<String, List<String>> healthInfo) { this.healthInfo = healthInfo; }
	
	public String getHealthBio() { return healthBio; }
	public void setHealthBio(String healthBio) { this.healthBio = healthBio; }
	
	public String getDiseaseName() { return diseaseName==null?"":diseaseName; }
	public void setDiseaseName(String diseaseName) { this.diseaseName = diseaseName; }

	public boolean isDefault() { return isDefault; }
	public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

	private boolean isNestedNotEmpty(HealthItemBean healthItem, TalkerDiseaseBean talkerDisease) {
		boolean flag=false;
		if(healthItem == null) return false;
		for(HealthItemBean item : healthItem.getChildren()) {
			if(item.getChildren().size()>0) {
				flag = isNestedNotEmpty(item,talkerDisease);
			}
			else {
				if(talkerDisease.getHealthItems().contains(item.getId())) flag=true;
			}
			if(flag) return true;
		}
		return flag;
	}
	

	public boolean isEmpty(String submap) {
		
		// if there is no health-items-map -- probably nothing, return Empty
		if(healthItemsMap==null) return true;
		
		// check if other-health-items contain any entries for here, return NotEmpty if so
		if(otherHealthItems!=null && otherHealthItems.get(submap)!=null) return false;
		
		// otherwise perform full nested lookup for anything in here
		return !isNestedNotEmpty(healthItemsMap.get(submap),this);
	}
	
	public String combinedToCommaString(String submap) {
		String result = "";

		// get regular items
		if(this.healthInfo != null && this.healthInfo.get(submap) != null) for(String val: this.healthInfo.get(submap)) result+=val + ", "; 			
		
		// get other items
		if(this.otherHealthItems != null && this.otherHealthItems.get(submap) != null) for(String val: this.otherHealthItems.get(submap)) result += val + ", ";			
		
		if(result.length()>2) return result.substring(0,result.length()-2); else return result;
	}
	
	public boolean isEmptyHealthInfo() {
		// check for any disease questions
		for(DiseaseQuestion question: disease.getQuestions()) {
			if(this.getHealthInfo().get(question.getName()) != null) return false;
		}
		
		if(this.symptomDate != null) return false;
		
		if(this.diagnoseDate != null) return false;
				
		if(this.recurrent != null && this.recurrent.length()>0) return false;
		
		return isEmpty("symptoms") && isEmpty("test") && isEmpty("procedures") && isEmpty("treatments")&& isEmpty("sideeffects");
	}
}


package models;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	
	public void setHealthItemsMap(Map<String, HealthItemBean> map) { this.healthItemsMap=map; }
	
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
		//TODO REVERSE
		if(true) return false;
		
		if(healthItemsMap==null) return true;
		
		return !isNestedNotEmpty(healthItemsMap.get(submap),this);
	}
	
	public boolean isEmptyHealthInfo() {
		return isEmpty("symptoms") && isEmpty("test") && isEmpty("procedures") && isEmpty("treatments")&& isEmpty("sideeffects");
	}
}


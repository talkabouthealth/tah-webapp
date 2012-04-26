package dao;

import static util.DBUtil.getCollection;
import static util.DBUtil.getStringList;
import static util.DBUtil.getStringSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.TalkerBean;
import models.TalkerDiseaseBean;

import org.bson.types.ObjectId;

import play.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class TalkerDiseaseDAO {
	
	/**
	 * Method added for save multiple diseases health information of talker.
	 * @param talkerDiseaseBean
	 * @param talkerId
	 */
	public static void saveTalkerDisease(TalkerDiseaseBean talkerDiseaseBean, String talkerId) {
	    DBCollection talkersColl = getCollection(TalkerDAO.TALKERS_COLLECTION);
	    
	    List<TalkerDiseaseBean> talkerDiseaseBeanList = getListByTalkerId(talkerId);
		List<TalkerDiseaseBean> talkerDiseaseList = new ArrayList<TalkerDiseaseBean>();
		if(talkerDiseaseBeanList != null){
			for(TalkerDiseaseBean talkerDis : talkerDiseaseBeanList){
				talkerDis.setDefault(false);
				if(!talkerDis.getDiseaseName().equalsIgnoreCase(talkerDiseaseBean.getDiseaseName()))
					talkerDiseaseList.add(talkerDis);
			}
		}
		talkerDiseaseList.add(talkerDiseaseBean);

		List<DBObject> diseaseListItems = new ArrayList<DBObject>();
		
		for(TalkerDiseaseBean talkerDisease : talkerDiseaseList){
		
		    DBObject healthInfoDBObject = new BasicDBObject(talkerDisease.getHealthInfo());
		    
		    //prepare other items
		    List<DBObject> otherHealthItems = new ArrayList<DBObject>();
		    for (String otherName : talkerDisease.getOtherHealthItems().keySet()) {
		    	List<String> otherValues = talkerDisease.getOtherHealthItems().get(otherName);
		    	DBObject otherDBObject = BasicDBObjectBuilder.start()
		    		.add("name", otherName)
		    		.add("values", otherValues)
		    		.get();
		    	
		    	otherHealthItems.add(otherDBObject);
		    }
			
			DBObject diseaseObject = BasicDBObjectBuilder.start()
				.add("disease_name", talkerDisease.getDiseaseName())
				.add("default", talkerDisease.isDefault())
				.add("recur", talkerDisease.getRecurrent())
				.add("symp_date", talkerDisease.getSymptomDate())
				.add("diag_date", talkerDisease.getDiagnoseDate())
				.add("health_bio", talkerDisease.getHealthBio())
				.add("health_info", healthInfoDBObject)
				.add("healthitems", talkerDisease.getHealthItems())
				.add("other_healthitems", otherHealthItems)
				.get();
			DBObject talkerDiseaseObject = new BasicDBObject("disease", diseaseObject);
			
			diseaseListItems.add(talkerDiseaseObject);
			
		}
		talkerDiseaseList.clear();
		
		DBObject talkersId = new BasicDBObject("_id", new ObjectId(talkerDiseaseBean.getUid()));
		DBObject talkerDisObject = new BasicDBObject("disease", diseaseListItems);
		
		talkersColl.update(talkersId, new BasicDBObject("$set", talkerDisObject));
		
	}
	
	/**
	 * Method added for get talker's multiple diseases health info
	 * @param talkerId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<TalkerDiseaseBean> getListByTalkerId(String talkerId) {
		DBCollection talkersColl = getCollection(TalkerDAO.TALKERS_COLLECTION);
				
		DBObject query = new BasicDBObject("_id", new ObjectId(talkerId));
		DBObject talkerDBObject = talkersColl.findOne(query, new BasicDBObject("disease", "1"));
		
		List<TalkerDiseaseBean> talkerDiseaseList = new ArrayList<TalkerDiseaseBean>();
		List<DBObject> talkerDBObjectList = (List<DBObject>)talkerDBObject.get("disease");
		if (talkerDBObjectList == null) {
			return null;
		}
		
		for(DBObject dbObject : talkerDBObjectList){
		
			//load and parse disease data from talker
			DBObject diseaseDBObject = dbObject;
			if (diseaseDBObject == null) {
				return null;
			}
			
			DBObject newDiseaseDBObject = (DBObject)diseaseDBObject.get("disease");
			if (newDiseaseDBObject == null) {
				return null;
			}		
			
			TalkerDiseaseBean talkerDisease = new TalkerDiseaseBean();
			talkerDisease.setUid(talkerId);
			talkerDisease.setDiseaseName((String)newDiseaseDBObject.get("disease_name"));
			talkerDisease.setDefault((Boolean)newDiseaseDBObject.get("default")==null ? false : (Boolean)newDiseaseDBObject.get("default"));
			talkerDisease.setRecurrent((String)newDiseaseDBObject.get("recur"));
			talkerDisease.setSymptomDate((Date)newDiseaseDBObject.get("symp_date"));
			talkerDisease.setDiagnoseDate((Date)newDiseaseDBObject.get("diag_date"));
			talkerDisease.setHealthBio((String)newDiseaseDBObject.get("health_bio"));
			
			DBObject healthInfoDBObject = (DBObject)newDiseaseDBObject.get("health_info");
			if (healthInfoDBObject != null) {
				talkerDisease.setHealthInfo(healthInfoDBObject.toMap());
			}
			
			talkerDisease.setHealthItems(getStringSet(newDiseaseDBObject, "healthitems"));
			
			Collection<DBObject> otherItemsList = (Collection<DBObject>)newDiseaseDBObject.get("other_healthitems");
			if (otherItemsList != null) {
				Map<String, List<String>> otherHealthItems = new HashMap<String, List<String>>();
				
				for (DBObject otherDBObject : otherItemsList) {
					String otherName = (String)otherDBObject.get("name");
					otherHealthItems.put(otherName, getStringList(otherDBObject, "values"));
				}
				talkerDisease.setOtherHealthItems(otherHealthItems);
			}
			talkerDiseaseList.add(talkerDisease);
		}
		return talkerDiseaseList;
	}

	/**
	 * Method added for get talker's disease health info
	 * @param talkerId
	 * @return
	 */
	public static TalkerDiseaseBean getByTalkerId(String talkerId) {
		DBCollection talkersColl = getCollection(TalkerDAO.TALKERS_COLLECTION);
				
		DBObject query = new BasicDBObject("_id", new ObjectId(talkerId));
		DBObject talkerDBObject = talkersColl.findOne(query, new BasicDBObject("disease", "1"));
		
		//load and parse disease data from talker
		DBObject diseaseDBObject = (DBObject)talkerDBObject.get("disease");
		if (diseaseDBObject == null) {
			return null;
		}
		
		TalkerDiseaseBean talkerDisease = new TalkerDiseaseBean();
		try{
			talkerDisease.setUid(talkerId);
			talkerDisease.setRecurrent((String)diseaseDBObject.get("recur"));
			talkerDisease.setSymptomDate((Date)diseaseDBObject.get("symp_date"));
			talkerDisease.setDiagnoseDate((Date)diseaseDBObject.get("diag_date"));
			talkerDisease.setHealthBio((String)diseaseDBObject.get("health_bio"));
			
			DBObject healthInfoDBObject = (DBObject)diseaseDBObject.get("health_info");
			if (healthInfoDBObject != null) {
				talkerDisease.setHealthInfo(healthInfoDBObject.toMap());
			}
			
			talkerDisease.setHealthItems(getStringSet(diseaseDBObject, "healthitems"));
			
			Collection<DBObject> otherItemsList = (Collection<DBObject>)diseaseDBObject.get("other_healthitems");
			if (otherItemsList != null) {
				Map<String, List<String>> otherHealthItems = new HashMap<String, List<String>>();
				
				for (DBObject otherDBObject : otherItemsList) {
					String otherName = (String)otherDBObject.get("name");
					otherHealthItems.put(otherName, getStringList(otherDBObject, "values"));
				}
				talkerDisease.setOtherHealthItems(otherHealthItems);
			}
		}catch (Exception e) {
			Logger.error(e, "MessagingDAO : getByTalkerId");
		}
		return talkerDisease;
	}
	
	/**
	 * Method added for conver DBObject health info to List<DBObject>
	 * @param 
	 * @return
	 */
	public static void convertDBObjectToDBList(){
		
		DBCollection talkersColl = getCollection(TalkerDAO.TALKERS_COLLECTION);
		List<TalkerBean> allTalkersList = TalkerDAO.loadAllTalkers(true);
		for(TalkerBean talker : allTalkersList){
			TalkerDiseaseBean talkerDiseaseBean = getByTalkerId(talker.getId());
			if(talkerDiseaseBean != null){
				String talkerCategory = talker.getCategory() == null ? "Breast Cancer" : talker.getCategory();
				talkerDiseaseBean.setDiseaseName(talkerCategory);
				talkerDiseaseBean.setDefault(true);
				List<TalkerDiseaseBean> talkerDiseaseList = new ArrayList<TalkerDiseaseBean>();
				
				talkerDiseaseList.add(talkerDiseaseBean);
	
				List<DBObject> diseaseListItems = new ArrayList<DBObject>();
				
				for(TalkerDiseaseBean talkerDisease : talkerDiseaseList){
				
				    DBObject healthInfoDBObject = new BasicDBObject(talkerDisease.getHealthInfo());
				    
				    //prepare other items
				    List<DBObject> otherHealthItems = new ArrayList<DBObject>();
				    for (String otherName : talkerDisease.getOtherHealthItems().keySet()) {
				    	List<String> otherValues = talkerDisease.getOtherHealthItems().get(otherName);
				    	DBObject otherDBObject = BasicDBObjectBuilder.start()
				    		.add("name", otherName)
				    		.add("values", otherValues)
				    		.get();
				    	
				    	otherHealthItems.add(otherDBObject);
				    }
					
					DBObject diseaseObject = BasicDBObjectBuilder.start()
						.add("disease_name", talkerDisease.getDiseaseName())
						.add("default", talkerDisease.isDefault())
						.add("recur", talkerDisease.getRecurrent())
						.add("symp_date", talkerDisease.getSymptomDate())
						.add("diag_date", talkerDisease.getDiagnoseDate())
						.add("health_bio", talkerDisease.getHealthBio())
						.add("health_info", healthInfoDBObject)
						.add("healthitems", talkerDisease.getHealthItems())
						.add("other_healthitems", otherHealthItems)
						.get();
					DBObject talkerDiseaseObject = new BasicDBObject("disease", diseaseObject);
					
					diseaseListItems.add(talkerDiseaseObject);
					
				}
				DBObject talkersId = new BasicDBObject("_id", new ObjectId(talkerDiseaseBean.getUid()));
				DBObject talkerDisObject = new BasicDBObject("disease", diseaseListItems);
				//diseaseListItems.clear();
				
				talkersColl.update(talkersId, new BasicDBObject("$set", talkerDisObject));
			}
			
		}
	}
}


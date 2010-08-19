package dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.TalkerDiseaseBean;

import org.bson.types.ObjectId;

import util.DBUtil;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import static util.DBUtil.*;

public class TalkerDiseaseDAO {
	
	public static void saveTalkerDisease(TalkerDiseaseBean talkerDisease) {
	    DBCollection talkersColl = getCollection(TalkerDAO.TALKERS_COLLECTION);
	    
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
			.add("stage", talkerDisease.getStage())
			.add("type", talkerDisease.getType())
			.add("recur", talkerDisease.isRecurrent())
			.add("symp_date", talkerDisease.getSymptomDate())
			.add("diag_date", talkerDisease.getDiagnoseDate())
			.add("healthitems", talkerDisease.getHealthItems())
			.add("other_healthitems", otherHealthItems)
			.get();
		DBObject talkerDiseaseObject = new BasicDBObject("disease", diseaseObject);
		
		DBObject talkerId = new BasicDBObject("_id", new ObjectId(talkerDisease.getUid()));
		talkersColl.update(talkerId, new BasicDBObject("$set", talkerDiseaseObject));
	}
	
	public static TalkerDiseaseBean getByTalkerId(String talkerId) {
		DBCollection talkersColl = getCollection(TalkerDAO.TALKERS_COLLECTION);
				
		DBObject query = new BasicDBObject("_id", new ObjectId(talkerId));
		DBObject talkerDBObject = talkersColl.findOne(query, new BasicDBObject("disease", ""));
		
		//load and parse disease data from talker
		DBObject diseaseDBObject = (DBObject)talkerDBObject.get("disease");
		if (diseaseDBObject == null) {
			return null;
		}
		
		TalkerDiseaseBean talkerDisease = new TalkerDiseaseBean();
		talkerDisease.setUid(talkerId);
		talkerDisease.setStage((String)diseaseDBObject.get("stage"));
		talkerDisease.setType((String)diseaseDBObject.get("type"));
		talkerDisease.setRecurrent((Boolean)diseaseDBObject.get("recur"));
		talkerDisease.setSymptomDate((Date)diseaseDBObject.get("symp_date"));
		talkerDisease.setDiagnoseDate((Date)diseaseDBObject.get("diag_date"));
		talkerDisease.setHealthItems(getStringSet(diseaseDBObject, "healthitems"));
		
		//TODO: template getList method in DBUtil?
		Collection<DBObject> otherItemsList = (Collection<DBObject>)diseaseDBObject.get("other_healthitems");
		if (otherItemsList != null) {
			Map<String, List<String>> otherHealthItems = new HashMap<String, List<String>>();
			
			for (DBObject otherDBObject : otherItemsList) {
				String otherName = (String)otherDBObject.get("name");
				otherHealthItems.put(otherName, getStringList(otherDBObject, "values"));
			}
			talkerDisease.setOtherHealthItems(otherHealthItems);
		}
		
		return talkerDisease;
	}

}


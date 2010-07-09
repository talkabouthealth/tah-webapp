package dao;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import models.TalkerDiseaseBean;

import org.bson.types.ObjectId;

import util.DBUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class TalkerDiseaseDAO {
	
	public static void saveTalkerDisease(TalkerDiseaseBean talkerDisease) {
	    DBCollection talkersColl = DBUtil.getCollection(TalkerDAO.TALKERS_COLLECTION);
		
		DBObject diseaseObject = BasicDBObjectBuilder.start()
			.add("stage", talkerDisease.getStage())
			.add("type", talkerDisease.getType())
			.add("recur", talkerDisease.isRecurrent())
			.add("symp_date", talkerDisease.getSymptomDate())
			.add("diag_date", talkerDisease.getDiagnoseDate())
			.add("healthitems", talkerDisease.getHealthItems())
			.get();
		DBObject talkerDiseaseObject = new BasicDBObject("disease", diseaseObject);
		
		DBObject talkerId = new BasicDBObject("_id", new ObjectId(talkerDisease.getUid()));
		talkersColl.update(talkerId, new BasicDBObject("$set", talkerDiseaseObject));
	}
	
	public static TalkerDiseaseBean getByTalkerId(String talkerId) {
		DBCollection talkersColl = DBUtil.getCollection(TalkerDAO.TALKERS_COLLECTION);
				
		DBObject query = new BasicDBObject("_id", new ObjectId(talkerId));
		DBObject talkerDBObject = talkersColl.findOne(query, new BasicDBObject("disease", ""));
		
		//load and parse disease data from talker
		DBObject diseaseDBObject = (DBObject)talkerDBObject.get("disease");
		TalkerDiseaseBean talkerDisease = new TalkerDiseaseBean();
		talkerDisease.setUid(talkerId);
		talkerDisease.setStage((String)diseaseDBObject.get("stage"));
		talkerDisease.setType((String)diseaseDBObject.get("type"));
		talkerDisease.setRecurrent((Boolean)diseaseDBObject.get("recur"));
		talkerDisease.setSymptomDate((Date)diseaseDBObject.get("symp_date"));
		talkerDisease.setDiagnoseDate((Date)diseaseDBObject.get("diag_date"));
		
		@SuppressWarnings("unchecked")
		Set<String> healthItems = new HashSet<String>((Collection<String>)diseaseDBObject.get("healthitems"));
		talkerDisease.setHealthItems(healthItems);
		
		return talkerDisease;
	}

}


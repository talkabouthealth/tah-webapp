package dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import util.DBUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class DiseaseDAO {
	
	public static final String DISEASES_COLLECTION = "diseases";
	
	//TODO: later we need select by Id of disease, not by name
	public static List<String> getValuesByDisease(String type, String diseaseName) {
		DBCollection diseasesColl = DBUtil.getCollection(DISEASES_COLLECTION);
		
		DBObject query = new BasicDBObject("name", diseaseName);
		DBObject diseaseDBObject = diseasesColl.findOne(query);
		
		@SuppressWarnings("unchecked")
		Collection<String> valuesDBList = (Collection<String>)diseaseDBObject.get(type);
		return new ArrayList<String>(valuesDBList);
	}
	
	//for testing
	public static void main(String[] args) {
		System.out.println(DiseaseDAO.getValuesByDisease("stages", "Breast Cancer"));
	}

}


package dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import models.DiseaseBean;
import models.DiseaseBean.DiseaseQuestion;

import util.DBUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.DB.WriteConcern;

import static util.DBUtil.*;

public class DiseaseDAO {
	
	public static final String DISEASES_COLLECTION = "diseases";
	
	public static void save(DiseaseBean disease) {
		DBCollection diseasesColl = getCollection(DISEASES_COLLECTION);
		
//		DBObject diseaseObject = BasicDBObjectBuilder.start()
//			.add("uid", talkerRef)
//			.add("tid", tid)
//			.add("topic", topic.getTopic())
//			.add("cr_date", topic.getCreationDate())
//			.add("disp_date", topic.getDisplayTime())
//			.add("main_url", topic.getMainURL())
//			.get();
//
//		diseasesColl.save(diseaseObject);
	}
	
	public static DiseaseBean getByName(String diseaseName) {
		DiseaseBean disease = new DiseaseBean();
		
		disease.setName(diseaseName);
		
		List<DiseaseQuestion> questions = new ArrayList<DiseaseQuestion>();
		
		//test question
//		- Is the cancer invasive or non-invasive?
//				- Invasive
//				- Non-invasive
		DiseaseQuestion quest = new DiseaseQuestion();
		quest.setType(DiseaseQuestion.DiseaseQuestionType.SELECT);
		quest.setText("Is the cancer invasive or non-invasive?");
		quest.setName("invasive");
		quest.setDisplayName("Invasive type");
		quest.setAnswers(new HashSet<String>(Arrays.asList("Invasive", "Non-invasive")));
		questions.add(quest);
		
		disease.setQuestions(questions);
		
		return disease;
	}
	
	//TODO: later we need select by Id of disease, not by name
	public static List<String> getValuesByDisease(String type, String diseaseName) {
		DBCollection diseasesColl = getCollection(DISEASES_COLLECTION);
		
		DBObject query = new BasicDBObject("name", diseaseName);
		DBObject diseaseDBObject = diseasesColl.findOne(query);
		
		return getStringList(diseaseDBObject, type);
	}
	
	//for testing
	public static void main(String[] args) {
		System.out.println(DiseaseDAO.getValuesByDisease("stages", "Breast Cancer"));
	}

	

}


package dao;

import static util.DBUtil.getCollection;
import static util.DBUtil.getStringList;
import static util.DBUtil.getStringSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;

import models.DiseaseBean;
import models.TalkerBean;
import models.DiseaseBean.DiseaseQuestion;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class DiseaseDAO {
	
	public static final String DISEASES_COLLECTION = "diseases";
	
	public static void save(DiseaseBean disease) {
		DBCollection diseasesColl = getCollection(DISEASES_COLLECTION);
		
		List<DBObject> questionsDBList = new ArrayList<DBObject>();
		for (DiseaseQuestion question : disease.getQuestions()) {
			DBObject questionDBObject = BasicDBObjectBuilder.start()
				.add("name", question.getName())
				.add("type", question.getType().toString())
				.add("display_name", question.getDisplayName())
				.add("text", question.getText())
				.add("choices", question.getChoices())
				.get();
			questionsDBList.add(questionDBObject);
		}
		
		DBObject diseaseObject = BasicDBObjectBuilder.start()
			.add("name", disease.getName())
			.add("questions", questionsDBList)
			.get();

		diseasesColl.save(diseaseObject);
	}
	
	public static DiseaseBean getByName(String diseaseName) {
		DBCollection diseasesColl = getCollection(DISEASES_COLLECTION);
		
		if(diseaseName == null )
			diseaseName = "Breast Cancer";
		else if(diseaseName.equals(""))
			diseaseName = "Breast Cancer";
		DBObject query = new BasicDBObject("name", diseaseName);
		DBObject diseaseDBObject = diseasesColl.findOne(query);
		
		if (diseaseDBObject == null) {
			return null;
		}
		
		DiseaseBean disease = new DiseaseBean();
		disease.setName(diseaseName);
		
		List<DiseaseQuestion> questions = new ArrayList<DiseaseQuestion>();
		for (DBObject questionDBObject : (Collection<DBObject>)diseaseDBObject.get("questions")) {
			DiseaseQuestion quest = new DiseaseQuestion();
			quest.setName((String)questionDBObject.get("name"));
			quest.setType(DiseaseQuestion.DiseaseQuestionType.valueOf((String)questionDBObject.get("type")));
			quest.setText((String)questionDBObject.get("text"));
			quest.setDisplayName((String)questionDBObject.get("display_name"));
			quest.setChoices(getStringSet(questionDBObject, "choices"));
			questions.add(quest);
		}
		disease.setQuestions(questions);
		
		return disease;
	}
	
	public static List<DiseaseBean> getDeiseaseList(){
		List<DiseaseBean> diseaseList = new ArrayList<DiseaseBean>();
		DBCollection diseasesColl = getCollection(DISEASES_COLLECTION);
		DBObject sortCond = new BasicDBObject("name", 1);
		List<DBObject>  diseaseDBList = diseasesColl.find().sort(sortCond).toArray();

		for (DBObject diseaseDBObject : diseaseDBList) {
			DiseaseBean diseaseBean = new DiseaseBean();
			diseaseBean.parseBasicFromDB(diseaseDBObject);
			diseaseList.add(diseaseBean);
		}
		return diseaseList;
	}
	
	public static String getDiseaseByName(String diseaseName){
		
		DBCollection diseasesColl = getCollection(DISEASES_COLLECTION);
		
		DBObject query = new BasicDBObject("name", diseaseName);
		DBObject diseaseDBObject = diseasesColl.findOne(query);
		
		if (diseaseDBObject == null) {
			return null;
		}else {
			return diseaseDBObject.get("_id").toString();
		}
	}
}


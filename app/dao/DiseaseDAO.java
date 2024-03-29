package dao;

import static util.DBUtil.getCollection;
import static util.DBUtil.getStringList;
import static util.DBUtil.getStringSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import logic.TalkerLogic;
import org.bson.BSONObject;
import models.ConversationBean;
import models.DiseaseBean;
import models.TalkerBean;
import models.TalkerDiseaseBean;
import models.DiseaseBean.DiseaseQuestion;

import org.bson.types.ObjectId;

import play.cache.Cache;
import play.mvc.Scope.Session;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class DiseaseDAO {
	
	public static final String DISEASES_COLLECTION = "diseases";
	
	public static void save(DiseaseBean disease) {
		DBCollection diseasesColl = getCollection(DISEASES_COLLECTION);
		if(!disease.getUpdateFlag().equals("false")){	
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
				.add("fileid",disease.getFileId())
				.get();
			if(disease.getUpdateFlag().equals("true")){
				System.out.println("Updating diseases::::::");
				DBObject query = new BasicDBObject("fileid", disease.getFileId());
				DBObject diseobj = diseasesColl.findOne(query);//, new BasicDBObject("_id", 0));
				
				if(diseobj==null){
					diseasesColl.save(diseaseObject);
				}else{
					String oldDiseaseName = getDiseaseNameByFileId(disease.getFileId());
					String diseId=diseobj.get("_id").toString();
					DBObject id = new BasicDBObject("_id", new ObjectId(diseId));
					diseasesColl.update(id, new BasicDBObject("$set", diseaseObject));
					if(!oldDiseaseName.equalsIgnoreCase(disease.getName()))
						updateDiseaseName(oldDiseaseName,disease.getName());
				}
				
			}else{
				System.out.println("Inserting diseases::::::");
			 diseasesColl.save(diseaseObject);
			}
			
		}
	}
	
	public static void update(String id,String fileid){
		
		DBCollection diseasesColl = getCollection(DISEASES_COLLECTION);
		DBObject diseaseObject = BasicDBObjectBuilder.start()
		.add("fileid", fileid)
		.get();
		
		DBObject disease = new BasicDBObject("_id", new ObjectId(id));
		diseasesColl.update(disease, new BasicDBObject("$set", diseaseObject));
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
		disease.parseBasicFromDB(diseaseDBObject);
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
	
	public static List<DiseaseBean> getCatchedDiseasesList(Session session) {
		List<DiseaseBean> diseaseList = (List<DiseaseBean>) Cache.get("diseasesList");
	    if (diseaseList == null) {	//nothing in cache - load a new one
	    	diseaseList = getDeiseaseList();
	    	Cache.set("diseasesList", diseaseList, "2h");
	    }
		return diseaseList;
	}
	
	public static List<DiseaseBean> getDeiseaseList() {
		List<DiseaseBean> diseaseList = new ArrayList<DiseaseBean>();
		DBCollection diseasesColl = getCollection(DISEASES_COLLECTION);
		DBObject sortCond = new BasicDBObject("name", 1);

		/*
		DBObject query = new BasicDBObject("name", new BasicDBObject("$nin", newCancerTypes()));
		List<DBObject>  diseaseDBList = diseasesColl.find(query).sort(sortCond).toArray();
		*/

		List<DBObject>  diseaseDBList = diseasesColl.find().sort(sortCond).toArray();
		DiseaseBean allCancer=new DiseaseBean();
		for (DBObject diseaseDBObject : diseaseDBList) {
			DiseaseBean diseaseBean = new DiseaseBean();
			diseaseBean.parseBasicFromDB(diseaseDBObject);
			if(diseaseBean.getName().equals("All Cancers"))
				allCancer=diseaseBean;
			else
				diseaseList.add(diseaseBean);
		}
		diseaseList.add(allCancer);
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
	
	public static List<DBObject> loadAllFileIds(){
		DBCollection diseasesColl = getCollection(DISEASES_COLLECTION);
		DBObject obj = BasicDBObjectBuilder.start()
		.get();
		List<DBObject> diseobj=diseasesColl.find(obj,new BasicDBObject("fileid","")).toArray();
		return diseobj;
	}
	
	public static void removeDisease(List<String> fileids){
		List<DBObject> diseobj=loadAllFileIds();
		DBCollection diseasesColl = getCollection(DISEASES_COLLECTION);
		for(DBObject obj:diseobj){
			String id=obj.get("fileid").toString();
			
			if(!fileids.contains(id)){
				diseasesColl.remove(obj);
			}
		}
	}
	
	/**
	 * Method added for change the diseaseName
	 * @param oldName
	 * @param newName
	 */
	public static void updateDiseaseName(String oldName, String newName){
		System.out.println("----------Updateing disease name is talkers---------------------");
		List<TalkerBean> talkersList = TalkerLogic.loadAllTalkersFromCache();
		if(talkersList != null && talkersList.size() > 0){
			for(int index = 0; index < talkersList.size(); index++){
				TalkerBean talker = talkersList.get(index);
				boolean flag = false;
				if(talker != null){
					//update talker category
					String category = talker.getCategory();
					if(category != null && !category.equals("")){
						if(category.equalsIgnoreCase(oldName)){
							flag = true;
							category = newName;
							talker.setCategory(category);
						}
					}
					
					//update talker other categories
					String[] otherCategories = talker.getOtherCategories();
					if(otherCategories != null && otherCategories.length > 0){
						for(int i = 0; i < otherCategories.length; i++){
							String otherCat = otherCategories[i];
							if(otherCat.equalsIgnoreCase(oldName)){
								flag = true;
								otherCat = newName;
								otherCategories[i] = otherCat;
								talker.setOtherCategories(otherCategories);
							}
						}
					}
					
					if(flag == true)
						TalkerDAO.updateTalkerForDisease(talker);
					
					//update talkers disease information
					List<TalkerDiseaseBean> talkerDiseaseList = TalkerDiseaseDAO.getListByTalkerId(talker.getId());
					if(talkerDiseaseList != null && talkerDiseaseList.size() > 0){
						for(int i = 0; i < talkerDiseaseList.size(); i++){
							TalkerDiseaseBean talkerDiseaseBean = talkerDiseaseList.get(i);
							if(talkerDiseaseBean != null){
								String diseaseName = talkerDiseaseBean.getDiseaseName();
								if(diseaseName.equalsIgnoreCase(oldName)){
									diseaseName = newName;
									talkerDiseaseBean.setDiseaseName(diseaseName);
									TalkerDiseaseDAO.saveTalkerDisease(talkerDiseaseBean, talker.getId());
								}
							}
						}
						
					}
					
				}
			}
		}
		System.out.println("----------Completed updating disease name is talkers---------------------");
		System.out.println("----------Updateing disease name is convos---------------------");
		List<ConversationBean> convoList = ConversationDAO.getAllConvosForScheduler();
		if(convoList != null && convoList.size() > 0){
			for(int index = 0; index < convoList.size(); index++){
				ConversationBean convo = convoList.get(index);
				boolean flag = false;
				if(convo != null){
					//update talker category
					String category = convo.getCategory();
					if(category != null && !category.equals("")){
						if(category.equalsIgnoreCase(oldName)){
							flag = true;
							category = newName;
							convo.setCategory(category);
						}
					}
					
					//update talker other categories
					String[] otherCategories = convo.getOtherDiseaseCategories();
					if(otherCategories != null && otherCategories.length > 0){
						for(int i = 0; i < otherCategories.length; i++){
							String otherCat = otherCategories[i];
							if(otherCat.equalsIgnoreCase(oldName)){
								flag = true;
								otherCat = newName;
								otherCategories[i] = otherCat;
								convo.setOtherDiseaseCategories(otherCategories);
							}
						}
					}
					if(flag == true)
						ConversationDAO.updateConvoForDisease(convo);
				}
			}
		}
		System.out.println("----------Completed updating disease name is convos---------------------");
	}
	
	/**
	 * Method added for get disease name by fileId
	 * @param fileId
	 * @return String
	 */
	public static String getDiseaseNameByFileId(String fileId) {
		DBCollection diseasesColl = getCollection(DISEASES_COLLECTION);
		
		DBObject query = new BasicDBObject("fileid", fileId);
		DBObject diseaseDBObject = diseasesColl.findOne(query);
		
		if (diseaseDBObject == null) {
			return null;
		}
		String diseaseName = (String)diseaseDBObject.get("name");
		return diseaseName;
	}

	public static Set<String> newCancerTypes(){
		Set<String> newCancerType = new HashSet<String>();
		newCancerType.add("ADHD");
		newCancerType.add("Sleep Disorders");
		newCancerType.add("Anorexia");
		newCancerType.add("Stress Management");
		newCancerType.add("Anger Management");
		newCancerType.add("Addiction");
		newCancerType.add("Psychopathy");
		newCancerType.add("Creativity");
		return newCancerType;
	}
}


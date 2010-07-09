package dao;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import models.HealthItemBean;

import org.bson.types.ObjectId;

import util.DBUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

public class HealthItemDAO {
	
	public static final String HEALTH_ITEMS_COLLECTION = "healthitems";
	
	public static String save(HealthItemBean healthItem, String parentId) {
		DBCollection healthItemsColl = DBUtil.getCollection(HEALTH_ITEMS_COLLECTION);
		
		DBRef diseaseRef = new DBRef(DBUtil.getDB(), 
				DiseaseDAO.DISEASES_COLLECTION, new ObjectId(healthItem.getDiseaseId()));
		//parent reference can be null
		DBRef parentRef = null;
		if (parentId != null) {
			parentRef = new DBRef(DBUtil.getDB(), HEALTH_ITEMS_COLLECTION, new ObjectId(parentId));
		}
				
		DBObject healthItemObject = BasicDBObjectBuilder.start()
			.add("dis_id", diseaseRef)
			.add("par_id", parentRef)
			.add("name", healthItem.getName())
			.get();

		healthItemsColl.save(healthItemObject);
		return healthItemObject.get("_id").toString();
	}
	
	//For now we don't use "diseaseId" because we have one disease
	//TODO: make it more efficient? - read Mongo articles
	public static HealthItemBean getHealthItemByName(String name, String diseaseId) {
		DBCollection healthItemsColl = DBUtil.getCollection(HEALTH_ITEMS_COLLECTION);
		
		//get root health item by name
		DBObject query = new BasicDBObject("name", name);
		DBObject healthItemDBObject = healthItemsColl.findOne(query);
		
		HealthItemBean healthItem = loadHealthItem(healthItemsColl, 
				healthItemDBObject.get("_id").toString(), (String)healthItemDBObject.get("name"));
		return healthItem;
	}

	/**
	 * Recursively load health item with given id and name
	 */
	private static HealthItemBean loadHealthItem(DBCollection healthItemsColl, String id, String name) {
		HealthItemBean healthItem = new HealthItemBean(id, name);
		
		DBRef parentRef = new DBRef(DBUtil.getDB(), HEALTH_ITEMS_COLLECTION, new ObjectId(id));
		DBObject query = new BasicDBObject("par_id", parentRef);
		
		// load all children
		List<DBObject> childrenList = healthItemsColl.find(query).toArray();
		Set<HealthItemBean> childrenSet = new LinkedHashSet<HealthItemBean>();
		for (DBObject childrenDBObject : childrenList) {
			HealthItemBean childHealthItem = loadHealthItem(healthItemsColl, 
					childrenDBObject.get("_id").toString(), (String)childrenDBObject.get("name"));
			childrenSet.add(childHealthItem);
		}
		healthItem.setChildren(childrenSet);	
		
		return healthItem;
	}
	
	/*  Test methods */
//	private void saveTree(HealthItemBean healthItem, String parentId) {
//		healthItem.setDiseaseId("4c2ddd873846000000001f4b");
//		String currentId = HealthItemDAO.save(healthItem, parentId);
//		Set<HealthItemBean> childs = healthItem.getChildren();
//		if (childs != null) {
//			for (HealthItemBean child : childs) {
//				saveTree(child, currentId);
//			}
//		}
//	}
	
	public static void main(String[] args) {
		HealthItemBean testsItem = HealthItemDAO.getHealthItemByName("tests", "sdf");
		System.out.println(testsItem.getChildren());
	}

}

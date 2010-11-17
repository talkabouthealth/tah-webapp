package dao;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import models.HealthItemBean;

import org.bson.types.ObjectId;

import util.DBUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import static util.DBUtil.*;

public class HealthItemDAO {
	
	public static final String HEALTH_ITEMS_COLLECTION = "healthitems";
	
	public static String save(HealthItemBean healthItem, String parentId) {
		DBCollection healthItemsColl = getCollection(HEALTH_ITEMS_COLLECTION);
		
		DBRef diseaseRef = createRef(DiseaseDAO.DISEASES_COLLECTION, healthItem.getDiseaseId());
		//parent reference can be null
		DBRef parentRef = null;
		if (parentId != null) {
			parentRef = createRef(HEALTH_ITEMS_COLLECTION, parentId);
		}
				
		DBObject healthItemObject = BasicDBObjectBuilder.start()
			.add("dis_id", diseaseRef)
			.add("par_id", parentRef)
			.add("name", healthItem.getName())
			.get();

		healthItemsColl.save(healthItemObject);
		return getString(healthItemObject, "_id");
	}
	
	//For now we don't use "diseaseId" because we have one disease
	//TODO: make it more efficient? - read Mongo articles
	//FIXME: add indexes!!
	public static HealthItemBean getHealthItemByName(String name, String diseaseId) {
		DBCollection healthItemsColl = getCollection(HEALTH_ITEMS_COLLECTION);
		
		//get root health item by name
		DBObject query = new BasicDBObject("name", name);
		DBObject healthItemDBObject = healthItemsColl.findOne(query);
		
		HealthItemBean healthItem = loadHealthItem(healthItemsColl, 
				healthItemDBObject.get("_id").toString(), (String)healthItemDBObject.get("name"), true);
		return healthItem;
	}
	
	public static List<HealthItemBean> getAllHealthItems(String diseaseId) {
		DBCollection healthItemsColl = getCollection(HEALTH_ITEMS_COLLECTION);
		
		List<DBObject> healthItemsDBList = healthItemsColl.find().toArray();
		
		List<HealthItemBean> healthItems = new ArrayList<HealthItemBean>();
		for (DBObject healthItemDBObject : healthItemsDBList) {
			HealthItemBean healthItem = 
				new HealthItemBean(healthItemDBObject.get("_id").toString(), (String)healthItemDBObject.get("name"));
			healthItems.add(healthItem);
		}
		return healthItems;
	}

	/**
	 * Recursively load health item with given id and name
	 * @topItem - defines if this item is top item in the tree (like "tests", "procedures")
	 */
	private static HealthItemBean loadHealthItem(DBCollection healthItemsColl, String id, 
			String name, boolean topItem) {
		HealthItemBean healthItem = new HealthItemBean(id, name);
		
		DBRef parentRef = createRef(HEALTH_ITEMS_COLLECTION, id);
		DBObject query = new BasicDBObject("par_id", parentRef);
		
		// load all children
		List<DBObject> childrenList = healthItemsColl.find(query).toArray();
		Set<HealthItemBean> childrenSet = null;
		if (topItem) {
			//top items are in saved order
			childrenSet = new LinkedHashSet<HealthItemBean>();
		}
		else {
			//children items sorted alphabetically
			childrenSet = new TreeSet<HealthItemBean>();
		}
		for (DBObject childrenDBObject : childrenList) {
			HealthItemBean childHealthItem = loadHealthItem(healthItemsColl, 
					childrenDBObject.get("_id").toString(), (String)childrenDBObject.get("name"), false);
			childrenSet.add(childHealthItem);
		}
		healthItem.setChildren(childrenSet);	
		
		return healthItem;
	}
	
	/**
	 * Saves given Health Items and all of its children recursively 
	 */
	public static void saveTree(HealthItemBean healthItem, String parentId, String diseaseId) {
		healthItem.setDiseaseId(diseaseId);
		String currentId = HealthItemDAO.save(healthItem, parentId);
		
		Set<HealthItemBean> childs = healthItem.getChildren();
		if (childs != null) {
			for (HealthItemBean child : childs) {
				saveTree(child, currentId, diseaseId);
			}
		}
	}
	
	

}

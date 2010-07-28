package dao;

import java.io.BufferedReader;
import java.io.FileReader;
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
		Set<HealthItemBean> childrenSet = new TreeSet<HealthItemBean>();
		for (DBObject childrenDBObject : childrenList) {
			HealthItemBean childHealthItem = loadHealthItem(healthItemsColl, 
					childrenDBObject.get("_id").toString(), (String)childrenDBObject.get("name"));
			childrenSet.add(childHealthItem);
		}
		healthItem.setChildren(childrenSet);	
		
		return healthItem;
	}
	
	/*  Methods for importing HealthItems for given disease in DB */
	private static void importHealthItems(String fileName) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = null;
		
		HealthItemBean topLevel = null;
		Set<HealthItemBean> topLevelChildren = null;
		HealthItemBean subLevel = null;
		Set<HealthItemBean> subLevelChildren = null;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			
			if (line.length() == 0) {
				continue;
			}
			
			if (line.startsWith("--")) {
				if (topLevel != null) {
					saveTree(topLevel, null);
				}
				topLevel = new HealthItemBean(line.substring(2));
				topLevelChildren = new HashSet<HealthItemBean>();
				topLevel.setChildren(topLevelChildren);
				
				subLevel = null;
				subLevelChildren = null;
			}
			else if (line.startsWith("-")) {
				subLevel = new HealthItemBean(line.substring(1));
				topLevelChildren.add(subLevel);
				
				subLevelChildren = new HashSet<HealthItemBean>();
				subLevel.setChildren(subLevelChildren);
			}
			else {
				HealthItemBean healthItem = new HealthItemBean(line);
				if (subLevelChildren == null) {
					topLevelChildren.add(healthItem);
				}
				else {
					subLevelChildren.add(healthItem);
				}
			}
		}
		
		if (topLevel != null) {
			saveTree(topLevel, null);
		}
	}
	
	
	private static void saveTree(HealthItemBean healthItem, String parentId) {
		healthItem.setDiseaseId("4c2ddd873846000000001f4b");
		String currentId = HealthItemDAO.save(healthItem, parentId);
		Set<HealthItemBean> childs = healthItem.getChildren();
		if (childs != null) {
			for (HealthItemBean child : childs) {
				saveTree(child, currentId);
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		importHealthItems("healthitems");
	}

}

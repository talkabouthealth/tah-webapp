package dao;

import static util.DBUtil.createRef;
import static util.DBUtil.getCollection;
import static util.DBUtil.getString;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import models.HealthItemBean;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

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
			.add("datfileid",healthItem.getDatFileId())
			.get();

		healthItemsColl.save(healthItemObject);
		return getString(healthItemObject, "_id");
	}
	
	public static String checkDatFileId(String datfileid){
		 
	     DBCollection healthItemColl = getCollection(HEALTH_ITEMS_COLLECTION);
		 DBObject query = new BasicDBObject("datfileid",datfileid);

	     DBObject healthDBobj = healthItemColl.findOne(query,new BasicDBObject("_id",1));
		  
		if(healthDBobj!=null)   
		 return getString(healthDBobj,"_id");
	    else
	     return null;
	    
	}
	
	
	public static void update(HealthItemBean healthItem) {
		DBCollection healthItemsColl = getCollection(HEALTH_ITEMS_COLLECTION);
		
		DBObject healthItemObject = BasicDBObjectBuilder.start()
			.add("name", healthItem.getName())
			.get();

		DBObject healthItemId = new BasicDBObject("_id", new ObjectId(healthItem.getId()));
		healthItemsColl.update(healthItemId, new BasicDBObject("$set", healthItemObject));
	}
	
	/**
	 * Load given health item and all its subtree.
	 * 
	 * @param name 
	 * @param diseaseId For now we don't use 'diseaseId' because we have one disease
	 * @return
	 */
	public static HealthItemBean getHealthItemByName(String name, String diseaseId) {
		DBCollection healthItemsColl = getCollection(HEALTH_ITEMS_COLLECTION);
		//get root health item by name
		DBObject query = null;
		if(diseaseId != null){
			String id = DiseaseDAO.getDiseaseByName(diseaseId);
			DBRef diseaseRef = createRef(DiseaseDAO.DISEASES_COLLECTION, id);
			query = BasicDBObjectBuilder.start()
				.add("name",name)
				.add("dis_id", diseaseRef)
				.get();
		}else{
			query = BasicDBObjectBuilder.start()
				.add("name",name)
				.get();
		}
		DBObject healthItemDBObject = healthItemsColl.findOne(query);
		if (healthItemDBObject == null) {
			return null;
		}
		
		HealthItemBean healthItem = loadHealthItem(healthItemsColl, 
				healthItemDBObject.get("_id").toString(), (String)healthItemDBObject.get("name"), true);
		return healthItem;
	}
	
	/**
	 * Get list of all health items.
	 * @param diseaseId Not used for now (we have only 'Breast cancer')
	 * @return
	 */
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
	 * @topItem - defines if this item is top/root item in the tree (e.g. "tests", "procedures")
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
					childrenDBObject.get("_id").toString(), (String)childrenDBObject.get("name"), true);
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
		
		String currentId=checkDatFileId(healthItem.getDatFileId());
		
		if(currentId==null){
	      currentId= HealthItemDAO.save(healthItem, parentId);
		}  
		
		Set<HealthItemBean> childs = healthItem.getChildren();
		if (childs != null) {
			for (HealthItemBean child : childs) {
				saveTree(child, currentId, diseaseId);
			}
		}
	}
	
	

}

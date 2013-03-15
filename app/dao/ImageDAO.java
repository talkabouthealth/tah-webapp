package dao;

import static util.DBUtil.getCollection;

import org.bson.types.ObjectId;

import models.ConversationBean;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class ImageDAO {

	public static final String IMAGE_NAME = "image_name";
	
	public static String getNewImageName() {
		DBCollection imageColl = getCollection(IMAGE_NAME);
		
		DBObject imageDBObject = imageColl.findOne();
		if (imageDBObject == null) {
			return "1";
		} else {
			return imageDBObject.get("imageName").toString();
		}
	}
	
	public static boolean updateImageName() {
		DBCollection convosColl = getCollection(IMAGE_NAME);
		DBObject obj = convosColl.findOne();
		System.out.println("Updating");
		if(obj != null) {
			String id = obj.get("_id").toString();
			DBObject query = new BasicDBObject("_id", new ObjectId(id));
			DBObject updateObj = new BasicDBObject("imageName", 1);
			convosColl.update(query, new BasicDBObject("$inc",updateObj));
			System.out.println("Updated" + id);
		} else {
			System.out.println("Created");
			DBObject imageDBObject = BasicDBObjectBuilder.start().add("imageName", 2).get();
			convosColl.save(imageDBObject);
		}
		return true;
	}
}
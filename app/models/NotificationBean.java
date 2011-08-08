package models;

import static util.DBUtil.getDB;
import static util.DBUtil.getString;

import java.util.Set;

import org.bson.types.ObjectId;

import logic.TalkerLogic;

import com.mongodb.DBObject;
import com.mongodb.DBRef;

import dao.ConversationDAO;

public class NotificationBean implements DBModel{

	@Override
	public DBObject toDBObject() {
		return null;
	}

	@Override
	public void parseDBObject(DBObject dbObject) {
		if (dbObject == null) {
			return;
		}
		
		setId(dbObject.get("_id").toString());
		setUid(dbObject.get("uid").toString());
		setConvoid(dbObject.get("_id")==null?"":dbObject.get("_id").toString());
		setTalker(TalkerLogic.loadTalkerFromCache(dbObject, "uid"));
		DBRef convoRef = (DBRef)dbObject.get("convoid");
		setConvos(ConversationDAO.getByIdBasicQuestion(convoRef.getId().toString()));
		setFlag(Boolean.parseBoolean(dbObject.get("flag").toString()));
		setType(dbObject.get("type").toString());
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getConvoid() {
		return convoid;
	}

	public void setConvoid(String convoid) {
		this.convoid = convoid;
	}

	public boolean isFlag() {
		return flag;
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public TalkerBean getTalker() {
		return talker;
	}

	public void setTalker(TalkerBean talker) {
		this.talker = talker;
	}

	public ConversationBean getConvos() {
		return convos;
	}

	public void setConvos(ConversationBean convos) {
		this.convos = convos;
	}

	private String id;
	private String uid;
	private String convoid;
	private boolean flag;
	private String type;
	private TalkerBean talker;
	private ConversationBean convos;

}
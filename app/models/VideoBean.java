package models;

import static util.DBUtil.createRef;

import java.util.Calendar;
import java.util.Collection;
import java.util.Set;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import dao.ConversationDAO;
import dao.TopicDAO;

public class VideoBean implements DBModel {

	private String id;
	private String videoId;
	private TalkerBean talkerBean;
	private ConversationBean convoBean;
	private Set<TopicBean> topics;

	public String getVideoId() {
		return videoId;
	}
	public void setVideoId(String videoId) {
		this.videoId = videoId;
	}
	public ConversationBean getConvoBean() {
		return convoBean;
	}
	public void setConvoBean(ConversationBean convoBean) {
		this.convoBean = convoBean;
	}
	public TalkerBean getTalkerBean() {
		return talkerBean;
	}
	public void setTalkerBean(TalkerBean talkerBean) {
		this.talkerBean = talkerBean;
	}
	public Set<TopicBean> getTopics() {
		return topics;
	}
	public void setTopics(Set<TopicBean> topics) {
		this.topics = topics;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	@Override
	public void parseDBObject(DBObject dbObject) {
		setId(dbObject.get("_id").toString());
		setVideoId(dbObject.get("videoId").toString());
		//setConvoBean(ConversationDAO.getConvoById(dbObject.get("convo").toString()));
	}
	
	public void parseDBObjectTopic(DBObject dbObject){
		setId(dbObject.get("_id").toString());
		setVideoId(dbObject.get("videoId").toString());
		DBRef topicDBRef = (DBRef)dbObject.get("convo"); 
		setConvoBean(ConversationDAO.getConvoById(topicDBRef.getId().toString()));
	}
	
	@Override
	public DBObject toDBObject() {
		DBRef convoDBRef = createRef(ConversationDAO.CONVERSATIONS_COLLECTION, getConvoBean().getId());

		DBObject videoDBObject = BasicDBObjectBuilder.start()
		.add("videoId", getVideoId())
		.add("talker", getTalkerBean())
		.add("convo", convoDBRef)
		.add("topics", getConvoBean().topicsToDB())
		.add("timestamp", Calendar.getInstance().getTime())
		.get();
		return videoDBObject;
	}
}
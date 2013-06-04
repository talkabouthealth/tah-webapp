package models;

import static util.DBUtil.createRef;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import util.DBUtil;

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
	private String videoTitle;
	private String homeVideoLink;
	private String cancerType;
	private Date creationDate;
	
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
	public String getVideoTitle() {
		return videoTitle;
	}
	public void setVideoTitle(String videoTitle) {
		this.videoTitle = videoTitle;
	}
	public String getHomeVideoLink() {
		return homeVideoLink;
	}
	public void setHomeVideoLink(String homeVideoLink) {
		this.homeVideoLink = homeVideoLink;
	}
	public String getCancerType() {
		return cancerType;
	}
	public void setCancerType(String cancerType) {
		this.cancerType = cancerType;
	}

	public Date getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}
	@Override
	public void parseDBObject(DBObject dbObject) {
		setId(dbObject.get("_id").toString());
		setVideoId(dbObject.get("videoId").toString());
		//setConvoBean(ConversationDAO.getConvoById(dbObject.get("convo").toString()));
	}
	
	public void parseDBObjectHome(DBObject dbObject) {
		setId(dbObject.get("_id").toString());
		setVideoId(dbObject.get("videoId").toString());
		setVideoTitle(dbObject.get("videoTitle").toString());
		setHomeVideoLink(dbObject.get("videoLink").toString());
		setCreationDate((Date)dbObject.get("timestamp"));
	}
	
	public void parseDBObjectTopic(DBObject dbObject) {
		setId(dbObject.get("_id").toString());
		setVideoId(dbObject.get("videoId").toString());
		if(dbObject.get("videoTitle")!= null) {
			setVideoTitle(dbObject.get("videoTitle").toString());
		}
		DBRef topicDBRef = (DBRef)dbObject.get("convo"); 
		setConvoBean(ConversationDAO.getConvoById(topicDBRef.getId().toString()));
		if(StringUtils.isBlank(getVideoTitle())) {
			setVideoTitle(getConvoBean().getTopic());
		}
		setHomeVideoLink(getConvoBean().getMainURL());
		setCreationDate((Date)dbObject.get("timestamp"));
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
		//.add("videoTitle",getVideoTitle())
		.get();
		return videoDBObject;
	}
}
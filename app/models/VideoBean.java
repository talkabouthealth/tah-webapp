package models;

import static util.DBUtil.createRef;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import util.DBUtil;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import dao.ConversationDAO;
import dao.DiseaseDAO;
import dao.TopicDAO;

public class VideoBean implements DBModel {

	private String id;
	private String videoId;
	private TalkerBean talkerBean;
	private ConversationBean convoBean;
	private Set<TopicBean> topics;
	private Set<DiseaseBean> diseases;
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
	public Set<DiseaseBean> getDiseases() {
		diseases = new LinkedHashSet();
		if(StringUtils.isNotBlank(getConvoBean().getCategory())) {
			diseases.add(DiseaseDAO.getByName(getConvoBean().getCategory()));
		}
		if(getConvoBean().getOtherDiseaseCategories() != null){
			for(int index = 0; index < getConvoBean().getOtherDiseaseCategories().length; index++){
				diseases.add(DiseaseDAO.getByName(getConvoBean().getOtherDiseaseCategories()[index]));
			}
		}
		return diseases;
	}
	public void setDiseases(Set<DiseaseBean> diseases) {
		this.diseases = diseases;
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
		.add("diseases",diseaseToDB())
		.get();
		return videoDBObject;
	}

	public DBObject toupdateDBObject() {
		DBRef convoDBRef = createRef(ConversationDAO.CONVERSATIONS_COLLECTION, getConvoBean().getId());

		DBObject videoDBObject = BasicDBObjectBuilder.start()
		.add("videoId", getVideoId())
		.add("talker", getTalkerBean())
		.add("convo", convoDBRef)
		.add("topics", getConvoBean().topicsToDB())
		//.add("timestamp", Calendar.getInstance().getTime())
		.add("diseases",diseaseToDB())
		.get();
		return videoDBObject;
	}
	
	public List<DBRef> diseaseToDB() {
		List<DBRef> diseaseDBList = new ArrayList<DBRef>();
		for (DiseaseBean disease : getDiseases()) {
			if(disease != null && disease.getId() != null) {
				DBRef diseaseRef = createRef(DiseaseDAO.DISEASES_COLLECTION, disease.getId());
				diseaseDBList.add(diseaseRef);
			}
		}
		return diseaseDBList;
	}
}
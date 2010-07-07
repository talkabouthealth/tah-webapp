package models;

import java.util.Date;

public class TopicBean {
	private String id;
	private String topic;
	private Date creationDate;
	private Date displayTime;
	private String uid;
	private TalkerBean talker;
	
	public TopicBean(){}
	
	public String getTopic() {
		return topic;
	}
	public Date getCreationDate() {
		return creationDate;
	}
	public void setTopic(String value) {
		topic = value;
	}
	public void setCreationDate(Date value) {
		creationDate = value;
	}
	public void setDisplayTime(Date displayTime) {
		this.displayTime = displayTime;
	}
	public Date getDisplayTime() {
		return displayTime;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public TalkerBean getTalker() {
		return talker;
	}

	public void setTalker(TalkerBean talker) {
		this.talker = talker;
	}
}

package models;

import java.util.Date;
import java.util.List;

public class TopicBean {
	private String id;
	private int tid;
	public int getTid() {
		return tid;
	}

	public void setTid(int tid) {
		this.tid = tid;
	}

	private String topic;
	private Date creationDate;
	private Date displayTime;
	private String uid;
	private TalkerBean talker;
	
	//new fields
	private String details;
	private List<String> tagList;
	
	private String summary;
	private List<String> contributorList;
	
	
	public List<String> getContributorList() {
		return contributorList;
	}

	public void setContributorList(List<String> contributorList) {
		this.contributorList = contributorList;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}

	public List<String> getTagList() {
		return tagList;
	}

	public void setTagList(List<String> tagList) {
		this.tagList = tagList;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

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

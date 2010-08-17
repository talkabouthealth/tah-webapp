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
	
	private String mainURL;
	
	public TopicBean() {}
	
	public TopicBean(String id) {
		this.id = id;
	}

	public String getMainURL() {
		if (mainURL == null || mainURL.length() == 0) {
			//TODO: temporary for old topics
			return ""+tid;
		}
		return mainURL;
	}

	public void setMainURL(String mainURL) {
		this.mainURL = mainURL;
	}

	//new fields
	//TODO: Set or List?
	private String details;
	private List<String> tags;
	
	private String summary;
	private List<String> sumContributors;
	
	private List<String> members;
	private List<MessageBean> messages;
	private List<String> followers;
	
	public List<String> getFollowers() {
		return followers;
	}

	public void setFollowers(List<String> followers) {
		this.followers = followers;
	}

	private int views;
	
	

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

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

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public List<String> getMembers() {
		return members;
	}

	public void setMembers(List<String> members) {
		this.members = members;
	}

	public List<MessageBean> getMessages() {
		return messages;
	}

	public void setMessages(List<MessageBean> messages) {
		this.messages = messages;
	}
	
	public List<String> getSumContributors() {
		return sumContributors;
	}

	public void setSumContributors(List<String> sumContributors) {
		this.sumContributors = sumContributors;
	}

	public int getViews() {
		return views;
	}

	public void setViews(int views) {
		this.views = views;
	}
	
	
}

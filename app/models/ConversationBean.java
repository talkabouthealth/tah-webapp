package models;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class ConversationBean {
	private String id;
	private int tid;
	private String mainURL;
	private Set<String> URLs;
	private String topic;
	private Date creationDate;
	private Date displayTime;	//TODO: old field? how to use it?
	
	//creator
	private String uid;
	private TalkerBean talker;
	
	//TODO: Set or List?
	private String details;
	private int views;
	private List<String> tags;
	private String summary;
	private List<String> sumContributors;
	private Set<String> members;
	private List<CommentBean> comments;
	private List<MessageBean> messages;
	private List<TalkerBean> followers;
	
	public ConversationBean() {}
	
	public ConversationBean(String id) {
		this.id = id;
	}

	
	public String getMainURL() {
		return mainURL;
	}
	public void setMainURL(String mainURL) { this.mainURL = mainURL; }

	public String getDetails() { return details; }
	public void setDetails(String details) { this.details = details; }

	public String getSummary() { return summary; }
	public void setSummary(String summary) { this.summary = summary; }

	public String getTopic() { return topic; }
	public void setTopic(String topic) { this.topic = topic; }
	
	public Date getCreationDate() { return creationDate; }
	public void setCreationDate(Date creationDate) { this.creationDate = creationDate; }
	
	public void setDisplayTime(Date displayTime) { this.displayTime = displayTime; }
	public Date getDisplayTime() { return displayTime; }

	public String getUid() { return uid; }
	public void setUid(String uid) { this.uid = uid; }

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public TalkerBean getTalker() { return talker; }
	public void setTalker(TalkerBean talker) { this.talker = talker; }

	public List<String> getTags() { return tags; }
	public void setTags(List<String> tags) { this.tags = tags; }

	public Set<String> getMembers() { return members; }
	public void setMembers(Set<String> members) { this.members = members; }

	public List<MessageBean> getMessages() { return messages; }
	public void setMessages(List<MessageBean> messages) { this.messages = messages; }
	
	public List<String> getSumContributors() { return sumContributors; }
	public void setSumContributors(List<String> sumContributors) { this.sumContributors = sumContributors; }

	public int getViews() { return views; }
	public void setViews(int views) { this.views = views; }
	
	public List<CommentBean> getComments() { return comments; }
	public void setComments(List<CommentBean> comments) { this.comments = comments; }
	
	public List<TalkerBean> getFollowers() { return followers; }
	public void setFollowers(List<TalkerBean> followers) { this.followers = followers; }
	
	public int getTid() { return tid; }
	public void setTid(int tid) { this.tid = tid; }

	public Set<String> getURLs() {
		return URLs;
	}

	public void setURLs(Set<String> uRLs) {
		URLs = uRLs;
	}
}

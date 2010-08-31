package models;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class TopicBean {
	
	private String id;
	private String title;
	private String summary;
	
	private String mainURL;
	
	private int views;
	private Date creationDate;
	
	private List<String> sumContributors;
	private List<TalkerBean> followers;
	
	public TopicBean() {
		super();
	}
	public TopicBean(String title) {
		super();
		this.title = title;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getMainURL() {
		return mainURL;
	}
	public void setMainURL(String mainURL) {
		this.mainURL = mainURL;
	}
	public Date getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}
	public int getViews() {
		return views;
	}
	public void setViews(int views) {
		this.views = views;
	}
	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}
	public List<String> getSumContributors() {
		return sumContributors;
	}
	public void setSumContributors(List<String> sumContributors) {
		this.sumContributors = sumContributors;
	}
	public List<TalkerBean> getFollowers() {
		return followers;
	}
	public void setFollowers(List<TalkerBean> followers) {
		this.followers = followers;
	}
	
}

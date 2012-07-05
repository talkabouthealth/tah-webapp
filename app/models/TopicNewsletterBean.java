package models;

import java.util.Set;

public class TopicNewsletterBean {

	private TopicBean topicBean;
	private Set<String> email;

	public TopicBean getTopicBean() {
		return topicBean;
	}
	public void setTopicBean(TopicBean topicBean) {
		this.topicBean = topicBean;
	}
	public Set<String> getEmail() {
		return email;
	}
	public void setEmail(Set<String> email) {
		this.email = email;
	}
}

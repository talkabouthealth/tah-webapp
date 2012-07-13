package models;

import java.util.Set;

public class TalkerNewsletterBean {

	private TalkerBean talkerBean;
	private Set<String> email;

	public TalkerBean getTalkerBean() {
		return talkerBean;
	}
	public void setTalkerBean(TalkerBean talkerBean) {
		this.talkerBean = talkerBean;
	}
	public Set<String> getEmail() {
		return email;
	}
	public void setEmail(Set<String> email) {
		this.email = email;
	}
}

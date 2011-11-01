package models;

import java.util.Date;
import java.util.List;
import models.actions.Action;

public class ThankYouBean implements Comparable<ThankYouBean> {
	
	private String id;
	private Date time;
	private String note;
	
	//IDs of talkers
	private String from;
	private String to;
	private TalkerBean fromTalker;
	
	private List<Action> profileComments;
	
	public int compareTo(ThankYouBean o) {
		//reverse order
		return o.time.compareTo(time);
	}
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public TalkerBean getFromTalker() { return fromTalker; }
	public void setFromTalker(TalkerBean fromTalker) { this.fromTalker = fromTalker; }
	
	public Date getTime() { return time; }
	public void setTime(Date time) { this.time = time; }
	
	public String getNote() { return note; }
	public void setNote(String note) { this.note = note; }
	
	public String getFrom() { return from; }
	public void setFrom(String from) { this.from = from; }
	
	public String getTo() { return to; }
	public void setTo(String to) { this.to = to; }
	
	public List<Action> getProfileComments() {
		return profileComments;
	}
	public void setProfileComments(List<Action> profileComments) {
		this.profileComments = profileComments;
	}
	
}

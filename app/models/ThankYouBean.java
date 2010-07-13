package models;

import java.util.Date;

public class ThankYouBean {
	
	private Date time;
	private String note;
	
	//IDs of talkers
	private String from;
	private String to;
	
	private TalkerBean fromTalker;
	
	public TalkerBean getFromTalker() {
		return fromTalker;
	}
	public void setFromTalker(TalkerBean fromTalker) {
		this.fromTalker = fromTalker;
	}
	public Date getTime() {
		return time;
	}
	public void setTime(Date time) {
		this.time = time;
	}
	public String getNote() {
		return note;
	}
	public void setNote(String note) {
		this.note = note;
	}
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getTo() {
		return to;
	}
	public void setTo(String to) {
		this.to = to;
	}

}

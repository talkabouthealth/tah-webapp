package models;

import java.util.Date;

public class ActivityBean {
	
	private String id;
	private TalkerBean talker;
	private Date time;
	private String text;
	
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	
	public TalkerBean getTalker() { return talker; }
	public void setTalker(TalkerBean talker) { this.talker = talker; }
	
	public Date getTime() { return time; }
	public void setTime(Date time) { this.time = time; }
	
	public String getText() { return text; }
	public void setText(String text) { this.text = text; }
}

package models;

import java.util.Date;

public class MessageBean {
	
	protected String id;
	private TalkerBean fromTalker;
	private String text;
	private Date time;
	//index in the 'messages' array, used for updating
	private int index;
	
	public MessageBean() {}

	public MessageBean(String messageId) {
		id = messageId;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MessageBean)) {
			return false;
		}
		
		MessageBean other = (MessageBean)obj;
		return id.equals(other.id);
	}
	
	@Override
	public int hashCode() {
		if (id == null) {
			return 47;
		}
		return id.hashCode();
	}
	
	
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	
	public TalkerBean getFromTalker() { return fromTalker; }
	public void setFromTalker(TalkerBean fromTalker) { this.fromTalker = fromTalker; }

	public String getText() { return text; }
	public void setText(String text) { this.text = text; }

	public Date getTime() { return time; }
	public void setTime(Date time) { this.time = time; }

	public int getIndex() { return index; }
	public void setIndex(int index) { this.index = index; }
}

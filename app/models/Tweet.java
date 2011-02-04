package models;

public class Tweet {
	
	private String id;
	private String userId;
	private String text;
	
	@Override
	public String toString() {
		return "["+userId+"]: "+text;
	}
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}

}

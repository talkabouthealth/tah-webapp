package models;

import java.util.Date;

/**
 * Facebook Post or Twitter tweet
 *
 */
public class ServicePost implements Comparable<ServicePost>{
	
	private String id;
	private String userId;
	private String text;
	//creation time
	private Date time;
	
	@Override
	public String toString() {
		return "["+id+"]: "+text;
	}
	
	@Override
	public int compareTo(ServicePost o) {
		return time.compareTo(o.time);
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
	public Date getTime() {
		return time;
	}
	public void setTime(Date time) {
		this.time = time;
	}
}

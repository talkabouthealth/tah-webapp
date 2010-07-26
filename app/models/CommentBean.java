package models;

import java.util.Date;
import java.util.List;

/**
 *	Bean for Profile Comments 
 *
 */
public class CommentBean {
	
	private String id;
	
	private String profileTalkerId;
	private TalkerBean fromTalker;
	
	private String text;
	private Date time;
	
	private String parentId;
	private List<CommentBean> children;
	

	public CommentBean() {}

	public CommentBean(String commentId) {
		id = commentId;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CommentBean)) {
			return false;
		}
		
		CommentBean other = (CommentBean)obj;
		return id.equals(other.id);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getProfileTalkerId() {
		return profileTalkerId;
	}

	public void setProfileTalkerId(String profileTalkerId) {
		this.profileTalkerId = profileTalkerId;
	}

	public TalkerBean getFromTalker() {
		return fromTalker;
	}

	public void setFromTalker(TalkerBean fromTalker) {
		this.fromTalker = fromTalker;
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
	
	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public List<CommentBean> getChildren() {
		return children;
	}

	public void setChildren(List<CommentBean> children) {
		this.children = children;
	}

	
	
}

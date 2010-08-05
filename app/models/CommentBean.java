package models;

import java.util.Date;
import java.util.List;

/**
 *	Bean for Profile Comments 
 *
 */
public class CommentBean extends MessageBean {
	
	private String profileTalkerId;
	
	private String parentId;
	private List<CommentBean> children;
	

	public CommentBean() {}

	public CommentBean(String commentId) {
		super(commentId);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CommentBean)) {
			return false;
		}
		
		CommentBean other = (CommentBean)obj;
		return id.equals(other.id);
	}
	
	public String getProfileTalkerId() {
		return profileTalkerId;
	}

	public void setProfileTalkerId(String profileTalkerId) {
		this.profileTalkerId = profileTalkerId;
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

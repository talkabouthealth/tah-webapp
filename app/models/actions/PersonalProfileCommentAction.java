package models.actions;

import models.CommentBean;
import models.TalkerBean;
import models.actions.Action.ActionType;

import com.mongodb.DBObject;

public class PersonalProfileCommentAction extends AbstractAction {
	
	//TODO: how to display them?
	public PersonalProfileCommentAction(TalkerBean talker, CommentBean profileComment, CommentBean profileReply, ActionType type) {
		super(type, talker);
		this.profileComment = profileComment;
		this.profileReply = profileReply;
	}

	public PersonalProfileCommentAction(DBObject dbObject) {
		super(dbObject);
	}
	
	protected boolean hasProfileComment() { return true; }
	protected boolean hasProfileReply() { return true; }
	
	public String toHTML() {
		StringBuilder result = new StringBuilder();
		result.append(fullUserName(talker));
		if (type == ActionType.PERSONAL_PROFILE_COMMENT) {
			result.append(" left a comment '"+profileComment.getText()+"'");
		}
		else if (type == ActionType.PERSONAL_PROFILE_REPLY) {
			result.append(" left a reply '"+profileReply.getText()+"' to a comment '"+profileComment.getText()+"'");
		}
		
		return result.toString();
	}
	
}

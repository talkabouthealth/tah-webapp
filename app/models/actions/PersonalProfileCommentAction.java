package models.actions;

import models.CommentBean;
import models.TalkerBean;
import models.actions.Action.ActionType;

import com.mongodb.DBObject;

public class PersonalProfileCommentAction extends AbstractAction {
	
	public PersonalProfileCommentAction(TalkerBean talker, TalkerBean profileTalker, 
			CommentBean profileComment, CommentBean profileReply, ActionType type) {
		super(type, talker);
		this.profileComment = profileComment;
		this.profileReply = profileReply;
		this.otherTalker = profileTalker;
	}

	public PersonalProfileCommentAction(DBObject dbObject) {
		super(dbObject);
	}
	
	protected boolean hasProfileComment() { return true; }
	protected boolean hasProfileReply() { return true; }
	protected boolean hasOtherTalker() { return true; }
	
	public String toHTML() {
		StringBuilder result = new StringBuilder();
		result.append(fullUserName(talker));
		if (type == ActionType.PERSONAL_PROFILE_COMMENT) {
			result.append(" left a comment for ");
		}
		else if (type == ActionType.PERSONAL_PROFILE_REPLY) {
			result.append(" replied to ");
		}
		result.append(fullUserName(otherTalker));
		
		return result.toString();
	}
	
}

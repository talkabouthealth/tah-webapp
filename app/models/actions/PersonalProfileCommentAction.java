package models.actions;

import models.CommentBean;
import models.TalkerBean;
import models.actions.Action.ActionType;

import com.mongodb.DBObject;

/**
 * Occurs when talker writes thought/reply for another talker
 */
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
	
	public String toHTML(boolean authenticated) {
		StringBuilder result = new StringBuilder();
		result.append(fullUserName(talker, authenticated));
		result.append(" left a comment for ");
		result.append(fullUserName(otherTalker, authenticated));
		
		return result.toString();
	}
	
}

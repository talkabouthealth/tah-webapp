package models.actions;

import models.TalkerBean;

import com.mongodb.DBObject;

public class ProfileCommentAction extends AbstractAction {
	
	public ProfileCommentAction(TalkerBean talker, TalkerBean otherTalker) {
		super("PROFILE_COMMENT", talker);
		this.otherTalker = otherTalker;
	}

	public ProfileCommentAction(DBObject dbObject) {
		super(dbObject);
	}
	
	protected boolean hasOtherTalker() { return true; }

	public String toHTML() {
		StringBuilder result = new StringBuilder();
		result.append(userName());
		result.append(" left a comment for ");
		result.append(otherTalker.getUserName());
		
		return result.toString();
	}
	
}

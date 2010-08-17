package models.actions;

import models.TalkerBean;

import com.mongodb.DBObject;

public class ProfileReplyAction extends AbstractAction {
	
	public ProfileReplyAction(TalkerBean talker, TalkerBean otherTalker) {
		super("PROFILE_REPLY", talker);
		this.otherTalker = otherTalker;
	}

	public ProfileReplyAction(DBObject dbObject) {
		super(dbObject);
	}
	
	protected boolean hasOtherTalker() { return true; }

	public String toHTML() {
		StringBuilder result = new StringBuilder();
		result.append(userName());
		result.append(" replied to a comment for ");
		result.append(otherTalker.getUserName());
		
		return result.toString();
	}
	
}

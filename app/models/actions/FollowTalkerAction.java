package models.actions;

import models.TalkerBean;
import models.actions.Action.ActionType;

import com.mongodb.DBObject;

public class FollowTalkerAction extends AbstractAction {
	
	public FollowTalkerAction(TalkerBean talker, TalkerBean otherTalker) {
		super(ActionType.FOLLOW_TALKER, talker);
		this.otherTalker = otherTalker;
	}

	public FollowTalkerAction(DBObject dbObject) {
		super(dbObject);
	}
	
	protected boolean hasOtherTalker() { return true; }

	public String toHTML(boolean authenticated) {
		StringBuilder result = new StringBuilder();
		result.append(userName());
		result.append(" began following ");
		result.append(otherTalker.getUserName());
		
		return result.toString();
	}
	
}

package models.actions;

import models.TalkerBean;
import models.ConversationBean;
import models.actions.Action.ActionType;

import com.mongodb.DBObject;

public class FollowConvoAction extends AbstractAction {
	
	public FollowConvoAction(TalkerBean talker, ConversationBean topic) {
		super(ActionType.FOLLOW_CONVO, talker);
		this.convo = topic;
	}

	public FollowConvoAction(DBObject dbObject) {
		super(dbObject);
	}
	
	protected boolean hasConvo() { return true; }

	public String toHTML() {
		StringBuilder result = new StringBuilder();
		result.append(userName());
		result.append(" began following: ");
		result.append(convoLink());
		
		return result.toString();
	}
}

package models.actions;

import models.TalkerBean;
import models.ConversationBean;
import models.actions.Action.ActionType;

import com.mongodb.DBObject;

public class JoinConvoAction extends AbstractAction {
	
	public JoinConvoAction(TalkerBean talker, ConversationBean topic) {
		super(ActionType.JOIN_CONVO, talker);
		this.convo = topic;
	}

	public JoinConvoAction(DBObject dbObject) {
		super(dbObject);
	}
	
	protected boolean hasConvo() { return true; }

	public String toHTML() {
		StringBuilder result = new StringBuilder();
		result.append(userName());
		result.append(" joined the conversation: ");
		result.append(topicLink());
		
		return result.toString();
	}
	
}

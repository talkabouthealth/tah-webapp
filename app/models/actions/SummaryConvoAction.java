package models.actions;

import models.ConversationBean;
import models.TalkerBean;
import models.actions.Action.ActionType;

import com.mongodb.DBObject;

public class SummaryConvoAction extends AbstractAction {
	
	public SummaryConvoAction(TalkerBean talker, ConversationBean topic, ActionType type) {
		super(type, talker);
		this.convo = topic;
	}

	public SummaryConvoAction(DBObject dbObject) {
		super(dbObject);
	}
	
	protected boolean hasConvo() { return true; }

	public String toHTML() {
		StringBuilder result = new StringBuilder();
		result.append(userName());
		if (type == ActionType.SUMMARY_ADDED) {
			result.append(" added summary for the conversation: ");
		}
		else if (type == ActionType.SUMMARY_EDITED) {
			result.append(" edited summary for the conversation: ");
		}
		
		result.append(topicLink());
		
		return result.toString();
	}
}

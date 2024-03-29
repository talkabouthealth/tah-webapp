package models.actions;

import models.ConversationBean;
import models.TalkerBean;
import models.actions.Action.ActionType;

import com.mongodb.DBObject;

/**
 * Occurs when talker creates or updates Conversation Summary
 */
public class SummaryConvoAction extends AbstractAction {
	
	public SummaryConvoAction(TalkerBean talker, ConversationBean topic, ActionType type) {
		super(type, talker);
		this.convo = topic;
	}

	public SummaryConvoAction(DBObject dbObject) {
		super(dbObject);
	}
	
	protected boolean hasConvo() { return true; }

	public String toHTML(boolean authenticated) {
		StringBuilder result = new StringBuilder();
		
		if (type == ActionType.SUMMARY_ADDED) {
			result.append("Answer summary created by ");
			result.append(fullUserName(talker, authenticated));
			result.append(convoTopics());
		}
		else if (type == ActionType.SUMMARY_EDITED) {
			result.append(fullUserName(talker, authenticated));
			result.append(" edited the summary ");
			result.append(convoTopics());
		}
		
		return result.toString();
	}
}

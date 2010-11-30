package models.actions;

import models.TalkerBean;
import models.ConversationBean;
import models.actions.Action.ActionType;

import com.mongodb.DBObject;

public class JoinConvoAction extends AbstractAction {
	
	public JoinConvoAction(TalkerBean talker, ConversationBean convo) {
		super(ActionType.JOIN_CONVO, talker);
		this.convo = convo;
	}

	public JoinConvoAction(DBObject dbObject) {
		super(dbObject);
	}
	
	protected boolean hasConvo() { return true; }

	public String toHTML(boolean authenticated) {
		StringBuilder result = new StringBuilder();
		result.append(fullUserName(talker, authenticated));
		result.append(" joined the live chat");
		result.append(convoTopics());
		
		return result.toString();
	}
	
}

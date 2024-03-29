package models.actions;

import models.TalkerBean;
import models.ConversationBean;
import models.actions.Action.ActionType;

import com.mongodb.DBObject;

/**
 * Occurs when talker follows a conversation 
 */
public class FollowConvoAction extends AbstractAction {
	
	public FollowConvoAction(TalkerBean talker, ConversationBean convo) {
		super(ActionType.FOLLOW_CONVO, talker);
		this.convo = convo;
	}
	public FollowConvoAction(DBObject dbObject) {
		super(dbObject);
	}
	
	protected boolean hasConvo() { return true; }

	public String toHTML(boolean authenticated) {
		StringBuilder result = new StringBuilder();
		result.append(fullUserName(talker, authenticated));
		result.append(" began following the conversation.");
		
		return result.toString();
	}
}

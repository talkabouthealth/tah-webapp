package models.actions;

import models.TalkerBean;
import models.ConversationBean;
import models.actions.Action.ActionType;

import com.mongodb.DBObject;

public class GiveThanksAction extends AbstractAction {
	
	public GiveThanksAction(TalkerBean talker, TalkerBean otherTalker) {
		super(ActionType.GIVE_THANKS, talker);
		this.otherTalker = otherTalker;
	}

	public GiveThanksAction(DBObject dbObject) {
		super(dbObject);
	}
	
	protected boolean hasOtherTalker() { return true; }

	public String toHTML(boolean authenticated) {
		StringBuilder result = new StringBuilder();
		result.append(fullUserName(talker, authenticated));
		result.append(" gave a 'Thank you' to ");
		result.append(fullUserName(otherTalker, authenticated));
		
		return result.toString();
	}
	
}

package models.actions;

import models.TalkerBean;
import models.TopicBean;

import com.mongodb.DBObject;

public class GiveThanksAction extends AbstractAction {
	
	public GiveThanksAction(TalkerBean talker, TalkerBean otherTalker) {
		super("GIVE_THANKS", talker);
		this.otherTalker = otherTalker;
	}

	public GiveThanksAction(DBObject dbObject) {
		super(dbObject);
	}
	
	protected boolean hasOtherTalker() { return true; }

	public String toHTML() {
		StringBuilder result = new StringBuilder();
		result.append(userName());
		result.append(" gave a 'Thank you' to ");
		result.append(otherTalker.getUserName());
		
		return result.toString();
	}
	
}

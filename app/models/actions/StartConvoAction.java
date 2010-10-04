package models.actions;

import org.bson.types.ObjectId;

import util.DBUtil;

import com.mongodb.DBObject;
import com.mongodb.DBRef;

import dao.TalkerDAO;
import dao.ConversationDAO;

import models.TalkerBean;
import models.ConversationBean;
import models.actions.Action.ActionType;

public class StartConvoAction extends AbstractAction {
	
	public StartConvoAction(TalkerBean talker, ConversationBean convo, ActionType type) {
		super(type, talker);
		this.convo = convo;
	}

	public StartConvoAction(DBObject dbObject) {
		super(dbObject);
	}
	
	protected boolean hasConvo() { return true; }

	public String toHTML() {
		StringBuilder result = new StringBuilder();
		result.append(fullUserName(talker));
		if (type == ActionType.START_CONVO) {
			result.append(" started the conversation ");
		}
		else if (type == ActionType.RESTART_CONVO) {
			result.append(" restarted the conversation ");
		}
		result.append(convoTopics());
		
		return result.toString();
	}
	
}

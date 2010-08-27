package models.actions;

import org.bson.types.ObjectId;

import util.DBUtil;

import com.mongodb.DBObject;
import com.mongodb.DBRef;

import dao.TalkerDAO;
import dao.ConversationDAO;

import models.TalkerBean;
import models.ConversationBean;

public class StartConvoAction extends AbstractAction {
	
	public StartConvoAction(TalkerBean talker, ConversationBean topic) {
		super("START_CONVO", talker);
		this.topic = topic;
	}

	public StartConvoAction(DBObject dbObject) {
		super(dbObject);
	}
	
	protected boolean hasTopic() { return true; }

	public String toHTML() {
		StringBuilder result = new StringBuilder();
		result.append(userName());
		result.append(" started the conversation: ");
		result.append(topicLink());
		
		return result.toString();
	}
	
}

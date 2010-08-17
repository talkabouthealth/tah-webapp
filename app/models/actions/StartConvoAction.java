package models.actions;

import org.bson.types.ObjectId;

import util.DBUtil;

import com.mongodb.DBObject;
import com.mongodb.DBRef;

import dao.TalkerDAO;
import dao.TopicDAO;

import models.TalkerBean;
import models.TopicBean;

public class StartConvoAction extends AbstractAction {
	
	public StartConvoAction(TalkerBean talker, TopicBean topic) {
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

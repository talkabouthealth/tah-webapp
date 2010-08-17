package models.actions;

import models.TalkerBean;
import models.TopicBean;

import com.mongodb.DBObject;

public class JoinConvoAction extends AbstractAction {
	
	public JoinConvoAction(TalkerBean talker, TopicBean topic) {
		super("JOIN_CONVO", talker);
		this.topic = topic;
	}

	public JoinConvoAction(DBObject dbObject) {
		super(dbObject);
	}
	
	protected boolean hasTopic() { return true; }

	public String toHTML() {
		StringBuilder result = new StringBuilder();
		result.append(userName());
		result.append(" joined the conversation: ");
		result.append(topicLink());
		
		return result.toString();
	}
	
}

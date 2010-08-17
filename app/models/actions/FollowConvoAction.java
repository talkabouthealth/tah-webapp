package models.actions;

import models.TalkerBean;
import models.TopicBean;

import com.mongodb.DBObject;

public class FollowConvoAction extends AbstractAction {
	
	public FollowConvoAction(TalkerBean talker, TopicBean topic) {
		super("FOLLOW_CONVO", talker);
		this.topic = topic;
	}

	public FollowConvoAction(DBObject dbObject) {
		super(dbObject);
	}
	
	protected boolean hasTopic() { return true; }

	public String toHTML() {
		StringBuilder result = new StringBuilder();
		result.append(userName());
		result.append(" began following: ");
		result.append(topicLink());
		
		return result.toString();
	}
}

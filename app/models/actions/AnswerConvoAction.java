package models.actions;

import models.TalkerBean;
import models.TopicBean;

import com.mongodb.DBObject;

//TODO: finish Convo actions
public class AnswerConvoAction extends AbstractAction {
	
	public AnswerConvoAction(TalkerBean talker, TopicBean topic) {
		super("ANSWER_CONVO", talker);
		this.topic = topic;
	}

	public AnswerConvoAction(DBObject dbObject) {
		super(dbObject);
	}
	
	protected boolean hasTopic() { return true; }

	public String toHTML() {
		StringBuilder result = new StringBuilder();
		result
			.append(userName())
			.append(" answered the conversation: ")
			.append(topicLink())
			.append("<br/>");
		
		return result.toString();
	}
}

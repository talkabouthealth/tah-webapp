package models.actions;

import models.ConversationBean;
import models.TalkerBean;
import models.TopicBean;
import models.actions.Action.ActionType;

import com.mongodb.DBObject;

public class TopicAddedAction extends AbstractAction {
	
	public TopicAddedAction(TalkerBean talker, ConversationBean convo, TopicBean topic) {
		super(ActionType.TOPIC_ADDED, talker);
		this.convo = convo;
		this.topic = topic;
	}

	public TopicAddedAction(DBObject dbObject) {
		super(dbObject);
	}
	
	protected boolean hasConvo() { return true; }
	protected boolean hasTopic() { return true; }

	public String toHTML(boolean authenticated) {
		StringBuilder result = new StringBuilder();
		
		//Conversation added to topic Breast Cancer
		result.append("Conversation added to topic ");
		result.append(topic());
		
		return result.toString();
	}
}

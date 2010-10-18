package models.actions;

import models.CommentBean;
import models.ConversationBean;
import models.TalkerBean;
import models.actions.Action.ActionType;

import com.mongodb.DBObject;

public class AnswerVotedAction extends AbstractAction {
	
	public AnswerVotedAction(TalkerBean talker, ConversationBean convo, 
			CommentBean answer) {
		super(ActionType.ANSWER_VOTED, talker);
		this.convo = convo;
		this.answer = answer;
	}

	public AnswerVotedAction(DBObject dbObject) {
		super(dbObject);
	}
	
	protected boolean hasConvo() { return true; }
	protected boolean hasAnswer() { return true; }

	public String toHTML(boolean authenticated) {
		StringBuilder result = new StringBuilder();
		result.append(fullUserName(talker, authenticated));
		result.append(" voted for answer by ");
		result.append(fullUserName(answer.getFromTalker(), authenticated));
		
		return result.toString();
	}

}

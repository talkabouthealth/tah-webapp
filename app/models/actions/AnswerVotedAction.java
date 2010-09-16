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

	public String toHTML() {
		StringBuilder result = new StringBuilder();
		result.append(userName());
		result.append(" voted for answer by ");
		result.append(answer.getFromTalker().getUserName()+" in ");
		result.append(convoLink());
		
		return result.toString();
	}

}
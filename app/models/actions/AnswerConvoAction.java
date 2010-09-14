package models.actions;

import models.CommentBean;
import models.TalkerBean;
import models.ConversationBean;

import com.mongodb.DBObject;

public class AnswerConvoAction extends AbstractAction {
	
	public AnswerConvoAction(TalkerBean talker, ConversationBean convo, 
			CommentBean answer, CommentBean reply, ActionType type) {
		super(type, talker);
		this.convo = convo;
		this.answer = answer;
		this.reply = reply;
	}

	public AnswerConvoAction(DBObject dbObject) {
		super(dbObject);
	}
	
	protected boolean hasConvo() { return true; }
	protected boolean hasAnswer() { return true; }
	protected boolean hasReply() { return true; }

	public String toHTML() {
		StringBuilder result = new StringBuilder();
		result.append(userName());
		if (type == ActionType.ANSWER_CONVO) {
			result.append(" answered the conversation: ");
			result.append(" (with "+answer.getText()+") ");
		}
		else if (type == ActionType.REPLY_CONVO) {
			result.append(" replied the conversation: ");
			result.append(" (with "+answer.getText()+", "+reply.getText()+") ");
		}
		result.append(convoLink());
		result.append("<br/>");
		
		return result.toString();
	}
}

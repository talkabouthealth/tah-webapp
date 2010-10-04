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
		if (type == ActionType.ANSWER_CONVO) {
			result.append("New answer by ");
			result.append(fullUserName(talker));
			result.append(convoTopics());
		}
		else if (type == ActionType.REPLY_CONVO) {
			result.append(fullUserName(talker));
			result.append(" replied to answer by ");
			result.append(fullUserName(answer.getFromTalker()));
		}
		
		return result.toString();
	}
}

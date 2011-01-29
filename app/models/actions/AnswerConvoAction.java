package models.actions;

import play.Play;
import models.CommentBean;
import models.TalkerBean;
import models.ConversationBean;

import com.mongodb.DBObject;

/**
 * Occurs when talker answers/replies to a conversation 
 */
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

	public String toHTML(boolean authenticated) {
		StringBuilder result = new StringBuilder();
		if (type == ActionType.ANSWER_CONVO) {
			result.append("New answer by ");
			result.append(fullUserName(talker, authenticated));
			result.append(convoTopics());
		}
		else if (type == ActionType.REPLY_CONVO) {
			result.append(fullUserName(talker, authenticated));
			result.append(" replied to answer by ");
			result.append(fullUserName(answer.getFromTalker(), authenticated));
		}
		
		return result.toString();
	}
}

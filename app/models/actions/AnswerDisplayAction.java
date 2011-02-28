package models.actions;

import models.CommentBean;
import models.ConversationBean;
import models.TalkerBean;
import models.ConversationBean.ConvoType;
import models.actions.Action.ActionType;

import com.mongodb.DBObject;

/**
 * Special action for displaying answers on "Answers" page
 *
 */
public class AnswerDisplayAction extends AbstractAction {
	
	private boolean showTopAnswer;
	
	
	public AnswerDisplayAction() {
		super(null, null);
	}

	public AnswerDisplayAction(TalkerBean talker, ConversationBean convo, 
			CommentBean answer, ActionType type, boolean showTopAnswer) {
		super(type, talker);
		this.convo = convo;
		this.answer = answer;
		this.showTopAnswer = showTopAnswer;
	}
	
	//Question by: [talker] in topic [topics]
	public String toHTML(boolean authenticated) {
		StringBuilder result = new StringBuilder();
		
		if (showTopAnswer) {
			result.append("Top Answer by: ");
		}
		else {
			if (convo.getConvoType() == ConvoType.CONVERSATION) {
				result.append("Live chat by: ");
			}
			else {
				result.append("Question by: ");
			}
		}
		
		result.append(fullUserName(talker, authenticated)+" ");		
		result.append(convoTopics());
		
		return result.toString();
	}
	
}

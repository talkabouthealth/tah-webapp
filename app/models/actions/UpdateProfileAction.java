package models.actions;

import models.TalkerBean;

import com.mongodb.DBObject;

/**
 * Occurs when talker updates bio/profile/health info
 */
public class UpdateProfileAction extends AbstractAction {
	
	public UpdateProfileAction(TalkerBean talker, ActionType type) {
		super(type, talker);
	}
	public UpdateProfileAction(DBObject dbObject) {
		super(dbObject);
	}
	
	public String toHTML(boolean authenticated) {
		StringBuilder result = new StringBuilder();
		result.append(fullUserName(talker, authenticated));
		result.append(" updated ");
		if (type == ActionType.UPDATE_BIO) {
			result.append("Bio");
		}
		else if (type == ActionType.UPDATE_PERSONAL) {
			result.append("Personal Details");
		}
		else if (type == ActionType.UPDATE_HEALTH) {
			result.append("Health Details");
		}
		
		return result.toString();
	}
	
}

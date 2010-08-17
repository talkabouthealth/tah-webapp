package models.actions;

import models.TalkerBean;

import com.mongodb.DBObject;

public class UpdateProfileAction extends AbstractAction {
	
	public UpdateProfileAction(TalkerBean talker, String type) {
		super("UPDATE_"+type, talker);
	}

	public UpdateProfileAction(DBObject dbObject) {
		super(dbObject);
	}
	
	public String toHTML() {
		StringBuilder result = new StringBuilder();
		result.append(userName());
		result.append(" updated ");
		if (type.equals("UPDATE_BIO")) {
			result.append("Bio");
		}
		else if (type.equals("UPDATE_PERSONAL")) {
			result.append("Personal Details");
		}
		else if (type.equals("UPDATE_HEALTH")) {
			result.append("Health Details");
		}
		
		return result.toString();
	}
	
}

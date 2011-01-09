package models.actions;

import java.util.Date;

import models.ConversationBean;
import models.TalkerBean;

import com.mongodb.DBObject;


/**
 * Main interface for actions that used in the feeds.
 *
 */
public interface Action {
	
	public enum ActionType {
		//convo-related
		START_CONVO,
		RESTART_CONVO,
		JOIN_CONVO,
		
		ANSWER_CONVO,
		REPLY_CONVO,
		
		ANSWER_VOTED,
		
		SUMMARY_ADDED,
		SUMMARY_EDITED,
		
		TOPIC_ADDED,
		
		//user-related
		GIVE_THANKS,
		FOLLOW_CONVO,
		FOLLOW_TALKER,
		PERSONAL_PROFILE_COMMENT,
		PERSONAL_PROFILE_REPLY,
		
		UPDATE_BIO,
		UPDATE_PERSONAL,
		UPDATE_HEALTH		
	}
	
	//convert to db
	public DBObject toDBObject();
	
	//get diferent action info
	public Date getTime();
	public ActionType getType();
	public ConversationBean getConvo();
	public TalkerBean getTalker();
	public String getId();

}

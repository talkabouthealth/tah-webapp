package models.actions;

import java.util.Date;

import models.ConversationBean;
import models.TalkerBean;

import com.mongodb.DBObject;


// http://www.pivotaltracker.com/story/show/4473910
/*

Conversation Actions (UI not designed yet, but will be added to User's main conversation page):
-"Just diagnosed, what do I do now?" - murray answered:
"Get a second opinion."
-"Just diagnosed, what do I do now?" - murray restarted the conversation
- "Just diagnosed, what do I do now?" - murray edited the Summary
- "Just diagnosed, what do I do now?" - murray replied to an answer
"Murray Get a second opinion."
"Kan I agree, second opinion is always wise. It is always good to ... more"

User Actions (will appear on User's "Public Profile"):
-murray started the conversation: "Just diagnosed, what do I do now?"
-murray joined the conversation: "Just diagnosed, what do I do now?"
-murray gave a 'Thank you' to Kan
-murray answered the conversation: "Just diagnosed, what do I do now?"
"Murray - Get a second opinion. It is always ... more"
-murray replied to: "Just diagnosed, what do I do now?"
"Murray - I agree, second opinion is always wise."
-murray edited the summary for: "Just diagnosed, what do I do now?"
-murray began following: "Just diagnosed, what do I do now?"
-murray began following Kan
-murray left a comment for Kan
-murray replied to a comment for Kan
-murray updated Bio
-murray updated Personal Details
-murray updated Health Details
-murray voted for an answer by Kan on the conversation - "Just diagnosed, what do I do now?"

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
	
	public DBObject toDBObject();
	
	public Date getTime();
	
	public ActionType getType();
	
	public ConversationBean getConvo();
	
	public TalkerBean getTalker();
	
	public String getId();

}

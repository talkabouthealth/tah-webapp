package models.actions;

import java.util.Date;

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
	
	public DBObject toDBObject();
	
	public Date getTime();
	
	public String getType();

}

package models.actions;

import java.util.Date;

import play.Logger;

import models.ConversationBean;
import models.TalkerBean;
import models.actions.Action.ActionType;

import com.mongodb.DBObject;
import com.mongodb.DBRef;

import dao.ActionDAO;
import dao.CommentsDAO;

/**
 * Used for pre-loading actions before filtering - saves memory resources and loading time
 */
public class PreloadAction implements Action {
	
	protected String id;
	protected ConversationBean convo;
	protected ActionType type;
	protected DBObject dbObject;
	protected Date time;
	protected TalkerBean talker;

	public PreloadAction(DBObject dbObject) {
		this.dbObject = dbObject;
		
		setId(dbObject.get("_id").toString());
		setType(ActionType.valueOf((String)dbObject.get("type")));
		
		//we need to preload this info for checking duplicate conversations during filtering
		DBRef convoRef = (DBRef)dbObject.get("convoId");
		if (convoRef != null) {
			ConversationBean convo = new ConversationBean();
			convo.setId(convoRef.getId().toString());
			setConvo(convo);
		}
		DBRef talkerRef = (DBRef)dbObject.get("uid");
		
		if(talkerRef != null) {
			TalkerBean tempTalker = new TalkerBean();
			tempTalker.setId(talkerRef.getId().toString());
			talker = tempTalker;
		}
	}
	
	public void setTalker(TalkerBean talker) {
		this.talker = talker;
	}

	/**
	 * Loads full action information
	 * @return
	 */
	public Action getFullAction() {
//		long start = System.currentTimeMillis();
		Action action = ActionDAO.actionFromDB(dbObject);
//		System.out.println(action.getClass().toString()+" : "+(System.currentTimeMillis()-start));
		return action;
	}
	

	@Override
	public DBObject toDBObject() {
		return null;
	}
	
	@Override
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	@Override
	public Date getTime() { return null; }

	@Override
	public ActionType getType() { return type; }
	public void setType(ActionType type) { this.type = type; }

	@Override
	public ConversationBean getConvo() { return convo; }
	public void setConvo(ConversationBean convo) { this.convo = convo; }
	
	@Override
	public TalkerBean getTalker() { return talker; }

	@Override
	public void setID(String id) {
		this.id=id;
	}

	@Override
	public void setTime(Date time) {
		this.time = time;		
	}


}

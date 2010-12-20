package models.actions;

import java.util.Date;

import models.ConversationBean;
import models.TalkerBean;

import com.mongodb.DBObject;
import com.mongodb.DBRef;

import dao.ActionDAO;
import dao.CommentsDAO;

/**
 * TODO: make better description?
 * Used for pre-loading actions before filtering
 *
 */
public class PreloadAction implements Action {
	
	protected String id;
	protected ConversationBean convo;
	protected DBObject dbObject;

	public PreloadAction(DBObject dbObject) {
		this.dbObject = dbObject;
		
		setId(dbObject.get("_id").toString());
		
		DBRef convoRef = (DBRef)dbObject.get("convoId");
		if (convoRef != null) {
			ConversationBean convo = new ConversationBean();
			convo.setId(convoRef.getId().toString());
			setConvo(convo);
		}
	}

	@Override
	public DBObject toDBObject() {
		return null;
	}

	@Override
	public Date getTime() {
		return null;
	}

	@Override
	public ActionType getType() {
		return null;
	}

	@Override
	public ConversationBean getConvo() {
		return convo;
	}
	
	@Override
	public TalkerBean getTalker() {
		return null;
	}

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setConvo(ConversationBean convo) {
		this.convo = convo;
	}

	public Action getFullAction() {
		Action action = ActionDAO.actionFromDB(dbObject);
		return action;
	}

}

package models;

import static util.DBUtil.getBoolean;
import static util.DBUtil.getInt;
import static util.DBUtil.getStringList;
import static util.DBUtil.getStringSet;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.swing.text.StyledEditorKit.BoldAction;

import org.apache.commons.collections.comparators.ComparableComparator;

import logic.TalkerLogic;

import com.mongodb.DBObject;

public class MessageBean implements Comparable{
	
	private static DateFormat format = new SimpleDateFormat("MMM,dd, hh:mm aaa");
	protected String id;
	//author of the message
	private TalkerBean fromTalker;
	private TalkerBean toTalker;
	private List<String>toTalers;
	public List<String> getToTalers() {
		return toTalers;
	}
	public void setToTalers(List<String> toTalers) {
		this.toTalers = toTalers;
	}
	private String text;
	private Date time;
	private String subject;
	private String fromTalkerId;
	private String toTalkerId;
	private String displayDate;
	private String displayMessage;
	private String rootId;
	private boolean readFlag;
	private boolean deleteFlag;
	private boolean archieveFlag;
	private boolean readFlagSender;
	private boolean deleteFlagSender;
	private boolean archieveFlagSender;
	private boolean replied;
	public String getDummyId() {
		return dummyId;
	}
	public void setDummyId(String dummyId) {
		this.dummyId = dummyId;
	}
	private String dummyId;
	//index in the 'messages' array, used for updating
	private int index;
	
	public boolean isReplied() {
		return replied;
	}
	public void setReplied(boolean replied) {
		this.replied = replied;
	}
	public MessageBean() {}
	public MessageBean(String messageId) {
		id = messageId;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MessageBean)) {
			return false;
		}
		
		MessageBean other = (MessageBean)obj;
		return id.equals(other.id);
	}
	
	@Override
	public int hashCode() {
		if (id == null) {
			return 47;
		}
		return id.hashCode();
	}
	
	
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	
	public TalkerBean getFromTalker() { return fromTalker; }
	public void setFromTalker(TalkerBean fromTalker) { this.fromTalker = fromTalker; }

	public TalkerBean getToTalker() {return toTalker;}
	public void setToTalker(TalkerBean toTalker) {this.toTalker = toTalker;	}
	
	public String getText() { return text; }
	public void setText(String text) { this.text = text; }

	public Date getTime() { return time; }
	public void setTime(Date time) { this.time = time; }

	public int getIndex() { return index; }
	public void setIndex(int index) { this.index = index; }
	
	public String getSubject() { return subject; }
	public void setSubject(String subject) { this.subject = subject; }
	
	public String getFromTalkerId() { return fromTalkerId; }
	public void setFromTalkerId(String fromTalkerId) { this.fromTalkerId = fromTalkerId; }
	
	public String getToTalkerId() { return toTalkerId; }
	public void setToTalkerId(String toTalkerId) { this.toTalkerId = toTalkerId; }
	
	public String getDisplayDate() {
		return format.format(getTime()); 
	}
	public void setDisplayDate(String displayDate) { this.displayDate = displayDate; }
	
	public String getDisplayMessage() {
		if(displayMessage != null && displayMessage.length() > 50){
			return displayMessage.substring(0, 50) + "...";
		}
		return displayMessage; 
	}
	public void setDisplayMessage(String displayMessage) {
		this.displayMessage = displayMessage; 
	}
	
	public String getRootId() {	return rootId; }
	public void setRootId(String rootId) { this.rootId = rootId; }
	
	public boolean isReadFlag() { return readFlag; }
	public void setReadFlag(boolean readFlag) { this.readFlag = readFlag; }
	
	public boolean isDeleteFlag() { return deleteFlag; }
	public void setDeleteFlag(boolean deleteFlag) { this.deleteFlag = deleteFlag; }
	
	public boolean isArchieveFlag() { return archieveFlag; }
	public void setArchieveFlag(boolean archieveFlag) { this.archieveFlag = archieveFlag; }
	
	public boolean isReadFlagSender() { return readFlagSender; }
	public void setReadFlagSender(boolean readFlagSender) { this.readFlagSender = readFlagSender; }
	
	public boolean isDeleteFlagSender() { return deleteFlagSender; }
	public void setDeleteFlagSender(boolean deleteFlagSender) { this.deleteFlagSender = deleteFlagSender; }
	
	public boolean isArchieveFlagSender() { return archieveFlagSender; }
	public void setArchieveFlagSender(boolean archieveFlagSender) { this.archieveFlagSender = archieveFlagSender; }
	
	public void parseFromDB(DBObject messageDBObject) {
		setId(messageDBObject.get("_id").toString());
		setSubject((String)messageDBObject.get("subject"));
		setText((String)messageDBObject.get("message"));
		setFromTalkerId(TalkerLogic.loadTalkerFromCache(messageDBObject, "fromTalker").getId());
		setToTalkerId(TalkerLogic.loadTalkerFromCache(messageDBObject, "toTalker").getId());
		setRootId((String)messageDBObject.get("rootid"));
		setTime((Date)messageDBObject.get("time"));
		setReadFlag(getBoolean(messageDBObject, "read_flag"));
		setDeleteFlag(getBoolean(messageDBObject, "delete_flag"));
		setArchieveFlag(getBoolean(messageDBObject, "archieve_flag"));
		setReadFlagSender(getBoolean(messageDBObject, "read_flag_sender"));
		setDeleteFlagSender(getBoolean(messageDBObject, "delete_flag_sender"));
		setArchieveFlagSender(getBoolean(messageDBObject, "archieve_flag_sender"));
		setReplied(getBoolean(messageDBObject, "replied"));
		setFromTalker(TalkerLogic.loadTalkerFromCache(messageDBObject, "fromTalker"));
		setToTalker(TalkerLogic.loadTalkerFromCache(messageDBObject, "toTalker"));
		setDummyId((String)messageDBObject.get("dummyid"));
	}
	@Override
	public int compareTo(Object arg0) {
		// TODO Auto-generated method stub
		return 0;
	}
}

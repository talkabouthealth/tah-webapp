package models;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LiveConversationBean {
	private TopicBean topic;
	private Map<String, TalkerBean> mTalkers = new HashMap<String, TalkerBean>();
	private Date StartTime = new Date();

	public LiveConversationBean() {
	}

	public void setTalkers(Map<String, TalkerBean> mTalkers) {
		this.mTalkers = mTalkers;
	}

	public Map<String, TalkerBean> getTalkers() {
		return mTalkers;
	}

	public void removeTalker(String tID) {
		mTalkers.remove(tID);
	}

	public void addTalker(String tID, TalkerBean c) {
		mTalkers.put(tID, c);
	}

	public TalkerBean getConversationMatch(String tID) {
		return mTalkers.get(tID);
	}

	public void setTopic(TopicBean topic) {
		this.topic = topic;
	}

	public TopicBean getTopic() {
		return topic;
	}

	public void setStartTime(Date startTime) {
		StartTime = startTime;
	}

	public Date getStartTime() {
		return StartTime;
	}

}

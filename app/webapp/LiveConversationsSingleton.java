package webapp;

import java.util.HashMap;
import java.util.Map;

import models.LiveConversationBean;

public class LiveConversationsSingleton {

	private static LiveConversationsSingleton liveConversations = new LiveConversationsSingleton();

	private Map<String, LiveConversationBean> mLiveConversations = new HashMap<String, LiveConversationBean>();

	private LiveConversationsSingleton() {}

	public static LiveConversationsSingleton getReference() {
		return liveConversations;
	}

	public void removeConversation(String tID) {
		mLiveConversations.remove(tID);
	}

	public void addConversation(String tID, LiveConversationBean c) {
		mLiveConversations.put(tID, c);
	}

	public LiveConversationBean getConversationMatch(String tID) {
		return mLiveConversations.get(tID);
	}

	public Map<String, LiveConversationBean> getLiveConversationMap() {
		return mLiveConversations;
	}
}

package models;

import static util.DBUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import dao.TalkerDAO;

/**
 * @author Osezno
 *
 */
public class ConversationBean {
	
	public enum ConvoType {
		CONVERSATION,
		QUESTION;
		
		public String stringValue() {
			return toString().toLowerCase();
		}
	}
	
	private String id;
	private int tid;
	//TODO: handle questions also
	private ConvoType convoType = ConvoType.CONVERSATION;
	private String mainURL;
	private Set<String> urls;
	private String topic;
	private Date creationDate;
	private Date displayTime;	//TODO: old field? how to use it?
	
	//Convo with no answers is opened
	private boolean opened;
	
	//creator
	private String uid;
	private TalkerBean talker;
	
	//TODO: Set or List?
	private String details;
	private int views;
	
	private String summary;
	private Set<TalkerBean> sumContributors;
	
	private Set<String> members;
	private List<CommentBean> comments;
	private List<MessageBean> messages;
	private List<TalkerBean> followers;
	
	private List<TopicBean> topics;
	
	public ConversationBean() {}
	
	public ConversationBean(String id) {
		this.id = id;
	}
	
	
	public void parseBasicFromDB(DBObject convoDBObject) {
		setId(convoDBObject.get("_id").toString());
    	setTid((Integer)convoDBObject.get("tid"));
    	setTopic((String)convoDBObject.get("topic"));
    	setCreationDate((Date)convoDBObject.get("cr_date"));
    	setDisplayTime((Date)convoDBObject.get("disp_date"));
    	setDetails((String)convoDBObject.get("details"));
    	
    	setOpened(getBoolean(convoDBObject, "opened"));
    	
    	setSummary((String)convoDBObject.get("summary"));
    	parseSumContributors((Collection<DBRef>)convoDBObject.get("sum_authors"));
    	
    	setMainURL((String)convoDBObject.get("main_url"));
    	setUrls(getStringSet(convoDBObject, "urls"));
    	
    	setViews(getInt(convoDBObject, "views"));
    	
    	parseTopics((Collection<DBRef>)convoDBObject.get("topics"));
    	
    	//author
    	DBObject talkerDBObject = ((DBRef)convoDBObject.get("uid")).fetch();
    	TalkerBean talker = new TalkerBean();
    	talker.parseBasicFromDB(talkerDBObject);
    	setTalker(talker);
    	
    	//topics(tags)
    	
	}
	
	private void parseSumContributors(Collection<DBRef> contributorsDBList) {
		sumContributors = new HashSet<TalkerBean>();
		if (contributorsDBList != null) {
			for (DBRef talkerDBRef : contributorsDBList) {
				TalkerBean talker = new TalkerBean();
		    	talker.parseBasicFromDB(talkerDBRef.fetch());
		    	sumContributors.add(talker);
			}
		}
	}
	
	private void parseTopics(Collection<DBRef> topicsDBList) {
		topics = new ArrayList<TopicBean>();
		if (topicsDBList != null) {
			for (DBRef topicDBRef : topicsDBList) {
				TopicBean topic = new TopicBean();
				topic.parseBasicFromDB(topicDBRef.fetch());
				topics.add(topic);
			}
		}
	}

	public void parseFromDB(DBObject convoDBObject) {
		parseBasicFromDB(convoDBObject);

    	//messages from Talk Window
    	List<MessageBean> messages = new ArrayList<MessageBean>();
    	Set<String> members = new HashSet<String>();
    	Collection<DBObject> messagesDBList = (Collection<DBObject>)convoDBObject.get("messages");
    	if (messagesDBList != null) {
    		for (DBObject messageDBObject : messagesDBList) {
    			MessageBean message = new MessageBean();
    			message.setText((String)messageDBObject.get("text"));
    			
    			DBObject fromTalkerDBObject = ((DBRef)messageDBObject.get("uid")).fetch();
    			TalkerBean fromTalker = 
    				new TalkerBean(fromTalkerDBObject.get("_id").toString(), (String)fromTalkerDBObject.get("uname"));
    			message.setFromTalker(fromTalker);
    			
    			members.add(fromTalker.getUserName());
    			messages.add(message);
    		}
    	}
    	setMembers(members);
    	setMessages(messages);
    	
    	//followers of this convo
    	DBCollection talkersColl = getCollection(TalkerDAO.TALKERS_COLLECTION);
    	DBObject query = new BasicDBObject("following_topics", getId());
    	DBObject fields = BasicDBObjectBuilder.start()
    		.add("uname", 1)
    		.add("email", 1)
    		.add("bio", 1)
    		.add("email_settings", 1)
    		.get();
    	List<DBObject> followersDBList = talkersColl.find(query, fields).toArray();
    	List<TalkerBean> followers = new ArrayList<TalkerBean>();
    	for (DBObject followerDBObject : followersDBList) {
    		TalkerBean followerTalker = new TalkerBean();
    		followerTalker.parseBasicFromDB(followerDBObject);
			followers.add(followerTalker);
    	}
    	setFollowers(followers);
	}
	
	
	
	
	public String getMainURL() {
		return mainURL;
	}
	public void setMainURL(String mainURL) { this.mainURL = mainURL; }

	public String getDetails() { return details; }
	public void setDetails(String details) { this.details = details; }

	public String getSummary() { return summary; }
	public void setSummary(String summary) { this.summary = summary; }

	public String getTopic() { return topic; }
	public void setTopic(String topic) { this.topic = topic; }
	
	public Date getCreationDate() { return creationDate; }
	public void setCreationDate(Date creationDate) { this.creationDate = creationDate; }
	
	public void setDisplayTime(Date displayTime) { this.displayTime = displayTime; }
	public Date getDisplayTime() { return displayTime; }

	public String getUid() { return uid; }
	public void setUid(String uid) { this.uid = uid; }

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public TalkerBean getTalker() { return talker; }
	public void setTalker(TalkerBean talker) { this.talker = talker; }

	public Set<String> getMembers() { return members; }
	public void setMembers(Set<String> members) { this.members = members; }

	public List<MessageBean> getMessages() { return messages; }
	public void setMessages(List<MessageBean> messages) { this.messages = messages; }
	
	public Set<TalkerBean> getSumContributors() {
		return sumContributors;
	}

	public void setSumContributors(Set<TalkerBean> sumContributors) {
		this.sumContributors = sumContributors;
	}

	public int getViews() { return views; }
	public void setViews(int views) { this.views = views; }
	
	public List<CommentBean> getComments() { return comments; }
	public void setComments(List<CommentBean> comments) { this.comments = comments; }
	
	public List<TalkerBean> getFollowers() { return followers; }
	public void setFollowers(List<TalkerBean> followers) { this.followers = followers; }
	
	public int getTid() { return tid; }
	public void setTid(int tid) { this.tid = tid; }

	public Set<String> getUrls() {
		return urls;
	}

	public void setUrls(Set<String> urls) {
		this.urls = urls;
	}

	public List<TopicBean> getTopics() {
		return topics;
	}

	public void setTopics(List<TopicBean> topics) {
		this.topics = topics;
	}

	public ConvoType getConvoType() {
		return convoType;
	}

	public void setConvoType(ConvoType convoType) {
		this.convoType = convoType;
	}

	public boolean isOpened() {
		return opened;
	}

	public void setOpened(boolean opened) {
		this.opened = opened;
	}
}

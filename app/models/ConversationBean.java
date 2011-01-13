package models;

import static util.DBUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import play.Logger;

import util.CommonUtil;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import dao.ConversationDAO;
import dao.TalkerDAO;
import dao.TopicDAO;

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
	private ConvoType convoType = ConvoType.CONVERSATION;
	private boolean deleted;
	
	//title (name) of this conversation
	private String topic;
	private String mainURL;
	//includes old titles and urls
	private Set<URLName> oldNames;
	
	//Short urls for ConvoSummary and Chat
	private String bitly;
	private String bitlyChat;
	
	private Date creationDate;
	//Convo with no answers is opened
	private boolean opened;
	
	//creator
	private String uid;
	private TalkerBean talker;
	
	private String details;
	private int views;
	private String summary;
	private Set<TalkerBean> sumContributors;
	
	//number of online people in the chat
	private int numOfChatters;
	
	private Set<ConversationBean> relatedConvos;
	private Set<String> members;
	private List<CommentBean> comments;
	private List<MessageBean> messages;
	private List<TalkerBean> followers;
	private Set<TopicBean> topics;
	
	//used for search display
	private String searchFragment;
	
	public ConversationBean() {}
	
	public ConversationBean(String id) {
		this.id = id;
	}
	
	public void parseSuperBasicFromDB(DBObject convoDBObject) {
		setId(convoDBObject.get("_id").toString());
    	setTid((Integer)convoDBObject.get("tid"));
    	setTopic((String)convoDBObject.get("topic"));
    	setCreationDate((Date)convoDBObject.get("cr_date"));
    	setDetails((String)convoDBObject.get("details"));
    	setBitly((String)convoDBObject.get("bitly"));
    	setBitlyChat((String)convoDBObject.get("bitly_chat"));
    	setDeleted(getBoolean(convoDBObject, "deleted"));
    	setMainURL((String)convoDBObject.get("main_url"));
    	
    	String type = (String)convoDBObject.get("type");
    	if (type != null) {
    		convoType = ConvoType.valueOf(type);
    	}
    	
    	//online talkers
    	BasicDBList talkersList = (BasicDBList)convoDBObject.get("talkers");
    	if (talkersList != null) {
    		numOfChatters = talkersList.size();
    	}
    	
    	//topics(tags)
    	parseTopics((Collection<DBRef>)convoDBObject.get("topics"));  
	}
	
	public void parseBasicFromDB(DBObject convoDBObject) {
		parseSuperBasicFromDB(convoDBObject);
    	
    	setOpened(getBoolean(convoDBObject, "opened"));
    	setOldNames(parseSet(URLName.class, convoDBObject, "old_names"));
    	setViews(getInt(convoDBObject, "views"));
    	
    	setSummary((String)convoDBObject.get("summary"));
    	parseSumContributors((Collection<DBRef>)convoDBObject.get("sum_authors"));
    	
    	//author
    	setTalker(parseTalker(convoDBObject, "uid"));
	}
	
	public void parseFromDB(DBObject convoDBObject) {
		parseBasicFromDB(convoDBObject);
		
		parseRelatedConvos((Collection<DBRef>)convoDBObject.get("related_convos"));

    	//messages from Talk Window
    	List<MessageBean> messages = new ArrayList<MessageBean>();
    	Set<String> members = new HashSet<String>();
    	Collection<DBObject> messagesDBList = (Collection<DBObject>)convoDBObject.get("messages");
    	if (messagesDBList != null) {
    		int cnt = -1;
    		for (DBObject messageDBObject : messagesDBList) {
    			cnt++;
    			
    			boolean isDeleted = getBoolean(messageDBObject, "deleted");
    			if (isDeleted) {
    				continue;
    			}
    			
    			MessageBean message = new MessageBean();
    			message.setText((String)messageDBObject.get("text"));
    			message.setIndex(cnt);
    			
    			DBObject fromTalkerDBObject = ((DBRef)messageDBObject.get("uid")).fetch();
    			if (fromTalkerDBObject != null) {
    				TalkerBean fromTalker = 
        				new TalkerBean(fromTalkerDBObject.get("_id").toString(), (String)fromTalkerDBObject.get("uname"));
        			message.setFromTalker(fromTalker);
        			members.add(fromTalker.getUserName());
        			
        			messages.add(message);
    			}
    			else {
    				//TODO: remove bad conversation 
    				//{ "_id" : ObjectId("4cd828541a98b19b4ec451e2"), "main_url" : "best-tips-on-handling-kids-with-adhd" }
    				//Logger.error("NULL talker in conversation message: "+getId()+", index: "+message.getIndex());
    			}
    		}
    	}
    	setMembers(members);
    	setMessages(messages);
    	
    	//followers of this convo
    	DBCollection talkersColl = getCollection(TalkerDAO.TALKERS_COLLECTION);
    	DBObject query = new BasicDBObject("following_convos", getId());
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
	
	//TODO: 3 same methods: Col<DBRef> -> Set<Object>
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
		topics = new HashSet<TopicBean>();
		if (topicsDBList != null) {
			for (DBRef topicDBRef : topicsDBList) {
				TopicBean topic = new TopicBean();
				topic.parseSuperBasicFromDB(topicDBRef.fetch());
				
				if (topic.getId() == null) {
					//maybe deleted topic
					continue;
				}
				
				topics.add(topic);
			}
		}
	}
	
	private void parseRelatedConvos(Collection<DBRef> relatedDBList) {
		relatedConvos = new HashSet<ConversationBean>();
		if (relatedDBList != null) {
			for (DBRef convoDBRef : relatedDBList) {
				ConversationBean convo = new ConversationBean();
				convo.parseBasicFromDB(convoDBRef.fetch());
				relatedConvos.add(convo);
			}
		}
	}
	
	public List<DBRef> topicsToDB() {
		List<DBRef> topicsDBList = new ArrayList<DBRef>();
		for (TopicBean topic : getTopics()) {
			DBRef topicRef = createRef(TopicDAO.TOPICS_COLLECTION, topic.getId());
			topicsDBList.add(topicRef);
		}
		return topicsDBList;
	}
	
	public Set<DBRef> relatedConvosToDB() {
		Set<DBRef> relatedDBList = new HashSet<DBRef>();
		if (getRelatedConvos() == null) {
			return relatedDBList;
		}
		for (ConversationBean convo : getRelatedConvos()) {
			DBRef topicRef = createRef(ConversationDAO.CONVERSATIONS_COLLECTION, convo.getId());
			relatedDBList.add(topicRef);
		}
		return relatedDBList;
	}
	
	
	public URLName getOldNameByTitle(String title) {
		for (URLName oldName : getOldNames()) {
			if (oldName.getTitle().equals(title)) {
				return oldName;
			}
		}
		return null;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ConversationBean)) {
			return false;
		}
		
		ConversationBean other = (ConversationBean)obj;
		return id.equals(other.id);
	}
	
	@Override
	public int hashCode() {
		if (id == null) {
			return 47;
		}
		return id.hashCode();
	}
	
	//for displaying (e.g. in OpenQuestions)
	public String getHtmlDetails(boolean authenticated) {
		StringBuilder htmlDetails = new StringBuilder();
		if (convoType == ConvoType.CONVERSATION) {
			htmlDetails.append("Live chat by ");
		}
		else {
			htmlDetails.append("Question by ");
		}
		htmlDetails.append(CommonUtil.talkerToHTML(talker, authenticated)+" ");
		htmlDetails.append(CommonUtil.topicsToHTML(this));
		
		return htmlDetails.toString();
	}
	
	public List<CommentBean> getNotHelpfulAnswers() {
		return filterAnswers(true);
	}
	
	public List<CommentBean> getHelpfulAnswers() {
		return filterAnswers(false);
	}
	
	public List<CommentBean> filterAnswers(boolean notHelpful) {
		List<CommentBean> filteredAnswers = new ArrayList<CommentBean>();
		for (CommentBean answer : getComments()) {
			if ((notHelpful && answer.isNotHelpful())
					|| (!notHelpful && !answer.isNotHelpful())) {
				filteredAnswers.add(answer);
			}
		}
		
		return filteredAnswers;
	}
	
	public boolean hasUserAnswer(TalkerBean talker) {
		if (comments == null || talker == null) {
			return false;
		}
		for (CommentBean comment : comments) {
			if (talker.equals(comment.getFromTalker())) {
				return true;
			}
		}
		return false;
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

	public Set<URLName> getOldNames() {
		return oldNames;
	}

	public void setOldNames(Set<URLName> oldNames) {
		this.oldNames = oldNames;
	}

	public Set<TopicBean> getTopics() {
		return topics;
	}

	public void setTopics(Set<TopicBean> topics) {
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

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public Set<ConversationBean> getRelatedConvos() {
		return relatedConvos;
	}

	public void setRelatedConvos(Set<ConversationBean> relatedConvos) {
		this.relatedConvos = relatedConvos;
	}

	public String getSearchFragment() {
		return searchFragment;
	}

	public void setSearchFragment(String searchFragment) {
		this.searchFragment = searchFragment;
	}

	public String getBitly() {
		return bitly;
	}

	public void setBitly(String bitly) {
		this.bitly = bitly;
	}

	public String getBitlyChat() {
		return bitlyChat;
	}

	public void setBitlyChat(String bitlyChat) {
		this.bitlyChat = bitlyChat;
	}

	public int getNumOfChatters() {
		return numOfChatters;
	}

	public void setNumOfChatters(int numOfChatters) {
		this.numOfChatters = numOfChatters;
	}
	
}

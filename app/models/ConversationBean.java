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
import util.TemplateExtensions;
import util.TwitterUtil;

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
		//Live Chat/Live Talk
		CONVERSATION,
		QUESTION;
		
		public String stringValue() {
			return toString().toLowerCase();
		}
	}
	
	private String id;
	//increment id, used for Live Chat page
	private int tid;
	private ConvoType convoType = ConvoType.CONVERSATION;
	
	//title (name) of this conversation
	private String topic;
	private String mainURL;
	//includes old titles and urls
	private Set<URLName> oldNames;
	
	//id of the conversation where this convo was merged
	private String mergedWith;
	
	//Where convo was created (e.g. 'twitter')
	private String from;
	//Id related to creation source (e.g. id of tweet)
	private String fromId;
	
	//Short urls for ConvoSummary and Chat
	private String bitly;
	private String bitlyChat;
	
	private Date creationDate;
	//Convo with no answers is opened
	private boolean opened;
	private boolean deleted;
	
	//creator
	private String uid;
	private TalkerBean talker;
	
	private String details;
	private int views;
	private Set<TopicBean> topics;
	private String summary;
	private Set<TalkerBean> sumContributors;
	
	//number of online people in the chat
	private int numOfChatters;
	
	private Set<ConversationBean> relatedConvos;
	private Set<ConversationBean> followupConvos;
	//answers/replies
	private List<CommentBean> comments;
	private List<CommentBean> replies;
	private List<TalkerBean> followers;
	
	//members and message from Live Chat
	private Set<String> members;
	private List<MessageBean> messages;
	
	//used for search display
	private String searchFragment;
	
	public ConversationBean() {}
	public ConversationBean(String id) {
		this.id = id;
	}
	
	public void parseBasicFromDB(DBObject convoDBObject) {
		setId(convoDBObject.get("_id").toString());
    	setTid((Integer)convoDBObject.get("tid"));
    	setTopic((String)convoDBObject.get("topic"));
    	setCreationDate((Date)convoDBObject.get("cr_date"));
    	setDetails((String)convoDBObject.get("details"));
    	setMainURL((String)convoDBObject.get("main_url"));
    	setOldNames(parseSet(URLName.class, convoDBObject, "old_names"));
    	setViews(getInt(convoDBObject, "views"));
    	
    	//"merged_with"
    	DBRef mergedWithRef = (DBRef)convoDBObject.get("merged_with");
		if (mergedWithRef != null) {
			setMergedWith(mergedWithRef.getId().toString());
		}

    	setBitly((String)convoDBObject.get("bitly"));
    	setBitlyChat((String)convoDBObject.get("bitly_chat"));
    	
    	setDeleted(getBoolean(convoDBObject, "deleted"));
    	setOpened(getBoolean(convoDBObject, "opened"));
    	
    	setFrom((String)convoDBObject.get("from"));
    	setFromId((String)convoDBObject.get("from_id"));
    	
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
    	//author
    	setTalker(parseTalker(convoDBObject, "uid"));
	}
	
	public void parseFromDB(DBObject convoDBObject) {
		parseBasicFromDB(convoDBObject);
		
		setSummary((String)convoDBObject.get("summary"));
    	parseSumContributors((Collection<DBRef>)convoDBObject.get("sum_authors"));
		
		parseRelatedConvos((Collection<DBRef>)convoDBObject.get("related_convos"));
		parseFollowupConvos((Collection<DBRef>)convoDBObject.get("followup_convos"));
    	parseChatMessages((Collection<DBObject>)convoDBObject.get("messages"));
    	setFollowers(ConversationDAO.getConversationFollowers(getId()));
	}
	
	/**
	 * Parses messages/members information from Chat messages
	 * @param messagesDBList
	 */
	private void parseChatMessages(Collection<DBObject> messagesDBList) {
		List<MessageBean> messages = new ArrayList<MessageBean>();
    	Set<String> members = new HashSet<String>();
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
    				Logger.error("NULL talker in conversation message: "+getId()+", index: "+message.getIndex());
    			}
    		}
    	}
    	setMembers(members);
    	setMessages(messages);
	}
	
	//TODO: use DBModel for them? and check other classes also
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
				topic.parseBasicFromDB(topicDBRef.fetch());
				
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
	
	private void parseFollowupConvos(Collection<DBRef> followupDBList) {
		followupConvos = new HashSet<ConversationBean>();
		if (followupDBList != null) {
			for (DBRef convoDBRef : followupDBList) {
				ConversationBean convo = new ConversationBean();
				convo.parseBasicFromDB(convoDBRef.fetch());
				followupConvos.add(convo);
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
			DBRef convoRef = createRef(ConversationDAO.CONVERSATIONS_COLLECTION, convo.getId());
			relatedDBList.add(convoRef);
		}
		return relatedDBList;
	}
	
	public Set<DBRef> followupConvosToDB() {
		Set<DBRef> followupDBList = new HashSet<DBRef>();
		if (getFollowupConvos() == null) {
			return followupDBList;
		}
		for (ConversationBean convo : getFollowupConvos()) {
			DBRef convoRef = createRef(ConversationDAO.CONVERSATIONS_COLLECTION, convo.getId());
			followupDBList.add(convoRef);
		}
		return followupDBList;
	}
	
	/**
	 * Returns old conversation name (URLname object) with given title
	 * @param title
	 * @return
	 */
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
	
	/**
	 * Used for displaying (e.g. in OpenQuestions)
	 * @param authenticated Is current user logged in?
	 * @return
	 */
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
	
	/**
	 * Returns helpful or not helpful answers based on parameter
	 * @param notHelpful
	 * @return
	 */
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
	
	/**
	 * Has talker answered in this conversation?
	 * @param talker
	 * @return
	 */
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
	
	/**
	 * Returns page description for ConvoSummary page.
	 * The rules are:
	 * - first 160 characters of the top answer;
	 * - or convo's description;
	 * - or convo's title
	 * 
	 */
	public String getPageDescription() {
		String pageDescription = null;
		if (getComments() != null && !getComments().isEmpty()) {
			//try top answer
			CommentBean topAnswer = getComments().get(0);
			String topAnswerText = topAnswer.getText();
			if (topAnswerText != null) {
				pageDescription = topAnswerText;
				if (pageDescription.length() > 160) {
					pageDescription = pageDescription.substring(0, 160);
				}
			}
		}
		else if (getDetails() != null && getDetails().length() > 0) {
			pageDescription = getDetails();
		}
		else {
			pageDescription = getTopic();
		}
		
		return pageDescription;
	}
	
	/**
	 * Returns page keywords for ConvoSummary page.
	 * Keywords consist of convo's topics
	 * 
	 */
	public String getPageKeywords() {
		String pageKeywords = null;
		if (getTopics() != null && !getTopics().isEmpty())  {
			List<String> topicsTitles = new ArrayList<String>();
			for (TopicBean topic : getTopics()) {
				topicsTitles.add(topic.getTitle());
			}
			pageKeywords = TemplateExtensions.toCommaString(topicsTitles, null);
		}
		return pageKeywords;
	}
	
	/**
	 * Return Twitter share text for not-loggedin users.
	 * Ex: TalkAboutHealth Q&A: <question> - 
	 */
	public String getTwitterShareText() {
		String sampleTwitterURL = "http://t.co/3tkmYZN";
		String shareText = TwitterUtil.prepareTwit("TalkAboutHealth Q&A: <PARAM> -"+sampleTwitterURL, getTopic());
		//remove Twitter url, it will be added by Twitter
		return shareText.substring(0, shareText.length()-sampleTwitterURL.length());
	}
	/**
	 * Return Facebook share text for not-loggedin users.
	 * Ex: TalkAboutHealth Q&A: <question> - http://talkabouthealth.com/question_title 
	 */
	public String getFacebookShareText() {
		String convoURL = CommonUtil.generateAbsoluteURL("ViewDispatcher.view", "name", getMainURL());
		String shareText = "TalkAboutHealth Q&A: "+getTopic()+" - "+convoURL;
		return shareText;
	}
	
	public String getMainURL() { return mainURL; }
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
	
	public Set<TalkerBean> getSumContributors() { return sumContributors; }
	public void setSumContributors(Set<TalkerBean> sumContributors) { this.sumContributors = sumContributors; }

	public int getViews() { return views; }
	public void setViews(int views) { this.views = views; }
	
	public List<CommentBean> getComments() { return comments; }
	public void setComments(List<CommentBean> comments) { this.comments = comments; }
	
	public List<TalkerBean> getFollowers() { return followers; }
	public void setFollowers(List<TalkerBean> followers) { this.followers = followers; }
	
	public int getTid() { return tid; }
	public void setTid(int tid) { this.tid = tid; }

	public Set<URLName> getOldNames() { return oldNames; }
	public void setOldNames(Set<URLName> oldNames) { this.oldNames = oldNames; }

	public Set<TopicBean> getTopics() { return topics; }
	public void setTopics(Set<TopicBean> topics) { this.topics = topics; }

	public ConvoType getConvoType() { return convoType; }
	public void setConvoType(ConvoType convoType) { this.convoType = convoType; }

	public boolean isOpened() { return opened; }
	public void setOpened(boolean opened) { this.opened = opened; }

	public boolean isDeleted() { return deleted; }
	public void setDeleted(boolean deleted) { this.deleted = deleted; }

	public Set<ConversationBean> getRelatedConvos() { return relatedConvos; }
	public void setRelatedConvos(Set<ConversationBean> relatedConvos) { this.relatedConvos = relatedConvos; }

	public String getSearchFragment() { return searchFragment; }
	public void setSearchFragment(String searchFragment) { this.searchFragment = searchFragment; }

	public String getBitly() { return bitly; }
	public void setBitly(String bitly) { this.bitly = bitly; }

	public String getBitlyChat() { return bitlyChat; }
	public void setBitlyChat(String bitlyChat) { this.bitlyChat = bitlyChat; }

	public int getNumOfChatters() { return numOfChatters; }
	public void setNumOfChatters(int numOfChatters) { this.numOfChatters = numOfChatters; }
	
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getFromId() {
		return fromId;
	}
	public void setFromId(String fromId) {
		this.fromId = fromId;
	}
	public List<CommentBean> getReplies() {
		return replies;
	}
	public void setReplies(List<CommentBean> replies) {
		this.replies = replies;
	}
	public String getMergedWith() {
		return mergedWith;
	}
	public void setMergedWith(String mergedWith) {
		this.mergedWith = mergedWith;
	}
	public Set<ConversationBean> getFollowupConvos() {
		return followupConvos;
	}
	public void setFollowupConvos(Set<ConversationBean> followupConvos) {
		this.followupConvos = followupConvos;
	}
}

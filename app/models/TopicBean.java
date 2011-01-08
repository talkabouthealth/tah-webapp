package models;

import static util.DBUtil.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sun.security.action.GetBooleanAction;
import util.DBUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import dao.ConversationDAO;
import dao.TalkerDAO;
import dao.TopicDAO;

public class TopicBean implements Comparable<TopicBean> {
	
	private String id;
	
	private String title;
	private String mainURL;
	private Set<URLName> oldNames;
	private Set<String> aliases;
	//top (fixed) topic?
	private boolean fixed;
	private String bitly;
	
	private String summary;
	private Set<String> sumContributors;
	
	private Set<TopicBean> parents;
	private Set<TopicBean> children;
	
	private int views;
	private Date creationDate;
	private boolean deleted;
	
	private List<TalkerBean> followers;
	
	private List<ConversationBean> conversations;
	
	public TopicBean() {
		super();
		
		children = new HashSet<TopicBean>();
		parents = new HashSet<TopicBean>();
	}
	public TopicBean(String id) {
		super();
		this.id = id;
	}
	
	@Override
	public String toString() {
		if (aliases == null) {
			return title;
		}
		else {
			return title+" "+aliases;
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof TopicBean)) {
			return false;
		}
		
		TopicBean other = (TopicBean)obj;
//		if (id == null) {
//			Exception e = new RuntimeException();
//			e.printStackTrace(System.out);
//			throw new RuntimeException("excepppppppp");
//		}
		return id.equals(other.id);
	}
	
	@Override
	public int hashCode() {
		if (id == null) {
			return 47;
		}
		return id.hashCode();
	}
	
	@Override
	public int compareTo(TopicBean o) {
		return title.compareTo(o.title);
	}
	
	public void parseSuperBasicFromDB(DBObject topicDBObject) {
		if (topicDBObject == null) {
			return;
		}
		
		setId(getString(topicDBObject, "_id"));
		setTitle((String)topicDBObject.get("title"));
		setFixed(getBoolean(topicDBObject, "fixed"));
		setDeleted(getBoolean(topicDBObject, "deleted"));
		setMainURL((String)topicDBObject.get("main_url"));
		setBitly((String)topicDBObject.get("bitly"));
	}
	
	public void parseBasicFromDB(DBObject topicDBObject) {
		if (topicDBObject == null) {
			return;
		}
		
		parseSuperBasicFromDB(topicDBObject);

		
		setSummary((String)topicDBObject.get("summary"));
		setAliases(getStringSet(topicDBObject, "aliases"));
		
		setOldNames(parseSet(URLName.class, topicDBObject, "old_names"));
    	
		setViews(DBUtil.getInt(topicDBObject, "views"));
		setCreationDate((Date)topicDBObject.get("cr_date"));
		
		parseRelatives(topicDBObject);
	}
	
	private void parseRelatives(DBObject topicDBObject) {
		//children
		Collection<DBRef> childrenDBList = (Collection<DBRef>)topicDBObject.get("children");
		children = new HashSet<TopicBean>();
		if (childrenDBList != null) {
			for (DBRef childDBRef : childrenDBList) {
				TopicBean child = new TopicBean();
				DBObject childDBObject = childDBRef.fetch();
				child.setId(childDBObject.get("_id").toString());
				child.setTitle((String)childDBObject.get("title"));
				child.setMainURL((String)childDBObject.get("main_url"));
				children.add(child);
			}
		}
		
		//parents
		setParents(TopicDAO.getParentTopics(getString(topicDBObject, "_id")));
	}
	
	public void parseFromDB(DBObject topicDBObject) {
		parseBasicFromDB(topicDBObject);
		
		setConversations(ConversationDAO.loadConversationsByTopic(getId()));
		
		//followers of this topic
    	DBCollection talkersColl = getCollection(TalkerDAO.TALKERS_COLLECTION);
    	DBRef topicRef = createRef(TopicDAO.TOPICS_COLLECTION, getId());
    	DBObject query = new BasicDBObject("following_topics", topicRef);
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
	
	public List<DBRef> childrenToList() {
		List<DBRef> dbRefList = new ArrayList<DBRef>();
		if (children == null) {
			return dbRefList;
		}
		for (TopicBean topic : children) {
			dbRefList.add(DBUtil.createRef(TopicDAO.TOPICS_COLLECTION, topic.getId()));
		}
		
		return dbRefList;
	}
	
	public URLName getOldNameByTitle(String title) {
		for (URLName oldName : getOldNames()) {
			if (oldName.getTitle().equals(title)) {
				return oldName;
			}
		}
		return null;
	}
	
	//titles of children for display
	public String getChildrenInfo() {
		StringBuilder sb = new StringBuilder();
		if (!children.isEmpty()) {
			for (TopicBean child : children) {
				sb.append(child.getTitle()+", ");
			}
			//delete last comma and space
			sb.delete(sb.length()-2, sb.length());
		}
		
		return sb.toString();
	}
	
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getMainURL() {
		return mainURL;
	}
	public void setMainURL(String mainURL) {
		this.mainURL = mainURL;
	}
	public Date getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}
	public int getViews() {
		return views;
	}
	public void setViews(int views) {
		this.views = views;
	}
	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}
	public Set<String> getSumContributors() {
		return sumContributors;
	}
	public void setSumContributors(Set<String> sumContributors) {
		this.sumContributors = sumContributors;
	}
	public List<TalkerBean> getFollowers() {
		return followers;
	}
	public void setFollowers(List<TalkerBean> followers) {
		this.followers = followers;
	}
	public List<ConversationBean> getConversations() {
		return conversations;
	}
	public void setConversations(List<ConversationBean> conversations) {
		this.conversations = conversations;
	}
	public Set<String> getAliases() {
		return aliases;
	}
	public void setAliases(Set<String> aliases) {
		this.aliases = aliases;
	}
	public Set<TopicBean> getParents() {
		return parents;
	}
	public void setParents(Set<TopicBean> parents) {
		this.parents = parents;
	}
	public Set<TopicBean> getChildren() {
		return children;
	}
	public void setChildren(Set<TopicBean> children) {
		this.children = children;
	}
	public boolean isDeleted() {
		return deleted;
	}
	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
	public Set<URLName> getOldNames() {
		return oldNames;
	}
	public void setOldNames(Set<URLName> oldNames) {
		this.oldNames = oldNames;
	}
	public boolean isFixed() {
		return fixed;
	}
	public void setFixed(boolean fixed) {
		this.fixed = fixed;
	}
	public String getBitly() {
		return bitly;
	}
	public void setBitly(String bitly) {
		this.bitly = bitly;
	}
}

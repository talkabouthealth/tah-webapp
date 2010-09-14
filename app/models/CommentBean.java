package models;

import static util.DBUtil.*;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sun.security.action.GetBooleanAction;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import dao.TalkerDAO;

import models.ConversationBean.ConvoName;

public class CommentBean extends MessageBean {
	
	//TODO: make similar other such classes (e.g. ConvoName)
	public static class Vote implements DBModel {
		
		private TalkerBean talker;
		private boolean up;
		
		public Vote(TalkerBean talker, boolean up) {
			this.talker = talker;
			this.up = up;
		}
		
		@Override
		public DBObject toDBObject() {
			DBRef talkerRef = createRef(TalkerDAO.TALKERS_COLLECTION, getTalker().getId());
			DBObject dbObject = BasicDBObjectBuilder.start()
				.add("talker", talkerRef)
				.add("up", isUp())
				.get();
			return dbObject;
		}
		
		@Override
		public void parseDBObject(DBObject dbObject) {
			TalkerBean talker = new TalkerBean();
			talker.parseBasicFromDB(((DBRef)dbObject.get("talker")).fetch());
			setTalker(talker);
			
			setUp(getBoolean(dbObject, "up"));
		}
		
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Vote)) {
				return false;
			}
			
			Vote other = (Vote)obj;
			return talker.equals(other.talker);
		}
		
		@Override
		public int hashCode() {
			if (talker == null) {
				return 47;
			}
			return talker.hashCode();
		}
		
		public TalkerBean getTalker() {
			return talker;
		}
		public void setTalker(TalkerBean talker) {
			this.talker = talker;
		}
		public boolean isUp() {
			return up;
		}
		public void setUp(boolean up) {
			this.up = up;
		}
	}
	
	private String profileTalkerId;
	private String topicId;
	
	private String parentId;
	private List<CommentBean> children;
	
	private int voteScore;
	private Set<Vote> votes;
	
	
	public CommentBean() {}

	public CommentBean(String commentId) {
		super(commentId);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CommentBean)) {
			return false;
		}
		
		CommentBean other = (CommentBean)obj;
		return id.equals(other.id);
	}
	
	public void parseBasicFromDB(DBObject commentDBObject) {
		if (commentDBObject == null) {
			return;
		}

		setId(getString(commentDBObject, "_id"));
		setText((String)commentDBObject.get("text"));
		setTime((Date)commentDBObject.get("time"));
		
		DBRef topicRef = (DBRef)commentDBObject.get("topic");
		if (topicRef != null) {
			setTopicId(topicRef.getId().toString());
		}
		
		setVoteScore(getInt(commentDBObject, "vote_score"));
		setVotes(parseSet(Vote.class, commentDBObject, "votes"));
		
		//TODO: the same as thankyou?
		DBObject fromTalkerDBObject = ((DBRef)commentDBObject.get("from")).fetch();
		TalkerBean fromTalker = new TalkerBean();
		fromTalker.parseBasicFromDB(fromTalkerDBObject);
		setFromTalker(fromTalker);
	}
	
	public Vote getVoteByTalker(TalkerBean talker) {
		if (getVotes() == null) {
			return null;
		}
		
		for (Vote vote : getVotes()) {
			if (vote.getTalker().equals(talker)) {
				return vote;
			}
		}
		return null;
	}
	
	public Set<Vote> getUpVotes() {
		Set<Vote> upVotes = new HashSet<Vote>();
		if (getVotes() == null) {
			return upVotes;
		}
		for (Vote vote : getVotes()) {
			if (vote.isUp()) {
				upVotes.add(vote);
			}
		}
		return upVotes;
	}

	public String getProfileTalkerId() { return profileTalkerId; }
	public void setProfileTalkerId(String profileTalkerId) { this.profileTalkerId = profileTalkerId; }

	public String getParentId() { return parentId; }
	public void setParentId(String parentId) { this.parentId = parentId; }

	public List<CommentBean> getChildren() { return children; }
	public void setChildren(List<CommentBean> children) { this.children = children; }

	public String getTopicId() { return topicId; }
	public void setTopicId(String topicId) { this.topicId = topicId; }

	public int getVoteScore() {
		return voteScore;
	}

	public void setVoteScore(int voteScore) {
		this.voteScore = voteScore;
	}

	public Set<Vote> getVotes() {
		return votes;
	}

	public void setVotes(Set<Vote> votes) {
		this.votes = votes;
	}
}

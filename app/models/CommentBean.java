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

public class CommentBean extends MessageBean {
	
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
	private String convoId;
	private boolean deleted;
	//is it answer or reply ?
	private boolean answer;
	
	//old versions of the text
	private Set<String> oldTexts;
	
	private String parentId;
	private List<CommentBean> children;
	
	private int voteScore;
	private Set<Vote> votes;
	
	private boolean notHelpful;
	private Set<Vote> notHelpfulVotes;
	
	
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
	
	@Override
	public int hashCode() {
		if (id == null) {
			return 47;
		}
		return id.hashCode();
	}
	
	public void parseBasicFromDB(DBObject commentDBObject) {
		if (commentDBObject == null) {
			return;
		}

		setId(getString(commentDBObject, "_id"));
		setText((String)commentDBObject.get("text"));
		setOldTexts(getStringSet(commentDBObject, "old_texts"));
		setTime((Date)commentDBObject.get("time"));
		setDeleted(getBoolean(commentDBObject, "deleted"));
		setAnswer(getBoolean(commentDBObject, "answer"));
		setNotHelpful(getBoolean(commentDBObject, "not_helpful"));
		
		DBRef convoRef = (DBRef)commentDBObject.get("convo");
		if (convoRef != null) {
			setConvoId(convoRef.getId().toString());
		}
		
		setVoteScore(getInt(commentDBObject, "vote_score"));
		setVotes(parseSet(Vote.class, commentDBObject, "votes"));
		
		setNotHelpfulVotes(parseSet(Vote.class, commentDBObject, "not_helpful_votes"));
		
		setFromTalker(parseTalker(commentDBObject, "from"));
		
		DBRef profileTalkeRef = (DBRef)commentDBObject.get("profile");
		if (profileTalkeRef != null) {
			setProfileTalkerId(profileTalkeRef.getId().toString());
		}
	}
	
	public Vote getVoteByTalker(TalkerBean talker, Set<Vote> votesSet) {
		if (votesSet == null) {
			return null;
		}
		
		for (Vote vote : votesSet) {
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

	public String getConvoId() {
		return convoId;
	}

	public void setConvoId(String convoId) {
		this.convoId = convoId;
	}

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

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public Set<String> getOldTexts() {
		return oldTexts;
	}

	public void setOldTexts(Set<String> oldTexts) {
		this.oldTexts = oldTexts;
	}

	public boolean isNotHelpful() {
		return notHelpful;
	}

	public void setNotHelpful(boolean notHelpful) {
		this.notHelpful = notHelpful;
	}

	public Set<Vote> getNotHelpfulVotes() {
		return notHelpfulVotes;
	}

	public void setNotHelpfulVotes(Set<Vote> notHelpfulVotes) {
		this.notHelpfulVotes = notHelpfulVotes;
	}

	public boolean isAnswer() {
		return answer;
	}

	public void setAnswer(boolean answer) {
		this.answer = answer;
	}
}

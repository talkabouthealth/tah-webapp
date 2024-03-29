package models;

import static util.DBUtil.*;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import logic.TalkerLogic;

import sun.security.action.GetBooleanAction;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import dao.TalkerDAO;

/**
 * Represent Conversation answer/replies and Profile thoughts/replies.
 */
public class CommentBean extends MessageBean {
	
	/**
	 * A vote for conversation answers
	 */
	public static class Vote implements DBModel {
		//who voted
		private TalkerBean talker;
		private boolean up;
		
		public Vote() {
		}
		
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
		
		public TalkerBean getTalker() { return talker; }
		public void setTalker(TalkerBean talker) { this.talker = talker; }
		public boolean isUp() { return up; }
		public void setUp(boolean up) { this.up = up; }
	}
	
	//for thoughts/replies - id of profile where it was published
	private String profileTalkerId;
	
	//for answers/replies - id of conversation where it was created
	private String convoId;
	//is it answer or reply ?
	private boolean answer;
	//is it reply to conversation?
	private boolean convoReply;
	
	//Where thought was created (e.g. 'facebook')
	private String from;
	//Id related to creation source (e.g. id of FB post)
	private String fromId;
	
	//old versions of the text
	private Set<String> oldTexts;
	private boolean deleted;
	
	private String rootId;
	private String parentId;
	private List<CommentBean> children;
	
	//votes are used for answers
	private int voteScore;
	private Set<Vote> votes;
	private boolean notHelpful;
	private Set<Vote> notHelpfulVotes;
	
	private String actionID;
	
	//For question text
	private String question;
	//For moderate answer notification
	private String moderate;
	
	private String thoughtCategory;
	
	
	public static final String[] MODERATE_ARRAY = new String[] {
		"Approve Answer","Delete Answer","Not Helpful","Ignore"
	};
	
	public void setActionId(String id) { this.actionID = id; }
	public String getActionId() { return this.actionID; }
	
	public CommentBean() {}
	public CommentBean(String commentId) {
		super(commentId);
	}
	
	public void parseFromDB(DBObject commentDBObject) {
		if (commentDBObject == null) {
			return;
		}

		setId(getString(commentDBObject, "_id"));
		setText((String)commentDBObject.get("text"));
		setOldTexts(getStringSet(commentDBObject, "old_texts"));
		setTime((Date)commentDBObject.get("time"));
		
		setDeleted(getBoolean(commentDBObject, "deleted"));
		setAnswer(getBoolean(commentDBObject, "answer"));
		setConvoReply(getBoolean(commentDBObject, "convoreply"));
		
		DBRef convoRef = (DBRef)commentDBObject.get("convo");
		if (convoRef != null) {
			setConvoId(convoRef.getId().toString());
		}
		
		// Add root-id character
		setRootId((String)commentDBObject.get("rootid"));
		
		setFromTalker(TalkerLogic.loadTalkerFromCache(commentDBObject, "from"));
		DBRef profileTalkeRef = (DBRef)commentDBObject.get("profile");
		if (profileTalkeRef != null) {
			setProfileTalkerId(profileTalkeRef.getId().toString());
		}
		
		setFrom(getString(commentDBObject, "from_service"));
		setFromId(getString(commentDBObject, "from_service_id"));
		
		setVoteScore(getInt(commentDBObject, "vote_score"));
		setVotes(parseSet(Vote.class, commentDBObject, "votes"));
		setNotHelpful(getBoolean(commentDBObject, "not_helpful"));
		setNotHelpfulVotes(parseSet(Vote.class, commentDBObject, "not_helpful_votes"));
		setModerate(getString(commentDBObject, "moderate"));
		setThoughtCategory((String)commentDBObject.get("category"));
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
		setConvoReply(getBoolean(commentDBObject, "convoreply"));
		
		DBRef convoRef = (DBRef)commentDBObject.get("convo");
		if (convoRef != null) {
			setConvoId(convoRef.getId().toString());
		}
		
		// Add root-id character
		setRootId((String)commentDBObject.get("rootid"));
		setThoughtCategory((String)commentDBObject.get("category"));
	}
	
	/**
	 * Get vote of given talker from given set of votes (usual or nothelful)
	 * 
	 * @param talker
	 * @param votesSet
	 * @return Vote object or 'null' if there is no vote by this talker
	 */
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
	
	/**
	 * Returns only 'up' votes
	 * @return
	 */
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

	public String getRootId() { return rootId; }
	public void setRootId(String rootId) { this.rootId = rootId; }	
	
	public String getParentId() { return parentId; }
	public void setParentId(String parentId) { this.parentId = parentId; }

	public List<CommentBean> getChildren() { return children; }
	public void setChildren(List<CommentBean> children) { this.children = children; }

	public String getConvoId() { return convoId; }
	public void setConvoId(String convoId) { this.convoId = convoId; }

	public int getVoteScore() { return voteScore; }
	public void setVoteScore(int voteScore) { this.voteScore = voteScore; }

	public Set<Vote> getVotes() { return votes; }
	public void setVotes(Set<Vote> votes) { this.votes = votes; }

	public boolean isDeleted() { return deleted; }
	public void setDeleted(boolean deleted) { this.deleted = deleted; }

	public Set<String> getOldTexts() { return oldTexts; }
	public void setOldTexts(Set<String> oldTexts) { this.oldTexts = oldTexts; }

	public boolean isNotHelpful() { return notHelpful; }
	public void setNotHelpful(boolean notHelpful) { this.notHelpful = notHelpful; }

	public Set<Vote> getNotHelpfulVotes() { return notHelpfulVotes; }
	public void setNotHelpfulVotes(Set<Vote> notHelpfulVotes) { this.notHelpfulVotes = notHelpfulVotes; }

	public boolean isAnswer() { return answer; }
	public void setAnswer(boolean answer) { this.answer = answer; }
	
	public boolean isConvoReply() {
		return convoReply;
	}
	public void setConvoReply(boolean convoReply) {
		this.convoReply = convoReply;
	}
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
	public String getQuestion() {
		return question;
	}
	public void setQuestion(String question) {
		this.question = question;
	}
	public String getModerate() {
		return moderate;
	}
	public void setModerate(String moderate) {
		this.moderate = moderate;
	}
	public String getThoughtCategory() {
		return thoughtCategory;
	}
	public void setThoughtCategory(String thoughtCategory) {
		this.thoughtCategory = thoughtCategory;
	}
	
}

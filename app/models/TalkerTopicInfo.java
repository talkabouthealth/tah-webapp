package models;

import java.util.HashSet;
import java.util.Set;

/**
 * Bean for Talker-Topic related info:
 * experience, endorsements, etc.
 *
 */
public class TalkerTopicInfo {
	
	private String experience;
	private Set<TalkerBean> endorsements;
	
	private int numOfAnswers;
	
	public TalkerTopicInfo() {
		endorsements = new HashSet<TalkerBean>();
	}

	public String getExperience() {
		return experience;
	}

	public void setExperience(String experience) {
		this.experience = experience;
	}
	
	public Set<TalkerBean> getEndorsements() {
		return endorsements;
	}

	public void setEndorsements(Set<TalkerBean> endorsements) {
		this.endorsements = endorsements;
	}

	public int getNumOfAnswers() {
		return numOfAnswers;
	}

	public void setNumOfAnswers(int numOfAnswers) {
		this.numOfAnswers = numOfAnswers;
	}
	
	
}

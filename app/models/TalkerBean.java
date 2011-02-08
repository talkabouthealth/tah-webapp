package models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.CommentBean.Vote;
import models.PrivacySetting.PrivacyType;
import models.PrivacySetting.PrivacyValue;
import models.ServiceAccountBean.ServiceType;
import models.actions.Action;

import org.bson.types.ObjectId;

import play.data.validation.Email;
import play.data.validation.Match;
import play.data.validation.Required;
import play.mvc.Scope.Session;
import util.DBUtil;
import util.ValidateData;
import util.EmailUtil.EmailTemplate;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import dao.CommentsDAO;
import dao.TalkerDAO;
import dao.TopicDAO;

import static util.DBUtil.*;

public class TalkerBean implements Serializable {
	
	public static final String[] CHILDREN_AGES_ARRAY = new String[] {
		"New born", "1-2 years old", "2-6 years old", 
		"6-12 years old", "12-18 years old", "Over 18 years old"
	};
	public static final String[] CONVERSATIONS_TYPES_ARRAY = new String[] {
		"Informational", "Advice and opinions", "Meet new people", "Emotional support"
	};
	public static final String[] CONNECTIONS_ARRAY = new String[] {
		"Patient", "Former Patient", "Parent", "Caregiver", "Family member", "Friend", 
		//"professionals"
		"Physician", "Pharmacist", "Nurse", "Psychologist", "Social worker", "Researcher",
		 "other"
	};
	// only professionals in this list
	public static final List<String> PROFESSIONAL_CONNECTIONS_LIST = Arrays.asList(
		"Physician", "Pharmacist", "Nurse", "Psychologist", "Social worker", "Researcher"
	);
	
	//Convo-related items start with "CONVO" - we use it for display
	public enum EmailSetting {
		RECEIVE_COMMENT ("Send me an email when I receive a comment in my Thoughts Feed.", EmailTemplate.NOTIFICATION_PROFILE_COMMENT),
		RECEIVE_THANKYOU ("Send me an email when I receive a 'Thank You'.", EmailTemplate.NOTIFICATION_THANKYOU),
		RECEIVE_DIRECT ("Send me an email when I receive a Direct Message.", EmailTemplate.NOTIFICATION_DIRECT_MESSAGE),
		NEW_FOLLOWER ("Send me an email when someone follows me.", EmailTemplate.NOTIFICATION_FOLLOWER),
		
		CONVO_RESTART ("Send me an email when a Conversation I follow is re-started.", 
				EmailTemplate.NOTIFICATION_CONVO_RESTART),
		CONVO_COMMENT ("Send me an email when an Answer is added to a Conversation I follow.", 
				EmailTemplate.NOTIFICATION_CONVO_ANSWER),
		CONVO_SUMMARY ("Send me an email when a Summary of a Conversation I follow is updated.", 
				EmailTemplate.NOTIFICATION_CONVO_SUMMARY);
		
		private final String description;
		private final EmailTemplate emailTemplate;
		
		private EmailSetting(String description, EmailTemplate emailTemplate) {
			this.description = description;
			this.emailTemplate = emailTemplate;
		}

		public String getDescription() { return description; }

		public EmailTemplate getEmailTemplate() {
			return emailTemplate;
		}
	}
	
	private String id;
	@Required @Match(ValidateData.USER_REGEX) private String userName;
	private String anonymousName;
	@Required private String password;
	private String confirmPassword;
	@Required @Email private String email;
	//code for email verification
	private String verifyCode;	
	//not-primary emails
	private Set<EmailBean> emails;
	
	private String firstName;
	private String lastName;
	//used to store original userName when account is deactivated
	private String originalUserName;
	private boolean deactivated;
	private boolean suspended;
	private Set<PrivacySetting> privacySettings;
	
	// FB/Twitter/IM accounts
	private Set<ServiceAccountBean> serviceAccounts;
	private Set<IMAccountBean> imAccounts;
	private String im;
	private String imUsername;
	private boolean imNotify;
	
	private Date dob;
	private int dobMonth;
	private int dobDay;
	private int dobYear;
	private Date regDate;
	
	//Patient/Caregiver/etc.
	private String connection;
	private boolean connectionVerified;
	//info from professional profiles
	private Map<String, String> profInfo;
	//for Physician
	private List<String> insuranceAccepted;
	
	private int childrenNum;
	//for editing (we can't show/save default value with int)
	private String childrenNumStr;
	private List<String> childrenAges;
	private String webpage;
	private List<String> keywords;
	private String bio;
	private String profStatement;
	private boolean newsletter;
	private String gender;
	private int invitations;
	private String maritalStatus;
	//Breast Cancer, etc.
	private String category;
	private String city;
	private String state;
	private String country;
	private String zip;
	//a set of hidden help popups for this user
	private Set<String> hiddenHelps = new HashSet<String>();
	
	//notifications settings
	private String[] ctype;
	private int nfreq;
	private int ntime;
	
	private List<ThankYouBean> thankYouList;
	private List<TalkerBean> followingList;
	private List<TalkerBean> followerList;
	private List<Action> activityList;
	//thoughts
	private List<CommentBean> profileCommentsList;
	private List<CommentBean> answerList;
	private List<String> followingConvosList;
	private Set<TopicBean> followingTopicsList;
	private Map<TopicBean, TalkerTopicInfo> topicsInfoMap;
	
	private Set<EmailSetting> emailSettings;
	
	//additional variables for displaying
	private int numOfThankYous;
	private int numOfConvoAnswers;
	private String profileCompletionMessage;
	private int profileCompletionValue;
	private String nextStepMessage;
	private String nextStepNote;
	
	private Date latestNotification;
	private long numOfNotifications;
	
	public TalkerBean(){}
	public TalkerBean(String id) {
		this.id = id;
	}
	public TalkerBean(String id, String userName) {
		this.id = id;
		this.userName = userName;
	}

	//TODO: verify equals & hashCode ? move to abstract DBModel class?
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof TalkerBean)) {
			return false;
		}
		
		TalkerBean other = (TalkerBean)obj;
		return id.equals(other.id);
	}
	
	@Override
	public int hashCode() {
		if (id == null) {
			return 47;
		}
		return id.hashCode();
	}
	
	// ----=================== Parse/Save information from/to DB =================----
	public void parseBasicFromDB(DBObject talkerDBObject) {
		setId(talkerDBObject.get("_id").toString());
		setUserName((String)talkerDBObject.get("uname"));
		setAnonymousName((String)talkerDBObject.get("anon_name"));
		setPassword((String)talkerDBObject.get("pass"));
		setEmail((String)talkerDBObject.get("email"));
		setEmails(parseSet(EmailBean.class, talkerDBObject, "emails"));
		setVerifyCode((String)talkerDBObject.get("verify_code"));
		
		setOriginalUserName((String)talkerDBObject.get("orig_uname"));
		setDeactivated(getBoolean(talkerDBObject, "deactivated")); 
		setSuspended(getBoolean(talkerDBObject, "suspended"));
		
		setBio((String)talkerDBObject.get("bio"));
		setConnection((String)talkerDBObject.get("connection"));
		setConnectionVerified(getBoolean(talkerDBObject, "connection_verified")); 
		setProfStatement((String)talkerDBObject.get("prof_statement"));
		parseProfInfo((DBObject)talkerDBObject.get("prof_info"));
		
		parseEmailSettings(getStringList(talkerDBObject, "email_settings"));
		setPrivacySettings(parseSet(PrivacySetting.class, talkerDBObject, "privacy_settings"));
		
		Collection<DBObject> thankYousCollection = (Collection<DBObject>)talkerDBObject.get("thankyous");
		setNumOfThankYous(thankYousCollection == null ? 0 : thankYousCollection.size());
	}
	
	public void parseFromDB(DBObject talkerDBObject) {
		parseBasicFromDB(talkerDBObject);
		
		setHiddenHelps(getStringSet(talkerDBObject, "hidden_helps"));
		
		setIm((String)talkerDBObject.get("im"));
		setImUsername((String)talkerDBObject.get("im_uname"));
		setImNotify(getBoolean(talkerDBObject, "im_notify"));
		setImAccounts(parseSet(IMAccountBean.class, talkerDBObject, "im_accounts"));
		setServiceAccounts(parseSet(ServiceAccountBean.class, talkerDBObject, "service_accounts"));
		
		setNfreq(getInt(talkerDBObject, "nfreq"));
		setNtime(getInt(talkerDBObject, "ntime"));
		Collection<String> ctype = (Collection<String>)talkerDBObject.get("ctype");
		if (ctype != null) {
			setCtype(ctype.toArray(new String[]{}));
		}
		
		setNewsletter(getBoolean(talkerDBObject, "newsletter"));
		setGender((String)talkerDBObject.get("gender"));
		setDob((Date)talkerDBObject.get("dob"));
		setInvitations(getInt(talkerDBObject, "invites"));
		setCity((String)talkerDBObject.get("city"));
		setState((String)talkerDBObject.get("state"));
		setCountry((String)talkerDBObject.get("country"));
		setChildrenNum(getInt(talkerDBObject, "ch_num"));
		setMaritalStatus((String)talkerDBObject.get("mar_status"));
		setCategory((String)talkerDBObject.get("category"));
		setRegDate((Date)talkerDBObject.get("timestamp"));
		setFirstName((String)talkerDBObject.get("firstname"));
		setLastName((String)talkerDBObject.get("lastname"));
		setZip((String)talkerDBObject.get("zip"));
		setWebpage((String)talkerDBObject.get("webpage"));
		setChildrenAges(getStringList(talkerDBObject, "ch_ages"));
		setKeywords(getStringList(talkerDBObject, "keywords"));
		
		setInsuranceAccepted(getStringList(talkerDBObject, "insurance_accept"));
		
		parseThankYous((Collection<DBObject>)talkerDBObject.get("thankyous"));
		parseFollowing((Collection<DBRef>)talkerDBObject.get("following"));
		setFollowingConvosList(getStringList(talkerDBObject, "following_convos"));
		parseFollowingTopics((Collection<DBRef>)talkerDBObject.get("following_topics"));
		parseTopicsInfo((Collection<DBObject>)talkerDBObject.get("topics_info"));
		setAnswerList(CommentsDAO.getTalkerAnswers(getId(), null));
	}
	
	public void parseThankYous(Collection<DBObject> thankYouDBList) {
		List<ThankYouBean> thankYous = new ArrayList<ThankYouBean>();
		if (thankYouDBList != null) {
			for (DBObject thankYouDBObject : thankYouDBList) {
				ThankYouBean thankYouBean = new ThankYouBean();
				thankYouBean.setTime((Date)thankYouDBObject.get("time"));
				thankYouBean.setNote((String)thankYouDBObject.get("note"));
				
				DBObject fromTalkerDBObject = ((DBRef)thankYouDBObject.get("from")).fetch();
				TalkerBean fromTalker = new TalkerBean();
				fromTalker.parseBasicFromDB(fromTalkerDBObject);
				thankYouBean.setFrom(fromTalker.getId());
				thankYouBean.setFromTalker(fromTalker);
				
				thankYous.add(thankYouBean);
			}
		}
		
		//sort by creation time
		Collections.sort(thankYous);
		thankYouList = thankYous;
	}
	
	private void parseFollowing(Collection<DBRef> followingDBList) {
		followingList = new ArrayList<TalkerBean>();
		if (followingDBList != null) {
			for (DBRef followingDBRef : followingDBList) {
				DBObject followingDBOBject = followingDBRef.fetch();
				
				boolean isDeactivated = getBoolean(followingDBOBject, "deactivated");
				boolean isSuspended = getBoolean(followingDBOBject, "suspended");
				if (isDeactivated || isSuspended) {
					continue;
				}
				
				TalkerBean followingTalker = new TalkerBean();
				followingTalker.parseBasicFromDB(followingDBOBject);
				followingList.add(followingTalker);
			}
		}
	}
	
	private void parseFollowingTopics(Collection<DBRef> followingTopicsDBList) {
		followingTopicsList = new LinkedHashSet<TopicBean>();
		if (followingTopicsDBList != null) {
			for (DBRef topicDBRef : followingTopicsDBList) {
				TopicBean topic = new TopicBean();
				if (topicDBRef.fetch() != null) {
					topic.parseFromDB(topicDBRef.fetch());
					if (!topic.isDeleted()) {
						followingTopicsList.add(topic);
					}
				}
			}
		}
	}
	
	/**
	 * Parse related info between talker and topic - experiences, endorsements
	 * @param topicsInfoCol
	 */
	private void parseTopicsInfo(Collection<DBObject> topicsInfoCol) {
		topicsInfoMap = new HashMap<TopicBean, TalkerTopicInfo>();
		
		if (topicsInfoCol != null) {
			for (DBObject topicInfoDBObject : topicsInfoCol) {
				//topic
				DBObject topicDBObject = ((DBRef)topicInfoDBObject.get("topic")).fetch();
				TopicBean topic = new TopicBean();
				topic.parseFromDB(topicDBObject);
				
				if (topic.getId() == null) {
					//maybe deleted topic
					continue;
				}
				
				//TopicInfo
				TalkerTopicInfo topicInfo = new TalkerTopicInfo();
				topicInfo.setExperience((String)topicInfoDBObject.get("experience"));
				
				Set<TalkerBean> endorsements = new HashSet<TalkerBean>();
				for (DBRef talkerDBRef : DBUtil.<DBRef>getSet(topicInfoDBObject, "endorsements")) {
					endorsements.add(parseTalker(talkerDBRef));
				}
				topicInfo.setEndorsements(endorsements);
				
				topicsInfoMap.put(topic, topicInfo);
			}
		}
	}
	
	private void parseProfInfo(DBObject profInfoDBObject) {
		profInfo = new HashMap<String, String>();
		if (profInfoDBObject != null) {
			profInfo = profInfoDBObject.toMap();
		}
	}
	
	public void parseEmailSettings(List<String> dbEmailSettingsList) {
		emailSettings = EnumSet.noneOf(EmailSetting.class);
		if (dbEmailSettingsList == null) {
			return;
		}
		
		for (String dbEmailSetting : dbEmailSettingsList) {
			EmailSetting emailSetting = EmailSetting.valueOf(dbEmailSetting);
			emailSettings.add(emailSetting);
		}
	}
	
	/* ---------------- Convert to DB format methods --------------------- */

	public List<DBObject> topicsInfoToDB() {
		List<DBObject> topicsInfoList = new ArrayList<DBObject>();
		if (topicsInfoMap != null) {
			for (TopicBean topic : topicsInfoMap.keySet()) {
				TalkerTopicInfo topicInfo = topicsInfoMap.get(topic);
				
				DBRef topicDBRef = createRef(TopicDAO.TOPICS_COLLECTION, topic.getId());
				List<DBRef> endorsementTalkers = new ArrayList<DBRef>();
				for (TalkerBean talker : topicInfo.getEndorsements()) {
					endorsementTalkers.add(createRef(TalkerDAO.TALKERS_COLLECTION, talker.getId()));
				}
				DBObject topicInfoDBObject = new BasicDBObjectBuilder().start()
					.add("topic", topicDBRef)
					.add("experience", topicInfo.getExperience())
					.add("endorsements", endorsementTalkers)
					.get();
				
				topicsInfoList.add(topicInfoDBObject);
			}
		}
		return topicsInfoList;
	}
	
	public List<String> emailSettingsToList() {
		List<String> emailSettingsStringList = new ArrayList<String>();
		if (emailSettings == null) {
			return emailSettingsStringList;
		}
		for (EmailSetting emailSetting : emailSettings) {
			emailSettingsStringList.add(emailSetting.toString());
		}
		
		return emailSettingsStringList;
	}
	
	public List<DBRef> followingTopicsToList() {
		List<DBRef> dbRefList = new ArrayList<DBRef>();
		if (followingTopicsList == null) {
			return dbRefList;
		}
		for (TopicBean topic : followingTopicsList) {
			dbRefList.add(DBUtil.createRef(TopicDAO.TOPICS_COLLECTION, topic.getId()));
		}
		
		return dbRefList;
	}
	
	/* -------------------- Useful methods for displaying data --------------------- */
	
	/**
	 * Returns userName or anonymous name, 
	 * based on privacy settings and current logged in talker
	 */
	public String getName() {
		String loggedinUsername = Session.current().get("username");
		
		PrivacyValue privacyValue = getPrivacyValue(PrivacyType.USERNAME);
		if (privacyValue == PrivacyValue.PUBLIC || loggedinUsername != null) {
			return getUserName();
		}
		return getAnonymousName();
	}
	
	/**
	 * Finds non-primary email by email value or verification code
	 */
	public EmailBean findNonPrimaryEmail(String email, String verifyCode) {
		EmailBean nonPrimaryEmail = null;
		for (EmailBean emailBean : getEmails()) {
			if (emailBean.getValue().equals(email)) {
				nonPrimaryEmail = emailBean;
				break;
			}
			if (emailBean.getVerifyCode() != null 
					&& emailBean.getVerifyCode().equals(verifyCode)) {
				nonPrimaryEmail = emailBean;
				break;
			}
		}
		return nonPrimaryEmail;
	}
	
	public ServiceAccountBean serviceAccountByType(ServiceType serviceType) {
		if (serviceAccounts == null) {
			return null;
		}
		for (ServiceAccountBean serviceAccount : serviceAccounts) {
			if (serviceAccount.getType() == serviceType) {
				return serviceAccount;
			}
		}
		return null;
	}
	
	public PrivacyValue getPrivacyValue(PrivacyType type) {
		for (PrivacySetting privacySetting : getPrivacySettings()) {
			if (privacySetting.getType() == type) {
				return privacySetting.getValue();
			}
		}
		return null;
	}
	public boolean isPrivate(PrivacyType type) {
		PrivacyValue value = getPrivacyValue(type);
		if (value == null) {
			return true;
		}
		return value == PrivacyValue.PRIVATE;
	}
	public boolean isPublic(PrivacyType type) {
		PrivacyValue value = getPrivacyValue(type);
		if (value == null) {
			return false;
		}
		return value == PrivacyValue.PUBLIC;
	}
	
	/**
	 * Returns true if talker has made at least one piece of information public 
	 * @return
	 */
	public boolean hasSomePublicInfo() {
		for (PrivacySetting privacySetting : getPrivacySettings()) {
			if (privacySetting.getValue() == PrivacyValue.PUBLIC) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Is this talker professional?
	 * @return
	 */
	public boolean isProf() {
		return PROFESSIONAL_CONNECTIONS_LIST.contains(connection);
	}
	
	public boolean isAdmin() {
		return (userName != null && userName.equals("admin"));
	}
	
	public String getAge() {
		if (dob == null) {
			return null;
		}
		Date now = new Date();
        long delta = (now.getTime() - dob.getTime()) / 1000;

        long years = delta / (365 * 24 * 60 * 60);
        return ""+years;
	}
	
	public String getFullGender() {
		if ("F".equals(gender)) {
			return "Female";
		}
		else if ("M".equals(gender)) {
			return "Male";
		}
		else {
			return null;
		}
	}
	
	/*
		Recognition levels should be the following:
		- New Member - 0 Thank you's
		- Supporter - 1 to 2 Thank you's
		- Companion - 3 to 5 Thank you's
		- Advocate - 6- 10 Thank you's
		- Fellow - 11- 20 Thank you's
		- Benefactor - 21 - 40 Thank you's
		- Patron 41- 100 Thank you's
		- Champion - great than 100 Thank you's
	*/
	public String getLevelOfRecognition() {
		String levelOfRecognition = null;
		
		int numberOfThankYous = 0;
		if (thankYouList == null) {
			numberOfThankYous = getNumOfThankYous();
		}
		else {
			numberOfThankYous = thankYouList.size();
		}
		
		if (numberOfThankYous == 0) {
			levelOfRecognition = "New Member";
		}
		else if (numberOfThankYous >= 1 && numberOfThankYous < 3) {
			levelOfRecognition = "Supporter";
		}
		else if (numberOfThankYous >= 3 && numberOfThankYous < 6) {
			levelOfRecognition = "Companion";
		}
		else if (numberOfThankYous >= 6 && numberOfThankYous < 11) {
			levelOfRecognition = "Advocate";
		}
		else if (numberOfThankYous >= 11 && numberOfThankYous < 21) {
			levelOfRecognition = "Fellow";
		}
		else if (numberOfThankYous >= 21 && numberOfThankYous < 41) {
			levelOfRecognition = "Benefactor";
		}
		else if (numberOfThankYous >= 41 && numberOfThankYous < 101) {
			levelOfRecognition = "Patron";
		}
		else if (numberOfThankYous >= 101) {
			levelOfRecognition = "Champion";
		}
		
		return levelOfRecognition;
	}
	
	//Returns other(non-standard) ctypes as comma-separated list
	public String getOtherCTypes() {
		final String defaultMessage = "Other (please separate by commas)";
		if (ctype == null) {
			return defaultMessage;
		}
		
		//get only non-standard (other) ctypes
		List<String> cTypesList = new ArrayList<String>(Arrays.asList(ctype));
		cTypesList.removeAll(Arrays.asList(CONVERSATIONS_TYPES_ARRAY));
		
		if (cTypesList.size() == 0) {
			return defaultMessage;
		}
		else {
			//format [entry1, entry2]
			String cTypesListString = cTypesList.toString();
			return cTypesListString.substring(1, cTypesListString.length()-1);
		}
	}
	
	
	// ---- Getters & Setters -------
	public String getUserName() { return userName; }
	public void setUserName(String userName) { this.userName = userName; }
	
	public String getAnonymousName() { return anonymousName; }
	public void setAnonymousName(String anonymousName) { this.anonymousName = anonymousName; }
	
	public String getPassword() { return password; }
	public void setPassword(String password) { this.password = password; }
	
	public void setEmail(String email) { this.email = email; }
	public String getEmail() { return email; }
	
	public String getGender() { return gender; }
	public void setGender(String gender) { this.gender = gender; }

	public void setDob(Date dob) { this.dob = dob; }
	public Date getDob() { return dob; }

	public int getInvitations() { return invitations; }
	public void setInvitations(int invitations) { this.invitations = invitations; }
	
	public String getMaritalStatus() { return maritalStatus; }
	public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }

	public String getCategory() { return category; }
	public void setCategory(String category) { this.category = category; }

	public String getCity() {
		if (city == null) {
			return "";
		}
		return city;
	}
	public void setCity(String city) { this.city = city; }

	public String getState() {
		if (state == null) {
			return "";
		}
		return state;
	}
	public void setState(String state) { this.state = state; }

	public String getCountry() {
		if (country == null) {
			return "";
		}
		return country;
	}
	public void setCountry(String country) { this.country = country; }
	
	public Set<EmailSetting> getEmailSettings() { return emailSettings; }
	public void setEmailSettings(Set<EmailSetting> emailSettings) { this.emailSettings = emailSettings; }

	public int getNfreq() { return nfreq; }
	public void setNfreq(int nfreq) { this.nfreq = nfreq; }

	public int getNtime() { return ntime; }
	public void setNtime(int ntime) { this.ntime = ntime; }

	public String[] getCtype() { return ctype; }
	public void setCtype(String[] ctype) { this.ctype = ctype; }

	public int getChildrenNum() { return childrenNum; }
	public void setChildrenNum(int childrenNum) { this.childrenNum = childrenNum; }

	public String getImUsername() { return imUsername; }
	public void setImUsername(String imUsername) { this.imUsername = imUsername; }

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public boolean isNewsletter() { return newsletter; }
	public void setNewsletter(boolean newsletter) { this.newsletter = newsletter; }

	public String getIm() { return im; }
	public void setIm(String im) { this.im = im; }
	
	public int getDobMonth() { return dobMonth; }
	public void setDobMonth(int dobMonth) { this.dobMonth = dobMonth; }

	public int getDobDay() { return dobDay; }
	public void setDobDay(int dobDate) { this.dobDay = dobDate; }

	public int getDobYear() { return dobYear; }
	public void setDobYear(int dobYear) { this.dobYear = dobYear; }

	public String getFirstName() { return firstName; }
	public void setFirstName(String firstName) { this.firstName = firstName; }

	public String getLastName() { return lastName; }
	public void setLastName(String lastName) { this.lastName = lastName; }

	public String getZip() { return zip; }
	public void setZip(String zip) { this.zip = zip; }

	public List<String> getChildrenAges() { return childrenAges; }
	public void setChildrenAges(List<String> childrenAges) { this.childrenAges = childrenAges; }

	public String getWebpage() { return webpage; }
	public void setWebpage(String webpage) { this.webpage = webpage; }

	public List<String> getKeywords() { return keywords; }
	public void setKeywords(List<String> keywords) { this.keywords = keywords; }

	public String getBio() { return bio; }
	public void setBio(String bio) { this.bio = bio; }

	public List<String> getFollowingConvosList() { return followingConvosList; }
	public void setFollowingConvosList(List<String> followingConvosList) { this.followingConvosList = followingConvosList; }
	
	public boolean isDeactivated() { return deactivated; }
	public void setDeactivated(boolean deactivated) { this.deactivated = deactivated; }

	public boolean isConnectionVerified() { return connectionVerified; }
	public void setConnectionVerified(boolean connectionVerified) { this.connectionVerified = connectionVerified; }

	public String getConnection() { return connection; }
	public void setConnection(String connection) { this.connection = connection; }
	
	public String getVerifyCode() { return verifyCode; }
	public void setVerifyCode(String verifyCode) { this.verifyCode = verifyCode; }
	
	public Date getRegDate() { return regDate; }
	public void setRegDate(Date regDate) { this.regDate = regDate; }

	public List<CommentBean> getProfileCommentsList() { return profileCommentsList; }
	public void setProfileCommentsList(List<CommentBean> profileCommentsList) { this.profileCommentsList = profileCommentsList; }

	public List<Action> getActivityList() { return activityList; }
	public void setActivityList(List<Action> activityList) { this.activityList = activityList; }

	public List<TalkerBean> getFollowerList() { return followerList; }
	public void setFollowerList(List<TalkerBean> followerList) { this.followerList = followerList; }

	public List<TalkerBean> getFollowingList() { return followingList; }
	public void setFollowingList(List<TalkerBean> followingList) { this.followingList = followingList; }

	public List<ThankYouBean> getThankYouList() { return thankYouList; }
	public void setThankYouList(List<ThankYouBean> thankYouList) { this.thankYouList = thankYouList; }

	public String getProfileCompletionMessage() { return profileCompletionMessage; }
	public void setProfileCompletionMessage(String profileCompletionMessage) { this.profileCompletionMessage = profileCompletionMessage; }
	
	public int getProfileCompletionValue() { return profileCompletionValue; }
	public void setProfileCompletionValue(int profileCompletionValue) { this.profileCompletionValue = profileCompletionValue; }
	
	public Set<EmailBean> getEmails() { return emails; }
	public void setEmails(Set<EmailBean> emails) { this.emails = emails; }
	
	public Set<IMAccountBean> getImAccounts() { return imAccounts; }
	public void setImAccounts(Set<IMAccountBean> imAccounts) { this.imAccounts = imAccounts; }
	
	public Date getLatestNotification() { return latestNotification; }
	public void setLatestNotification(Date latestNotification) { this.latestNotification = latestNotification; }
	
	public long getNumOfNotifications() { return numOfNotifications; }
	public void setNumOfNotifications(long numOfNotifications) { this.numOfNotifications = numOfNotifications; }
	
	public int getNumOfThankYous() { return numOfThankYous; }
	public void setNumOfThankYous(int numOfThankYous) { this.numOfThankYous = numOfThankYous; }
	
	public Set<TopicBean> getFollowingTopicsList() { return followingTopicsList; }
	public void setFollowingTopicsList(Set<TopicBean> followingTopicsList) { this.followingTopicsList = followingTopicsList; }
	
	public boolean isSuspended() { return suspended; }
	public void setSuspended(boolean suspended) { this.suspended = suspended; }
	
	public Map<TopicBean, TalkerTopicInfo> getTopicsInfoMap() { return topicsInfoMap; }
	public void setTopicsInfoMap(Map<TopicBean, TalkerTopicInfo> topicsInfoMap) { this.topicsInfoMap = topicsInfoMap; }
	
	public String getOriginalUserName() { return originalUserName; }
	public void setOriginalUserName(String originalUserName) { this.originalUserName = originalUserName; }
	
	public int getNumOfConvoAnswers() { return numOfConvoAnswers; }
	public void setNumOfConvoAnswers(int numOfConvoAnswers) { this.numOfConvoAnswers = numOfConvoAnswers; }
	
	public String getChildrenNumStr() { return childrenNumStr; }
	public void setChildrenNumStr(String childrenNumStr) { this.childrenNumStr = childrenNumStr; }
	
	public Set<String> getHiddenHelps() { return hiddenHelps; }
	public void setHiddenHelps(Set<String> hiddenHelps) { this.hiddenHelps = hiddenHelps; }
	
	public String getConfirmPassword() { return confirmPassword; }
	public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
	
	public Map<String, String> getProfInfo() { return profInfo; }
	public void setProfInfo(Map<String, String> profInfo) { this.profInfo = profInfo; }
	
	public String getNextStepMessage() { return nextStepMessage; }
	public void setNextStepMessage(String nextStepMessage) { this.nextStepMessage = nextStepMessage; }
	
	public String getNextStepNote() { return nextStepNote; }
	public void setNextStepNote(String nextStepNote) { this.nextStepNote = nextStepNote; }
	
	public String getProfStatement() { return profStatement; }
	public void setProfStatement(String profStatement) { this.profStatement = profStatement; }
	
	public List<String> getInsuranceAccepted() { return insuranceAccepted; }
	public void setInsuranceAccepted(List<String> insuranceAccepted) { this.insuranceAccepted = insuranceAccepted; }
	
	public Set<ServiceAccountBean> getServiceAccounts() { return serviceAccounts; }
	public void setServiceAccounts(Set<ServiceAccountBean> serviceAccounts) { this.serviceAccounts = serviceAccounts; }
	
	public boolean isImNotify() { return imNotify; }
	public void setImNotify(boolean imNotify) { this.imNotify = imNotify; }
	
	public List<CommentBean> getAnswerList() { return answerList; }
	public void setAnswerList(List<CommentBean> answerList) { this.answerList = answerList; }
	
	public Set<PrivacySetting> getPrivacySettings() { return privacySettings; }
	public void setPrivacySettings(Set<PrivacySetting> privacySettings) { this.privacySettings = privacySettings; }
}	

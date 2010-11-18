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
import models.actions.Action;

import org.bson.types.ObjectId;

import play.data.validation.Email;
import play.data.validation.Match;
import play.data.validation.Required;
import util.DBUtil;
import util.ValidateData;
import util.EmailUtil.EmailTemplate;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import dao.TalkerDAO;
import dao.TopicDAO;

import static util.DBUtil.*;

public class TalkerBean implements Serializable {
	
	public static final String[] CHILDREN_AGES_ARRAY = new String[] {
		"New born", "1-2 years old", "2-6 years old", 
		"6-12 years old", "12-18 years old", "Over 18 years old"
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
	
	/**
	 * An order of elements is important - displayed on Profile Preference page.
	 * 
	 * TODO: save as array in db?
	 * TODO: user should be able to view own hidden info
	 * Value is needed for conversion EnumSet to integer value (to save in DB).
	 */
	public enum ProfilePreference {
		PERSONAL_INFO(0, "Display my Personal Info to the community (location, age, etc.)"),
		HEALTH_INFO(1, "Display my Health Info to the community (disease, symptoms, medications, etc.)"),
//		BASIC_INFO(2, "Display my Basic Info in my Public Profile (Recognition level, " +
//				"No. of conversations)"),
//		BIO(3, "Display my Bio to the community"),
		FOLLOWERS(4, "Display my Followers to the community"),
		FOLLOWING(5, "Display who I Follow to the community"),
		THANKYOUS(6, "Display my Thank you's to the community"),
		CONVERSATIONS(7, "Display the Conversations I started and joined to the community"),
		COMMENTS(8, "Display my Thoughts Feed to the community"),
		ACTIVITY_STREAM(9, "Display my Activity Stream to the community"),
		CONVERSATIONS_FOLLOWED(10, "Display the Conversations I Follow to the community"),
		TOPICS_FOLLOWED(11, "Display the Topics I Follow to the community");
		
		private final int value;
		private final String description;
		
		private ProfilePreference(int value, String description) {
			this.value = value;
			this.description = description;
		}

		public int getValue() { return value; }

		public String getDescription() { return description; }
	}
	
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
	@Required private String password;
	private String confirmPassword;
	@Required @Email private String email;
	private String verifyCode;	//code for email verification
	private Set<EmailBean> emails;
	
	//used to store original userName when account is deactivated
	private String originalUserName;
	private boolean deactivated;
	private boolean suspended;
	
	//Twitter or Facebook account info
	private String accountType;
	private String accountId;

	private String im;
	private String imUsername;
	private Set<IMAccountBean> imAccounts;
	
	//info from professional profiles
	private Map<String, String> profInfo;
	
	private Date dob;
	private int dobMonth;
	private int dobDay;
	private int dobYear;
	private Date regDate;
	
	//Patient/Caregiver/etc.
	private String connection;
	private boolean connectionVerified;
	
	private String firstName;
	private String lastName;
	
	private int childrenNum;
	//for editing (we can't show/save default value with int)
	private String childrenNumStr;
	private List<String> childrenAges;
	private String webpage;
	private List<String> keywords;
	private String bio;
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
	private int numberOfTopics;
	
	//a set of hidden help popups for this user
	private Set<String> hiddenHelps;
	
	//Notifications settings
	private String[] ctype;
	private int nfreq;
	private int ntime;
	
	//TODO: List or (s) ?
	private List<ThankYouBean> thankYouList;
	private int numOfThankYous;
	
	private List<TalkerBean> followingList;
	private List<TalkerBean> followerList;
	private List<Action> activityList;
	private List<CommentBean> profileCommentsList;
	
	//TODO: what's the best solution for partial load?
	private List<String> followingConvosList;
	private List<ConversationBean> followingConvosFullList;
	private List<TopicBean> followingTopicsList;
	private List<ConversationBean> startedTopicsList;
	private List<ConversationBean> joinedTopicsList;
	
	private EnumSet<ProfilePreference> profilePreferences;
	private EnumSet<EmailSetting> emailSettings;
	
	//additional
	private int numOfConvoAnswers;
	
	//variable for displaying
	private String profileCompletionMessage;
	private int profileCompletionValue;
	private String nextStepMessage;
	private String nextStepNote;
	
	private Date latestNotification;
	private long numOfNotifications;
	
	private Map<TopicBean, TalkerTopicInfo> topicsInfoMap;
	
	public TalkerBean(){}
	public TalkerBean(String id) {
		this.id = id;
	}
	public TalkerBean(String id, String userName) {
		this.id = id;
		this.userName = userName;
	}

	public TalkerBean(String id, String userName, String connection, boolean connectionVerified) {
		this.id = id;
		this.userName = userName;
		this.connection = connection;
		this.connectionVerified = connectionVerified;
	}
	
	//TODO: verify equals & hashCode ?
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
	
	
	//TODO: we don't have binder for EnumSet now, so we can't use get&set methods
	// here is workaround
	public EnumSet<ProfilePreference> loadProfilePreferences() {
		return profilePreferences;
	}
	public void saveProfilePreferences(EnumSet<ProfilePreference> profilePreferences) {
		this.profilePreferences = profilePreferences;
	}
	
	public EnumSet<EmailSetting> loadEmailSettings() {
		return emailSettings;
	}
	public void saveEmailSettings(EnumSet<EmailSetting> emailSettings) {
		this.emailSettings = emailSettings;
	}
	
	// ----=================== Parse/Save information from/to DB =================----
	public void parseBasicFromDB(DBObject talkerDBObject) {
		setId(talkerDBObject.get("_id").toString());
		setUserName((String)talkerDBObject.get("uname"));
		setPassword((String)talkerDBObject.get("pass"));
		
		setEmail((String)talkerDBObject.get("email"));
		setEmails(parseSet(EmailBean.class, talkerDBObject, "emails"));
		setVerifyCode((String)talkerDBObject.get("verify_code"));
		
		setOriginalUserName((String)talkerDBObject.get("orig_uname"));
		setDeactivated(getBoolean(talkerDBObject.get("deactivated"))); 
		setSuspended(getBoolean(talkerDBObject.get("suspended")));
		
		setBio((String)talkerDBObject.get("bio"));
		setConnection((String)talkerDBObject.get("connection"));
		setConnectionVerified(getBoolean(talkerDBObject.get("connection_verified"))); 
		
		parseEmailSettings(parseStringList(talkerDBObject.get("email_settings")));
		parseProfilePreferences(parseInt(talkerDBObject.get("prefs")));
		
		Collection<DBObject> thankYousCollection = (Collection<DBObject>)talkerDBObject.get("thankyous");
		setNumOfThankYous(thankYousCollection == null ? 0 : thankYousCollection.size());
	}
	
	public void parseFromDB(DBObject talkerDBObject) {
		parseBasicFromDB(talkerDBObject);
		
		setHiddenHelps(getStringSet(talkerDBObject, "hidden_helps"));
		
		setIm((String)talkerDBObject.get("im"));
		setImUsername((String)talkerDBObject.get("im_uname"));
		setImAccounts(parseSet(IMAccountBean.class, talkerDBObject, "im_accounts"));
		setAccountType((String)talkerDBObject.get("act_type"));
		setAccountId((String)talkerDBObject.get("act_id"));
		
		setNewsletter(getBoolean(talkerDBObject.get("newsletter")));
		setGender((String)talkerDBObject.get("gender"));
		setDob((Date)talkerDBObject.get("dob"));
		setInvitations(parseInt(talkerDBObject.get("invites")));
		
		setCity((String)talkerDBObject.get("city"));
		setState((String)talkerDBObject.get("state"));
		setCountry((String)talkerDBObject.get("country"));
		
		setNfreq(parseInt(talkerDBObject.get("nfreq")));
		setNtime(parseInt(talkerDBObject.get("ntime")));
		Collection<String> ctype = (Collection<String>)talkerDBObject.get("ctype");
		if (ctype != null) {
			setCtype(ctype.toArray(new String[]{}));
		}
		
		setChildrenNum(parseInt(talkerDBObject.get("ch_num")));
		setMaritalStatus((String)talkerDBObject.get("mar_status"));
		setCategory((String)talkerDBObject.get("category"));
		setRegDate((Date)talkerDBObject.get("timestamp"));
		
		setFirstName((String)talkerDBObject.get("firstname"));
		setLastName((String)talkerDBObject.get("lastname"));
		setZip((String)talkerDBObject.get("zip"));
		setWebpage((String)talkerDBObject.get("webpage"));
		setChildrenAges(parseStringList(talkerDBObject.get("ch_ages")));
		setKeywords(parseStringList(talkerDBObject.get("keywords")));
		
		parseThankYous((Collection<DBObject>)talkerDBObject.get("thankyous"));
		
		parseFollowing((Collection<DBRef>)talkerDBObject.get("following"));
		setFollowingConvosList(parseStringList(talkerDBObject.get("following_convos")));
		
		parseFollowingTopics((Collection<DBRef>)talkerDBObject.get("following_topics"));
		parseTopicsInfo((Collection<DBObject>)talkerDBObject.get("topics_info"));
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
				fromTalker.setUserName((String)fromTalkerDBObject.get("uname"));
				fromTalker.setConnection((String)fromTalkerDBObject.get("connection"));
				fromTalker.setConnectionVerified(getBoolean(fromTalkerDBObject.get("connection_verified"))); 
				
				Collection<DBObject> thankYousCollection = (Collection<DBObject>)fromTalkerDBObject.get("thankyous");
				fromTalker.setNumOfThankYous(thankYousCollection == null ? 0 : thankYousCollection.size());
				
				thankYouBean.setFrom(fromTalkerDBObject.get("_id").toString());
				thankYouBean.setFromTalker(fromTalker);
				
				thankYous.add(thankYouBean);
			}
		}
		
		Collections.sort(thankYous);
		thankYouList = thankYous;
	}
	
	private void parseFollowing(Collection<DBRef> followingDBList) {
		followingList = new ArrayList<TalkerBean>();
		if (followingDBList != null) {
			for (DBRef followingDBRef : followingDBList) {
				DBObject followingDBOBject = followingDBRef.fetch();
				
				boolean isDeactivated = getBoolean(followingDBOBject.get("deactivated"));
				boolean isSuspended = getBoolean(followingDBOBject.get("suspended"));
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
		followingTopicsList = new ArrayList<TopicBean>();
		if (followingTopicsDBList != null) {
			for (DBRef topicDBRef : followingTopicsDBList) {
				TopicBean topic = new TopicBean();
				if (topicDBRef.fetch() != null) {
					topic.parseBasicFromDB(topicDBRef.fetch());
					if (!topic.isDeleted()) {
						followingTopicsList.add(topic);
					}
				}
			}
		}
	}
	
	private void parseTopicsInfo(Collection<DBObject> topicsInfoCol) {
		topicsInfoMap = new HashMap<TopicBean, TalkerTopicInfo>();
		
		if (topicsInfoCol != null) {
			for (DBObject topicInfoDBObject : topicsInfoCol) {
				//topic
				DBObject topicDBObject = ((DBRef)topicInfoDBObject.get("topic")).fetch();
				TopicBean topic = new TopicBean();
				topic.parseBasicFromDB(topicDBObject);
				
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
	
	/* 
	 * Convert from Integer to ProfilePreferences EnumSet and vice versa 
	 */
	public int profilePreferencesToInt() {
		int dbValue = 0;
		for (ProfilePreference preference : profilePreferences) {
			dbValue |= (1 << preference.getValue());
		}
		return dbValue;
	}
	
	public void parseProfilePreferences(int dbValue) {
		profilePreferences = EnumSet.noneOf(ProfilePreference.class);
		for (ProfilePreference preference : ProfilePreference.values()) {
			if ( (dbValue & (1 << preference.getValue())) != 0) {
				profilePreferences.add(preference);
			}
		}
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
	
	//TODO: move to some common DB function? 
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
	
	
	//TODO: make private and unify with all other getters
	public boolean getBoolean(Object object) {
		if (object == null) {
			return false;
		}
		else {
			return ((Boolean)object).booleanValue();
		}
	}

	public List<String> parseStringList(Object fieldValue) {
		Collection<String> fieldCollection = (Collection<String>)fieldValue;
		if (fieldCollection == null) {
			//TODO: empty list or null?
			return new ArrayList<String>();
		}
		else {
			return new ArrayList<String>(fieldCollection);
		}
	}

	public int parseInt(Object value) {
		if (value == null) {
			return 0;
		}
		else {
			return (Integer)value;
		}
	}
	
	public EmailBean findNonPrimaryEmail(String newPrimaryEmail, String verifyCode) {
		EmailBean nonPrimaryEmail = null;
		for (EmailBean emailBean : getEmails()) {
			if (emailBean.getValue().equals(newPrimaryEmail)) {
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
	
	
	// ----========= Useful methods for displaying data ============----
	public boolean isAllowed(ProfilePreference preference) {
		if (profilePreferences == null) {
			return false;
		}
		return profilePreferences.contains(preference);
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
		List<String> cTypesList = new ArrayList<String>();
		//TODO: make constant list?
		List<String> standardTypes = 
			Arrays.asList("Informational", "Advice and opinions", "Meet new people", "Emotional support");
		for (String ctypeEntry : ctype) {
			if (!standardTypes.contains(ctypeEntry)) {
				cTypesList.add(ctypeEntry);
			}
		}
		
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

	public String getAccountType() { return accountType; }
	public void setAccountType(String accountType) { this.accountType = accountType; }

	public String getAccountId() { return accountId; }
	public void setAccountId(String accountId) { this.accountId = accountId; }
	
	public String getIm() { return im; }
	public void setIm(String im) { this.im = im; }
	
	public int getNumberOfTopics() { return numberOfTopics; }
	public void setNumberOfTopics(int numberOfTopics) { this.numberOfTopics = numberOfTopics; }

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

	public List<String> getFollowingConvosList() {
		return followingConvosList;
	}
	public void setFollowingConvosList(List<String> followingConvosList) {
		this.followingConvosList = followingConvosList;
	}
	
	public List<ConversationBean> getFollowingConvosFullList() {
		return followingConvosFullList;
	}
	public void setFollowingConvosFullList(
			List<ConversationBean> followingConvosFullList) {
		this.followingConvosFullList = followingConvosFullList;
	}
	public boolean isDeactivated() { return deactivated; }
	public void setDeactivated(boolean deactivated) { this.deactivated = deactivated; }

	public List<ConversationBean> getStartedTopicsList() { return startedTopicsList; }
	public void setStartedTopicsList(List<ConversationBean> startedTopicsList) { this.startedTopicsList = startedTopicsList; }

	public List<ConversationBean> getJoinedTopicsList() { return joinedTopicsList; }
	public void setJoinedTopicsList(List<ConversationBean> joinedTopicsList) { this.joinedTopicsList = joinedTopicsList; }
	
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

	//FIXME: fix limit number of activities?
	public List<Action> getActivityList() { return activityList; }
	public void setActivityList(List<Action> activityList) { this.activityList = activityList; }

	public List<TalkerBean> getFollowerList() { return followerList; }
	public void setFollowerList(List<TalkerBean> followerList) { this.followerList = followerList; }

	public List<TalkerBean> getFollowingList() { return followingList; }
	public void setFollowingList(List<TalkerBean> followingList) { this.followingList = followingList; }

	public List<ThankYouBean> getThankYouList() { return thankYouList; }
	public void setThankYouList(List<ThankYouBean> thankYouList) { this.thankYouList = thankYouList; }

	public String getProfileCompletionMessage() {
		return profileCompletionMessage;
	}
	public void setProfileCompletionMessage(String profileCompletionMessage) {
		this.profileCompletionMessage = profileCompletionMessage;
	}
	public int getProfileCompletionValue() {
		return profileCompletionValue;
	}
	public void setProfileCompletionValue(int profileCompletionValue) {
		this.profileCompletionValue = profileCompletionValue;
	}
	public Set<EmailBean> getEmails() {
		return emails;
	}
	public void setEmails(Set<EmailBean> emails) {
		this.emails = emails;
	}
	public Set<IMAccountBean> getImAccounts() {
		return imAccounts;
	}
	public void setImAccounts(Set<IMAccountBean> imAccounts) {
		this.imAccounts = imAccounts;
	}
	public Date getLatestNotification() {
		return latestNotification;
	}
	public void setLatestNotification(Date latestNotification) {
		this.latestNotification = latestNotification;
	}
	public long getNumOfNotifications() {
		return numOfNotifications;
	}
	public void setNumOfNotifications(long numOfNotifications) {
		this.numOfNotifications = numOfNotifications;
	}
	public int getNumOfThankYous() {
		return numOfThankYous;
	}
	public void setNumOfThankYous(int numOfThankYous) {
		this.numOfThankYous = numOfThankYous;
	}
	public List<TopicBean> getFollowingTopicsList() {
		return followingTopicsList;
	}
	public void setFollowingTopicsList(List<TopicBean> followingTopicsList) {
		this.followingTopicsList = followingTopicsList;
	}
	public boolean isSuspended() {
		return suspended;
	}
	public void setSuspended(boolean suspended) {
		this.suspended = suspended;
	}
	public Map<TopicBean, TalkerTopicInfo> getTopicsInfoMap() {
		return topicsInfoMap;
	}
	public void setTopicsInfoMap(Map<TopicBean, TalkerTopicInfo> topicsInfoMap) {
		this.topicsInfoMap = topicsInfoMap;
	}
	public String getOriginalUserName() {
		return originalUserName;
	}
	public void setOriginalUserName(String originalUserName) {
		this.originalUserName = originalUserName;
	}
	public int getNumOfConvoAnswers() {
		return numOfConvoAnswers;
	}
	public void setNumOfConvoAnswers(int numOfConvoAnswers) {
		this.numOfConvoAnswers = numOfConvoAnswers;
	}
	public String getChildrenNumStr() {
		return childrenNumStr;
	}
	public void setChildrenNumStr(String childrenNumStr) {
		this.childrenNumStr = childrenNumStr;
	}
	public Set<String> getHiddenHelps() {
		return hiddenHelps;
	}
	public void setHiddenHelps(Set<String> hiddenHelps) {
		this.hiddenHelps = hiddenHelps;
	}
	public String getConfirmPassword() {
		return confirmPassword;
	}
	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}
	public Map<String, String> getProfInfo() {
		return profInfo;
	}
	public void setProfInfo(Map<String, String> profInfo) {
		this.profInfo = profInfo;
	}
	public String getNextStepMessage() {
		return nextStepMessage;
	}
	public void setNextStepMessage(String nextStepMessage) {
		this.nextStepMessage = nextStepMessage;
	}
	public String getNextStepNote() {
		return nextStepNote;
	}
	public void setNextStepNote(String nextStepNote) {
		this.nextStepNote = nextStepNote;
	}
	
}	

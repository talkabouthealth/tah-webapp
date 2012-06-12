package models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import logic.TalkerLogic;
import models.CommentBean.Vote;
import models.PrivacySetting.PrivacyType;
import models.PrivacySetting.PrivacyValue;
import models.ServiceAccountBean.ServiceType;
import models.actions.Action;

import org.bson.types.ObjectId;

import play.Logger;
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
	
	public static final String[] CONNECTIONS_ARRAY = new String[] {
		//"Patient","High Risk Individual","Former Patient",
		"High Risk Individual","Just Diagnosed","Current Patient","Survivor (1 year)","Survivor (2 - 5 years)",
		"Survivor (5 - 10 years)","Survivor (10 - 20 years)","Survivor (Greater than 20 years)","",
		"Parent" ,"Caregiver", "Family member", "Friend","",
		//"professionals"
		"Physician", "Pharmacist", "Nurse", "Psychologist","Professional Therapist", "Social worker", "Complementary Care Expert","","Researcher", 
		"Organization","Support Group",
		 "other"
	};
	public static final List<String> CONNECTIONS_INTERRUPTORS = Arrays.asList(
		"Survivor (Greater than 20 years)","Friend","Researcher"
	);
	// only professionals in this list
	public static final List<String> PROFESSIONAL_CONNECTIONS_LIST = Arrays.asList(
		"Physician", "Pharmacist", "Nurse", "Psychologist","Professional Therapist", "Social worker","Complementary Care Expert","Researcher", "Organization","Support Group"
	);
	// only organizations in this list
	public static final List<String> ORGANIZATIONS_CONNECTIONS_LIST = Arrays.asList(
		"Organization","Support Group"
	);	
	public static final List<String> HIGH_RISK_QUESTIONS_SET = Arrays.asList(
			"hrgeneral","hrgenetic","hrbenigh1","hrbenigh2"
	);
	public static final String[] CHILDREN_AGES_ARRAY = new String[] {
		"New born", "1-2 years old", "2-6 years old", 
		"6-12 years old", "12-18 years old", "Over 18 years old"
	};
	public static final String[] ETHNICITY_ARRAY = new String[] {
		"Asian", "Middle Eastern", "Black", "Native American",
		"Indian", "Pacific Islander", "Hispanic / Latin", "White",
		"Other", "Undeclared"
	};
	public static final String[] LANGUAGE_ARRAY = new String[] {
		"English", "Afrikaans", "Albanian", "Arabic", 
		"Basque", "Belarusan", "Bengali", "Breton", "Bulgarian", 
		"Catalan", "Cebuano", "Chechen", "Chinese", "Croatian", "Czech", 
		"Danish", "Dutch", "Esperanto", "Estonian", "Farsi", "Finnish", "French", "Frisian", 
		"Georgian", "German", "Greek", "Ancient Greek", "Hawaiian", "Hebrew", "Hindi", "Hungarian", 
		"Icelandic", "Ilongo", "Indonesian", "Irish", "Italian", "Japanese", "Khmer", "Korean", 
		"Latin", "Latvian", "Lithuanian", "Malay", "Maori", "Mongolian", "Norwegian", 
		"Occitan", "Other", "Persian", "Polish", "Portuguese", "Romanian", "Rotuman", "Russian", 
		"Sanskrit", "Sardinian", "Serbian", "Sign Language", "Slovak", "Slovenian", "Spanish", "Swahili", "Swedish", 
		"Tagalog", "Tamil", "Thai", "Tibetan", "Turkish", "Ukrainian", "Urdu", "Vietnamese", "Welsh", "Yiddish"
	};
	public static final String[] CONVERSATIONS_TYPES_ARRAY = new String[] {
		"Informational", "Advice and opinions", "Meet new people", "Emotional support"
	};
	
	//Convo-related items start with "CONVO" - we use it for display
	public enum EmailSetting {
		RECEIVE_COMMENT ("", EmailTemplate.NOTIFICATION_PROFILE_COMMENT),//Send me an email when I receive a comment in my Thoughts Feed.
		
		RECEIVE_THOUGHT_MENTION ("Send me mail when I am mentioned", EmailTemplate.NOTIFICATION_OF_THOUGHT_MENTION),
		RECEIVE_ANSWER_MENTION ("", EmailTemplate.NOTIFICATION_OF_ANSWER_MENTION),
		
		RECEIVE_THOUGHT ("Send me an email when I receive a Thought or reply to a Thought.", EmailTemplate.NOTIFICATION_PROFILE_COMMENT),
		RECEIVE_THANKYOU ("Send me an email when I receive a 'Thank You' or reply to 'Thank You'.", EmailTemplate.NOTIFICATION_THANKYOU), //Send me an email when I receive a 'Thank You'.
		RECEIVE_DIRECT ("Send me an email when I receive a Direct Message.", EmailTemplate.NOTIFICATION_DIRECT_MESSAGE),
		NEW_FOLLOWER ("Send me an email when someone follows me.", EmailTemplate.NOTIFICATION_FOLLOWER),
		
		CONVO_RESTART ("",
				EmailTemplate.NOTIFICATION_CONVO_RESTART),//Send me an email when a Conversation I follow is re-started.
		CONVO_COMMENT ("Send me an email when an Answer is added to a Question I follow.", 
				EmailTemplate.NOTIFICATION_CONVO_ANSWER),
		CONVO_SUMMARY ("Send me an email when a Answer Summary is edited or updated for a Question I follow.", 
				EmailTemplate.NOTIFICATION_CONVO_SUMMARY),
		CONVO_PERSONAL ("",
				EmailTemplate.NOTIFICATION_PERSONAL_QUESTION),
		NOTIFY_CONVO ("Notify me of relevant questions via email.",
				EmailTemplate.NOTIFICATION_CONVO_RESTART),
		REPLY_TO_THANKYOU ("",
				EmailTemplate.NOTIFICATION_REPLY_TO_THANKYOU);

		
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
	
	//Added filed for old verification code
	private String oldVerifyCode;
	
	//not-primary emails
	private Set<EmailBean> emails;
	
	private String firstName;
	private String lastName;
	
	private String profileName;
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
	private Set<String> ethnicities;
	private String religion;
	private String religionSerious;
	private List<LanguageBean> languagesList = new ArrayList<LanguageBean>();
	
	private String webpage;
	private List<String> keywords;
	private String bio;
	private String profStatement;
	private boolean newsletter;
	private boolean workshop;
	private boolean workshopSummery;
	private String gender;
	private int invitations;
	private String maritalStatus;
	//Breast Cancer, etc.
	private String category;
	private String city;
	private String state;
	private String country;
	private String zip;
	private boolean passwordUpdate;
	
	//Contains different settings for this user
	//(e.g. name of help panels that were hidden by this user)
	private Set<String> hiddenHelps = new HashSet<String>();
	
	//notifications settings
	//what conversations interesting?
	private String[] ctype;
	private List<String> otherCtype;
	//how often?
	private int nfreq;
	//when?
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
	
	//For supporting multiple categories to talker.
	private String[] otherCategories;
	
	private NewsLetterBean newsLetterBean;
	
	public TalkerBean(){}
	public TalkerBean(String id) {
		this.id = id;
	}
	public TalkerBean(String id, String userName) {
		this.id = id;
		this.userName = userName;
	}

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
		setProfileName((String)talkerDBObject.get("profilename"));
		setAnonymousName((String)talkerDBObject.get("anon_name"));
		setPassword((String)talkerDBObject.get("pass"));
		setEmail((String)talkerDBObject.get("email"));
		setEmails(parseSet(EmailBean.class, talkerDBObject, "emails"));
		setVerifyCode((String)talkerDBObject.get("verify_code"));
		setOldVerifyCode((String)talkerDBObject.get("old_verify_code"));
		setCategory((String)talkerDBObject.get("category"));
		setRegDate((Date)talkerDBObject.get("timestamp"));
		
		setOriginalUserName((String)talkerDBObject.get("orig_uname"));
		setDeactivated(getBoolean(talkerDBObject, "deactivated")); 
		setSuspended(getBoolean(talkerDBObject, "suspended"));
		
		setBio((String)talkerDBObject.get("bio"));
		setConnection((String)talkerDBObject.get("connection"));
		setConnectionVerified(getBoolean(talkerDBObject, "connection_verified")); 
		setProfStatement((String)talkerDBObject.get("prof_statement"));
		parseProfInfo((DBObject)talkerDBObject.get("prof_info"));
		
		setIm((String)talkerDBObject.get("im"));
		setImUsername((String)talkerDBObject.get("im_uname"));
		setImNotify(getBoolean(talkerDBObject, "im_notify"));
		setImAccounts(parseSet(IMAccountBean.class, talkerDBObject, "im_accounts"));
		setServiceAccounts(parseSet(ServiceAccountBean.class, talkerDBObject, "service_accounts"));
		
		parseEmailSettings(getStringList(talkerDBObject, "email_settings"));
		setPrivacySettings(parseSet(PrivacySetting.class, talkerDBObject, "privacy_settings"));
		
		Collection<String> otherCategories = (Collection<String>)talkerDBObject.get("otherCategories");
		if (otherCategories != null) {
			setOtherCategories(otherCategories.toArray(new String[]{}));
		}
		
		Collection<DBObject> thankYousCollection = (Collection<DBObject>)talkerDBObject.get("thankyous");
		setNumOfThankYous(thankYousCollection == null ? 0 : thankYousCollection.size());
		setPasswordUpdate(getBoolean(talkerDBObject,"password_update"));
	}
	
	public void parseFromDB(DBObject talkerDBObject) {
		parseBasicFromDB(talkerDBObject);
		
		setHiddenHelps(getStringSet(talkerDBObject, "hidden_helps"));
		setNfreq(getInt(talkerDBObject, "nfreq"));
		setNtime(getInt(talkerDBObject, "ntime"));
		Collection<String> ctype = (Collection<String>)talkerDBObject.get("ctype");
		if (ctype != null) {
			setCtype(ctype.toArray(new String[]{}));
		}
		setOtherCtype(getStringList(talkerDBObject, "ctype_other"));
		
		setNewsletter(getBoolean(talkerDBObject, "newsletter"));
		setWorkshop(getBoolean(talkerDBObject, "workshop"));
		setWorkshopSummery(getBoolean(talkerDBObject, "workshopSummery"));
		setGender((String)talkerDBObject.get("gender"));
		setDob((Date)talkerDBObject.get("dob"));
		setInvitations(getInt(talkerDBObject, "invites"));
		setCity((String)talkerDBObject.get("city"));
		setState((String)talkerDBObject.get("state"));
		setCountry((String)talkerDBObject.get("country"));
		setChildrenNum(getInt(talkerDBObject, "ch_num"));
		setMaritalStatus((String)talkerDBObject.get("mar_status"));
		setFirstName((String)talkerDBObject.get("firstname"));
		setLastName((String)talkerDBObject.get("lastname"));
		setZip((String)talkerDBObject.get("zip"));
		setWebpage((String)talkerDBObject.get("webpage"));
		setChildrenAges(getStringList(talkerDBObject, "ch_ages"));
		setKeywords(getStringList(talkerDBObject, "keywords"));
		setEthnicities(getStringSet(talkerDBObject, "ethnicities"));
		setReligion((String)talkerDBObject.get("religion"));
		setReligionSerious((String)talkerDBObject.get("religion_serious"));
		parseLanguages((Collection<DBObject>)talkerDBObject.get("languages"));
		
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
				thankYouBean.setId((String)thankYouDBObject.get("id"));
				thankYouBean.setTime((Date)thankYouDBObject.get("time"));
				thankYouBean.setNote((String)thankYouDBObject.get("note"));
				thankYouBean.setTo(getId());
				
				TalkerBean fromTalker = TalkerDAO.parseTalker(thankYouDBObject, "from");
				if(fromTalker != null){
				thankYouBean.setFrom(fromTalker.getId());
				thankYouBean.setFromTalker(fromTalker);
				}
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
				TalkerBean followingTalker = 
					TalkerLogic.loadTalkerFromCache(followingDBRef.getId().toString());
				if (followingTalker != null && ! (followingTalker.isDeactivated() || followingTalker.isSuspended()) ) {
					followingList.add(followingTalker);
				}
			}
		}
	}
	
	private void parseFollowingTopics(Collection<DBRef> followingTopicsDBList) {
		followingTopicsList = new LinkedHashSet<TopicBean>();
		if (followingTopicsDBList != null) {
			for (DBRef topicDBRef : followingTopicsDBList) {
				TopicBean followingTopic = 
					TalkerLogic.loadTopicFromCache(topicDBRef.getId().toString());
				
				if (!followingTopic.isDeleted()) {
					followingTopicsList.add(followingTopic);
				}
			}
		}
	}
	
	private void parseLanguages(Collection<DBObject> languagesDBList) {
		languagesList = new ArrayList<LanguageBean>();
		if (languagesDBList != null) {
			int count = 0;
			for (DBObject langDBObject : languagesDBList) {
				LanguageBean language = new LanguageBean();
				language.parseDBObject(langDBObject);
				if (count == 0) {
					//first language is English by default
					language.setName("English");
				}
				count++;
				languagesList.add(language);
			}
		}
		if (languagesList.size() == 0) {
			languagesList.add(new LanguageBean("English", null));
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
					endorsements.add(TalkerDAO.parseTalker(talkerDBRef));
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
	
	public List<DBObject> languagesToDB() {
		List<DBObject> languagesDBList = new ArrayList<DBObject>();
		for (LanguageBean language : getLanguagesList()) {
			languagesDBList.add(language.toDBObject());
		}
		return languagesDBList;
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
	
	/* -------------------- Different useful methods --------------------- */
	
	/**
	 * Returns userName or anonymous name, 
	 * based on privacy settings and current logged-in talker
	 */
	public String getName() {
		String loggedinUsername = Session.current().get("username");
		
		PrivacyValue privacyValue = getPrivacyValue(PrivacyType.USERNAME);
		if (privacyValue == PrivacyValue.PUBLIC ){
			return getUserName();
		}else if ( loggedinUsername != null ){
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
	
	public ServiceAccountBean getTwitterAccount() {
		return this.serviceAccountByType(ServiceType.TWITTER);
	}

	public ServiceAccountBean getFacebookAccount() {
		return this.serviceAccountByType(ServiceType.FACEBOOK);
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
	 * Parse professional info from http params (submitted from the form)
	 * @param paramsMap
	 * @return
	 */
	public Map<String, String> parseProfInfoFromParams(Map<String, String> paramsMap) {
		Map<String, String> profInfo = new HashMap<String, String>();
		//parse "pr_" fields - proffesional fields
		for (Entry<String, String> param : paramsMap.entrySet()) {
			if (param.getKey().startsWith("pr_")) {
				//profile info parameter
				String value = param.getValue();
				if (value != null && value.equals("(separate by commas if multiple)")) {
					value = null;
				}
				
				//remove "pr_" part and add to map
				profInfo.put(param.getKey().substring(3), value);
			}
		}
		return profInfo;
	}
	
	/**
	 * Is this talker professional?
	 * @return
	 */
	public boolean isProf() {
		return PROFESSIONAL_CONNECTIONS_LIST.contains(connection);
	}
	
	public boolean isOrg() {
		return ORGANIZATIONS_CONNECTIONS_LIST.contains(connection);
	}
	
	static public boolean isInterruptor(String name) {
		return TalkerBean.CONNECTIONS_INTERRUPTORS.contains(name);
	}	
	
	public boolean isHighRiskQ(String name) {
		return HIGH_RISK_QUESTIONS_SET.contains(name);
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
		else if ("O".equals(gender)) {
			return "Other";
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
	
	public ThankYouBean getThankYouById(String thankYouId) {
		if (getThankYouList() == null) {
			return null;
		}
		for (ThankYouBean thankYou : getThankYouList()) {
			if (thankYou.getId().equals(thankYouId)) {
				return thankYou;
			}
		}
		return null;
	}
	
	public String getLanguagesAsString() {
		StringBuilder languagesString = new StringBuilder();
		for (LanguageBean language : getLanguagesList()) {
			if (language.getName() != null && language.getName().length() > 0) {
				languagesString.append(language.toString()).append(", ");
			}
		}
		//remove last comma
		languagesString.delete(languagesString.length()-2, languagesString.length());
		return languagesString.toString();
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

	public String getCategory() { 
		//if(category == null){
			//return "Breast Cancer";//Breast Cancer
		//}//else if(category.trim().equals(""))
		//	return "Breast Cancer";//Breast Cancer
		//else
			return category;
	}
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
	
	public String getOldVerifyCode() { return oldVerifyCode; }
	public void setOldVerifyCode(String oldVerifyCode) { this.oldVerifyCode = oldVerifyCode; }
	
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

	public Set<String> getEthnicities() {
		return ethnicities;
	}
	public void setEthnicities(Set<String> ethnicities) {
		this.ethnicities = ethnicities;
	}
	public String getReligion() {
		return religion;
	}
	public void setReligion(String religion) {
		this.religion = religion;
	}
	public String getReligionSerious() {
		return religionSerious;
	}
	public void setReligionSerious(String religionSerious) {
		this.religionSerious = religionSerious;
	}
	public List<LanguageBean> getLanguagesList() {
		return languagesList;
	}
	public void setLanguagesList(List<LanguageBean> languagesList) {
		this.languagesList = languagesList;
	}
	public List<String> getOtherCtype() {
		return otherCtype;
	}
	public void setOtherCtype(List<String> otherCtype) {
		this.otherCtype = otherCtype;
	}
	public String[] getOtherCategories() {
		return otherCategories;
	}
	public void setOtherCategories(String[] otherCategories) {
		this.otherCategories = otherCategories;
	}
	public boolean isWorkshop() {
		return workshop;
	}
	public void setWorkshop(boolean workshop) {
		this.workshop = workshop;
	}
	public NewsLetterBean getNewsLetterBean() {
		return newsLetterBean;
	}
	public void setNewsLetterBean(NewsLetterBean newsLetterBean) {
		this.newsLetterBean = newsLetterBean;
	}
	public boolean isPasswordUpdate() {
		return passwordUpdate;
	}
	public void setPasswordUpdate(boolean passwordUpdate) {
		this.passwordUpdate = passwordUpdate;
	}
	public String getProfileName() {
		return profileName;
	}
	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}
	public boolean isWorkshopSummery() {
		return workshopSummery;
	}
	public void setWorkshopSummery(boolean workshopSummery) {
		this.workshopSummery = workshopSummery;
	}
}
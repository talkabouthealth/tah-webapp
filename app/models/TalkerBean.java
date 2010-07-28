package models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import org.bson.types.ObjectId;

import play.data.validation.Email;
import play.data.validation.Match;
import play.data.validation.Required;
import play.i18n.Messages;
import util.ValidateData;

import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

public class TalkerBean implements Serializable {
	
	//TODO: move it to some constants?
	public static final String DEFAULT_IMAGE = "/public/images/img1.gif"; 
	public static final String[] CHILDREN_AGES_ARRAY = new String[] {
		"New born", "1-2 years old", "2-6 years old", "6-12 years old", "12-18 years old"
	};
	public static final String[] CONNECTIONS_ARRAY = new String[] {
		"Patient", "Former Patient", "Parent", "Caregiver", "Family member", "Friend", 
		 "other"
		
		/*
		  	Temporarily disabled items
		  	http://www.pivotaltracker.com/story/show/4460673
		  	"Physician", "Pharmacist", "Nurse", "Psychiatrist", "Social worker"
		 */
	};
	
	public enum ProfilePreference {
		PERSONAL_INFO(1, "Display my Personal Info in my Public Profile"),
		HEALTH_INFO(2, "Display my Health Info in my Public Profile"),
		BASIC_INFO(4, "Display my Basic Info in my Public Profile (Recognition level, " +
				"No. of conversations)"),
		FOLLOWERS(8, "Display my Followers in my Public Profile"),
		FOLLOWING(16, "Display who I am Following in my Public Profile"),
		CONVERSATIONS(32, "Display the Conversations I have started and joined in my Public Profile"),
		COMMENTS(64, "Display my Comments in my Public Profile"),
		BIO(128, "Display my Bio in my Public Profile"),
		ACTIVITY_STREAM(256, "Display my Activity Stream in my Public Profile");
		
		private final int value;
		private final String description;
		
		private ProfilePreference(int value, String description) {
			this.value = value;
			this.description = description;
		}

		public int getValue() {
			return value;
		}

		public String getDescription() {
			return description;
		}
	}
	
	private String id;
	@Required
	@Match(ValidateData.USER_REGEX)
	private String userName;
	@Required
	@Match(ValidateData.USER_REGEX)
	private String password;
	@Required
	@Email
	private String email;
	
	private String im;
	private String imUsername;
	
	private Date dob;
	private int dobMonth;
	private int dobDay;
	private int dobYear;
	
	private String gender;
	private int invitations;
	private String maritalStatus;
	private String Category;
	//Patient/Caregiver/etc.
	private String connection;
	public String getConnection() {
		return connection;
	}

	public void setConnection(String connection) {
		this.connection = connection;
	}

	private String city;
	private String state;
	private String country;
	
	//types of conversations
	private String[] ctype;
	private int nfreq;
	private int ntime;
	
	private int childrenNum;
	private String imagePath;
	
	private boolean newsletter;
	private String accountType;
	private String accountId;
	
	private Date regDate;
	
	public Date getRegDate() {
		return regDate;
	}

	public void setRegDate(Date regDate) {
		this.regDate = regDate;
	}

	private List<ThankYouBean> thankYouList;
	private List<TalkerBean> followingList;
	private List<TalkerBean> followerList;
	private List<ActivityBean> activityList;
	private List<CommentBean> profileCommentsList;
	
	
	//new fields
	private String firstName;
	private String lastName;
	private String zip;
	private List<String> childrenAges;
	private String webpage;
	private List<String> keywords;
	private String bio;
	
	
	public List<CommentBean> getProfileCommentsList() {
		return profileCommentsList;
	}

	public void setProfileCommentsList(List<CommentBean> profileCommentsList) {
		this.profileCommentsList = profileCommentsList;
	}

	public List<ActivityBean> getActivityList() {
		return activityList;
	}

	public void setActivityList(List<ActivityBean> activityList) {
		this.activityList = activityList;
	}

	public List<TalkerBean> getFollowerList() {
		return followerList;
	}

	public void setFollowerList(List<TalkerBean> followerList) {
		this.followerList = followerList;
	}

	public List<TalkerBean> getFollowingList() {
		return followingList;
	}

	public void setFollowingList(List<TalkerBean> followingList) {
		this.followingList = followingList;
	}

	public List<ThankYouBean> getThankYouList() {
		return thankYouList;
	}

	public void setThankYouList(List<ThankYouBean> thankYouList) {
		this.thankYouList = thankYouList;
	}

	private int numberOfTopics;
	
	private EnumSet<ProfilePreference> profilePreferences;
	
	public TalkerBean(){}
	
	public TalkerBean(String id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof TalkerBean)) {
			return false;
		}
		
		TalkerBean other = (TalkerBean)obj;
		return id.equals(other.id);
	}

	public String getUserName() {
		return userName;
	}
	public String getPassword() {
		return password;
	}
	public void setUserName(String value) {
		userName = value;
	}
	public void setPassword(String value) {
		password = value;
	}
	
	public void parseLoginRequest(String un, String pw){
		setUserName(un);
		setPassword(pw);
	}
	
	public void setEmail(String email) {
		this.email = email;
	}

	public String getEmail() {
		return email;
	}
	
	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public void setDob(Date dob) {
		this.dob = dob;
	}

	public Date getDob() {
		return dob;
	}

	public int getInvitations() {
		return invitations;
	}

	public void setInvitations(int invitations) {
		this.invitations = invitations;
	}

	//TODO: we don't have binder for EnumSet now - here is workaround
	public EnumSet<ProfilePreference> loadProfilePreferences() {
		return profilePreferences;
	}

	public void saveProfilePreferences(EnumSet<ProfilePreference> profilePreferences) {
		this.profilePreferences = profilePreferences;
	}
	
	public void parseFromDB(DBObject talkerDBObject) {
		ObjectId objectId = (ObjectId)talkerDBObject.get("_id");
		setId(objectId.toString());
		
		setUserName((String)talkerDBObject.get("uname"));
		setPassword((String)talkerDBObject.get("pass"));
		setEmail((String)talkerDBObject.get("email"));
		setIm((String)talkerDBObject.get("im"));
		setImUsername((String)talkerDBObject.get("im_uname"));
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
		setConnection((String)talkerDBObject.get("connection"));
		setImagePath((String)talkerDBObject.get("img"));
		setRegDate((Date)talkerDBObject.get("timestamp"));
		parseProfilePreferences(parseInt(talkerDBObject.get("prefs")));
		
		setFirstName((String)talkerDBObject.get("firstname"));
		setLastName((String)talkerDBObject.get("lastname"));
		setZip((String)talkerDBObject.get("zip"));
		setWebpage((String)talkerDBObject.get("webpage"));
		setBio((String)talkerDBObject.get("bio"));
		setChildrenAges(parseStringList(talkerDBObject.get("ch_ages")));
		setKeywords(parseStringList(talkerDBObject.get("keywords")));
		
		parseThankYous((Collection<DBObject>)talkerDBObject.get("thankyous"));
		parseFollowing((Collection<DBRef>)talkerDBObject.get("following"));
	}
	
	private List<String> parseStringList(Object fieldValue) {
		Collection<String> fieldCollection = (Collection<String>)fieldValue;
		if (fieldCollection == null) {
			return null;
		}
		else {
			return new ArrayList<String>(fieldCollection);
		}
	}
	
	private void parseThankYous(Collection<DBObject> thankYouDBList) {
		//TODO: move thanks you load to separate function (to prevent delays)?
		thankYouList = new ArrayList<ThankYouBean>();
		if (thankYouDBList != null) {
			for (DBObject thankYouDBObject : thankYouDBList) {
				ThankYouBean thankYouBean = new ThankYouBean();
				thankYouBean.setTime((Date)thankYouDBObject.get("time"));
				thankYouBean.setNote((String)thankYouDBObject.get("note"));
				
				DBObject fromTalkerDBObject = ((DBRef)thankYouDBObject.get("from")).fetch();
				TalkerBean fromTalker = new TalkerBean();
				fromTalker.setUserName((String)fromTalkerDBObject.get("uname"));
				fromTalker.setImagePath((String)fromTalkerDBObject.get("img"));
				
				thankYouBean.setFrom(fromTalkerDBObject.get("_id").toString());
				thankYouBean.setFromTalker(fromTalker);
				
				thankYouList.add(thankYouBean);
			}
		}
	}
	
	private void parseFollowing(Collection<DBRef> followingDBList) {
		followingList = new ArrayList<TalkerBean>();
		if (followingDBList != null) {
			for (DBRef followingDBRef : followingDBList) {
				TalkerBean followingTalker = new TalkerBean();
				
				DBObject followingDBOBject = followingDBRef.fetch();
				followingTalker.setId(followingDBOBject.get("_id").toString());
				followingTalker.setUserName(followingDBOBject.get("uname").toString());
				followingTalker.setImagePath((String)followingDBOBject.get("img"));
				
				followingList.add(followingTalker);
			}
		}
	}

	private int parseInt(Object value) {
		if (value == null) {
			return 0;
		}
		else {
			return (Integer)value;
		}
	}
	
	/* 
	 * Convert from Integer to ProfilePreferences EnumSet and vice versa 
	 */
	public int profilePreferencesToInt() {
		int dbValue = 0;
		for (ProfilePreference preference : profilePreferences) {
			dbValue |= preference.getValue();
		}
		return dbValue;
	}
	
	public void parseProfilePreferences(int dbValue) {
		profilePreferences = EnumSet.noneOf(ProfilePreference.class);
		for (ProfilePreference preference : ProfilePreference.values()) {
			if ((dbValue & preference.getValue()) != 0) {
				profilePreferences.add(preference);
			}
		}
	}
	
	public long getAge() {
		Date now = new Date();
        long delta = (now.getTime() - dob.getTime()) / 1000;

        long years = delta / (365 * 24 * 60 * 60);
        return years;
	}
	
	public String getFullGender() {
		if ("F".equals(gender)) {
			return "Female";
		}
		else {
			return "Male";
		}
	}
	
	/*
		New Member - 0 to 5 conversations
		Supporter - 5 to 15 conversations
		Companion - 15 to 35 conversations
		Advocate - 35 to 75 conversations
		Fellow - 75 to 200 conversations
		Benefactor - 200 to 500 conversations
		Patron - 500 to 1,000 conversations
		Champion - over 1,000 conversations
	*/
	public String getLevelOfRecognition() {
		String levelOfRecognition = "New Member";
		
		//TODO: better implementation?
		if (numberOfTopics >= 5 && numberOfTopics < 15) {
			levelOfRecognition = "Supporter";
		}
		else if (numberOfTopics >= 15 && numberOfTopics < 35) {
			levelOfRecognition = "Companion";
		}
		else if (numberOfTopics >= 35 && numberOfTopics < 75) {
			levelOfRecognition = "Advocate";
		}
		else if (numberOfTopics >= 75 && numberOfTopics < 200) {
			levelOfRecognition = "Fellow";
		}
		else if (numberOfTopics >= 200 && numberOfTopics < 500) {
			levelOfRecognition = "Benefactor";
		}
		else if (numberOfTopics >= 500 && numberOfTopics < 1000) {
			levelOfRecognition = "Patron";
		}
		else if (numberOfTopics >= 1000) {
			levelOfRecognition = "Champion";
		}
		
		return levelOfRecognition;
	}
	
	public String getMaritalStatus() {
		return maritalStatus;
	}

	public void setMaritalStatus(String maritalStatus) {
		this.maritalStatus = maritalStatus;
	}

	public String getCategory() {
		return Category;
	}

	public void setCategory(String category) {
		Category = category;
	}

	public String getCity() {
		if (city == null) {
			return "";
		}
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		if (state == null) {
			return "";
		}
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getCountry() {
		if (country == null) {
			return "";
		}
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public int getNfreq() {
		return nfreq;
	}

	public void setNfreq(int nfreq) {
		this.nfreq = nfreq;
	}

	public int getNtime() {
		return ntime;
	}

	public void setNtime(int ntime) {
		this.ntime = ntime;
	}

	public String[] getCtype() {
		return ctype;
	}
	
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
	
	public void setCtype(String[] ctype) {
		this.ctype = ctype;
	}

	public int getChildrenNum() {
		return childrenNum;
	}

	public void setChildrenNum(int childrenNum) {
		this.childrenNum = childrenNum;
	}

	public String getImagePath() {
		if (imagePath == null || imagePath.trim().length() == 0) {
			//return default
			return DEFAULT_IMAGE;
		}
		return imagePath;
	}

	public void setImagePath(String imagePath) {
		if (imagePath == null || imagePath.trim().length() == 0) {
			imagePath = DEFAULT_IMAGE;
		}
		this.imagePath = imagePath;
	}

	public String getImUsername() {
		return imUsername;
	}

	public void setImUsername(String imUsername) {
		this.imUsername = imUsername;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean isNewsletter() {
		return newsletter;
	}

	public void setNewsletter(boolean newsletter) {
		this.newsletter = newsletter;
	}

	public String getAccountType() {
		return accountType;
	}

	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}
	
	public String getIm() {
		return im;
	}

	public void setIm(String im) {
		this.im = im;
	}
	
	public int getNumberOfTopics() {
		return numberOfTopics;
	}

	public void setNumberOfTopics(int numberOfTopics) {
		this.numberOfTopics = numberOfTopics;
	}

	public int getDobMonth() {
		return dobMonth;
	}

	public void setDobMonth(int dobMonth) {
		this.dobMonth = dobMonth;
	}

	public int getDobDay() {
		return dobDay;
	}

	public void setDobDay(int dobDate) {
		this.dobDay = dobDate;
	}

	public int getDobYear() {
		return dobYear;
	}

	public void setDobYear(int dobYear) {
		this.dobYear = dobYear;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	public List<String> getChildrenAges() {
		return childrenAges;
	}

	public void setChildrenAges(List<String> childrenAges) {
		this.childrenAges = childrenAges;
	}

	public String getWebpage() {
		return webpage;
	}

	public void setWebpage(String webpage) {
		this.webpage = webpage;
	}

	public List<String> getKeywords() {
		return keywords;
	}

	public void setKeywords(List<String> keywords) {
		this.keywords = keywords;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}
	
	
}	
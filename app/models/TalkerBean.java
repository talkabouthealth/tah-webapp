package models;

import java.io.Serializable;
import java.util.ArrayList;
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

public class TalkerBean implements Serializable {
	
	public enum ProfilePreference {
		PERSONAL_INFO(1, "Display my Personal Info in my profile"),
		HEALTH_INFO(2, "Display my Health Info in my profile"),
		BASIC_INFO(4, "Display my Basic Info in my profile (Recognition level, " +
				"No. of conversations)"),
		FOLLOWERS(8, "Display my Followers in my profile"),
		FOLLOWING(16, "Display who I am Following in my profile"),
		CONVERSATIONS(32, "Display the Conversations I have started and joined in my profile"),
		COMMENTS(64, "Display my Comments in my profile"),
		BIO(128, "Display my Bio in my profile"),
		ACTIVITY_STREAM(256, "Display my Activity Stream in my profile");
		
		private int value;
		private String description;
		
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
	private String city;
	private String state;
	private String country;
	//conversations types
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
	
	public List<ThankYouBean> getThankYouList() {
		return thankYouList;
	}

	public void setThankYouList(List<ThankYouBean> thankYouList) {
		this.thankYouList = thankYouList;
	}

	private int numberOfTopics;
	
	private EnumSet<ProfilePreference> profilePreferences;
	
	public TalkerBean(){}
	
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
		
		@SuppressWarnings("unchecked")
		Collection<String> ctype = (Collection<String>)talkerDBObject.get("ctype");
		if (ctype != null) {
			setCtype(ctype.toArray(new String[]{}));
		}
		
		setChildrenNum(parseInt(talkerDBObject.get("ch_num")));
		setMaritalStatus((String)talkerDBObject.get("mar_status"));
		setCategory((String)talkerDBObject.get("category"));
		setImagePath((String)talkerDBObject.get("img"));
		setRegDate((Date)talkerDBObject.get("timestamp"));
		parseProfilePreferences(parseInt(talkerDBObject.get("prefs")));
		
//		parseThankYous((Collection<DBObject>)talkerDBObject.get("thankyous"));
	}
	
	private void parseThankYous(Collection<DBObject> thankYouDBList) {
		thankYouList = new ArrayList<ThankYouBean>();
		for (DBObject thankYouDBObject : thankYouDBList) {
			ThankYouBean thankYouBean = new ThankYouBean();
			thankYouBean.setTime((Date)thankYouDBObject.get("time"));
			thankYouBean.setNote((String)thankYouDBObject.get("note"));
			thankYouBean.setFrom(thankYouDBObject.get("from").toString());
			
			thankYouList.add(thankYouBean);
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
		if (imagePath == null) {
			//return default
			return "/public/images/img1.gif";
		}
		return imagePath;
	}

	public void setImagePath(String imagePath) {
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
	
}	
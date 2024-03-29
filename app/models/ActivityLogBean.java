package models;

import org.apache.commons.lang.StringUtils;

import util.DBUtil;

import com.mongodb.DBObject;

public class ActivityLogBean {

	private String id;
	private String ipAddress;
	private String pageName;
	private String pageURL;
	private String referer;
	private String userAgent;
	private String userLanguage;
	private String userCookie;
	private String cancerSite;

	private String sessionId;
	private String timeStamp;

	private String userEmail;
	private String userName;
	//Location Details
	private String userLocationCode;
	private String userLocationCountry;
	private String userLocationState;
	private String userLocationCity;
	private String userLocationLatitude;
	private String userLocationLongitude;

	public ActivityLogBean() { }

	public ActivityLogBean(String ipAddress, String pageName,
			String pageURL, String referer, String userAgent, String sessionId,
			String userEmail, String userName) {
		super();
		this.ipAddress = ipAddress;
		this.pageName = pageName;
		this.pageURL = pageURL;
		this.referer = referer;
		this.userAgent = userAgent;
		this.sessionId = sessionId;
		this.userEmail = userEmail;
		this.userName = userName;
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public String getPageName() {
		return pageName;
	}
	public void setPageName(String pageName) {
		this.pageName = pageName;
	}
	public String getPageURL() {
		return pageURL;
	}
	public void setPageURL(String pageURL) {
		this.pageURL = pageURL;
	}
	public String getReferer() {
		return referer;
	}
	public void setReferer(String referer) {
		this.referer = referer;
	}
	public String getUserAgent() {
		return userAgent;
	}
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}
	public String getSessionId() {
		return sessionId;
	}
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	public String getUserEmail() {
		return userEmail;
	}
	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}
	public String getUserLanguage() {
		return userLanguage;
	}
	public void setUserLanguage(String userLanguage) {
		this.userLanguage = userLanguage;
	}
	public String getUserCookie() {
		return userCookie;
	}
	public void setUserCookie(String userCookie) {
		this.userCookie = userCookie;
	}
	public String getCancerSite() {
		return cancerSite;
	}
	public void setCancerSite(String cancerSite) {
		this.cancerSite = cancerSite;
	}
	public String getUserLocationCode() {
		return userLocationCode;
	}
	public void setUserLocationCode(String userLocationCode) {
		this.userLocationCode = userLocationCode;
	}
	public String getUserLocationCountry() {
		return userLocationCountry;
	}
	public void setUserLocationCountry(String userLocationCountry) {
		this.userLocationCountry = userLocationCountry;
	}
	public String getUserLocationState() {
		return userLocationState;
	}
	public void setUserLocationState(String userLocationState) {
		this.userLocationState = userLocationState;
	}
	public String getUserLocationCity() {
		return userLocationCity;
	}
	public void setUserLocationCity(String userLocationCity) {
		this.userLocationCity = userLocationCity;
	}
	public String getUserLocationLatitude() {
		return userLocationLatitude;
	}
	public void setUserLocationLatitude(String userLocationLatitude) {
		this.userLocationLatitude = userLocationLatitude;
	}
	public String getUserLocationLongitude() {
		return userLocationLongitude;
	}
	public void setUserLocationLongitude(String userLocationLongitude) {
		this.userLocationLongitude = userLocationLongitude;
	}

	public void parseFromDB(DBObject activityLogDBObject) {
		setId(activityLogDBObject.get("_id").toString());

		setIpAddress(activityLogDBObject.get("ipAddress").toString());
		setPageName(activityLogDBObject.get("pageName").toString());
		setPageURL(activityLogDBObject.get("pageURL").toString());
		setReferer(activityLogDBObject.get("referer").toString());
		setSessionId(activityLogDBObject.get("sessionId").toString());
		setUserAgent(activityLogDBObject.get("userAgent").toString());
		setUserLanguage(DBUtil.getString(activityLogDBObject, "userLanguage"));
		setUserCookie(DBUtil.getString(activityLogDBObject, "userCookie"));
		setCancerSite(DBUtil.getString(activityLogDBObject, "cancerSite"));
		setUserEmail(activityLogDBObject.get("userEmail").toString());
		setUserName(activityLogDBObject.get("userName").toString());
		
		//Location
		setUserLocationCode(DBUtil.getString(activityLogDBObject, "userLocationCode"));
		setUserLocationCountry(DBUtil.getString(activityLogDBObject, "userLocationCountry"));
		setUserLocationState(DBUtil.getString(activityLogDBObject, "userLocationState"));
		setUserLocationCity(DBUtil.getString(activityLogDBObject, "userLocationCity"));
		setUserLocationLatitude(DBUtil.getString(activityLogDBObject, "userLocationLatitude"));
		setUserLocationLongitude(DBUtil.getString(activityLogDBObject, "userLocationLongitude"));

		if(StringUtils.isBlank(getUserName())) { //Anonymous
			setUserName("Guest");
		}
		setTimeStamp(activityLogDBObject.get("timestamp").toString());
	}
}
package models;

import org.apache.commons.lang.StringUtils;

import com.mongodb.DBObject;

public class ActivityLogBean {

	private String id;
	private String ipAddress;
	private String pageName;
	private String pageURL;
	private String referer;
	private String userAgent;
	private String sessionId;
	private String timeStamp;

	private String userEmail;
	private String userName;

	public ActivityLogBean() {
	}

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

	public void parseFromDB(DBObject activityLogDBObject) {
		setId(activityLogDBObject.get("_id").toString());
		
		setIpAddress(activityLogDBObject.get("ipAddress").toString());
		setPageName(activityLogDBObject.get("pageName").toString());
		setPageURL(activityLogDBObject.get("pageURL").toString());
		setReferer(activityLogDBObject.get("referer").toString());
		setSessionId(activityLogDBObject.get("sessionId").toString());
		setUserAgent(activityLogDBObject.get("userAgent").toString());
		setUserEmail(activityLogDBObject.get("userEmail").toString());
		setUserName(activityLogDBObject.get("userName").toString());
		
		if(StringUtils.isBlank(getUserName())) { //Anonymous
			setUserName("Guest");
		}
		
		setTimeStamp(activityLogDBObject.get("timestamp").toString());
	}
}
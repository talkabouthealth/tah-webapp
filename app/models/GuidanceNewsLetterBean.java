package models;

import static util.DBUtil.createRef;
import static util.DBUtil.getBoolean;
import static util.DBUtil.getString;
import java.util.Date;
import java.util.Collection;
import util.DBUtil;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.DBRef;



/**
 * Used for storing not-primary emails.
 *
 */
public class GuidanceNewsLetterBean {
	
	private String email;
	private String newsLetterName;
	private String day;
	private String month;
	private String year;
	private boolean isHighRisk;

	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getNewsLetterName() {
		return newsLetterName;
	}
	public void setNewsLetterName(String newsLetterName) {
		this.newsLetterName = newsLetterName;
	}
	public String getDay() {
		return day;
	}
	public void setDay(String day) {
		this.day = day;
	}
	public String getMonth() {
		return month;
	}
	public void setMonth(String month) {
		this.month = month;
	}
	public String getYear() {
		return year;
	}
	public void setYear(String year) {
		this.year = year;
	}
	public boolean isHighRisk() {
		return isHighRisk;
	}
	public void setHighRisk(boolean isHighRisk) {
		this.isHighRisk = isHighRisk;
	}
}
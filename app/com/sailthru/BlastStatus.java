package com.sailthru;

import java.util.Date;

public class BlastStatus extends BaseStatus {
	private int blastId;
	private String name;
	private String list;
	private String fromName;
	private String fromEmail;
	private String replyTo;
	private String subject;
	private String contentHtml;
	private String contentText;
	private boolean googleAnalytics;
	private boolean linkTracking;
	private String reportEmail;
	private Date scheduleTime;
	private Date startTime;
	private Date endTime;
	private String status;
	private int emailCount;
	public int getBlastId() {
		return blastId;
	}
	public void setBlastId(int blastId) {
		this.blastId = blastId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getList() {
		return list;
	}
	public void setList(String list) {
		this.list = list;
	}
	public String getFromName() {
		return fromName;
	}
	public void setFromName(String fromName) {
		this.fromName = fromName;
	}
	public String getFromEmail() {
		return fromEmail;
	}
	public void setFromEmail(String fromEmail) {
		this.fromEmail = fromEmail;
	}
	public String getReplyTo() {
		return replyTo;
	}
	public void setReplyTo(String replyTo) {
		this.replyTo = replyTo;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getContentHtml() {
		return contentHtml;
	}
	public void setContentHtml(String contentHtml) {
		this.contentHtml = contentHtml;
	}
	public String getContentText() {
		return contentText;
	}
	public void setContentText(String contentText) {
		this.contentText = contentText;
	}
	public boolean isGoogleAnalytics() {
		return googleAnalytics;
	}
	public void setGoogleAnalytics(boolean googleAnalytics) {
		this.googleAnalytics = googleAnalytics;
	}
	public boolean isLinkTracking() {
		return linkTracking;
	}
	public void setLinkTracking(boolean linkTracking) {
		this.linkTracking = linkTracking;
	}
	public String getReportEmail() {
		return reportEmail;
	}
	public void setReportEmail(String reportEmail) {
		this.reportEmail = reportEmail;
	}
	public Date getScheduleTime() {
		return scheduleTime;
	}
	public void setScheduleTime(Date scheduleTime) {
		this.scheduleTime = scheduleTime;
	}
	public Date getStartTime() {
		return startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	public Date getEndTime() {
		return endTime;
	}
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public int getEmailCount() {
		return emailCount;
	}
	public void setEmailCount(int emailCount) {
		this.emailCount = emailCount;
	}
	
}

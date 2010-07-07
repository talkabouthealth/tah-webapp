package com.sailthru;

import java.util.HashMap;

public class EmailStatus extends BaseStatus {
	private boolean verified;
	private boolean optout;
	private boolean blacklist;
	private HashMap<String, String> vars;
	private HashMap<String, String> lists;
	private HashMap<String, String> templates;
	
	public boolean isVerified() {
		return verified;
	}
	public void setVerified(boolean verified) {
		this.verified = verified;
	}
	public boolean isOptout() {
		return optout;
	}
	public void setOptout(boolean optout) {
		this.optout = optout;
	}
	public boolean isBlacklist() {
		return blacklist;
	}
	public void setBlacklist(boolean blacklist) {
		this.blacklist = blacklist;
	}
	public HashMap<String, String> getVars() {
		return vars;
	}
	public void setVars(HashMap<String, String> vars) {
		this.vars = vars;
	}
	public HashMap<String, String> getLists() {
		return lists;
	}
	public void setLists(HashMap<String, String> lists) {
		this.lists = lists;
	}
	public HashMap<String, String> getTemplates() {
		return templates;
	}
	public void setTemplates(HashMap<String, String> templates) {
		this.templates = templates;
	}
}

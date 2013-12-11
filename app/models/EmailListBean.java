package models;

public class EmailListBean {

	private String listName;
	private String email;
	private boolean listFlag = true;

	public EmailListBean() {
		listName = "";
		email = "";
		listFlag = true;
	}

	public EmailListBean(String listName, String email) {
		this.listName = listName;
		this.email = email;
		listFlag = true;
	}
	
	public EmailListBean(String listName, String email,boolean listFlag) {
		this.listName = listName;
		this.email = email;
		this.listFlag = listFlag;
	}

	public String getListName() {
		return listName;
	}

	public void setListName(String listName) {
		this.listName = listName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean isListFlag() {
		return listFlag;
	}

	public void setListFlag(boolean listFlag) {
		this.listFlag = listFlag;
	}
	
}

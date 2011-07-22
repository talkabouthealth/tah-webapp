package models;

public class EmailListBean {

	private String listName;
	private String email;

	public EmailListBean() {
		listName = "";
		email = "";
	}

	public EmailListBean(String listName, String email) {
		this.listName = listName;
		this.email = email;
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
}

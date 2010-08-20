package models;

public class EmailBean {
	
	private String value;
	private String verifyCode;
	
	public EmailBean(String value, String verifyCode) {
		this.value = value;
		this.verifyCode = verifyCode;
	}
	
	@Override
	public String toString() {
		return value;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof EmailBean)) {
			return false;
		}
		
		EmailBean other = (EmailBean)obj;
		return value.equals(other.value);
	}
	
	@Override
	public int hashCode() {
		if (value == null) {
			return 47;
		}
		return value.hashCode();
	}
	
	
	public String getValue() { return value; }
	public void setValue(String value) { this.value = value; }
	
	public String getVerifyCode() { return verifyCode; }
	public void setVerifyCode(String verifyCode) { this.verifyCode = verifyCode; }

}

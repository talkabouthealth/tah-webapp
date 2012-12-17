package models;

public class AdvertisementBean {

	private String timestamp;
	private String recordType;
	private int adCount;

	public AdvertisementBean() { }

	public AdvertisementBean(String timestamp, String recordType, int adCount) {
		super();
		this.timestamp = timestamp;
		this.recordType = recordType;
		this.adCount = adCount;
	}

	public String getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	public String getRecordType() {
		return recordType;
	}
	public void setRecordType(String recordType) {
		this.recordType = recordType;
	}
	public int getAdCount() {
		return adCount;
	}
	public void setAdCount(int adCount) {
		this.adCount = adCount;
	}
}
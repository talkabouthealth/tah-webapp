package models;

public class TalkerImageBean {

	byte[] imageArray;
	String [] coords;
	String imageType;

	public byte[] getImageArray() {
		return imageArray;
	}
	public void setImageArray(byte[] imageArray) {
		this.imageArray = imageArray;
	}
	public String[] getCoords() {
		return coords;
	}
	public void setCoords(String[] coords) {
		this.coords = coords;
	}
	public String getImageType() {
		return imageType;
	}
	public void setImageType(String imageType) {
		this.imageType = imageType;
	}
}
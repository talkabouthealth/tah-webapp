package util;

import java.util.Collections;
import java.util.List;

import models.TalkerBean;
import models.PrivacySetting.PrivacyType;
import models.PrivacySetting.PrivacyValue;

import play.templates.JavaExtensions;

/**
 * Extensions that can be used in Play! templates.
 * Must extend "JavaExtensions" class, so Play! could find them on start-up. 
 *
 */
public class TemplateExtensions extends JavaExtensions {

	/**
	 * Converts list of strings to comma-separated string.
	 * Used for "other" fields in the forms.
	 * 
	 */
	public static String toCommaString(List<String> list, String fieldName) {
		if (list == null || list.size() == 0) {
			return fieldName+" (please separate by commas)";
		}
		else {
			StringBuilder listString = new StringBuilder();
			for (String value : list) {
				listString.append(value).append(", ");
			}
			//remove last comma
			listString.delete(listString.length()-2, listString.length());
			return listString.toString();
		}
	}
	
	public static String toCommaStringView(List<String> list, String fieldName) {
		if (list == null || list.size() == 0) {
			return "";
		}
		else {
			return toCommaString(list, fieldName);
		}
	}
	
	/**
	 * Returns part of given list.
	 * @param list
	 * @param limitValue size of returned part
	 * @return
	 */
	public static List<?> limit (List<?> list, int limitSize) {
		return limit(list, 0, limitSize);
	}
	
	/**
	 * Returns part of given list.
	 * @param list
	 * @param limitValue size of returned part
	 * @return
	 */
	public static List<?> limit (List<?> list, int start, int limitSize) {
		start = (start < 0 ? 0 : start);
		int size = list.size();
		int end = start + limitSize;
		
		if (start >= size) {
			return Collections.emptyList();
		}
		else if (end <= size) {
			return list.subList(start, end);
		}
		else {
			return list.subList(start, size);
		}
	}
	
	public static String limitSize(String source, int limit) {
		return limitSize(source, limit, "...");
	}
	
	/**
	 * Limits number of characters in given string to 'limitSize'
	 * @param source
	 * @param size
	 * @param prefix added to the end if string was truncated
	 * @return
	 */
	public static String limitSize(String source, int limit, String prefix) {
		int size = source.length();
		
		if (size > limit) {
			return source.substring(0, limit)+prefix;
		}
		else {
			return source;
		}
	}
	
	public static boolean isAllowed(TalkerBean talker, PrivacyType privacyType, TalkerBean currentTalker) {
		PrivacyValue privacyValue = talker.getPrivacyValue(privacyType);
		
		if (privacyValue == PrivacyValue.PUBLIC || talker.equals(currentTalker)) {
			return true;
		}
		if (privacyValue == PrivacyValue.COMMUNITY && currentTalker != null) {
			return true;
		}
		return false;
	}

}

package util;

import java.util.Collections;
import java.util.List;

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
			//format [entry1, entry2]
			String listString = list.toString();
			return listString.substring(1, listString.length()-1);
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

}

package util;

import java.util.List;

import play.templates.JavaExtensions;

public class StringExtensions extends JavaExtensions {

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
	
	//TODO: move somewhere?
	public static List<?> limit (List<?> list, int limitValue) {
		int size = list.size();
		if (limitValue <= size) {
			return list.subList(0, limitValue);
		}
		else {
			return list;
		}
	}

}

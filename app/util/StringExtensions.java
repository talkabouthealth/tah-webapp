package util;

import java.util.List;

import play.templates.JavaExtensions;

public class StringExtensions extends JavaExtensions {

	public static String toCommaString(List<String> list) {
		if (list == null || list.size() == 0) {
			return "Other (please separate by commas)";
		}
		else {
			//format [entry1, entry2]
			String listString = list.toString();
			return listString.substring(1, listString.length()-1);
		}
	}

}

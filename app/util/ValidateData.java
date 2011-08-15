package util;

import java.util.regex.Pattern;

public class ValidateData {
	// username can only have alphanumeric, -, and . and cannot be greater than 25 characters
	public static final Pattern unpwPattern = Pattern
			.compile("([\\w-\\.]){1,25}");
	
	public static final String USER_REGEX = "([\\w-_\\.]){1,25}";
	
	public static final Pattern emailPattern = Pattern
			.compile("^[_a-zA-Z0-9-]+(\\.[_a-zA-Z0-9-]+)*@[a-zA-Z0-9-]+" +
					"(\\.[a-zA-Z0-9-]+)*\\.(([0-9]{1,3})|([a-zA-Z]{2,3})|(aero|coop|info|museum|name))$");
	public static final Pattern monthPattern = Pattern
			.compile("^[1-9]{1}$|^[01]{1}[0-9]{1}$");
	public static final Pattern dayPattern = Pattern
			.compile("^[1-9]{1}$|^[1-3]{1}[0-9]{1}$");
	public static final Pattern yearPattern = Pattern
			.compile("^[1-2]{1}[09]{1}[0-9]{1}[0-9]{1}$");
	public static final Pattern genderPattern = Pattern.compile("^[MF]{1}$");
	public static final Pattern topicPattern = Pattern
			.compile("([\\w\\s-_\\.,<>/?':;\"\\[\\]\\{\\}|\\\\!~`@#$%^&*()+=и’”]){1,140}");

	public static boolean validateEmail(String email) {
		if (emailPattern.matcher(email).matches()) {
			return true;
		}
		return false;
	}
        public static String escapeText(String text) {
            text = text.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)");
            text = text.replaceAll("\\{", "\\\\{").replaceAll("\\}", "\\\\}");
            text = text.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]");
            return text;
        }
}

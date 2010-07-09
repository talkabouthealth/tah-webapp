package util;

import java.util.regex.Pattern;

public class ValidateData {
	public static final Pattern unpwPattern = Pattern
			.compile("([\\w-_\\.]){1,25}");
	
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

	//TODO: remove this?
	
	public static boolean validateUserName(String un) {
		// username can only have alphanumeric, _,-, and . and cannot be greater than 25 characters
		if (!unpwPattern.matcher(un).matches()) {
			return false;
		}
		return true;
	}

	public static boolean validatePassword(String pw) {
		// password can only have alphanumeric, _,-, and . and cannot be greater than 25 characters
		if (unpwPattern.matcher(pw).matches()) {
			return true;
		}
		return false;
	}

	public static boolean validateEmail(String email) {
		if (emailPattern.matcher(email).matches()) {
			return true;
		}
		return false;
	}

	public static boolean validateMonth(String month) {
		if (monthPattern.matcher(month).matches()) {
			return true;
		}
		return false;
	}

	public static boolean validateDay(String day) {
		if (dayPattern.matcher(day).matches()) {
			return true;
		}
		return false;
	}

	public static boolean validateYear(String year) {
		if (yearPattern.matcher(year).matches()) {
			return true;
		}
		return false;
	}

	public static boolean validateGender(String gender) {
		if (genderPattern.matcher(gender).matches()) {
			return true;
		}
		return false;
	}

	public static boolean validateTopic(String topic) {
		if (topicPattern.matcher(topic).matches()) {
			return true;
		}
		return false;
	}
}

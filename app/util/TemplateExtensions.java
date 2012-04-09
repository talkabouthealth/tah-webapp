package util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import controllers.AnswerNotification;

import models.CommentBean;
import models.MessageBean;
import models.TalkerBean;
import models.PrivacySetting.PrivacyType;
import models.PrivacySetting.PrivacyValue;

import play.templates.JavaExtensions;

/**
 * Extensions that can be used in Play! templates.
 * Must extend "JavaExtensions" class, so Play! could find it on start-up. 
 *
 */
public class TemplateExtensions extends JavaExtensions {

	/**
	 * Converts list of strings to comma-separated string.
	 * Used for "other" fields in the forms.
	 * 
	 */
	public static String toCommaString(Collection<String> list, String fieldName) {
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
	
	public static String toCommaStringView(Collection<String> list, String fieldName) {
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
	
	/**
	 * Checks if talker allows to show particular info to currentTalker
	 * 
	 * @param talker 
	 * @param privacyType Information belongs to this privacy type
	 * @param currentTalker Logged in talker or 'null'
	 * @return
	 */
	public static boolean isAllowed(TalkerBean talker, PrivacyType privacyType, TalkerBean currentTalker) {
		PrivacyValue privacyValue = talker.getPrivacyValue(privacyType);
		if(currentTalker == null){
			if (privacyValue == PrivacyValue.PUBLIC )
				return true;
			else
				return false;
		}else if (privacyValue == PrivacyValue.PUBLIC || talker.equals(currentTalker)) {
			return true;
		}
		if (privacyValue == PrivacyValue.COMMUNITY && currentTalker != null) {
			return true;
		}
		return false;
	}
	
	public static Object printThoughtOrAnswer(CommentBean thoughtOrAnswer,String userName) {
		/*String htmlText = "";
		if(thoughtOrAnswer.getModerate() != null && thoughtOrAnswer.getModerate().equalsIgnoreCase(AnswerNotification.DELETE_ANSWER)){
		}else if(thoughtOrAnswer.getModerate() != null && thoughtOrAnswer.getModerate().equals("")){
			
		}else if(thoughtOrAnswer.getFromTalker().getUserName().equalsIgnoreCase(userName)){
			htmlText = CommonUtil.commentToHTML(thoughtOrAnswer);
		}else if(thoughtOrAnswer.getModerate() != null){
			htmlText = CommonUtil.commentToHTML(thoughtOrAnswer);
		}else{
			htmlText = CommonUtil.commentToHTML(thoughtOrAnswer);
		}
		return JavaExtensions.raw(htmlText);
		 */
		String htmlText = CommonUtil.commentToHTML(thoughtOrAnswer);
		return JavaExtensions.raw(htmlText);
	}
	
	public static Object printMessage(MessageBean message,String userName) {
		String text = message.getText();
		if (text == null) {
			return "";
		}
		String htmlText = text;
		htmlText = htmlText.replace("\n", "<br/>");
		return JavaExtensions.raw(htmlText);
	}
	
	public static Object convertToURL(String text) {
		if (text == null) {
			return "";
		}
		text = text.replaceAll(" ", "_");
		return text;
	}
}

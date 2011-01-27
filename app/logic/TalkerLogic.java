package logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import play.Logger;
import play.templates.JavaExtensions;
import dao.ActionDAO;
import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.HealthItemDAO;
import dao.TalkerDAO;
import dao.TalkerDiseaseDAO;
import dao.TopicDAO;

import util.CommonUtil;
import util.FacebookUtil;
import util.NotificationUtils;
import util.TwitterUtil;

import models.CommentBean;
import models.ConversationBean;
import models.HealthItemBean;
import models.ServiceAccountBean;
import models.TalkerBean;
import models.TalkerDiseaseBean;
import models.TopicBean;
import models.ConversationBean.ConvoType;
import models.ServiceAccountBean.ServiceType;
import models.TalkerBean.EmailSetting;
import models.actions.Action;
import models.actions.AnswerDisplayAction;
import models.actions.PersonalProfileCommentAction;
import models.actions.Action.ActionType;

public class TalkerLogic {
	
	private static Map<String, List<String>> fieldsDataMap;
	private static Map<String, List<String>> healthItems2TopicsMap;
	
	/**
	 * Map for connecting professional fields' names with fields' text descriptions (used on the Profile page)
	 */
	public static Map<String, String> PROF_FIELDS_MAP = new LinkedHashMap<String, String>();
	static {
		PROF_FIELDS_MAP.put("credentials", "Credential");
		PROF_FIELDS_MAP.put("licenses", "Licenses");
		PROF_FIELDS_MAP.put("prim_specialty", "Primary specialty");
		PROF_FIELDS_MAP.put("sec_specialty", "Secondary specialty");
		PROF_FIELDS_MAP.put("states_lic", "State Licenses");
		PROF_FIELDS_MAP.put("specialty", "Specialty");
		PROF_FIELDS_MAP.put("languages", "Languages");
		PROF_FIELDS_MAP.put("gender", "Gender");
		PROF_FIELDS_MAP.put("age", "Age");
		PROF_FIELDS_MAP.put("nurse_school", "Nursing school");
		PROF_FIELDS_MAP.put("pharm_school", "Pharmacy school");
		PROF_FIELDS_MAP.put("school_uni", "School / University");
		PROF_FIELDS_MAP.put("educ", "Education");
		PROF_FIELDS_MAP.put("med_school", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Medical school");
		PROF_FIELDS_MAP.put("residency", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Residency");
		PROF_FIELDS_MAP.put("internship", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Internship");
		PROF_FIELDS_MAP.put("board_certs", "Board certifications");
		PROF_FIELDS_MAP.put("memberships", "Professional memberships");
		PROF_FIELDS_MAP.put("expertise", "Areas of expertise");
		PROF_FIELDS_MAP.put("research_interests", "Research interests");
		PROF_FIELDS_MAP.put("awards", "Awards and publications");
		PROF_FIELDS_MAP.put("affiliation", "Hospital affiliation");
		PROF_FIELDS_MAP.put("other_affiliation", "Hospital or other affiliation");
		PROF_FIELDS_MAP.put("pract_name", "Practice name");
		PROF_FIELDS_MAP.put("pract_adr", "Practice address");
		PROF_FIELDS_MAP.put("pract_phone", "Practice phone number");
		PROF_FIELDS_MAP.put("web", "Web page");
		PROF_FIELDS_MAP.put("vitals", "Vitals.com page");
		PROF_FIELDS_MAP.put("zocdoc", "ZocDoc.com page");
	}
	
	public static void setFieldsDataMap(Map<String, List<String>> fieldsDataMap) {
		TalkerLogic.fieldsDataMap = fieldsDataMap;
	}
	public static void setHealthItems2TopicsMap(
			Map<String, List<String>> healthItems2TopicsMap) {
		TalkerLogic.healthItems2TopicsMap = healthItems2TopicsMap;
	}

	//load data for combo boxes on EditProfile page
	public static List<String> getFieldsData(String fieldName, String talkerType) {
		String key = fieldName+"|"+talkerType;
		if (fieldsDataMap.containsKey(key)) {
			return fieldsDataMap.get(key);
		}
		else {
			key = fieldName+"|all";
			return fieldsDataMap.get(key);
		}
	}

	enum ProfileCompletion {
		BASIC(25, "Sign Up"),
		UPDATE_HEALTH(5, 
			"Share your <a href='"+CommonUtil.generateAbsoluteURL("Profile.healthDetails")+"'>Health Info</a>",
			"So we can match you with Members, Topics, and Conversations most relevant to you."),
		UPDATE_PERSONAL(10, 
			"Update your <a href='"+CommonUtil.generateAbsoluteURL("Profile.edit")+"'>Profile Info</a>",
			"So other member similar to you can find you if you choose."
			),
		VIEW_PRIVACY(10,
			"Update your <a href='"+CommonUtil.generateAbsoluteURL("Profile.preferences")+"'>Privacy Settings</a>",
			"So other member similar to you can find and reach out to you if you choose."),
		ASK_QUESTION(10, "Ask a <a href='#' onclick='return showStartConvoDialog(\"question\");'>Question</a>"),
		COMMENT_CONVO(10, "Answer a <a href='"+CommonUtil.generateAbsoluteURL("Explore.openQuestions")+"'>Question</a>"),
		GIVE_THANKYOU(10, "Give a Thank you"),
		COMMENT_THOUGHTS(5, "Comment in your <a href='"+
				CommonUtil.generateAbsoluteURL("PublicProfile.thoughts", "userName", "<username>")+"'>Thoughts Feed</a>"),
		FOLLOW(5, "Follow <a href='"+
				CommonUtil.generateAbsoluteURL("Community.browseMembers", "action", "active")+"'>another member</a>"),
		FOLLOW_TOPIC(5, "Follow a <a href='"+CommonUtil.generateAbsoluteURL("Explore.browseTopics")+"'>Topic</a>"),
		START_OR_JOIN_TALK(5, "Start or join a <a href='"+CommonUtil.generateAbsoluteURL("Explore.liveTalks")+"'>Live Chat</a>");
		
		
		private final int value;
		private final String description;
		private final String stepMessage;
		private final String stepNote;
		
		private ProfileCompletion(int value, String stepMessage) {
			this(value, null, stepMessage, null);
		}
		
		private ProfileCompletion(int value, String stepMessage, String stepNote) {
			this(value, null, stepMessage, stepNote);
		}
		
		private ProfileCompletion(int value, String description, String stepMessage, String stepNote) {
			this.value = value;
			this.description = description;
			this.stepMessage = stepMessage;
			this.stepNote = stepNote;
		}

		public int getValue() {
			return value;
		}
		public String getDescription() {
			if (description == null) {
				return stepMessage+" to get to ";
			}
			return description;
		}
		public String getStepMessage() {
			return stepMessage;
		}
		public String getStepNote() {
			return stepNote;
		}
	}
	
	public static void preloadTalkerInfo(TalkerBean talker) {
		preloadTalkerInfo(talker, null);
	}
	public static void preloadTalkerInfo(TalkerBean talker, String page) {
		talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
		talker.setActivityList(ActionDAO.loadTalkerActions(talker.getId()));
		
		calculateProfileCompletion(talker, page);
	}
	
	private static void calculateProfileCompletion(TalkerBean talker, String page) {
		//check what items are completed
		EnumSet<ProfileCompletion> profileActions = EnumSet.of(ProfileCompletion.BASIC);
		
		for (Action action : talker.getActivityList()) {
			ActionType type = action.getType();
			switch (type) {
			case START_CONVO:
				profileActions.add(ProfileCompletion.START_OR_JOIN_TALK);
				profileActions.add(ProfileCompletion.ASK_QUESTION);
				break;
			case JOIN_CONVO:
			case RESTART_CONVO:
				profileActions.add(ProfileCompletion.START_OR_JOIN_TALK);
				break;
			case ANSWER_CONVO:
				profileActions.add(ProfileCompletion.COMMENT_CONVO);
				break;
			case GIVE_THANKS:
				profileActions.add(ProfileCompletion.GIVE_THANKYOU);
				break;
			case UPDATE_PERSONAL:
				profileActions.add(ProfileCompletion.UPDATE_PERSONAL);
				break;
			case UPDATE_HEALTH:
				profileActions.add(ProfileCompletion.UPDATE_HEALTH);
				break;
			case PERSONAL_PROFILE_COMMENT:
				profileActions.add(ProfileCompletion.COMMENT_THOUGHTS);
				break;
			}
		}
		if (!talker.getFollowingList().isEmpty()) {
			profileActions.add(ProfileCompletion.FOLLOW);
		}
		if (!talker.getFollowingTopicsList().isEmpty()) {
			profileActions.add(ProfileCompletion.FOLLOW_TOPIC);
		}
		if (talker.getHiddenHelps().contains("privacyViewed")) {
			profileActions.add(ProfileCompletion.VIEW_PRIVACY);
		}
		if (talker.isProf()) {
			//Profs can skip these steps and automatically receive the percentage.
			profileActions.add(ProfileCompletion.UPDATE_HEALTH);
			profileActions.add(ProfileCompletion.START_OR_JOIN_TALK);
		}
		
		if (page != null) {
			//depending on page we complete some items
			if (page.equals("profile")) {
				profileActions.add(ProfileCompletion.UPDATE_PERSONAL);
			}
			else if (page.equals("health")) {
				profileActions.add(ProfileCompletion.UPDATE_HEALTH);
			}
			else if (page.equals("privacy")) {
				profileActions.add(ProfileCompletion.VIEW_PRIVACY);
			}
		}
		
		//calculate current sum and next item to complete
		ProfileCompletion nextItem = null;
		int sum = 0;
		for (ProfileCompletion profileCompletion : ProfileCompletion.values()) {
			if (profileActions.contains(profileCompletion)) {
				sum += profileCompletion.getValue();
			}
			else {
				if (nextItem == null) {
					nextItem = profileCompletion;
				}
			}
		}
		talker.setProfileCompletionValue(sum);
		
		if (sum != 100) {
			int nextSum = sum + nextItem.getValue();
			String nextMessage = nextItem.getStepMessage();
			nextMessage = nextMessage.replaceAll("<username>", talker.getUserName());
			String message = nextMessage+" to go to "+nextSum+"%.";
			
			talker.setProfileCompletionMessage(message);
			talker.setNextStepMessage(nextMessage);
			talker.setNextStepNote(nextItem.getStepNote());
		}
	}
	
	public static List<TopicBean> getRecommendedTopics(TalkerDiseaseBean talkerDisease) {
		List<TopicBean> recommendedTopics = new ArrayList<TopicBean>();
		
		//No topics should be followed for "What part of the breast did the cancer begin?"
		//Tumor grade, Level of lymph node involvement,
		List<String> skipHealthItems = Arrays.asList("beginpart", "tumorgrade", "lymphnodes");
		
		List<String> topicNames = new ArrayList<String>();
		Map<String, List<String>> healthInfo = talkerDisease.getHealthInfo();
		for (String key : healthInfo.keySet()) {
			if (skipHealthItems.contains(key)) {
				continue;
			}
			
			for (String healthItemValue : healthInfo.get(key)) {
				//If the user selects 'yes' for HER2+, automatically have user follow "Her2-Positive Breast Cancer (Her2+)"
				if (key.equals("her2")) {
					if (healthItemValue.equals("yes")) {
						topicNames.add("Her2-Positive Breast Cancer (Her2+)");
					}
					continue;
				}
				
				List<String> healthItemsTopics = healthItems2TopicsMap.get(healthItemValue);
				if (healthItemsTopics != null) {
					topicNames.addAll(healthItemsTopics);
				}
				else {
					String possibleTopic = JavaExtensions.capitalizeWords(healthItemValue);
					topicNames.add(possibleTopic);
				}
			}
		}
		
		//If user selects 'yes' Recurrent, make sure they follow the 'Recurrent (Recurring) topic.
		if ("yes".equals(talkerDisease.getRecurrent())) {
			topicNames.add("Recurrent (Recurring)");
		}
		
		Set<String> healthItems = talkerDisease.getHealthItems();
		List<HealthItemBean> allHealthItems = HealthItemDAO.getAllHealthItems(null);
		for (HealthItemBean healthItem : allHealthItems) {
			if (healthItems.contains(healthItem.getId())) {
				List<String> healthItemsTopics = healthItems2TopicsMap.get(healthItem.getName());
				if (healthItemsTopics != null) {
					topicNames.addAll(healthItemsTopics);
				}
				else {
					String possibleTopic = JavaExtensions.capitalizeWords(healthItem.getName());
					topicNames.add(possibleTopic);
				}
			}
		}
		
		for (String possibleTopic : topicNames) {
			TopicBean topic = TopicDAO.getOrRestoreByTitle(possibleTopic);
			if (topic != null) {
				//Do not ever recommend "Unorganized" or "Chats" as topics to follow
				if (! (topic.getTitle().equals(ConversationLogic.DEFAULT_QUESTION_TOPIC) 
						|| topic.getTitle().equals(ConversationLogic.DEFAULT_TALK_TOPIC)) ) {
					recommendedTopics.add(topic);
				}
			}
		}
		
		return recommendedTopics;
	}
	
	public static int prepareTalkerAnswers(String talkerId, List<Action> answersFeed) {
		int numOfTopAnswers = 0;
		List<CommentBean> allAnswers = CommentsDAO.getTalkerAnswers(talkerId, null);
		for (CommentBean answer : allAnswers) {
			ConversationBean convo = ConversationDAO.getById(answer.getConvoId());
			convo.setComments(CommentsDAO.loadConvoAnswers(convo.getId()));
			
			if (!convo.getComments().isEmpty() && convo.getComments().get(0).equals(answer)) {
				numOfTopAnswers++;
			}
			
			AnswerDisplayAction answerAction = new AnswerDisplayAction(convo.getTalker(), convo, answer, ActionType.ANSWER_CONVO, false);
			answerAction.setTime(answer.getTime());
			
			answersFeed.add(answerAction);
		}
		return numOfTopAnswers;
	}
	
	public static List<Action> prepareTalkerConvos(List<ConversationBean> loadFollowingConversations) {
		List<Action> convosFeed = new ArrayList<Action>();
		for (ConversationBean convo : loadFollowingConversations) {
			convo.setComments(CommentsDAO.loadConvoAnswers(convo.getId()));
			
			TalkerBean activityTalker = convo.getTalker();
			//show top answer or simple convo
			CommentBean topAnswer = null;
			if (!convo.getComments().isEmpty()) {
				topAnswer = convo.getComments().get(0);
				topAnswer = CommentsDAO.getConvoAnswerById(topAnswer.getId());
				activityTalker = topAnswer.getFromTalker();
			}
			
			AnswerDisplayAction convoAction =
				new AnswerDisplayAction(activityTalker, convo, topAnswer, ActionType.ANSWER_CONVO, topAnswer != null);
			convoAction.setTime(convo.getCreationDate());
			
			convosFeed.add(convoAction);
		}
		return convosFeed;
	}
	
	public static List<ConversationBean> loadFollowingConversations(TalkerBean talker) {
		if (talker == null) {
			return new ArrayList<ConversationBean>();
		}
		
		List<ConversationBean> followingConvoList = new ArrayList<ConversationBean>();
		for (String convoId : talker.getFollowingConvosList()) {
			ConversationBean convo = ConversationDAO.getById(convoId);
			followingConvoList.add(convo);
		}
		
		return followingConvoList;
	}

	public static boolean talkerHasNoHealthInfo(TalkerBean talker) {
		TalkerDiseaseBean talkerDisease = TalkerDiseaseDAO.getByTalkerId(talker.getId());
		return (talkerDisease == null && !talker.isProf());
	}
	
	/**
	 * Save thought or reply
	 * @param talker
	 * @param profileTalkerId
	 * @param parentId
	 * @param text
	 * @return
	 */
	public static CommentBean saveProfileComment(TalkerBean talker, String profileTalkerId, String parentId, String text) {
		//find profile talker by parent thought or given talker id
		if (parentId != null && parentId.length() != 0) {
			CommentBean parentAnswer = CommentsDAO.getProfileCommentById(parentId);
			if (parentAnswer != null) {
				profileTalkerId = parentAnswer.getProfileTalkerId();
			}
		}
		TalkerBean profileTalker = null;
		if (profileTalkerId == null) {
			profileTalker = talker;
			profileTalkerId = profileTalker.getId();
		}
		else {
			profileTalker = TalkerDAO.getById(profileTalkerId);
		}
		if (profileTalker == null) {
			return null;
		}
		
		CommentBean comment = new CommentBean();
		if (parentId == null || parentId.trim().length() == 0) {
			comment.setParentId(null);
		}
		else {
			comment.setParentId(parentId);
		}
		comment.setProfileTalkerId(profileTalkerId);
		comment.setFromTalker(talker);
		comment.setText(text);
		comment.setTime(new Date());
		CommentsDAO.saveProfileComment(comment);
		
		if (comment.getParentId() == null) {
			//post to personal Thoughts?
			if (talker.equals(profileTalker)) {
				ActionDAO.saveAction(new PersonalProfileCommentAction(
						talker, profileTalker, comment, null, ActionType.PERSONAL_PROFILE_COMMENT));
				
				for (ServiceAccountBean serviceAccount : talker.getServiceAccounts()) {
					if (!serviceAccount.isTrue("SHARE_FROM_THOUGHTS")) {
						continue;
					}
					
					Logger.debug(serviceAccount.getType().toString()+", Share from Thoughts, Info: "+
							serviceAccount.getToken()+" : "+serviceAccount.getTokenSecret());
					
					if (serviceAccount.getType() == ServiceType.TWITTER) {
						TwitterUtil.makeUserTwit(comment.getText(), 
								serviceAccount.getToken(), serviceAccount.getTokenSecret());
					}
					else if (serviceAccount.getType() == ServiceType.FACEBOOK) {
						FacebookUtil.post(comment.getText(), serviceAccount.getToken());
					}
				}
			}
			
			if (!talker.equals(profileTalker)) {
				Map<String, String> vars = new HashMap<String, String>();
				vars.put("other_talker", talker.getUserName());
				vars.put("comment_text", comment.getText());
				NotificationUtils.sendEmailNotification(EmailSetting.RECEIVE_COMMENT, 
						profileTalker, vars);
			}
		}
		else {
			//for replies we update action for parent comment
			ActionDAO.updateProfileCommentActionTime(comment.getParentId());
			
			CommentBean thought = CommentsDAO.getProfileCommentById(comment.getParentId());
			if (!talker.equals(thought.getFromTalker())) {
				//when user leaves post in someone else's Thoughts Feed, if there are replies, 
				//send email to the owner of the Thoughts Feed as well as the user who started the thread.
				Map<String, String> vars = new HashMap<String, String>();
				vars.put("other_talker", talker.getUserName());
				vars.put("comment_text", thought.getText());
				vars.put("reply_text", comment.getText());
				vars.put("profile_talker", profileTalker.getUserName());
				NotificationUtils.sendEmailNotification(EmailSetting.RECEIVE_COMMENT, 
						thought.getFromTalker(), vars);
			}
		}
		
		return comment;
	}
}

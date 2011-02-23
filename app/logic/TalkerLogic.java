package logic;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import play.Logger;
import play.mvc.Scope.Session;
import play.templates.JavaExtensions;
import dao.ActionDAO;
import dao.ApplicationDAO;
import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.HealthItemDAO;
import dao.TalkerDAO;
import dao.TalkerDiseaseDAO;
import dao.TopicDAO;

import util.CommonUtil;
import util.EmailUtil;
import util.FacebookUtil;
import util.ImageUtil;
import util.NotificationUtils;
import util.TwitterUtil;
import util.EmailUtil.EmailTemplate;

import models.CommentBean;
import models.ConversationBean;
import models.HealthItemBean;
import models.PrivacySetting;
import models.ServiceAccountBean;
import models.TalkerBean;
import models.TalkerDiseaseBean;
import models.TopicBean;
import models.ConversationBean.ConvoType;
import models.PrivacySetting.PrivacyType;
import models.PrivacySetting.PrivacyValue;
import models.ServiceAccountBean.ServiceType;
import models.TalkerBean.EmailSetting;
import models.actions.Action;
import models.actions.AnswerDisplayAction;
import models.actions.PersonalProfileCommentAction;
import models.actions.Action.ActionType;

public class TalkerLogic {
	
	/**
	 * Describes steps required for profile completion.
	 *
	 */
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
				CommonUtil.generateAbsoluteURL("Explore.browseMembers", "action", "active")+"'>another member</a>"),
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
		
		/**
		 * 
		 * @param value Value of this step in percents.
		 * @param description Message used for ProfileCompletion panel.
		 * @param stepMessage Message used for NextStep feature.
		 * @param stepNote Additional note for NextStep feature.
		 */
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
	
	//Field choices (options in <select>) for different fields/profiles(i.e. Nurse, etc)
	private static Map<String, List<String>> fieldsDataMap;
	
	//Matches HealthItem with list of topics
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

	/**
	 * Get data for combo boxes on EditProfile page
	 * @param fieldName
	 * @param talkerType Talker's connection (e.g. Nurse, Caregiver, etc.)
	 * @return
	 */
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

	/**
	 * Prepares talker information and calculates profile completion for displaying.
	 * 
	 */
	public static void preloadTalkerInfo(TalkerBean talker) {
		preloadTalkerInfo(talker, null);
	}
	public static void preloadTalkerInfo(TalkerBean talker, String page) {
		talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
		talker.setActivityList(ActionDAO.loadTalkerActions(talker.getId()));
		
		calculateProfileCompletion(talker, page);
	}
	
	/**
	 * Calculates and prepares ProfileCompletion and NextStep features.
	 * @param talker
	 * @param page Page where information will be displayed
	 */
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
		//for some steps we check lists, not actions
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
			//prepare messages for dispaying
			int nextSum = sum + nextItem.getValue();
			String nextMessage = nextItem.getStepMessage();
			nextMessage = nextMessage.replaceAll("<username>", talker.getUserName());
			String message = nextMessage+" to go to "+nextSum+"%.";
			
			talker.setProfileCompletionMessage(message);
			talker.setNextStepMessage(nextMessage);
			talker.setNextStepNote(nextItem.getStepNote());
		}
	}
	
	/**
	 * Loads all answers of this talker in Feed format
	 * 
	 * @param talkerId
	 * @param answersFeed Feed where answer actions are stored
	 * @return Number of top answers of this talker
	 */
	public static int prepareTalkerAnswers(String talkerId, List<Action> answersFeed) {
		int numOfTopAnswers = 0;
		List<CommentBean> allAnswers = CommentsDAO.getTalkerAnswers(talkerId, null);
		for (CommentBean answer : allAnswers) {
			ConversationBean convo = ConversationDAO.getById(answer.getConvoId());
			convo.setComments(CommentsDAO.loadConvoAnswers(convo.getId()));
			
			//if first in the list - top answer
			if (!convo.getComments().isEmpty() && convo.getComments().get(0).equals(answer)) {
				numOfTopAnswers++;
			}

			//Use special action for answer displaying.
			AnswerDisplayAction answerAction = new AnswerDisplayAction(convo.getTalker(), convo, answer, ActionType.ANSWER_CONVO, false);
			answerAction.setTime(answer.getTime());
			answersFeed.add(answerAction);
		}
		return numOfTopAnswers;
	}
	
	public static Set<ConversationBean> loadFollowingConversations(TalkerBean talker) {
		if (talker == null) {
			return new HashSet<ConversationBean>();
		}
		
		Set<ConversationBean> followingConvoSet = new LinkedHashSet<ConversationBean>();
		for (String convoId : talker.getFollowingConvosList()) {
			ConversationBean convo = ConversationDAO.getById(convoId);
			followingConvoSet.add(convo);
		}
		
		return followingConvoSet;
	}

	public static boolean talkerHasNoHealthInfo(TalkerBean talker) {
		TalkerDiseaseBean talkerDisease = TalkerDiseaseDAO.getByTalkerId(talker.getId());
		return (talkerDisease == null && !talker.isProf());
	}
	
	/**
	 * Save thought or reply
	 * TODO: update doc
	 * 
	 * @param talker
	 * @param profileTalkerId
	 * @param parentId
	 * @param text
	 * @param cleanText 
	 * @param ccTwitter If null - nothing happens, if true/false - determines if thought should be posted on Twitter
	 * @param ccFacebook Same as ccTwitter but for Facebook
	 * 
	 * @return
	 */
	public static CommentBean saveProfileComment(TalkerBean talker, String profileTalkerId, 
			String parentId, String text, String cleanText, String from, String fromId, Boolean ccTwitter, Boolean ccFacebook) {
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
		comment.setFrom(from);
		comment.setFromId(fromId);
		CommentsDAO.saveProfileComment(comment);
		
		if (comment.getParentId() == null) {
			//If post to personal thoughts and this thought isn't imported from Tw/Fb
			if (talker.equals(profileTalker) && comment.getFrom() == null) {
				ActionDAO.saveAction(new PersonalProfileCommentAction(
						talker, profileTalker, comment, null, ActionType.PERSONAL_PROFILE_COMMENT));
				
				//TODO: better implement?
				for (ServiceAccountBean serviceAccount : talker.getServiceAccounts()) {
					if (serviceAccount.getType() == ServiceType.TWITTER) {
						if (ccTwitter != null && !ccTwitter) {
							continue;
						}
						if (ccTwitter == null && !serviceAccount.isTrue("SHARE_FROM_THOUGHTS")) {
							continue;
						}
						TwitterUtil.tweet(cleanText, serviceAccount);
					}
					else if (serviceAccount.getType() == ServiceType.FACEBOOK) {
						if (ccFacebook != null && !ccFacebook) {
							continue;
						}
						if (ccFacebook == null && !serviceAccount.isTrue("SHARE_FROM_THOUGHTS")) {
							continue;
						}
						FacebookUtil.post(cleanText, serviceAccount);
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
			
			//when user leaves post in someone else's Thoughts Feed, if there are replies, 
			//send email to the owner of the Thoughts Feed as well as the user who started the thread.
			Map<String, String> vars = new HashMap<String, String>();
			vars.put("other_talker", talker.getUserName());
			vars.put("comment_text", thought.getText());
			vars.put("reply_text", comment.getText());
			vars.put("profile_talker", profileTalker.getUserName());
			
			if (!talker.equals(thought.getFromTalker())) {
				NotificationUtils.sendEmailNotification(EmailSetting.RECEIVE_COMMENT, 
						thought.getFromTalker(), vars);
			}
			if (!talker.equals(profileTalker)) {
				NotificationUtils.sendEmailNotification(EmailSetting.RECEIVE_COMMENT, 
						profileTalker, vars);
			}
		}
		
		return comment;
	}
	
	/**
	 * The Personal Info and Health Info should be private by default. 
	 * All other items should be set to viewable by the community by default.
	 */
	public static Set<PrivacySetting> getDefaultPrivacySettings() {
		Set<PrivacySetting> privacySettings = new HashSet<PrivacySetting>();
		for (PrivacyType type : PrivacyType.values()) {
			PrivacyValue value = PrivacyValue.COMMUNITY;
			if (type == PrivacyType.PROFILE_INFO || type == PrivacyType.HEALTH_INFO) {
				value = PrivacyValue.PRIVATE;
			}
			
			PrivacySetting privacySetting = new PrivacySetting(type, value);
			privacySettings.add(privacySetting);
		}
		return privacySettings;
	}
	
	
	/* -------------------------- Recommendations ---------------------- */
	
	public static List<TopicBean> getRecommendedTopics(TalkerBean talker) {
		List<TopicBean> loadedTopics = new ArrayList<TopicBean>();
		List<TopicBean> recommendedTopics = new ArrayList<TopicBean>();
		
		//try topics based on HealthInfo
		TalkerDiseaseBean talkerDisease = TalkerDiseaseDAO.getByTalkerId(talker.getId());
		if (!talker.isProf() && talkerDisease != null) {
			loadedTopics = TalkerLogic.getTopicsByHealthInfo(talkerDisease);
		}
		
		if (loadedTopics.isEmpty()) {
			//display most popular Topics based on views
			loadedTopics = new ArrayList<TopicBean>(TopicDAO.loadAllTopics(true));
		}
		
		for (TopicBean topic : loadedTopics) {
			//not following and not default
			if (!talker.getFollowingTopicsList().contains(topic)) {
				if (! (topic.getTitle().equals(ConversationLogic.DEFAULT_QUESTION_TOPIC) 
						|| topic.getTitle().equals(ConversationLogic.DEFAULT_TALK_TOPIC)) ) {
					recommendedTopics.add(topic);
				}
			}
			if (recommendedTopics.size() == 3) {
				break;
			}
		}
		return recommendedTopics;
	}
	
	public static void getRecommendedTalkers(TalkerBean talker, List<TalkerBean> similarMembers,
			List<TalkerBean> experts) {
		List<TalkerBean> allMembers = TalkerDAO.loadAllTalkers(true);
		for (TalkerBean member : allMembers) {
			  if (member.isSuspended() || member.isDeactivated() || member.isAdmin()) {
				  continue;
			  }
			  if (talker.equals(member) || talker.getFollowingList().contains(member)) {
				  continue;
			  }
			  
			  if (member.isProf()) {
				  if (experts.size() < 3) {
					  experts.add(member);
				  }
			  }
			  else {
				  if (similarMembers.size() < 3) {
					  similarMembers.add(member);
				  }
			  }
		}
	}
	
	public static List<ConversationBean> getRecommendedConvos(TalkerBean talker) {
		/* 
		 	Ideas for recommended conversations :
				1) match member following topics with convo topics
				2) match member info with full convo info
				3) what conversations are being followed by other members the user follows. 
		*/
		
		List<ConversationBean> recommendedConvos = new ArrayList<ConversationBean>();
		Set<ConversationBean> allConvos = new LinkedHashSet<ConversationBean>();
		for (TopicBean topic : talker.getFollowingTopicsList()) {
			allConvos.addAll(ConversationDAO.loadConversationsByTopic(topic.getId()));
		}
		allConvos.addAll(ConversationDAO.loadPopularConversations());
		
		for (ConversationBean convo : allConvos) {
			if (talker.getFollowingConvosList().contains(convo.getId())) {
				continue;
			}
			recommendedConvos.add(convo);
			if (recommendedConvos.size() == 3) {
				break;
			}
		}
		
		return recommendedConvos;
	}
	
	/**
	 * Gets list of recommended topics based on talker's HealthInfo
	 * @param talkerDisease
	 * @return
	 */
	public static List<TopicBean> getTopicsByHealthInfo(TalkerDiseaseBean talkerDisease) {
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
		
		//convert health items to topic names
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
	
	
	
	/*----------------------- SignUp methods ---------------------------- */
	/**
     * Initializes talker with different default values or parsed info
     */
	public static void prepareTalkerForSignup(TalkerBean talker) {
		talker.setInvitations(100);
		//by default we notify user through IM
		talker.setImNotify(true);
		
		talker.setAnonymousName(CommonUtil.generateRandomUserName(true));
		
//		String imService = talker.getIm();
//        String imUsername = talker.getImUsername();
//        if (!imService.isEmpty()) {
//        	//if userName empty - parse from email
//        	if (imUsername.trim().isEmpty()) {
//    			int atIndex = talker.getEmail().indexOf('@');
//    			imUsername = talker.getEmail().substring(0, atIndex);
//    		}
//        	
//        	IMAccountBean imAccount = new IMAccountBean(imUsername, imService);
//        	talker.setImAccounts(new HashSet<IMAccountBean>(Arrays.asList(imAccount)));
//        }
        
        /*
         	TODO: later - better to use Enum (these settings are from the first prototype version)
         	Default notification settings:
			- 2 to 5 times per day
			- whenever I am online
			- types of conversations: check all
        */
        talker.setNfreq(3);
        talker.setNtime(1);
        talker.setCtype(TalkerBean.CONVERSATIONS_TYPES_ARRAY);
        
		talker.setPrivacySettings(TalkerLogic.getDefaultPrivacySettings());
		
        //By default all email notifications are checked
        EnumSet<EmailSetting> emailSettings = EnumSet.allOf(EmailSetting.class);
        talker.setEmailSettings(emailSettings);
        
        String hashedPassword = CommonUtil.hashPassword(talker.getPassword());
        talker.setPassword(hashedPassword);
        
        //code for email validation
        talker.setVerifyCode(CommonUtil.generateVerifyCode());
        
        talker.setConnectionVerified(false);
	}
	
	/**
	 * Handler method after successful signup.
	 * 
	 * @param talker Just registered talker
	 * @param session
	 */
	public static void onSignup(TalkerBean talker, Session session) {
		//Reserve this name as URL
        ApplicationDAO.createURLName(talker.getUserName(), true);
		
        //Send 'email verification' and 'welcome' emails
		Map<String, String> vars = new HashMap<String, String>();
		
		if (talker.getVerifyCode() != null) {
			vars.put("username", talker.getUserName());
			vars.put("verify_code", talker.getVerifyCode());
			EmailUtil.sendEmail(EmailTemplate.VERIFICATION, talker.getEmail(), vars, null, false);
		}
		
		vars = new HashMap<String, String>();
		vars.put("username", talker.getUserName());
		EmailUtil.sendEmail(EmailTemplate.WELCOME, talker.getEmail(), vars, null, false);
		
		ServiceAccountBean twitterAccount = talker.serviceAccountByType(ServiceType.TWITTER);
		if (twitterAccount != null && twitterAccount.isTrue("FOLLOW")) {
			//follow TAH by this user
			TwitterUtil.followTAH(twitterAccount);
		}

		//manually login this talker
		ApplicationDAO.saveLogin(talker.getId(), "signup");
		session.put("username", talker.getUserName());
		if (talker.isProf()) {
    		session.put("prof", "true");
    	}
	}
	
	/**
	 * Signup with Twitter or Facebook account
	 * 
	 * @param session
	 * @param screenName
	 * @param accountId
	 */
	public static void signupFromService(ServiceType serviceType, Session session, 
			String screenName, String email, String verifyCode, String accountId) {
		TalkerBean talker = new TalkerBean();
		
		//initial username will be their username on Facebook or Twitter, 
		//or if it is taken already, add a number to the username - i.e. murray1
		String newUsername = screenName;
		int i=1; 
		while (ApplicationDAO.isURLNameExists(newUsername)) {
			newUsername = screenName+i;
			i++;
		}
		talker.setUserName(newUsername);
		
		//for Tw/Fb users password is random
		talker.setPassword(CommonUtil.generateRandomPassword());
		
		prepareTalkerForSignup(talker);
		
		talker.setEmail(email);
		talker.setVerifyCode(verifyCode);
		
		//default connection
		talker.setConnection("Patient");
		
		//add Tw/Fb as notification account
		ServiceAccountBean account = new ServiceAccountBean(accountId, screenName, serviceType);
		account.setToken(session.get("token"));
		account.setTokenSecret(session.get("token_secret"));
		
		Set<ServiceAccountBean> serviceAccounts = new HashSet<ServiceAccountBean>();
		serviceAccounts.add(account);
		talker.setServiceAccounts(serviceAccounts);
		
		Set<String> hiddenHelps = talker.getHiddenHelps();
		if (serviceType == ServiceType.TWITTER) {
			hiddenHelps.add("updateFacebookSettings");
		}
		else {
			hiddenHelps.add("updateTwitterSettings");
		}
		
		TalkerDAO.save(talker);
		
		//if user signs up with Facebook, let's use the user's FB image on TAH
		if (serviceType == ServiceType.FACEBOOK) {
//			URL pictureURL = new URL("http://graph.facebook.com/"+accountId+"/picture");
//			BufferedImage bsrc = ImageIO.read(pictureURL);
//			ByteArrayOutputStream baos = ImageUtil.createThumbnail(bsrc);
//        	TalkerDAO.updateTalkerImage(talker, baos.toByteArray());
		}
		
		onSignup(talker, session);
	}
}

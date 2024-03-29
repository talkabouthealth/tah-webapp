package controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mongodb.DBRef;

import logic.ConversationLogic;
import logic.FeedsLogic;
import logic.TalkerLogic;
import logic.TopicLogic;
import logic.FeedsLogic.FeedType;
import models.CommentBean;
import models.ConversationBean;
import models.DiseaseBean;
import models.IMAccountBean;
import models.ServiceAccountBean;
import models.TalkerBean;
import models.ConversationBean;
import models.TalkerDiseaseBean;
import models.ThankYouBean;
import models.TopicBean;
import models.ServiceAccountBean.ServiceType;
import models.actions.Action;
import models.actions.PersonalProfileCommentAction;
import models.actions.PreloadAction;
import models.actions.Action.ActionType;
import play.Logger;
import play.cache.Cache;
import play.data.validation.Validation.ValidationResult;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Scope.Session;
import play.mvc.With;
import util.CommonUtil;
import util.EmailUtil;
import util.FacebookUtil;
import util.NotificationUtils;
import util.TwitterUtil;
import util.EmailUtil.EmailTemplate;
import util.ValidateData;
import dao.ActionDAO;
import dao.ApplicationDAO;
import dao.CommentsDAO;
import dao.DiseaseDAO;
import dao.MessagingDAO;
import dao.TalkerDAO;
import dao.ConversationDAO;
import dao.TalkerDiseaseDAO;
import dao.TopicDAO;

@With( { Secure.class, LoggerController.class } )
public class Home extends Controller {

	/**
	 * Home page
	 */
    public static void index() {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);

    	Logger.info("Home: ---"+talker.getUserName()+"---");
    	List<ConversationBean> liveConversations = ConversationDAO.getLiveConversations();
    	
    	String talkerCat = talker.getCategory();
    	Map<String, Set<Action>> allDiseaseList = new LinkedHashMap<String, Set<Action>>();
    	Set<Action> allFeed = null;
    	
    	if(talker.getCategory() != null && !talker.getCategory().equals("")){
			//Code added for display talker's cancer fees
			
	    	if(talkerCat != null)
	    		allDiseaseList.put(talker.getCategory().replaceAll(" ", "_"),null);
			
	    	if(talker.getOtherCategories() != null) {
	    		for (int i = 0; i < talker.getOtherCategories().length; i++) {
	    			talker.setCategory(talker.getOtherCategories()[i]);
	    			allDiseaseList.put(talker.getOtherCategories()[i].replaceAll(" ", "_"),null);
	    		}
	    	}
	    	talker.setCategory(talkerCat);
	    	talkerCat = talkerCat.replaceAll(" ", "_");
    	} else {
    		allFeed = FeedsLogic.getAllCancerFeed(null, true,talker);
    	}
		
    	boolean showNotificationAccounts = prepareNotificationPanel(session, talker);
		TalkerLogic.preloadTalkerInfo(talker);
		
    	//List<TopicBean> popularTopics = TopicLogic.loadPopularTopics(TopicLogic.TOPICS_PER_PAGE);

        session.put("topicCount", TopicLogic.TOPICS_PER_PAGE);

        session.put("inboxUnreadCount", MessagingDAO.getUnreadMessageCount(talker.getId()));

		boolean emailVerification = false;
		if (session.contains("justloggedin") && talker.getVerifyCode() != null) {
			emailVerification = true;
			session.remove("justloggedin");
		}

		boolean newsLetterFlag = ApplicationDAO.isEmailExists(talker.getEmail());
		boolean rewardLetterFlag = ApplicationDAO.isnewsLetterSubscribe(talker.getEmail(),"TalkAboutHealth Rewards");

		List<DiseaseBean> diseaseList = DiseaseDAO.getCatchedDiseasesList(session);

		render("@newhome", talker, emailVerification, liveConversations, showNotificationAccounts,
		newsLetterFlag, rewardLetterFlag, allDiseaseList, diseaseList, talkerCat, allFeed);
    }

    private static boolean prepareNotificationPanel(Session session, TalkerBean talker) {
		boolean showNotificationAccounts = (!talker.getHiddenHelps().contains("notificationAccounts") && !talker.isAdmin());
		if (showNotificationAccounts) {
			//If user signs up via twitter, populate the Username field with the twitter username 
			//and automatically have the Twitter option checked
			ServiceAccountBean twitterAccount = talker.serviceAccountByType(ServiceType.TWITTER);
			if (twitterAccount != null) {
				talker.setIm("Twitter");
			}
			else {
				//if user provides an email from gmail, windows live, or yahoo - 
				//automatically populate the username with that item and check the option for what they provided
				IMAccountBean imInfo = CommonUtil.parseIMAccount(talker.getEmail());
				talker.setIm(imInfo.getService());
			}
			talker.setImUsername(talker.getUserName());
		}
		
		return showNotificationAccounts;
	}

	public static void conversationFeed() {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
    	Set<Action> convoFeed = FeedsLogic.getConvoFeed(talker, null);
    	Set<Action> communityFeed = FeedsLogic.getCommunityFeed(null, true,talker);
		TalkerLogic.preloadTalkerInfo(talker);
		
		render(talker, convoFeed, communityFeed);
    }
	

	public static void feedAjaxUpdate(String feedType,String beforeActionId,String talkerName,String isheader) {
    	TalkerBean _talker = CommonUtil.loadCachedTalker(session);
    	
    	boolean loggedIn = (_talker != null);
    	
    	Set<Action> _feedItems = null;
    	if ("convoFeed".equalsIgnoreCase(feedType)) {
    		_feedItems = FeedsLogic.updateFeed(FeedType.CONVERSATION,beforeActionId,_talker,true);
    	}else if ("communityFeed".equalsIgnoreCase(feedType)) {
    		_feedItems = FeedsLogic.updateFeed(FeedType.COMMUNITY,beforeActionId,_talker,loggedIn);
    	}else if ("allFeed".equalsIgnoreCase(feedType)) {
    		_feedItems = FeedsLogic.updateFeed(FeedType.ALL_CANCER,beforeActionId,_talker,loggedIn);
    	}else if ("mentions".equalsIgnoreCase(feedType)) {
    		TalkerBean profileTalker = TalkerDAO.getByUserName(talkerName);
    		if (profileTalker != null) {
    			List<Action> mentionList = CommentsDAO.getTalkerMentions(profileTalker,beforeActionId);
    			if(mentionList!=null){
    				_feedItems = new LinkedHashSet<Action>();
        			for (Action action : mentionList) {
        				PersonalProfileCommentAction preAction = (PersonalProfileCommentAction)action;
        				_feedItems.add(preAction);
        			}
    			}
    		}
    	}else {
   		TalkerBean profileTalker = TalkerDAO.getByUserName(talkerName);
    		if (profileTalker != null) {
        		_feedItems = FeedsLogic.updateFeed(FeedType.TALKER,beforeActionId,profileTalker,true);
    		}
    	}
    	int counter = 0;
    	if(_feedItems != null)
    		counter = _feedItems.size();
    	if(isheader.equals("1")) render("tags/feed/feedCounter.html",counter); else render("tags/feed/feedList.html", _feedItems, _talker);		
	}

	/**
	 * Used by "More" button in different feeds.
	 * @param afterActionId load actions after given action
	 */
    public static void feedAjaxLoad(String feedType, String afterActionId, String talkerName) {
    	TalkerBean _talker = CommonUtil.loadCachedTalker(session);
    	boolean loggedIn = (_talker != null);
    	Set<Action> _feedItems = null;
    	List<TalkerBean> _similarMembers = null;
    	List<ConversationBean> popularConvos = null;
    	String cancerType = session.get("cancerType");
    	if (feedType != null && feedType.contains("CommunityFeed")) {
    		int index = feedType.indexOf("CommunityFeed");
    		cancerType = feedType.substring(0, index);
    		cancerType = cancerType.replaceAll("_", " ");
    		String category = _talker.getCategory();
		 	_talker.setCategory(cancerType);
     		_feedItems = FeedsLogic.getCommunityFeed(afterActionId, loggedIn, _talker);
     		_talker.setCategory(category);
     		render("tags/feed/feedList.html", _feedItems, _talker);
     	}else if ("convoFeed".equalsIgnoreCase(feedType)) {
    		_feedItems = FeedsLogic.getConvoFeed(_talker, afterActionId);
    		render("tags/feed/feedList.html", _feedItems, _talker);
    	} else if ("popularConvo".equalsIgnoreCase(feedType)) {
    	     popularConvos = ConversationDAO.loadPopularConversations(afterActionId);
    	     render("tags/convo/convoList.html", popularConvos);
        } else if("USR".equalsIgnoreCase(feedType) || "EXP".equalsIgnoreCase(feedType)){
    		_similarMembers = TalkerLogic.getRecommendedTalkers(_talker,feedType,afterActionId,cancerType);
    		render("tags/profile/similarMemberList.html", _similarMembers);
    	} else if("TOPIC".equals(feedType)) {
    		List<TopicBean> _recommendedTopics = TalkerLogic.getRecommendedTopics(_talker,afterActionId);
    		render("tags/topicList.html", _recommendedTopics);
    	} else if("allFeed".equals(feedType)) {
    		_feedItems = FeedsLogic.getAllCancerFeed(afterActionId, loggedIn, _talker);
    		render("tags/feed/feedList.html", _feedItems, _talker);
    	} else if("openConvo".equalsIgnoreCase(feedType)) {
			List<ConversationBean> openConvo = ConversationDAO.getOpenQuestions(afterActionId,cancerType);
			render("tags/convo/convoList.html",openConvo, feedType);
		}  else {
    		TalkerBean profileTalker = TalkerDAO.getByUserName(talkerName);
    		if (profileTalker != null) {
    			_feedItems = FeedsLogic.getTalkerFeed(profileTalker, afterActionId);
    		}
    		render("tags/feed/feedList.html", _feedItems, _talker);
    	}
    }
    
    /**
     * Loads popular topics
     */
    public static void feedPopularTopics() {
    	List<TopicBean> _popularTopics = TopicLogic.loadPopularTopics(TopicLogic.TOPICS_PER_PAGE);
    	render("tags/common/popularTopics.html", _popularTopics,false,TopicLogic.TOPICS_PER_PAGE);
    	///tah-dev/app/views/tags/common/popularTopics.html
    }
    /* ---------------- Invitations ----------------- */
    public static void invitations() {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	int invitations = talker.getInvitations();
    	
    	flash.put("note", "I've joined TalkAboutHealth to get real-time health support. " +
    			"Here's an invitation for you to try it as well.");
    	render(invitations);
    }
    
    public static void sendInvitations(String emails, String note) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		Set<String> emailsToSend = CommonUtil.parseEmailsFromString(emails);
		
		validation.isTrue(!emailsToSend.isEmpty()).message("emails.incorrect");
		validation.isTrue(emailsToSend.size() <= talker.getInvitations()).message("emails.noinvites");
		
		if(validation.hasErrors()) {
			params.flash();
			int invitations = talker.getInvitations();
			render("@invitations", invitations);
            return;
        }
		
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("username", talker.getUserName());
		vars.put("invitation_note", note);
		for (String email : emailsToSend) {
			EmailUtil.sendEmail(EmailTemplate.INVITATION, email, vars, null, false);
		}
		
		//decrease invitations count
		talker.setInvitations(talker.getInvitations()-emailsToSend.size());
		CommonUtil.updateTalker(talker, session);
		
    	flash.success("ok");
    	invitations();
    }

    /**
     * Back-end method for Share features on Home/ConvoSummary/Topic pages
     * @param type Share type: 'email', 'twitter' or 'facebook'
     * @param from Username of talker who shares
     * @param pageType Type where sharing was: 'Topic', 'Conversation', 'TalkAboutHealth', ..
     * @param pageInfo Information related to this page (i.e. title of the topic or conversation)
     */
    public static void share(String type, String emails, String from, String note, String pageType, String pageInfo) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	
    	if (type != null) {
    		if (type.equalsIgnoreCase("email")) {
    			Set<String> emailsToSend = CommonUtil.parseEmailsFromString(emails);
    			
    			validation.isTrue(!emailsToSend.isEmpty()).message("emails.incorrect");
    			if(validation.hasErrors()) {
    				renderText("Error: Please input correct emails");
    	            return;
    	        }
    			
    			//preparing email parameters
    			Map<String, String> vars = new HashMap<String, String>();
    			vars.put("title", NotificationUtils.prepareEmailShareMessage(from, pageType, pageInfo));
    			note = note.replaceAll("\n", "<br/>");
    			vars.put("note", note);
    			for (String email : emailsToSend) {
    				EmailUtil.sendEmail(EmailTemplate.SHARE, email, vars, null, false);
    			}
    		}
    		else if (type.equalsIgnoreCase("twitter")) {
    			ServiceAccountBean twitterAccount = talker.serviceAccountByType(ServiceType.TWITTER);
    			if (twitterAccount != null) {
    				TwitterUtil.tweet(note, twitterAccount);
    			}
    		}
    		else if (type.equalsIgnoreCase("facebook")) {
    			ServiceAccountBean fbAccount = talker.serviceAccountByType(ServiceType.FACEBOOK);
    			if (fbAccount != null) {
    				FacebookUtil.post(note, fbAccount);
    			}
    		}
    	}
		
		renderText("Ok");
    }

    /**
     * Loading feeds using ajax
     * @param feedType, cancerType
     */
    public static void loadCancerFeed(String feedType, String csrType){
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	if(feedType.equalsIgnoreCase("allFeed")){
    		Set<Action> feedItems = FeedsLogic.getAllCancerFeed(null, true,talker);
    		render("tags/feed/allFeedList.html", feedItems, feedType);
    	}else if(feedType.equalsIgnoreCase("convoFeed")){
    		Set<Action> feedItems = FeedsLogic.getConvoFeed(talker, null);
    		render("tags/feed/allFeedList.html", feedItems, feedType);
    	}else if(feedType.equalsIgnoreCase("mentions")){
    		List<Action> feedItems = CommentsDAO.getTalkerMentions(talker,null);
    		render("tags/feed/allFeedList.html", feedItems, feedType);
    	}else if(feedType.equalsIgnoreCase("openConvo")){
    		List<ConversationBean> feedItems = null;
    		if(Security.isConnected())
    			feedItems = ConversationDAO.getOpenQuestions(null,csrType);
    		else
    			feedItems = ConversationDAO.getOpenQuestions(null,csrType);
			render("tags/feed/openFeedList.html",feedItems, feedType);
    	} else {
	    	Set<Action> multipleCancerCommunityFeed = null;
	    	Map<String, Set<Action>> allDiseaseList = new LinkedHashMap<String, Set<Action>>();
	    	String talkerCat = talker.getCategory();
	    	csrType = csrType.replaceAll("_", " ");
	    	talker.setCategory(csrType);
	    	multipleCancerCommunityFeed = FeedsLogic.getCommunityFeed(null, true,talker);
			allDiseaseList.put(talker.getCategory().replaceAll(" ", "_"),multipleCancerCommunityFeed);
			talker.setCategory(talkerCat);
			render("tags/feed/allCancerFeed.html", allDiseaseList, csrType);
    	}
    }
}

package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import logic.FeedsLogic;
import logic.TalkerLogic;
import models.IMAccountBean;
import models.ServiceAccountBean;
import models.TalkerBean;
import models.ConversationBean;
import models.TalkerDiseaseBean;
import models.TopicBean;
import models.ServiceAccountBean.ServiceType;
import models.actions.Action;
import models.actions.Action.ActionType;
import play.Logger;
import play.cache.Cache;
import play.data.validation.Validation.ValidationResult;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;
import util.EmailUtil;
import util.TwitterUtil;
import util.EmailUtil.EmailTemplate;
import util.ValidateData;
import dao.ActionDAO;
import dao.TalkerDAO;
import dao.ConversationDAO;
import dao.TalkerDiseaseDAO;
import dao.TopicDAO;

@With( { Secure.class, LoggerController.class } )
public class Home extends Controller {

    public static void index() {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		List<ConversationBean> liveConversations = ConversationDAO.getLiveConversations();
		
		Logger.error("Start Convo Feed");
//		Set<Action> convoFeed = FeedsLogic.getConvoFeed(talker, null);
//		Set<Action> communityFeed = null;
////		Logger.error(convoFeed.size()+" :::::: "+FeedsLogic.FEEDS_PER_PAGE);
//		if (convoFeed.size() < FeedsLogic.FEEDS_PER_PAGE) {
//			communityFeed = FeedsLogic.getCommunityFeed(null);
//		}
		
		Set<Action> convoFeed = FeedsLogic.getConvoFeed(talker, null);
    	Set<Action> communityFeed = FeedsLogic.getCommunityFeed(null, true);
    	
		
		boolean hasNoIMAccounts = (talker.getImAccounts() == null || talker.getImAccounts().size() == 0);
		//not show if twitter account or fb account has 'notify' option
		ServiceAccountBean twitterAccount = talker.serviceAccountByType(ServiceType.TWITTER);
		if (twitterAccount != null && twitterAccount.isTrue("NOTIFY")) {
			hasNoIMAccounts = false;
		}
		ServiceAccountBean fbAccount = talker.serviceAccountByType(ServiceType.FACEBOOK);
		if (fbAccount != null && fbAccount.isTrue("NOTIFY")) {
			hasNoIMAccounts = false;
		}
		boolean isAdmin = "admin".equals(Security.connected());
		boolean showIMPopup = (session.get("justregistered") != null && hasNoIMAccounts && !isAdmin);
		if (showIMPopup) {
			//pre-populate the fields based on user entries
			
			//If user signs up via twitter, populate the Username field with the twitter username 
			//and automatically have the Twitter option checked
			System.out.println(talker.getAccountType());
			if (talker.getAccountType() != null && talker.getAccountType().equalsIgnoreCase("twitter")) {
				talker.setIm("Twitter");
				talker.setImUsername(talker.getAccountName());
			}
			else {
				//if user provides an email from gmail, windows live, or yahoo - 
				//automatically populate the username with that item and check the option for what they provided
				IMAccountBean imInfo = CommonUtil.parseIMAccount(talker.getEmail());
				talker.setIm(imInfo.getService());
				talker.setImUsername(imInfo.getUserName());
			}
		}
		session.remove("justregistered");
		
		TalkerLogic.preloadTalkerInfo(talker);
		
//		Yes, for HealthInfo, let's use all of the data. 
//		For Profile info, we can use gender, age, marital status, number of children, and ages of children.
		
		TalkerDiseaseBean talkerDisease = TalkerDiseaseDAO.getByTalkerId(talker.getId());
		List<TopicBean> recommendedTopics = new ArrayList<TopicBean>();
		List<TopicBean> loadedTopics = new ArrayList<TopicBean>();
		if (!talker.isProf() && talkerDisease != null) {
			loadedTopics = TalkerLogic.getRecommendedTopics(talkerDisease);
		}
		if (recommendedTopics.isEmpty()) {
			//display most popular Topics based on number of questions
			loadedTopics = new ArrayList<TopicBean>(TopicDAO.loadAllTopics());
		}
		
		for (TopicBean topic : loadedTopics) {
			if (!talker.getFollowingTopicsList().contains(topic)) {
				recommendedTopics.add(topic);
			}
			if (recommendedTopics.size() == 3) {
				break;
			}
		}
		
		//talk
		List<TalkerBean> similarMembers = new ArrayList<TalkerBean>();
		List<TalkerBean> experts = new ArrayList<TalkerBean>();
		
		List<TalkerBean> allMembers = TalkerDAO.loadAllTalkers();
		for (TalkerBean member : allMembers) {
			  if (member.isSuspended() || member.isAdmin()) {
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
		
		List<ConversationBean> recommendedConvos = new ArrayList<ConversationBean>();
		List<ConversationBean> popularConvos = ConversationDAO.loadPopularConversations();	
		for (ConversationBean convo : popularConvos) {
			if (talker.getFollowingConvosList().contains(convo.getId())) {
				continue;
			}
			recommendedConvos.add(convo);
			if (recommendedConvos.size() == 3) {
				break;
			}
		}
		
		render("@newhome", talker, 
				liveConversations, convoFeed, communityFeed, showIMPopup,
				recommendedTopics, similarMembers, experts, recommendedConvos);
    }
    
    public static void conversationFeed() {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
    	Set<Action> convoFeed = FeedsLogic.getConvoFeed(talker, null);
    	Set<Action> communityFeed = FeedsLogic.getCommunityFeed(null, true);
    	
		TalkerLogic.preloadTalkerInfo(talker);
		
		render(talker, convoFeed, communityFeed);
    }
    
    public static void feedAjaxLoad(String feedType, String afterActionId, String talkerName) {
    	TalkerBean _talker = CommonUtil.loadCachedTalker(session);
    	boolean loggedIn = (_talker != null);
    	
    	Set<Action> _feedItems = null;
    	if ("convoFeed".equalsIgnoreCase(feedType)) {
    		_feedItems = FeedsLogic.getConvoFeed(_talker, afterActionId);
    	}
    	else if ("communityFeed".equalsIgnoreCase(feedType)) {
    		_feedItems = FeedsLogic.getCommunityFeed(afterActionId, loggedIn);
    	}
    	else {
    		TalkerBean profileTalker = TalkerDAO.getByUserName(talkerName);
    		if (profileTalker != null) {
    			_feedItems = FeedsLogic.getTalkerFeed(profileTalker, afterActionId);
    		}
    	}
    	
    	render("tags/feed/feedList.html", _feedItems, _talker);
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
		
		//parse and validate emails
		Set<String> emailsToSend = new HashSet<String>();
		String[] emailsArr = emails.split(",");	
		for (String email : emailsArr) {
			email = email.trim();
			if (ValidateData.validateEmail(email)) {
				emailsToSend.add(email);
			}
		}
		
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
    
    
    public static void share(String emails, String from, String note) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	
//    	System.out.println(emails+" : "+from+" : "+note);
		
		//parse and validate emails
		Set<String> emailsToSend = new HashSet<String>();
		String[] emailsArr = emails.split(",");	
		for (String email : emailsArr) {
			email = email.trim();
			if (ValidateData.validateEmail(email)) {
				emailsToSend.add(email);
			}
		}
		
		validation.isTrue(!emailsToSend.isEmpty()).message("emails.incorrect");
		
		if(validation.hasErrors()) {
			renderText("Error: Please input correct emails");
            return;
        }
		
		Map<String, String> vars = new HashMap<String, String>();
		//FIXME: finish subject?
		String subject = from+" thought you would be interested in the topic";
		vars.put("title", subject);
		note = note.replaceAll("\n", "<br/>");
		vars.put("note", note);
		for (String email : emailsToSend) {
			EmailUtil.sendEmail(EmailTemplate.SHARE, email, vars, null, false);
		}
		
		renderText("Ok");
    }

}

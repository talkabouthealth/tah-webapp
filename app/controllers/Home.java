package controllers;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import logic.FeedsLogic;
import models.IMAccountBean;
import models.TalkerBean;
import models.ConversationBean;
import models.actions.Action;
import models.actions.Action.ActionType;
import play.cache.Cache;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;
import util.EmailUtil;
import util.EmailUtil.EmailTemplate;
import util.ValidateData;
import webapp.LiveConversationsSingleton;
import dao.ActionDAO;
import dao.TalkerDAO;
import dao.ConversationDAO;

@With(Secure.class)
public class Home extends Controller {

    public static void index(String newTopic) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
//    	talker.setJoinedTopicsList(ConversationDAO.loadConversations(talker.getId(), ActionType.JOIN_CONVO));
    	talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
		
		if (newTopic == null || newTopic.trim().length() == 0) {
			newTopic = "Please enter your Conversation here ...";
		}
		
		//--------- For new Home Page --------------
		List<ConversationBean> liveConversations = ConversationDAO.getLiveConversations();
		
		Set<Action> convoFeed = FeedsLogic.getConvoFeed(talker, null);
		Set<Action> communityFeed = null;
//		System.out.println(convoFeed.size()+" :::::: "+FeedsLogic.FEEDS_PER_PAGE);
		if (convoFeed.size() < FeedsLogic.FEEDS_PER_PAGE) {
			communityFeed = FeedsLogic.getCommunityFeed(null);
		}
		
		boolean hasNoIMAccounts = (talker.getImAccounts() == null || talker.getImAccounts().size() == 0);
		boolean isAdmin = "admin".equals(Security.connected());
		boolean showIMPopup = (session.get("justregistered") != null && hasNoIMAccounts && !isAdmin);
		session.remove("justregistered");
		
		//FIXME: number of answers for this user??!
		
//        render(talker, mapTalkmiTopics, newTopic, convoFeed, showIMPopup);
		render("@newhome", talker, newTopic, liveConversations, convoFeed, communityFeed, showIMPopup);
    }
    
    public static void conversationFeed() {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
		
    	Set<Action> convoFeed = FeedsLogic.getConvoFeed(talker, null);
    	Set<Action> communityFeed = FeedsLogic.getCommunityFeed(null);
		
		render(talker, convoFeed, communityFeed);
    }
    
    public static void feedAjaxLoad(String feedType, String afterActionId) {
    	TalkerBean _talker = CommonUtil.loadCachedTalker(session);
    	
    	Set<Action> _convoFeed = null;
    	if ("convoFeed".equalsIgnoreCase(feedType)) {
    		_convoFeed = FeedsLogic.getConvoFeed(_talker, afterActionId);
    	}
    	else {
    		_convoFeed = FeedsLogic.getCommunityFeed(afterActionId);
    	}
    	render("tags/convoFeedList.html", _convoFeed, _talker);
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

}

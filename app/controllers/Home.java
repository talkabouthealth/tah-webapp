package controllers;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    	talker.setJoinedTopicsList(ConversationDAO.loadConversations(talker.getId(), ActionType.JOIN_CONVO));
    	//TODO: load only count?
    	talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
		
		if (newTopic == null || newTopic.trim().length() == 0) {
			newTopic = "Please enter your Conversation here ...";
		}
		
		//--------- For new Home Page --------------
		List<ConversationBean> liveConversations = ConversationDAO.getLiveConversations();
//		for (ConversationBean convoBean : liveConversations) {
//			System.out.println("!!: "+convoBean.getTopic());
//		}
		
//		List<ConversationBean> openedConversations = ConversationDAO.getOpenedConversations();
//		for (ConversationBean convoBean : openedConversations) {
//			System.out.println("??: "+convoBean.getTopic());
//		}
		
		//Convo feed?
		Set<Action> convoFeedActions = ActionDAO.loadConvoFeed(talker);
		
		//Action feed?
//		List<Action> activityFeed = ActionDAO.loadActivityFeed(talker);
		
//		Community Convo Feed
//		- Question started or restarted in the community
//		- Question answered
//		- Summary added or edited
//		- Answer voted for
		if (convoFeedActions.size() < 40) {
			List<Action> communityConvoFeed = ActionDAO.loadCommunityConvoFeed();
			convoFeedActions.addAll(communityConvoFeed);
		}
	
		//TODO: better truncate to 40?
		Set<Action> convoFeed = new LinkedHashSet<Action>();
		if (convoFeedActions.size() > 20) {
			for (Action action : convoFeedActions) {
				convoFeed.add(action);
				if (convoFeed.size() > 20) {
					break;
				}
			}
		}
		else {
			convoFeed = convoFeedActions;
		}
		
		boolean hasNoIMAccounts = (talker.getImAccounts() == null || talker.getImAccounts().size() == 0);
		boolean isAdmin = "admin".equals(Security.connected());
		boolean showIMPopup = (session.get("justloggedin") != null && hasNoIMAccounts && !isAdmin);
		session.remove("justloggedin");
		
//        render(talker, mapTalkmiTopics, newTopic, convoFeed, showIMPopup);
		render("@newhome", talker, newTopic, convoFeed, liveConversations, showIMPopup);
    }
    
    public static void conversationFeed() {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
		
		Set<Action> convoFeedActions = ActionDAO.loadConvoFeed(talker);
		
//		if (convoFeedActions.size() < 40) {
//			List<Action> communityConvoFeed = ActionDAO.loadCommunityConvoFeed();
//			convoFeedActions.addAll(communityConvoFeed);
//		}
	
		//TODO: better truncate to 40?
		Set<Action> convoFeed = new LinkedHashSet<Action>();
		if (convoFeedActions.size() > 20) {
			for (Action action : convoFeedActions) {
				convoFeed.add(action);
				if (convoFeed.size() > 20) {
					break;
				}
			}
		}
		else {
			convoFeed = convoFeedActions;
		}
		
		render(talker, convoFeed);
    }
    
    public static void openQuestions() {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
		
		List<ConversationBean> openQuestions = ConversationDAO.getOpenQuestions();
		render(talker, openQuestions);
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

package controllers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import models.TalkerBean;
import models.ConversationBean;
import play.cache.Cache;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;
import util.EmailUtil;
import util.ValidateData;
import webapp.LiveConversationsSingleton;
import dao.TalkerDAO;
import dao.ConversationDAO;

@With(Secure.class)
public class Home extends Controller {

    public static void index(String newTopic) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	//TODO: load Joined, not Started?
    	talker.setNumberOfTopics(ConversationDAO.getNumberOfTopics(talker.getId()));
    	//TODO: load only count?
    	talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
		
    	//TODO: use cache?
    	Map<String, ConversationBean> mapTalkmiTopics = ConversationDAO.queryTopics();
//		Map<String, TopicBean> mapTalkmiTopics = new LinkedHashMap<String, TopicBean>(40);
//		LiveConversationsSingleton lcs = LiveConversationsSingleton.getReference();
//		if(lcs.getLiveConversationMap().size() < 20) {
//			mapTalkmiTopics = TopicDAO.queryTopics();
//		}
//		Cache.set(session.getId()+"-mapTalkmiTopics", mapTalkmiTopics);
		
		if (newTopic == null || newTopic.trim().length() == 0) {
			newTopic = "Please enter your Conversation here ...";
		}
		
        render(talker, mapTalkmiTopics, newTopic);
    }
    
    public static void invitations() {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	int invitations = talker.getInvitations();
    	
    	flash.put("note", "I've joined TalkAboutHealth to get real-time health support. Here's an invitation for you to try it as well.");
    	
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
			EmailUtil.sendEmail(EmailUtil.INVITATION_TEMPLATE, email, vars, null, false);
		}
		
		//decrease invitations count
		talker.setInvitations(talker.getInvitations()-emailsToSend.size());
		TalkerDAO.updateTalker(talker);
		
    	flash.success("ok");
    	invitations();
    }

}

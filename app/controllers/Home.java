package controllers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import models.TalkerBean;
import models.TopicBean;
import play.cache.Cache;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;
import util.EmailUtil;
import util.ValidateData;
import webapp.LiveConversationsSingleton;
import dao.TalkerDAO;
import dao.TopicDAO;

@With(Secure.class)
public class Home extends Controller {

    public static void index(String newTopic) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	talker.setNumberOfTopics(TopicDAO.getNumberOfTopics(talker.getId()));
    	//TODO: load only count?
    	talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
		
    	//TODO: use cache?
    	Map<String, TopicBean> mapTalkmiTopics = TopicDAO.queryTopics();
//		Map<String, TopicBean> mapTalkmiTopics = new LinkedHashMap<String, TopicBean>(40);
//		LiveConversationsSingleton lcs = LiveConversationsSingleton.getReference();
//		if(lcs.getLiveConversationMap().size() < 20) {
//			mapTalkmiTopics = TopicDAO.queryTopics();
//		}
//		Cache.set(session.getId()+"-mapTalkmiTopics", mapTalkmiTopics);
		
		if (newTopic == null || newTopic.trim().length() == 0) {
			newTopic = "Please enter your Conversation here ...";
		}
		
		//For loading previous/next topics. Do we need to use this?
//		if (count == 1) {
//			SimpleDateFormat SQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//			session.setAttribute("latesttimestamp", SQL_DATE_FORMAT.format(tbTalkmiTopic.getDisplayTime()));
//			//System.out.println("Latest date: " + SQL_DATE_FORMAT.format(tbTalkmiTopic.getDisplayTime()));
//		} else if (count == 40) {
//			SimpleDateFormat SQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//			session.setAttribute("earliesttimestamp", SQL_DATE_FORMAT.format(tbTalkmiTopic.getDisplayTime()));
//			//System.out.println("Earliest date: " + SQL_DATE_FORMAT.format(tbTalkmiTopic.getDisplayTime()));
//		}
		
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
			EmailUtil.sendEmail(EmailUtil.INVITATION_TEMPLATE, email, vars);
		}
		
		//decrease invitations count
		talker.setInvitations(talker.getInvitations()-emailsToSend.size());
		TalkerDAO.updateTalker(talker);
		
    	flash.success("ok");
    	invitations();
    }

}

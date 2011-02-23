package controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

import util.CommonUtil;
import util.FacebookUtil;
import util.NotificationUtils;
import util.TwitterUtil;

import logic.TalkerLogic;
import models.CommentBean;
import models.TalkerBean;
import models.ConversationBean.ConvoType;
import models.ServiceAccountBean.ServiceType;
import models.TalkerBean.EmailSetting;
import models.ServiceAccountBean;
import models.TalkerTopicInfo;
import models.ThankYouBean;
import models.TopicBean;
import models.actions.Action;
import models.actions.Action.ActionType;
import models.actions.FollowTalkerAction;
import models.actions.GiveThanksAction;
import models.actions.PersonalProfileCommentAction;
import models.actions.StartConvoAction;
import dao.ActionDAO;
import dao.CommentsDAO;
import dao.TalkerDAO;
import dao.TopicDAO;

/**
 * TODO: move to other controller
 * Back-end for Ajax methods related to talker
 */
@With(Secure.class)
public class Actions extends Controller {
	
	/**
	 * Create 'thank you' from authenticated talker to talker with id 'toTalkerId'
	 * @param tagFile tag template file that is used for response
	 */
	public static void createThankYou(String toTalkerId, String note, String tagFile) {
		TalkerBean fromTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean toTalker = TalkerDAO.getById(toTalkerId);
		//TODO: better design for permissions?
		if (fromTalker.equals(toTalker)) {
			forbidden();
			return;
		}
		
		ThankYouBean thankYouBean = new ThankYouBean();
		thankYouBean.setTime(new Date());
		thankYouBean.setNote(note);
		thankYouBean.setFromTalker(fromTalker);
		thankYouBean.setTo(toTalkerId);
		TalkerDAO.saveThankYou(thankYouBean);
		
		//TODO: actions and email notifications? - better design?
		ActionDAO.saveAction(new GiveThanksAction(fromTalker, toTalker));
		
		//email notification
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("other_talker", fromTalker.getUserName());
		vars.put("thankyou_text", thankYouBean.getNote());
		NotificationUtils.sendEmailNotification(EmailSetting.RECEIVE_THANKYOU, 
				toTalker, vars);
		
		ThankYouBean _thankYou = thankYouBean;
		TalkerBean _currentTalker = fromTalker;
		render("tags/publicprofile/thanksThankYou.html", _thankYou, _currentTalker);
	}
	
	/**
	 * Deletes thank you
	 * 
	 * @param talkerId Id of talker to whom this ThankYou is
	 * @param thankYouId
	 */
	public static void deleteThankYou(String talkerId, String thankYouId) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getById(talkerId);
		ThankYouBean thankYou = talker.getThankYouById(thankYouId);
		if ( !(currentTalker.isAdmin() || currentTalker.equals(thankYou.getFromTalker())) ) {
			//only admin or thankyou creator can delete it
			forbidden();
			return;
		}
		
		TalkerDAO.deleteThankYou(thankYou);
		renderText("ok");
	}
	
	/**
	 * Follow or unfollow given talker by authenticated talker.
	 */
	public static void followTalker(String followingId) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		TalkerBean followingTalker = TalkerDAO.getById(followingId);
		
		boolean follow = true;
		if (talker.getFollowingList().contains(followingTalker)) {
			//we need unfollow
			follow = false;
		}
		
		TalkerDAO.followAction(talker.getId(), followingId, follow);
		CommonUtil.refreshCachedTalker(session);
		
		//Text for the follow link after this action
		if (follow) {
			//follow notifications
			ActionDAO.saveAction(new FollowTalkerAction(talker, followingTalker));
			
			Map<String, String> vars = new HashMap<String, String>();
			vars.put("other_talker", talker.getUserName());
			NotificationUtils.sendEmailNotification(EmailSetting.NEW_FOLLOWER, 
					followingTalker, vars);
			
			renderText("Unfollow");
		}
		else {
			renderText("Follow");
		}
	}
	
	/**
	 * Save thought/reply in the given profile
	 * @param profileTalkerId
	 * @param parentId Id of the parent thought (for replies) or null
	 * @param text
	 * @param cleanText Text of comment without html (used for links)
	 * @param from page where request was made
	 */
	//TODO: check better option for cleanText?
	//TODO: too many params?
	public static void saveProfileComment(String profileTalkerId, String parentId, 
			String text, String cleanText, String from, Boolean ccTwitter, Boolean ccFacebook) {
		CommentBean comment = 
			TalkerLogic.saveProfileComment(CommonUtil.loadCachedTalker(session), 
					profileTalkerId, parentId, text, cleanText, null, null, ccTwitter, ccFacebook);
		notFoundIfNull(comment);
		
		if (from != null && from.equals("home")) {
    		TalkerBean _talker = comment.getFromTalker();
    		Action _activity = new PersonalProfileCommentAction(_talker, _talker, comment, null, ActionType.PERSONAL_PROFILE_COMMENT);
    		render("tags/feed/feedActivity.html", _talker, _activity);
		}
		else {
			List<CommentBean> _commentsList = Arrays.asList(comment);
			int _level = (comment.getParentId() == null ? 1 : 2);
			boolean _showDelete = false;
			boolean _isFeed = false;
			render("tags/publicprofile/profileCommentsTree.html", _commentsList, _level, _showDelete, _isFeed);
		}
	}
	
}

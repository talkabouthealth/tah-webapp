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
		
		ActionDAO.saveAction(new GiveThanksAction(fromTalker, toTalker));
		NotificationUtils.emailNotifyOnThankYou(fromTalker, toTalker, thankYouBean);
		
		ThankYouBean _thankYou = thankYouBean;
		TalkerBean _currentTalker = fromTalker;
		TalkerBean _talker = toTalker;
		render("tags/publicprofile/thanksThankYou.html", _thankYou, _talker, _currentTalker);
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
		if ( !(currentTalker.isAdmin() 
				|| currentTalker.equals(thankYou.getFromTalker())
				|| currentTalker.equals(talker)) ) {
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
			ActionDAO.saveAction(new FollowTalkerAction(talker, followingTalker));
			NotificationUtils.emailNotifyOnFollow(talker, followingTalker);
			
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
	public static void saveProfileComment(String profileTalkerId, String parentId, 
			String text, String cleanText, String from, Boolean ccTwitter, Boolean ccFacebook) {
		CommentBean comment = 
			TalkerLogic.saveProfileComment(CommonUtil.loadCachedTalker(session), 
					profileTalkerId, parentId, text, cleanText, null, null, ccTwitter, ccFacebook);
		notFoundIfNull(comment);
		
		if (from != null && from.equals("home")) {
			//for Home page we add new thought to feeds, so we return thought as feed activity item
    		TalkerBean _talker = comment.getFromTalker();
    		Action _activity = new PersonalProfileCommentAction(_talker, _talker, comment, null, ActionType.PERSONAL_PROFILE_COMMENT);
    		_activity.setID(comment.getActionId());
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
	
	/**
	 * Delete thought/reply
	 * @param commentId
	 * @throws Throwable 
	 */
	public static void deleteComment(String commentId) throws Throwable {
		Secure.checkAccess();
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	CommentBean comment = CommentsDAO.getProfileCommentById(commentId);
    	notFoundIfNull(comment);
    	
    	//only admin or author can delete
    	if ( !(talker.getId().equals(comment.getProfileTalkerId()) || talker.isAdmin()) ) {
    		forbidden();
    		return;
    	}
    	
    	comment.setDeleted(true);
		CommentsDAO.updateProfileComment(comment);
		
		//remove all actions connected with this comment
		ActionDAO.deleteActionsByProfileComment(comment);
    	renderText("ok");
    }
	
	/**
	 * Update thought/reply
	 * @param commentId
	 * @param newText
	 * @throws Throwable 
	 */
	public static void updateComment(String commentId, String newText) throws Throwable {
		Secure.checkAccess();
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	CommentBean comment = CommentsDAO.getProfileCommentById(commentId);
    	notFoundIfNull(comment);
    	
    	//only author can update
    	if (!talker.getId().equals(comment.getProfileTalkerId())) {
    		forbidden();
    		return;
    	}
    	
    	String oldText = comment.getText();
		if (!oldText.equals(newText)) {
			comment.getOldTexts().add(oldText);
			comment.setText(newText);
			CommentsDAO.updateProfileComment(comment);
		}
		
    	renderText("ok");
    }
	
	
	
}

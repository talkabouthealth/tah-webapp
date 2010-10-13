package controllers;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.mvc.Controller;
import play.mvc.With;

import util.CommonUtil;
import util.NotificationUtils;

import models.CommentBean;
import models.TalkerBean;
import models.TalkerBean.EmailSetting;
import models.TalkerTopicInfo;
import models.ThankYouBean;
import models.TopicBean;
import models.actions.Action.ActionType;
import models.actions.FollowTalkerAction;
import models.actions.GiveThanksAction;
import models.actions.PersonalProfileCommentAction;
import dao.ActionDAO;
import dao.CommentsDAO;
import dao.TalkerDAO;
import dao.TopicDAO;

@With(Secure.class)
public class Actions extends Controller {
	
	public static void createThankYou(String toTalker, String note, String tagFile) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		TalkerBean toTalkerBean = TalkerDAO.getById(toTalker);
		
		if (talker.equals(toTalkerBean)) {
			forbidden();
			return;
		}
		
		ThankYouBean thankYouBean = new ThankYouBean();
		thankYouBean.setTime(new Date());
		thankYouBean.setNote(note);
		thankYouBean.setFromTalker(talker);
		thankYouBean.setTo(toTalker);
		TalkerDAO.saveThankYou(thankYouBean);
		
		//TODO: every email notification is with action? can we use it?
		ActionDAO.saveAction(new GiveThanksAction(talker, toTalkerBean));
		
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("other_talker", talker.getUserName());
		vars.put("thankyou_text", thankYouBean.getNote());
		NotificationUtils.sendEmailNotification(EmailSetting.RECEIVE_THANKYOU, 
				toTalkerBean, vars);
		
		//render html of new comment using tag - different for PublicProfile and ThankYous page
		ThankYouBean _thankYou = thankYouBean;
		if (tagFile == null) {
			tagFile = "profileThankYou";
		}
		render("tags/"+tagFile+".html", _thankYou);
	}
	
	public static void follow(String followingId) {
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
			//Follow notifications
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
	
	public static void saveProfileComment(String profileTalkerId, String parentId, String text) {
		CommentBean comment = saveProfileCommentInternal(profileTalkerId, parentId, text);
		
		//render html of new comment using tag
		List<CommentBean> _commentsList = Arrays.asList(comment);
		int _level = (comment.getParentId() == null ? 1 : 2);
		render("tags/profileCommentsTree.html", _commentsList, _level);
	}
	
	/**
	 * Version for new design - returns other html to caller
	 * @param profileTalkerId
	 * @param parentId
	 * @param text
	 */
	public static void saveProfileComment2(String profileTalkerId, String parentId, String text) {
		CommentBean comment = saveProfileCommentInternal(profileTalkerId, parentId, text);
		
		//render html of new comment using tag
		List<CommentBean> _commentsList = Arrays.asList(comment);
		int _level = (comment.getParentId() == null ? 1 : 2);
		render("tags/profileCommentsTree2.html", _commentsList, _level);
	}
	
	
	private static CommentBean saveProfileCommentInternal(String profileTalkerId, String parentId, String text) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		if (parentId != null && parentId.length() != 0) {
			CommentBean parentAnswer = CommentsDAO.getProfileCommentById(parentId);
			if (parentAnswer != null) {
				profileTalkerId = parentAnswer.getProfileTalkerId();
			}
		}
		
		TalkerBean profileTalker = TalkerDAO.getById(profileTalkerId);
		notFoundIfNull(profileTalker);
//		if ( !(profileTalker.getFollowingList().contains(talker) || profileTalker.equals(talker)) ) {
//			forbidden();
//			return;
//		}
		
		CommentBean comment = new CommentBean();
		comment.setParentId(parentId.trim().length() == 0 ? null : parentId);
		comment.setProfileTalkerId(profileTalkerId);
		comment.setFromTalker(talker);
		comment.setText(text);
		comment.setTime(new Date());
		
//		System.out.println(comment.getParentId());
//		System.out.println(comment.getProfileTalkerId());
		
		String id = CommentsDAO.saveProfileComment(comment);
		comment.setId(id);
		
		if (comment.getParentId() == null) {
			ActionDAO.saveAction(new PersonalProfileCommentAction(
					talker, profileTalker, comment, null, ActionType.PERSONAL_PROFILE_COMMENT));
		}
		else {
			CommentBean parentAnswer = new CommentBean(comment.getParentId());
			ActionDAO.saveAction(new PersonalProfileCommentAction(
					talker, profileTalker, parentAnswer, comment, ActionType.PERSONAL_PROFILE_REPLY));
		}
		
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("other_talker", talker.getUserName());
		vars.put("comment_text", comment.getText());
		NotificationUtils.sendEmailNotification(EmailSetting.RECEIVE_COMMENT, 
				profileTalker, vars);
		
		return comment;
	}

}

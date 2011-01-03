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
		
		ActionDAO.saveAction(new GiveThanksAction(talker, toTalkerBean));
		
		Map<String, String> vars = new HashMap<String, String>();
		vars.put("other_talker", talker.getUserName());
		vars.put("thankyou_text", thankYouBean.getNote());
		NotificationUtils.sendEmailNotification(EmailSetting.RECEIVE_THANKYOU, 
				toTalkerBean, vars);
		
		//render html of new comment using tag - different for PublicProfile and ThankYous page
		ThankYouBean _thankYou = thankYouBean;
		if (tagFile == null) {
			tagFile = "publicprofile/profileThankYou";
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
	
	/**
	 * Version for new design - returns other html to caller
	 * @param profileTalkerId
	 * @param parentId
	 * @param text
	 */
	public static void saveProfileComment2(String profileTalkerId, String parentId, String text, String from) {
		CommentBean comment = saveProfileCommentInternal(profileTalkerId, parentId, text);
		
		if (from != null && from.equals("home")) {
    		TalkerBean _talker = comment.getFromTalker();
    		Action _activity = new PersonalProfileCommentAction(_talker, _talker, comment, null, ActionType.PERSONAL_PROFILE_COMMENT);
    		render("tags/feed/feedActivity.html", _talker, _activity);
		}
		else {
			//render html of new comment using tag
			List<CommentBean> _commentsList = Arrays.asList(comment);
			int _level = (comment.getParentId() == null ? 1 : 2);
			boolean _showDelete = false;
			boolean _isFeed = false;
			render("tags/publicprofile/profileCommentsTree2.html", _commentsList, _level, _showDelete, _isFeed);
		}
	}
	
	
	private static CommentBean saveProfileCommentInternal(String profileTalkerId, String parentId, String text) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
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
		notFoundIfNull(profileTalker);
		
		CommentBean comment = new CommentBean();
		comment.setParentId(parentId.trim().length() == 0 ? null : parentId);
		comment.setProfileTalkerId(profileTalkerId);
		comment.setFromTalker(talker);
		comment.setText(text);
		comment.setTime(new Date());
		
		CommentsDAO.saveProfileComment(comment);
		
		if (comment.getParentId() == null) {
			//post to personal Thoughts Feed?
			if (talker.equals(profileTalker)) {
				ActionDAO.saveAction(new PersonalProfileCommentAction(
						talker, profileTalker, comment, null, ActionType.PERSONAL_PROFILE_COMMENT));
				
				for (ServiceAccountBean serviceAccount : talker.getServiceAccounts()) {
//					Logger.error("SHARE? "+serviceAccount.getType().toString()+" : "+serviceAccount.isTrue("SHARE_FROM_THOUGHTS"));
					if (!serviceAccount.isTrue("SHARE_FROM_THOUGHTS")) {
						continue;
					}
					
					Logger.error(serviceAccount.getToken()+" : "+serviceAccount.getTokenSecret());
					
					if (serviceAccount.getType() == ServiceType.TWITTER) {
						TwitterUtil.makeUserTwit(comment.getText(), 
								serviceAccount.getToken(), serviceAccount.getTokenSecret());
					}
					else if (serviceAccount.getType() == ServiceType.FACEBOOK) {
						FacebookUtil.post(comment.getText(), serviceAccount.getToken());
					}
				}
			}
		}
		else {
			//for replies we update action for parent comment
			ActionDAO.updateProfileCommentAction(comment.getParentId());
			
			CommentBean thought = CommentsDAO.getProfileCommentById(comment.getParentId());
			if (!talker.equals(thought.getFromTalker())) {
				//send to user who started the thread
				Map<String, String> vars = new HashMap<String, String>();
				vars.put("other_talker", talker.getUserName());
				vars.put("comment_text", comment.getText());
				NotificationUtils.sendEmailNotification(EmailSetting.RECEIVE_COMMENT, 
						thought.getFromTalker(), vars);
			}
		}
		
		if (!talker.equals(profileTalker)) {
			Map<String, String> vars = new HashMap<String, String>();
			vars.put("other_talker", talker.getUserName());
			vars.put("comment_text", comment.getText());
			NotificationUtils.sendEmailNotification(EmailSetting.RECEIVE_COMMENT, 
					profileTalker, vars);
		}
		
		return comment;
	}

}

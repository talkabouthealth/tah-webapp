package controllers;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import play.mvc.Controller;
import play.mvc.With;

import util.CommonUtil;

import models.CommentBean;
import models.TalkerBean;
import models.ThankYouBean;
import models.actions.FollowTalkerAction;
import models.actions.GiveThanksAction;
import models.actions.ProfileCommentAction;
import models.actions.ProfileReplyAction;
import dao.ActivityDAO;
import dao.TalkerDAO;

@With(Secure.class)
public class Actions extends Controller {
	
	public static void createThankYou(String toTalker, String note, String tagFile) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		ThankYouBean thankYouBean = new ThankYouBean();
		thankYouBean.setTime(new Date());
		thankYouBean.setNote(note);
		thankYouBean.setFromTalker(talker);
		thankYouBean.setTo(toTalker);
		TalkerDAO.saveThankYou(thankYouBean);
		
		ActivityDAO.saveActivity(new GiveThanksAction(talker, new TalkerBean(toTalker)));
		
		CommonUtil.updateCachedTalker(session);
		
		//render html of new comment using tag
		ThankYouBean _thankYou = thankYouBean;
		
		//default tag
		if (tagFile == null) {
			tagFile = "profileThankYou";
		}
		render("tags/"+tagFile+".html", _thankYou);
	}
	
	public static void follow(String followingId) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		boolean followAction = true;
		if (talker.getFollowingList().contains(new TalkerBean(followingId))) {
			//we need unfollow
			followAction = false;
		}
		
		TalkerDAO.followAction(talker.getId(), followingId, followAction);
		CommonUtil.updateCachedTalker(session);
		
		//Text for the follow link after this action
		if (followAction) {
			ActivityDAO.saveActivity(new FollowTalkerAction(talker, new TalkerBean(followingId)));
			
			renderText("Unfollow");
		}
		else {
			renderText("Follow");
		}
	}
	
	public static void saveProfileComment(String profileTalkerId, String parentId, String text) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		//only people who you follow should be able to leave a 'Comment'
		TalkerBean profileTalker = TalkerDAO.getById(profileTalkerId);
		notFoundIfNull(profileTalker);
		if ( !(profileTalker.getFollowingList().contains(talker) || profileTalker.equals(talker)) ) {
			forbidden();
			return;
		}
		
		CommentBean comment = new CommentBean();
		comment.setParentId(parentId.trim().length() == 0 ? null : parentId);
		comment.setProfileTalkerId(profileTalkerId);
		comment.setFromTalker(talker);
		comment.setText(text);
		comment.setTime(new Date());
		
		String id = TalkerDAO.saveProfileComment(comment);
		comment.setId(id);
		
		if (comment.getParentId() == null) {
			ActivityDAO.saveActivity(new ProfileCommentAction(talker, profileTalker));
		}
		else {
			ActivityDAO.saveActivity(new ProfileReplyAction(talker, profileTalker));
		}
		
		//render html of new comment using tag
		List<CommentBean> _commentsList = Arrays.asList(comment);
		int _level = (comment.getParentId() == null ? 1 : 2);
		render("tags/profileCommentsTree.html", _commentsList, _level);
	}

}

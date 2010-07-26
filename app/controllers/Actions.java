package controllers;

import java.util.Date;

import play.mvc.Controller;

import util.CommonUtil;

import models.CommentBean;
import models.TalkerBean;
import models.ThankYouBean;
import dao.TalkerDAO;

public class Actions extends Controller {
	
	public static void createThankYou(String toTalker, String note) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		ThankYouBean thankYouBean = new ThankYouBean();
		thankYouBean.setTime(new Date());
		thankYouBean.setNote(note);
		thankYouBean.setFrom(talker.getId());
		thankYouBean.setTo(toTalker);
		TalkerDAO.saveThankYou(thankYouBean);
		
		CommonUtil.updateCachedTalker(session);
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
			renderText("Unfollow");
		}
		else {
			renderText("Follow");
		}
	}
	
	public static void saveProfileComment(String profileTalkerId, String parentId, String text) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		CommentBean comment = new CommentBean();
		//TODO: check?
		comment.setParentId(parentId);
		comment.setProfileTalkerId(profileTalkerId);
		comment.setFromTalker(talker);
		comment.setText(text);
		comment.setTime(new Date());
		
		TalkerDAO.saveProfileComment(comment);
	}

}

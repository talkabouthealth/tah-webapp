package controllers;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logic.TalkerLogic;
import models.CommentBean;
import models.DiseaseBean;
import models.HealthItemBean;
import models.TalkerBean;
import models.TalkerDiseaseBean;
import models.ConversationBean;
import models.TalkerTopicInfo;
import models.TopicBean;
import models.actions.Action;
import models.actions.FollowConvoAction;
import models.actions.FollowTalkerAction;
import models.actions.GiveThanksAction;
import models.actions.JoinConvoAction;
import models.actions.StartConvoAction;
import models.actions.UpdateProfileAction;
import models.actions.Action.ActionType;
import dao.ActionDAO;
import dao.CommentsDAO;
import dao.DiseaseDAO;
import dao.HealthItemDAO;
import dao.TalkerDAO;
import dao.TalkerDiseaseDAO;
import dao.ConversationDAO;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;

@With(Secure.class)
public class PublicProfile extends Controller {
	
	/**
	 * 
	 * @param userName
	 * @param action
	 * @param from item to start from (used for paging)
	 */
	public static void userBasedActions(String userName, String action, int from) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByUserName(userName);
		notFoundIfNull(talker);
		
		talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
		
		render(talker, currentTalker, action, from);
	}
	
	public static void journal(String userName) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByUserName(userName);
		notFoundIfNull(talker);
		
		talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
		talker.setProfileCommentsList(CommentsDAO.loadProfileComments(talker.getId()));
		
		//If the user views his own journal he should see the following text under the text box 
		//until the user posts for the first time (even if another user posts first, this should still appear):
		boolean firstTimeComment = false;
		if (talker.equals(currentTalker)) {
			firstTimeComment = true;
			//user views his own journal - check if he's made comment
			for (CommentBean cb : talker.getProfileCommentsList()) {
				if (cb.getFromTalker().equals(talker)) {
					firstTimeComment = false;
					break;
				}
			}
		}
		
		render(talker, currentTalker, firstTimeComment);
	}
	
	public static void deleteComment(String commentId) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	CommentBean comment = CommentsDAO.getProfileCommentById(commentId);
    	notFoundIfNull(comment);
    	
    	if (!talker.getId().equals(comment.getProfileTalkerId())) {
    		forbidden();
    		return;
    	}
    	
    	comment.setDeleted(true);
		CommentsDAO.updateProfileComment(comment);
		
		//remove all actions connected with this comment
		ActionDAO.deleteActionByProfileComment(comment);

    	renderText("ok");
    }
	
	
	public static void conversationsFollowing(String userName) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByUserName(userName);
		notFoundIfNull(talker);
		
		String topicsType = "Following";
		List<ConversationBean> topicsList = TalkerDAO.loadFollowingConversations(talker.getId());
		
		render("@conversationsList", talker, currentTalker, topicsType, topicsList);
	}
	
	public static void conversationsStarted(String userName) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByUserName(userName);
		notFoundIfNull(talker);
		
		String topicsType = "Started";
		List<ConversationBean> topicsList = 
			ConversationDAO.loadConversations(talker.getId(), ActionType.START_CONVO);
		
		render("@conversationsList", talker, currentTalker, topicsType, topicsList);
	}
	
	public static void conversationsJoined(String userName) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByUserName(userName);
		notFoundIfNull(talker);
		
		String topicsType = "Joined";
		List<ConversationBean> topicsList = 
			ConversationDAO.loadConversations(talker.getId(), ActionType.JOIN_CONVO);
		
		render("@conversationsList", talker, currentTalker, topicsType, topicsList);
	}
	
	public static void topicsFollowing(String userName) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByUserName(userName);
		notFoundIfNull(talker);
		
		for (TopicBean topic : talker.getFollowingTopicsList()) {
			List<CommentBean> answers = 
				CommentsDAO.getTalkerAnswers(talker.getId(), topic);
			
			TalkerTopicInfo talkerTopicInfo = talker.getTopicsInfoMap().get(topic);
			if (talkerTopicInfo == null) {
				talkerTopicInfo = new TalkerTopicInfo();
				talker.getTopicsInfoMap().put(topic, talkerTopicInfo);
			}
			talkerTopicInfo.setNumOfAnswers(answers.size());
		}
		
		talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
		//TODO: we need it only for Profile Completion calculation?
		talker.setActivityList(ActionDAO.load(talker.getId()));
		TalkerLogic.calculateProfileCompletion(talker);
		
		render(talker, currentTalker);
	}
	
}

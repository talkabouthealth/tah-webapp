package controllers;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import logic.FeedsLogic;
import logic.TalkerLogic;
import models.CommentBean;
import models.DiseaseBean;
import models.HealthItemBean;
import models.TalkerBean;
import models.TalkerDiseaseBean;
import models.ConversationBean;
import models.TalkerTopicInfo;
import models.TopicBean;
import models.CommentBean.Vote;
import models.actions.Action;
import models.actions.AnswerConvoAction;
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
import dao.TopicDAO;
import play.i18n.Messages;
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
		
		talker.setProfileCommentsList(CommentsDAO.loadProfileComments(talker.getId()));
		TalkerLogic.preloadTalkerInfo(talker);
		
		render(talker, currentTalker, action, from);
	}
	
	public static void thoughts(String userName) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByUserName(userName);
		notFoundIfNull(talker);
		
		talker.setProfileCommentsList(CommentsDAO.loadProfileComments(talker.getId()));
		TalkerLogic.preloadTalkerInfo(talker);
		
		//If the user views his own thoughts he should see the following text under the text box 
		//until the user posts for the first time (even if another user posts first, this should still appear):
		boolean firstTimeComment = false;
		if (talker.equals(currentTalker)) {
			firstTimeComment = true;
			//user views his own thoughts - check if he's made comment
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
	
	public static void updateComment(String commentId, String newText) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	CommentBean comment = CommentsDAO.getProfileCommentById(commentId);
    	notFoundIfNull(comment);
    	
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
	
	public static void answers(String userName) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByUserName(userName);
		notFoundIfNull(talker);
		
		TalkerLogic.preloadTalkerInfo(talker);
		
		List<CommentBean> allAnswers = CommentsDAO.getTalkerAnswers(talker.getId(), null);
		
		List<Action> answersFeed = new ArrayList<Action>();
		int numOfTopAnswers = 0;
		for (CommentBean answer : allAnswers) {
			ConversationBean convo = ConversationDAO.getByConvoId(answer.getConvoId());
			convo.setComments(CommentsDAO.loadConvoAnswers(convo.getId()));
			
			if (!convo.getComments().isEmpty() && convo.getComments().get(0).equals(answer)) {
				numOfTopAnswers++;
			}
			
//			Action answerAction = new StartConvoAction(convo.getTalker(), convo, ActionType.START_CONVO);
			AnswerConvoAction answerAction = new AnswerConvoAction(talker, convo, answer, null, ActionType.ANSWER_CONVO);
			answerAction.setTime(answer.getTime());
			
			answersFeed.add(answerAction);
		}
		
		render(talker, currentTalker, answersFeed, numOfTopAnswers);
	}
	
	public static void conversations(String userName) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByUserName(userName);
		notFoundIfNull(talker);
		
		//TODO: check permissions?
		//TODO: use sets?
		//TODO: not here - wildcards in related convos!
		List<ConversationBean> followingList = TalkerDAO.loadFollowingConversations(talker.getId());
		List<ConversationBean> startedList = 
			ConversationDAO.loadConversations(talker.getId(), ActionType.START_CONVO);
		List<ConversationBean> joinedList = 
			ConversationDAO.loadConversations(talker.getId(), ActionType.JOIN_CONVO);
		
		talker.setProfileCommentsList(CommentsDAO.loadProfileComments(talker.getId()));
		TalkerLogic.preloadTalkerInfo(talker);
		
		render(talker, currentTalker, followingList, startedList, joinedList);
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
		
		TalkerLogic.preloadTalkerInfo(talker);
		
		TalkerDiseaseBean talkerDisease = TalkerDiseaseDAO.getByTalkerId(talker.getId());
		
		List<TopicBean> recommendedTopics = loadRecommendedTopics(talker, talkerDisease, null);
		
		render(talker, currentTalker, talkerDisease, recommendedTopics);
	}
	
	public static void recommendedTopicsAjaxLoad(String afterId) {
		//FIXME: improve speed?
    	TalkerBean _talker = CommonUtil.loadCachedTalker(session);
    	TalkerDiseaseBean talkerDisease = TalkerDiseaseDAO.getByTalkerId(_talker.getId());
    	
    	List<TopicBean> _recommendedTopics = loadRecommendedTopics(_talker, talkerDisease, afterId);
    	
    	render("tags/recommendedTopicsList.html", _recommendedTopics, _talker);
    }
	
	private static List<TopicBean> loadRecommendedTopics(TalkerBean talker, 
			TalkerDiseaseBean talkerDisease, String afterId) {
		List<TopicBean> recommendedTopics = new ArrayList<TopicBean>();
		List<TopicBean> loadedTopics = new ArrayList<TopicBean>();
		if (!talker.isProf() && talkerDisease != null) {
			loadedTopics = TalkerLogic.getRecommendedTopics(talkerDisease);
		}
		if (recommendedTopics.isEmpty()) {
			//display most popular Topics based on number of questions
			loadedTopics = new ArrayList<TopicBean>(TopicDAO.loadAllTopics());
		}
		
		final int numberOfPages = 10;
		boolean canAdd = (afterId == null);
		for (TopicBean topic : loadedTopics) {
			if (canAdd && !talker.getFollowingTopicsList().contains(topic)) {
				recommendedTopics.add(topic);
			}
			
			if (topic.getId().equals(afterId)) {
				canAdd = true;
			}
			
			if (recommendedTopics.size() == numberOfPages) {
				break;
			}
		}
		
		return recommendedTopics;
	}
	
}

package controllers;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import logic.ConversationLogic;
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
import models.actions.AnswerDisplayAction;
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

public class PublicProfile extends Controller {
	
	//FIXME: check all methods PublicProfile
	/**
	 * ThankYous, Following/Followers
	 * @param userName
	 * @param action
	 * @param from item to start from (was used for paging)
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
	
	public static void answers(String userName) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByUserName(userName);
		notFoundIfNull(talker);
		
		TalkerLogic.preloadTalkerInfo(talker);
		boolean noHealthInfo = TalkerLogic.talkerHasNoHealthInfo(talker);
		
		List<Action> answersFeed = new ArrayList<Action>();
		int numOfTopAnswers = TalkerLogic.prepareTalkerAnswers(talker.getId(), answersFeed);
		
		render(talker, currentTalker, answersFeed, numOfTopAnswers, noHealthInfo);
	}
	
	/**
	 * Started/Joined/Following conversations
	 * @param userName
	 */
	public static void conversations(String userName) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByUserName(userName);
		notFoundIfNull(talker);
		
		List<Action> startedConvosFeed = 
			TalkerLogic.prepareTalkerConvos(ConversationDAO.loadConversations(talker.getId(), ActionType.START_CONVO));
		List<Action> joinedConvosFeed = 
			TalkerLogic.prepareTalkerConvos(ConversationDAO.loadConversations(talker.getId(), ActionType.JOIN_CONVO));
		List<Action> followingConvosFeed = TalkerLogic.prepareTalkerConvos(TalkerLogic.loadFollowingConversations(talker));
		
		talker.setProfileCommentsList(CommentsDAO.loadProfileComments(talker.getId()));
		TalkerLogic.preloadTalkerInfo(talker);
		boolean noHealthInfo = TalkerLogic.talkerHasNoHealthInfo(talker);
		
		//TODO: move to recommended conversations
		/* 
		 	Ideas for recommended conversations :
				1) match member following topics with convo topics
				2) match member info with full convo info
				3) what conversations are being followed by other members the user follows. 
		*/
		Set<ConversationBean> allConvos = new LinkedHashSet<ConversationBean>();
		for (TopicBean topic : talker.getFollowingTopicsList()) {
			allConvos.addAll(ConversationDAO.loadConversationsByTopic(topic.getId()));
		}
		allConvos.addAll(ConversationDAO.loadPopularConversations());
		
		List<ConversationBean> recommendedConvos = new ArrayList<ConversationBean>();
		for (ConversationBean convo : allConvos) {
			if (talker.getFollowingConvosList().contains(convo.getId())) {
				continue;
			}
			recommendedConvos.add(convo);
			if (recommendedConvos.size() == 3) {
				break;
			}
		}
		
		render(talker, currentTalker, startedConvosFeed, joinedConvosFeed, followingConvosFeed, 
				noHealthInfo, recommendedConvos);
	}
	
	
	public static void topicsFollowing(String userName) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByUserName(userName);
		notFoundIfNull(talker);
		
		//get talker answers and related info for each topic
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
    	TalkerBean _talker = CommonUtil.loadCachedTalker(session);
    	TalkerDiseaseBean talkerDisease = TalkerDiseaseDAO.getByTalkerId(_talker.getId());
    	
    	List<TopicBean> _recommendedTopics = loadRecommendedTopics(_talker, talkerDisease, afterId);
    	render("tags/publicprofile/recommendedTopicsList.html", _recommendedTopics, _talker);
    }
	
	/**
	 * Return recommended topics for given talker.
	 * Two criterias:
	 * - based on HealthInfo;
	 * - popularity of topics (number of questions)
	 * 
	 * @param afterId id of last topic from previous load (used for paging)
	 */
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
		
		final int numberPerPage = 10;
		boolean canAdd = (afterId == null);
		for (TopicBean topic : loadedTopics) {
			if (canAdd && !talker.getFollowingTopicsList().contains(topic)) {
				//recommended topics shouldn't contain default topics
				if (! (topic.getTitle().equals(ConversationLogic.DEFAULT_QUESTION_TOPIC) 
						|| topic.getTitle().equals(ConversationLogic.DEFAULT_TALK_TOPIC)) ) {
					recommendedTopics.add(topic);
				}
			}
			if (topic.getId().equals(afterId)) {
				canAdd = true;
			}
			//enough for this page?
			if (recommendedTopics.size() == numberPerPage) {
				break;
			}
		}
		
		return recommendedTopics;
	}
	
}

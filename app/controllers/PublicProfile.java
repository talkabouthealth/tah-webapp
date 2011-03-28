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
import logic.TopicLogic;
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
import play.Logger;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;

public class PublicProfile extends Controller {
	
	/**
	 * ThankYous, Following/Followers
	 * @param userName
	 * @param action
	 * @param from item to start from (was used for paging)
	 */
	public static void userBasedActions(String userName, String action, int from) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByURLName(userName);
		notFoundIfNull(talker);
		
		TalkerLogic.preloadTalkerInfo(talker);
		
		render(talker, currentTalker, action, from);
	}
	
	public static void thoughts(String userName) {
		Logger.info("====== Thoughts ("+userName+") =======");
		long start = System.currentTimeMillis();
		Logger.info("Th0:"+System.currentTimeMillis());
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		Logger.info("Th1:"+System.currentTimeMillis());
		TalkerBean talker = TalkerDAO.getByURLName(userName);
		notFoundIfNull(talker);
		
		Logger.info("Th2:"+System.currentTimeMillis());
		
		talker.setProfileCommentsList(CommentsDAO.loadProfileComments(talker.getId()));
		TalkerLogic.preloadTalkerInfo(talker);
		
		Logger.info("Th3:"+System.currentTimeMillis());
		
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
		
		Logger.info("Th4:"+System.currentTimeMillis());
		Logger.info("ThF1:"+ (System.currentTimeMillis() - start));
		
		render(talker, currentTalker, firstTimeComment);
	}
	
	public static void answers(String userName) {
		Logger.info("====== Answers ("+userName+") =======");
		Logger.info("AN0:"+System.currentTimeMillis());
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByURLName(userName);
		notFoundIfNull(talker);
		
		Logger.info("AN1:"+System.currentTimeMillis());
		
		TalkerLogic.preloadTalkerInfo(talker);
		Logger.info("ANa:"+System.currentTimeMillis());
		boolean noHealthInfo = TalkerLogic.talkerHasNoHealthInfo(talker);
		
		Logger.info("AN2:"+System.currentTimeMillis());
		
		List<Action> answersFeed = new ArrayList<Action>();
		int numOfTopAnswers = TalkerLogic.prepareTalkerAnswers(talker.getId(), answersFeed, true);
		
		Logger.info("AN3:"+System.currentTimeMillis());
		
		render(talker, currentTalker, answersFeed, numOfTopAnswers, noHealthInfo);
	}
	
	/**
	 * Started/Joined/Following conversations
	 * @param userName
	 */
	//TODO: later - add paging? as we have many convos on this page
	public static void conversations(String userName) {
		Logger.info("====== Questions ("+userName+") =======");
		Logger.info("QU0:"+System.currentTimeMillis());
		//TODO: can we improver this? very common
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByURLName(userName);
		notFoundIfNull(talker);
		
		Logger.info("QU1:"+System.currentTimeMillis());
		
		List<Action> startedConvosFeed = 
			ConversationLogic.convosToFeed(ConversationDAO.getStartedConvos(talker.getId()));
		Logger.info("QUa:"+System.currentTimeMillis());
		List<Action> joinedConvosFeed = 
			ConversationLogic.convosToFeed(ConversationDAO.loadConversations(talker.getId(), ActionType.JOIN_CONVO));
		Logger.info("QUb:"+System.currentTimeMillis());
		List<Action> followingConvosFeed = ConversationLogic.convosToFeed(TalkerLogic.loadFollowingConversations(talker));
		
		Logger.info("QU2:"+System.currentTimeMillis());
		
		TalkerLogic.preloadTalkerInfo(talker);
		boolean noHealthInfo = TalkerLogic.talkerHasNoHealthInfo(talker);
		
		Logger.info("QU3:"+System.currentTimeMillis());
		
		List<ConversationBean> recommendedConvos = new ArrayList<ConversationBean>();
		if (talker.equals(currentTalker)) {
			recommendedConvos = TalkerLogic.getRecommendedConvos(talker);
		}
		
		Logger.info("QU4:"+System.currentTimeMillis());
		
		render(talker, currentTalker, startedConvosFeed, joinedConvosFeed, followingConvosFeed, 
				noHealthInfo, recommendedConvos);
	}
	
	
	public static void topicsFollowing(String userName) {
		Logger.info("====== Topics Following ("+userName+") =======");
		Logger.info("TF0:"+System.currentTimeMillis());
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByURLName(userName);
		notFoundIfNull(talker);
		
		Logger.info("TF1:"+System.currentTimeMillis());
		
		//get talker answers and related info for each topic
		for (TopicBean topic : talker.getFollowingTopicsList()) {
			//TODO: improve here?
			List<CommentBean> answers = 
				CommentsDAO.getTalkerAnswers(talker.getId(), topic);
			
			TalkerTopicInfo talkerTopicInfo = talker.getTopicsInfoMap().get(topic);
			if (talkerTopicInfo == null) {
				talkerTopicInfo = new TalkerTopicInfo();
				talker.getTopicsInfoMap().put(topic, talkerTopicInfo);
			}
			talkerTopicInfo.setNumOfAnswers(answers.size());
		}
		
		Logger.info("TF2:"+System.currentTimeMillis());
		
		TalkerLogic.preloadTalkerInfo(talker);
		
		Logger.info("TF3:"+System.currentTimeMillis());
		
		TalkerDiseaseBean talkerDisease = null;
		List<TopicBean> recommendedTopics = null;
		if (talker.equals(currentTalker)) {
			talkerDisease = TalkerDiseaseDAO.getByTalkerId(talker.getId());
			recommendedTopics = TopicLogic.loadRecommendedTopics(talker, talkerDisease, null);
		}
		
		Logger.info("TF4:"+System.currentTimeMillis());
		
		render(talker, currentTalker, talkerDisease, recommendedTopics);
	}
	
	public static void recommendedTopicsAjaxLoad(String afterId) throws Throwable {
		Secure.checkAccess();
    	TalkerBean _talker = CommonUtil.loadCachedTalker(session);
    	TalkerDiseaseBean talkerDisease = TalkerDiseaseDAO.getByTalkerId(_talker.getId());
    	
    	List<TopicBean> _recommendedTopics = TopicLogic.loadRecommendedTopics(_talker, talkerDisease, afterId);
    	render("tags/publicprofile/recommendedTopicsList.html", _recommendedTopics, _talker);
    }
	
}

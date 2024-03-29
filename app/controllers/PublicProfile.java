package controllers;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;
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
import models.ThankYouBean;
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
		
		//For getting thank you's list
		TalkerLogic.preloadTalkerInfo(talker);
		List<ThankYouBean> thankyouList = talker.getThankYouList();
		for(ThankYouBean thankYouBean : thankyouList){
			List<Action> profileComments = CommentsDAO.getProfileComments(thankYouBean.getId(),talker);
			thankYouBean.setProfileComments(profileComments);
		}
		
		//Displaying thank you's in most recent order
		if(thankyouList != null && thankyouList.size() > 1){
			for(int i=0;i<thankyouList.size();i++){
				for(int index=0;index<thankyouList.size()-1;index++){
					Date indexdate;
					Date indexdate1;
					List<Action> indexProfilecomments=((ThankYouBean)thankyouList.get(index)).getProfileComments();
					if(indexProfilecomments.size()==0)
						indexdate=(Date)((ThankYouBean)thankyouList.get(index)).getTime().clone();
					else
						indexdate=(Date)indexProfilecomments.get(indexProfilecomments.size()-1).getTime().clone();
					
					List<Action> indexProfilecomments1=((ThankYouBean)thankyouList.get(index+1)).getProfileComments();
					if(indexProfilecomments1.size()==0)
						indexdate1=(Date)((ThankYouBean)thankyouList.get(index+1)).getTime().clone();
					else
						indexdate1=(Date)indexProfilecomments1.get(indexProfilecomments1.size()-1).getTime().clone();
					if(indexdate.before(indexdate1)){
						ThankYouBean temp=((ThankYouBean)thankyouList.get(index));
						thankyouList.set(index, ((ThankYouBean)thankyouList.get(index+1)));
						thankyouList.set(index+1, temp);
					}
				}
			}
		}
		int numOfStartedConvos = ConversationDAO.getNumOfStartedConvos(talker.getId());
		int commentCount = CommentsDAO.loadProfileCommentCount(talker.getId());
		render(talker, currentTalker, action, from, thankyouList,numOfStartedConvos,commentCount);
	}
	
	public static void thoughts(String userName) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByURLName(userName);
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
		int numOfStartedConvos = ConversationDAO.getNumOfStartedConvos(talker.getId());
		int commentCount = CommentsDAO.loadProfileCommentCount(talker.getId());
		render(talker, currentTalker, firstTimeComment,commentCount,numOfStartedConvos);
	}
	
	public static void answers(String userName) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByURLName(userName);
		notFoundIfNull(talker);
		
		TalkerLogic.preloadTalkerInfo(talker);
		boolean noHealthInfo = TalkerLogic.talkerHasNoHealthInfo(talker);
		
		List<Action> answersFeedTemp = new ArrayList<Action>();
		List<Action> answersFeed = new ArrayList<Action>();
		int numOfTopAnswers = TalkerLogic.prepareTalkerAnswers(talker.getId(), answersFeedTemp, true);
		
		//added because if convo is null then answers are not displayed.
		for(int index=0; index < answersFeedTemp.size(); index++){
			if(answersFeedTemp.get(index).getConvo()!=null)
				answersFeed.add(answersFeedTemp.get(index));
		}
		answersFeedTemp.clear();
		int numOfStartedConvos = ConversationDAO.getNumOfStartedConvos(talker.getId());
		int commentCount = CommentsDAO.loadProfileCommentCount(talker.getId());
		render(talker, currentTalker, answersFeed, numOfTopAnswers, noHealthInfo,commentCount,numOfStartedConvos);
	}
	
	/**
	 * Started/Joined/Following conversations
	 * @param userName
	 */
	public static void conversations(String userName) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByURLName(userName);
		notFoundIfNull(talker);
		
		List<Action> startedConvosFeed = 
			ConversationLogic.convosToFeed(
					ConversationDAO.getStartedConvos(talker.getId(), null, ConversationLogic.CONVERSATIONS_PER_PAGE));
		int numOfStartedConvos = ConversationDAO.getNumOfStartedConvos(talker.getId());
		
		//Chats were removed temporarily
		List<Action> joinedConvosFeed = new ArrayList<Action>();
		/*
		List<Action> joinedConvosFeed = 
			ConversationLogic.convosToFeed(ConversationDAO.loadConversations(talker.getId(), ActionType.JOIN_CONVO));
		*/
		
		List<Action> followingConvosFeed = 
			ConversationLogic.convosToFeed(
					TalkerLogic.loadFollowingConversations(talker, null, ConversationLogic.CONVERSATIONS_PER_PAGE));
		int numOfFollowingConvos = TalkerLogic.getNumOfFollowingConversations(talker);

		TalkerLogic.preloadTalkerInfo(talker);
		boolean noHealthInfo = TalkerLogic.talkerHasNoHealthInfo(talker);
		
		List<ConversationBean> recommendedConvos = new ArrayList<ConversationBean>();
		if (talker.equals(currentTalker)) {
			recommendedConvos = TalkerLogic.getRecommendedConvos(talker);
		}
		int commentCount = CommentsDAO.loadProfileCommentCount(talker.getId());
		
		render(talker, currentTalker, 
				startedConvosFeed, joinedConvosFeed, followingConvosFeed,
				numOfStartedConvos, numOfFollowingConvos,
				noHealthInfo, recommendedConvos,commentCount);
	}
	
	/**
	 * Used by "More" button for loading started, joined and following conversations
	 */
    public static void convoAjaxLoad(String convoType, String afterConvoId, String talkerId) {
    	TalkerBean _talker = CommonUtil.loadCachedTalker(session);
    	
    	List<Action> _feedItems = null;
    	if ("questionsAsked".equalsIgnoreCase(convoType)) {
    		_feedItems = ConversationLogic.convosToFeed(
					ConversationDAO.getStartedConvos(talkerId, afterConvoId, ConversationLogic.CONVERSATIONS_PER_PAGE));
    	}
    	else if ("questionsFollowing".equalsIgnoreCase(convoType)) {
    		TalkerBean talkerForLoad = TalkerDAO.getById(talkerId);
    		_feedItems = ConversationLogic.convosToFeed(
					TalkerLogic.loadFollowingConversations(talkerForLoad, afterConvoId, ConversationLogic.CONVERSATIONS_PER_PAGE));
    	}
    	
    	render("tags/feed/feedList.html", _feedItems, _talker);
    }
	
	
	public static void topicsFollowing(String userName) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getByURLName(userName);
		notFoundIfNull(talker);
		
		//get talker answers and related info for each topic
		for (TopicBean topic : talker.getFollowingTopicsList()) {
			TalkerTopicInfo talkerTopicInfo = talker.getTopicsInfoMap().get(topic);
			if (talkerTopicInfo == null) {
				talkerTopicInfo = new TalkerTopicInfo();
				talker.getTopicsInfoMap().put(topic, talkerTopicInfo);
			}
			talkerTopicInfo.setNumOfAnswers(CommentsDAO.getTalkerNumberOfAnswers(talker.getId(), topic));
		}
		
		TalkerLogic.preloadTalkerInfo(talker);
		
		List<TalkerDiseaseBean> talkerDiseaseList = TalkerDiseaseDAO.getListByTalkerId(talker.getId());
		TalkerDiseaseBean talkerDisease = null;
		List<TopicBean> recommendedTopics = null;
		if (talker.equals(currentTalker)) {
			if(talkerDiseaseList != null){
				for(TalkerDiseaseBean diseaseBean : talkerDiseaseList){
					if(diseaseBean != null && diseaseBean.getDiseaseName().equalsIgnoreCase(talker.getCategory())){
						talkerDisease = diseaseBean;
					}
				}
			}
			recommendedTopics = TopicLogic.loadRecommendedTopics(talker, talkerDisease, null);
		}
		int numOfStartedConvos = ConversationDAO.getNumOfStartedConvos(talker.getId());
		int commentCount = CommentsDAO.loadProfileCommentCount(talker.getId());
		render(talker, currentTalker, talkerDisease, recommendedTopics,numOfStartedConvos,commentCount);
	}
	
	public static void recommendedTopicsAjaxLoad(String afterId) throws Throwable {
		Secure.checkAccess();
    	TalkerBean _talker = CommonUtil.loadCachedTalker(session);
    	List<TalkerDiseaseBean> talkerDiseaseList = TalkerDiseaseDAO.getListByTalkerId(_talker.getId());
        TalkerDiseaseBean talkerDisease = null;
        if(talkerDiseaseList != null){
			for(TalkerDiseaseBean diseaseBean : talkerDiseaseList){
				if(diseaseBean != null && diseaseBean.getDiseaseName().equalsIgnoreCase(_talker.getCategory())){
					talkerDisease = diseaseBean;
				}
			}
		}
    	
    	List<TopicBean> _recommendedTopics = TopicLogic.loadRecommendedTopics(_talker, talkerDisease, afterId);
    	render("tags/publicprofile/recommendedTopicsList.html", _recommendedTopics, _talker);
    }
	
	public static void loadProfileFeedList(String type, String lastActionId,String userId) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		TalkerBean talker = TalkerDAO.getById(userId);
		List<Action> _feedItems = new ArrayList<Action>();
		notFoundIfNull(talker);
		if(lastActionId != null && "".equals(lastActionId))
			lastActionId = null;
		if(type.equals("answer")) {
			List<Action> answersFeedTemp = TalkerLogic.prepareTalkerAnswers(talker.getId(), true,lastActionId);
			//added because if convo is null then answers are not displayed.
			for(int index=0; index < answersFeedTemp.size(); index++) {
				if(answersFeedTemp.get(index).getConvo()!=null)
					_feedItems.add(answersFeedTemp.get(index));
			}
			answersFeedTemp.clear();
			render("tags/feed/feedList_new.html",_feedItems,type);
		} else if(type.equals("thoughts")) {

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
			int numOfStartedConvos = ConversationDAO.getNumOfStartedConvos(talker.getId());
			int commentCount = CommentsDAO.loadProfileCommentCount(talker.getId());
			render("tags/feed/thoughtfeedList_new.html",talker, currentTalker, firstTimeComment,commentCount,numOfStartedConvos);
		} else if(type.equals("thankyou")) {
			List<ThankYouBean> thankyouList = talker.getThankYouList();
			for(ThankYouBean thankYouBean : thankyouList){
				List<Action> profileComments = CommentsDAO.getProfileComments(thankYouBean.getId(),talker);
				thankYouBean.setProfileComments(profileComments);
			}
			
			//Displaying thank you's in most recent order
			if(thankyouList != null && thankyouList.size() > 1){
				for(int i=0;i<thankyouList.size();i++){
					for(int index=0;index<thankyouList.size()-1;index++){
						Date indexdate;
						Date indexdate1;
						List<Action> indexProfilecomments=((ThankYouBean)thankyouList.get(index)).getProfileComments();
						if(indexProfilecomments.size()==0)
							indexdate=(Date)((ThankYouBean)thankyouList.get(index)).getTime().clone();
						else
							indexdate=(Date)indexProfilecomments.get(indexProfilecomments.size()-1).getTime().clone();
						
						List<Action> indexProfilecomments1=((ThankYouBean)thankyouList.get(index+1)).getProfileComments();
						if(indexProfilecomments1.size()==0)
							indexdate1=(Date)((ThankYouBean)thankyouList.get(index+1)).getTime().clone();
						else
							indexdate1=(Date)indexProfilecomments1.get(indexProfilecomments1.size()-1).getTime().clone();
						if(indexdate.before(indexdate1)){
							ThankYouBean temp=((ThankYouBean)thankyouList.get(index));
							thankyouList.set(index, ((ThankYouBean)thankyouList.get(index+1)));
							thankyouList.set(index+1, temp);
						}
					}
				}
			}
			talker.setThankYouList(thankyouList);
			render("tags/publicprofile/thankYouTree_new.html",talker);
		} else {
			_feedItems = ConversationLogic.convosToFeed(ConversationDAO.getStartedConvos(userId, lastActionId, ConversationLogic.CONVERSATIONS_PER_PAGE));
			render("tags/feed/feedList_new.html",_feedItems,type);
		}
	}
}

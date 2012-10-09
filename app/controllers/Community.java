package controllers;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import logic.FeedsLogic;
import models.CommentBean;
import models.ConversationBean;
import models.DiseaseBean;
import models.TalkerBean;
import models.VideoBean;
import models.actions.Action;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.With;
import util.CommonUtil;
import util.SearchUtil;
import dao.ActionDAO;
import dao.ApplicationDAO;
import dao.CommentsDAO;
import dao.CommunityDAO;
import dao.ConversationDAO;
import dao.DiseaseDAO;
import dao.TalkerDAO;
import dao.VideoDAO;

public class Community extends Controller {
	
	public static void index() {
		String cancerType = "";
		if (Security.isConnected()) {
    		//redirect to Home page if user is logged in
    		Home.index();
    	} else {
    		String[] arr = request.host.split("\\.");
    		if (arr != null && arr.length > 0) {
    			if(arr.length == 3) {
    				//cancerType= arr[0];
    				cancerType = "Breast Cancer";
    			} else {
    				cancerType = params.get("type");
    				if(StringUtils.isBlank(cancerType) ){
    					cancerType = session.get("cancerType");
    				}
    			}
        	} else {
        		cancerType = params.get("type");
        		if(StringUtils.isBlank(cancerType) ){
					cancerType = session.get("cancerType");
				}
        	}
    		if(StringUtils.isBlank(cancerType) || DiseaseDAO.getDiseaseByName(cancerType) == null)
    			cancerType = "Breast Cancer";
    		
    		System.out.println("cancerType: " + cancerType);
    		session.put("cancerType", cancerType);
    		
    		long numberOfMembers = TalkerDAO.getNumberOfTalkers();
    		long numberOfAnswers = CommentsDAO.getNumberOfAnswers();
    		List<DiseaseBean> diseaseList = DiseaseDAO.getCatchedDiseasesList(session);
    		List<VideoBean> videoList = VideoDAO.loadVideoForHome(3);
    		//future communities
    		render("@index",diseaseList, videoList, numberOfMembers, numberOfAnswers,cancerType);
    	}
	}
	
	public static void viewCommunity() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		List<TalkerBean> communityMembers = TalkerDAO.loadAllTalkers();
		for (TalkerBean member : communityMembers) {
			List<CommentBean> answers = CommentsDAO.getTalkerAnswers(member.getId(), null);
			member.setNumOfConvoAnswers(answers.size());
		}
		//get only Top 3 members by number of answers
		Collections.sort(communityMembers, new Comparator<TalkerBean>() {
			@Override
			public int compare(TalkerBean talker1, TalkerBean talker2) {
				return talker2.getNumOfConvoAnswers()-talker1.getNumOfConvoAnswers();
			}
		});
		if (communityMembers.size() > 3) {
			communityMembers = communityMembers.subList(0, 3);
		}
		
		List<ConversationBean> liveTalks = ConversationDAO.getLiveConversations();
		Set<Action> communityConvoFeed = FeedsLogic.getCommunityFeed(null, (talker != null), talker);
		
		render(talker, liveTalks, communityConvoFeed, communityMembers);
	}

	public static void homePageFeed(String type, String lastActionId) {
		String cancerType = session.get("cancerType");
		/*String[] arr = request.host.split("\\.");
		if (arr != null && arr.length > 0) {
			if(arr.length > 2){
				//cancerType= arr[0];
				cancerType = "Breast Cancer";
				//cancerType = "Lung Cancer";
			}
    	}*/
		
		if(lastActionId != null && "".equals(lastActionId))
			lastActionId = null;
		if(type.equals("expert")) {
			List<ConversationBean> convoFeed = CommunityDAO.loadExpertsAnswer(lastActionId,cancerType);
			render("Explore/homeFeedList.html",convoFeed, type);
		}
		if(type.equals("open")) {
			List<ConversationBean> convoFeed = CommunityDAO.getOpenQuestions(lastActionId,cancerType);
			render("Explore/homeFeedList.html",convoFeed, type);
		}
		if(type.equals("recent")){
			TalkerBean talker = CommonUtil.loadCachedTalker(session);
			if(talker == null){
				talker = new TalkerBean();
				talker.setCategory(cancerType);
			}
			Set<Action> convoFeed = FeedsLogic.getAllCancerFeed(lastActionId, Security.isConnected(), talker);
			//Set<Action> convoFeed = FeedsLogic.getCommunityFeed(lastActionId, Security.isConnected(), talker);
			render("Explore/homeFeedList.html",convoFeed, type);
		}
	}
}

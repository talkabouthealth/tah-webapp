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

import logic.FeedsLogic;
import models.CommentBean;
import models.ConversationBean;
import models.TalkerBean;
import models.actions.Action;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;
import util.SearchUtil;
import dao.ActionDAO;
import dao.ApplicationDAO;
import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;

//TODO: html/js todos 
public class Community extends Controller {
	
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
		Set<Action> communityConvoFeed = FeedsLogic.getCommunityFeed(null, (talker != null));
		
		render(talker, liveTalks, communityConvoFeed, communityMembers);
	}
	
	public static void browseMembers(String action) throws Throwable {
		Secure.checkAccess();
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);

		//Active talkers on this day
		Calendar oneDayBeforeNow = Calendar.getInstance();
		oneDayBeforeNow.add(Calendar.DAY_OF_MONTH, -1);
		Set<TalkerBean> activeTalkers = ApplicationDAO.getActiveTalkers(oneDayBeforeNow.getTime());
		Set<TalkerBean> newTalkers = ApplicationDAO.getNewTalkers();
		
		//check if search is performed now
		String query = params.get("query");
		List<TalkerBean> results = null;
		if (query != null) {
			params.flash("query");
			
			try {
				results = SearchUtil.searchTalker(query);
			}
			catch (Exception e) {
				Logger.error(e, "Talker search on Browser Members page.");
			}
		}
		
		//Move members to particular tabs based on member's connection
		Map<String, Set<TalkerBean>> members = new LinkedHashMap<String, Set<TalkerBean>>();
		members.put("Experts", new LinkedHashSet<TalkerBean>());
		members.put("Patients", new LinkedHashSet<TalkerBean>());
		members.put("Former Patients", new LinkedHashSet<TalkerBean>());
		members.put("Parents", new LinkedHashSet<TalkerBean>());
		members.put("Caregivers", new LinkedHashSet<TalkerBean>());
		members.put("Family & Friends", new LinkedHashSet<TalkerBean>());
		
		//match tabs with possible connections
		Map<String, List<String>> memberTypes = new LinkedHashMap<String, List<String>>();
		memberTypes.put("Experts", TalkerBean.PROFESSIONAL_CONNECTIONS_LIST);
		memberTypes.put("Patients", Arrays.asList("Patient"));
		memberTypes.put("Former Patients", Arrays.asList("Former Patient"));
		memberTypes.put("Parents", Arrays.asList("Parent"));
		memberTypes.put("Caregivers", Arrays.asList("Caregiver"));
		memberTypes.put("Family & Friends", Arrays.asList("Family member", "Friend"));
		
		Set<TalkerBean> allActiveTalkers = ApplicationDAO.getActiveTalkers(null);
		for (TalkerBean talker : allActiveTalkers) {
			for (Entry<String, List<String>> memberTypeEntry : memberTypes.entrySet()) {
				if (memberTypeEntry.getValue().contains(talker.getConnection())) {
					members.get(memberTypeEntry.getKey()).add(talker);
				}
			}
		}
		
		//default tab is 'active'
		if (action == null) {
			action = "active";
		}
		render(currentTalker, action, activeTalkers, newTalkers, results, members);
	}

}

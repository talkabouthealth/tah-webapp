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
import java.util.Set;

import logic.FeedsLogic;
import models.CommentBean;
import models.ConversationBean;
import models.TalkerBean;
import models.actions.Action;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;
import util.SearchUtil;
import dao.ActionDAO;
import dao.ApplicationDAO;
import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;

//TODO: different html/js todos 
public class Community extends Controller {
	
	public static void viewCommunity() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		List<TalkerBean> communityMembers = TalkerDAO.loadAllTalkers();
		for (TalkerBean member : communityMembers) {
			List<CommentBean> answers = CommentsDAO.getTalkerAnswers(member.getId(), null);
			member.setNumOfConvoAnswers(answers.size());
		}
		
		//get only Top 3 by answers
		Collections.sort(communityMembers, new Comparator<TalkerBean>() {
			@Override
			public int compare(TalkerBean o1, TalkerBean o2) {
				return o2.getNumOfConvoAnswers()-o1.getNumOfConvoAnswers();
			}
		});
		if (communityMembers.size() > 3) {
			communityMembers = communityMembers.subList(0, 3);
		}
		
		List<ConversationBean> liveTalks = ConversationDAO.getLiveConversations();
		Set<Action> communityConvoFeed = FeedsLogic.getCommunityFeed(null, (talker != null));
		
		render(talker, liveTalks, communityConvoFeed, communityMembers);
	}
	
	//TODO: move it to Explore?
//	For the 4 tabs:
//		Active Users - Latest users to log in
//		New Users - Latest users to sign up
//		Like You - we will implement logic later, I think we can use Lucene or Sphinx
//		Search - implement later with Lucene or Sphinx
	
//	1) Active
//	2) New
//	3) Search
//	4) Experts
//	5) Patients
//	6) Former Patients
//	7) Parents
//	8) Caregivers
//	9) Family & Friends
	public static void browseMembers(String action) throws Throwable {
		Secure.checkAccess();
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);

		Calendar oneDayBeforeNow = Calendar.getInstance();
		oneDayBeforeNow.add(Calendar.DAY_OF_MONTH, -1);
		Set<TalkerBean> activeTalkers = ApplicationDAO.getActiveTalkers(oneDayBeforeNow.getTime());
		Set<TalkerBean> newTalkers = ApplicationDAO.getNewTalkers();
		
		String query = params.get("query");
		List<TalkerBean> results = null;
		if (query != null) {
			params.flash("query");
			
			try {
				results = SearchUtil.searchTalker(query);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		Map<String, Set<TalkerBean>> members = new LinkedHashMap<String, Set<TalkerBean>>();
		members.put("Experts", new LinkedHashSet<TalkerBean>());
		members.put("Patients", new LinkedHashSet<TalkerBean>());
		members.put("Former Patients", new LinkedHashSet<TalkerBean>());
		members.put("Parents", new LinkedHashSet<TalkerBean>());
		members.put("Caregivers", new LinkedHashSet<TalkerBean>());
		members.put("Family & Friends", new LinkedHashSet<TalkerBean>());
		
		Map<String, List<String>> memberTypes = new LinkedHashMap<String, List<String>>();
		memberTypes.put("Experts", TalkerBean.PROFESSIONAL_CONNECTIONS_LIST);
		memberTypes.put("Patients", Arrays.asList("Patient"));
		memberTypes.put("Former Patients", Arrays.asList("Former Patient"));
		memberTypes.put("Parents", Arrays.asList("Parent"));
		memberTypes.put("Caregivers", Arrays.asList("Caregiver"));
		memberTypes.put("Family & Friends", Arrays.asList("Family member", "Friend"));
		
		Set<TalkerBean> allActiveTalkers = ApplicationDAO.getActiveTalkers(null);
		for (TalkerBean talker : allActiveTalkers) {
			for (String memberType : memberTypes.keySet()) {
				if (memberTypes.get(memberType).contains(talker.getConnection())) {
					members.get(memberType).add(talker);
				}
			}
		}
		
		if (action == null) {
			action = "active";
		}
		render(currentTalker, action, activeTalkers, newTalkers, results, members);
	}

}

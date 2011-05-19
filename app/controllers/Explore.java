package controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.mongodb.DBObject;
import com.mongodb.DBRef;

import play.Logger;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

import logic.FeedsLogic;
import logic.TalkerLogic;
import logic.TopicLogic;
import models.ConversationBean;
import models.ServiceAccountBean;
import models.PrivacySetting.PrivacyType;
import models.ServiceAccountBean.ServiceType;
import models.TalkerBean;
import models.TopicBean;
import models.actions.Action;
import util.BitlyUtil;
import util.CommonUtil;
import util.DBUtil;
import util.FacebookUtil;
import util.SearchUtil;
import util.TwitterUtil;
import util.jobs.ConvoFromTwitterJob;
import util.jobs.EmailReminderJob;
import util.jobs.ThoughtsFromServicesJob;
import dao.ActionDAO;
import dao.ApplicationDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;
import dao.TopicDAO;

@With(LoggerController.class)
public class Explore extends Controller {

	
	enum  MEMBERS_TYPE {
		ACTIVE,
		NEW,
		ALL
		
	};
	static boolean nextActive=true;
	static boolean nextNew=true;
	static boolean nextAll=true;
	static List<TalkerBean> activeTalkers;
	static List<TalkerBean> newTalkers;
	static List<TalkerBean> allTalkers;
	static Iterator<List<TalkerBean>> active_it=getActiveTalkers().iterator();
	static Iterator<List<TalkerBean>> new_it=getNewTalkers().iterator();
	static Iterator<List<TalkerBean>> all_it=getAllTalkers().iterator();
	
	@Before
	static void prepareParams() {
		// used for Tw/Fb sharing and SEO
		String currentURL = "http://" + request.host + request.path;
		renderArgs.put("currentURL", currentURL);
	}

	public static void openQuestions() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		if (talker != null) {
			TalkerLogic.preloadTalkerInfo(talker);
		}

		List<TopicBean> popularTopics = TopicLogic.loadPopularTopics();
		List<ConversationBean> openQuestions = ConversationDAO
				.getOpenQuestions();

		render(talker, openQuestions, popularTopics);
	}

	public static void liveTalks() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		List<TopicBean> popularTopics = null;
		if (talker != null) {
			TalkerLogic.preloadTalkerInfo(talker);
		} else {
			popularTopics = TopicLogic.loadPopularTopics();
		}

		List<ConversationBean> liveTalks = ConversationDAO
				.getLiveConversations();
		render(talker, liveTalks, popularTopics);
	}

	public static void browseTopics() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		if (talker != null) {
			TalkerLogic.preloadTalkerInfo(talker);
		}

		List<TopicBean> popularTopics = TopicLogic.loadPopularTopics();
		Set<TopicBean> topicsTree = TopicLogic.getAllTopicsTree();

		render(topicsTree, talker, popularTopics);
	}

	public static void browseMembers(String action) throws Throwable {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);

		// Active talkers on this day
		Calendar oneWeekBeforeNow = Calendar.getInstance();
		oneWeekBeforeNow.add(Calendar.DAY_OF_MONTH, -7);
		//if(newTalkersItr==null && activeTalkersItr == null){
		
		//activeTalkersItr=getActiveTalkers().listIterator(index);
	    //newTalkersItr=getNewTalkers().listIterator(index);
		//}
		Set<TalkerBean> activeTalkers = nextTalkers(MEMBERS_TYPE.ACTIVE);


		Set<TalkerBean> newTalkers =nextTalkers(MEMBERS_TYPE.NEW);
			//ApplicationDAO.getNewTalkers();// 
		//;

		// check if search is performed now
		String query = params.get("query");
		List<TalkerBean> results = null;
		if (query != null) {
			params.flash("query");
			try {
				results = SearchUtil.searchTalker(query);
			} catch (Exception e) {
				Logger.error(e, "Talker search on Browser Members page.");
			}
		}

		// Move members to particular tabs based on member's connection
		Map<String, Set<TalkerBean>> members = new LinkedHashMap<String, Set<TalkerBean>>();
		members.put("Experts", new LinkedHashSet<TalkerBean>());
		members.put("Patients", new LinkedHashSet<TalkerBean>());
		members.put("Former Patients", new LinkedHashSet<TalkerBean>());
		members.put("Parents", new LinkedHashSet<TalkerBean>());
		members.put("Caregivers", new LinkedHashSet<TalkerBean>());
		members.put("Family & Friends", new LinkedHashSet<TalkerBean>());

		// match tabs with possible connections
		Map<String, List<String>> memberTypes = new LinkedHashMap<String, List<String>>();
		memberTypes.put("Experts", TalkerBean.PROFESSIONAL_CONNECTIONS_LIST);
		memberTypes.put("Patients",
				Arrays.asList("Just Diagnosed", "Current Patient"));
		memberTypes
				.put("Former Patients", Arrays.asList("Survivor (1 year)",
						"Survivor (2 - 5 years)", "Survivor (5 - 10 years)",
						"Survivor (10 - 20 years)",
						"Survivor (Greater than 20 years)"));
		memberTypes.put("Parents", Arrays.asList("Parent"));
		memberTypes.put("Caregivers", Arrays.asList("Caregiver"));
		memberTypes.put("Family & Friends",
				Arrays.asList("Family member", "Friend"));

		Set<TalkerBean> allActiveTalkers = nextTalkers(MEMBERS_TYPE.ALL);
			
		// re-structure members by connection type
		for (TalkerBean talker : allActiveTalkers) {
			for (Entry<String, List<String>> memberTypeEntry : memberTypes
					.entrySet()) {
				if (memberTypeEntry.getValue().contains(talker.getConnection())) {
					members.get(memberTypeEntry.getKey()).add(talker);
				}
			}
		}

		// default tab is 'active'
		if (action == null || action.equals("browsemembers")) {
			action = "active";
		}
	  String nextActiveEnabled= (nextActive==true)?"true":null;
	  String nextNewEnabled= (nextNew==true)?"true":null;
	  String nextAllEnabled= (nextAll==true)?"true":null;
	  
		render(currentTalker, action, activeTalkers, newTalkers, results,
				members,nextActiveEnabled,nextNewEnabled,nextAllEnabled);
	}
	
	private static Set<TalkerBean> nextTalkers(MEMBERS_TYPE mem_type){
		List<TalkerBean> nextTalkers=null;
		switch( mem_type){
		case ACTIVE :
			if(active_it.hasNext())
				nextTalkers=active_it.next();
			
			else{
				nextTalkers=activeTalkers;	
				nextActive=false;
				break;
			}
			activeTalkers=nextTalkers;
			if(!active_it.hasNext()){
				nextActive=false;
			}
			break;
		case NEW :
			if(new_it.hasNext())
				nextTalkers=new_it.next();
			else{
				nextTalkers=newTalkers;	
			   nextNew=false;
			   break;
		}
			newTalkers=nextTalkers;
			if(!new_it.hasNext()){
				nextNew=false;
			}
			break;
		case ALL :
			if(all_it.hasNext())
				nextTalkers=all_it.next();
			else{
				nextTalkers=allTalkers;	
			    nextAll=false;
			    break;
		}
			allTalkers=nextTalkers;
			if(!all_it.hasNext()){
				nextAll=false;
			}
			break;
			
		}
			System.out.println(nextTalkers);
			
		return new HashSet(nextTalkers);
	}
	public static List<List<TalkerBean>> getActiveTalkers() {
		// Active talkers on this day
		Calendar oneWeekBeforeNow = Calendar.getInstance();
		oneWeekBeforeNow.add(Calendar.DAY_OF_MONTH, -7);
		Set<TalkerBean> activeTalkers = ApplicationDAO
				.getActiveTalkers(oneWeekBeforeNow.getTime());
		return CommonUtil.partition(new ArrayList<TalkerBean>(activeTalkers), 12);

	}
	
	public static List<List<TalkerBean>> getAllTalkers() {
		
		Set<TalkerBean> activeTalkers = ApplicationDAO.getActiveTalkers(null);
		return CommonUtil.partition(new ArrayList<TalkerBean>(activeTalkers), 12);

	}
	
	public static List<List<TalkerBean>> getNewTalkers() {
		
		Set<TalkerBean> newTalkers = ApplicationDAO.getNewTalkers();
		List<List<TalkerBean>> res= CommonUtil.partition(new ArrayList<TalkerBean>(newTalkers), 12);
		System.out.println(res.size());
		return res;

	}
	
	public static void searchConversations() {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);

		String query = params.get("query");
		List<ConversationBean> results = null;
		if (query != null) {
			params.flash("query");
			try {
				results = SearchUtil.searchConvo(query, 10);
			} catch (Exception e) {
				Logger.error(e, "Search Conversations error");
			}
		}
		render(talker, results);
	}

	/**
	 * Page with conversations feed and popular conversations
	 * 
	 * @param action
	 *            default tab ('feed' or 'popular')
	 */
	public static void conversations(String action) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		boolean loggedIn = (talker != null);
		Set<Action> communityFeed = FeedsLogic.getCommunityFeed(null, loggedIn);

		List<TopicBean> popularTopics = null;
		if (talker == null) {
			popularTopics = TopicLogic.loadPopularTopics();
		}

		// "Popular Conversations" - ordered by page views
		List<ConversationBean> popularConvos = ConversationDAO
				.loadPopularConversations();

		if (action == null) {
			action = "feed";
		}
		render(action, communityFeed, popularConvos, popularTopics);
	}
}

package controllers;

import java.util.HashMap;
import java.util.Map;

import models.HealthItemBean;
import models.TalkerBean;
import models.TalkerDiseaseBean;
import dao.ActivityDAO;
import dao.HealthItemDAO;
import dao.TalkerDAO;
import dao.TalkerDiseaseDAO;
import dao.TopicDAO;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;

@With(Secure.class)
public class PublicProfile extends Controller {

	public static void view(String userName) {
		TalkerBean currentTalker = CommonUtil.loadCachedTalker(session);
		
		TalkerBean talker = TalkerDAO.getByUserName(userName);
		
		notFoundIfNull(talker);
		
		// Health info
		//For now we have only one disease - Breast Cancer
		final String diseaseName = "Breast Cancer";
		TalkerDiseaseBean talkerDisease = TalkerDiseaseDAO.getByTalkerId(talker.getId());
		if (talkerDisease != null) {
			talkerDisease.setName(diseaseName);
		}
		
		//Load all healthItems for this disease
		//TODO: duplicate code?
		Map<String, HealthItemBean> healthItemsMap = new HashMap<String, HealthItemBean>();
		for (String itemName : new String[] {"symptoms", "tests", 
				"procedures", "treatments", "sideeffects"}) {
			HealthItemBean healthItem = HealthItemDAO.getHealthItemByName(itemName, diseaseName);
			healthItemsMap.put(itemName, healthItem);
		}
		
		talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
		talker.setActivityList(ActivityDAO.load(talker.getId()));
		talker.setProfileCommentsList(TalkerDAO.loadProfileComments(talker.getId()));
		//TODO: Temporarily - later we'll load all list of topics
		talker.setNumberOfTopics(TopicDAO.getNumberOfTopics(talker.getId()));
		
		render(talker, talkerDisease, healthItemsMap, currentTalker);
	}
	
	public static void thankYous(String userName) {
		TalkerBean talker = TalkerDAO.getByUserName(userName);
		
		notFoundIfNull(talker);
		
		render(talker);
	}
}

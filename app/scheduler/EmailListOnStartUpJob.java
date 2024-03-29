package scheduler;

import static util.DBUtil.getCollection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import models.EmailListBean;
import models.NewsLetterBean;
import models.TalkerBean;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import dao.TalkerDAO;
import play.jobs.*;
import util.EmailUtil;

/** Fire on application start up **/ 
@OnApplicationStart
public class EmailListOnStartUpJob extends Job {

	public void doJob() {
		System.out.println("Job : Email list population one time on sailthrou");
		//runEmailJob();
		runDiseaseJob();
	}

	private boolean runDiseaseJob() {
		List<TalkerBean> talkerBeans = TalkerDAO.loadAllTalkers();
		for (Iterator<TalkerBean> iterator = talkerBeans.iterator(); iterator.hasNext();) {
			TalkerBean talkerBean = (TalkerBean) iterator.next();
			Map<String, Boolean> lists = new HashMap<String, Boolean>();
			if(StringUtils.isNotBlank(talkerBean.getCategory())){
				lists.put(talkerBean.getCategory().replaceAll(" ", "-"), true);
			}
			if(talkerBean.getOtherCategories() != null) {
	    		for (int i = 0; i < talkerBean.getOtherCategories().length; i++) {
	    			lists.put(talkerBean.getOtherCategories()[i].replaceAll(" ", "-"), true);
	    		}
	    	}
			if(!lists.isEmpty()){
				EmailUtil.setEmail(lists, talkerBean.getEmail());
			}
			lists.clear();
		}
		return true;
	}
	
	private boolean runEmailJob(){
		String NEWSLETTER_COLLECTION = "newsletter";
		DBCollection newsLetterCol = getCollection(NEWSLETTER_COLLECTION);
		List<DBObject> newsletterDBList = null;
		newsletterDBList = newsLetterCol.find().toArray();
		ArrayList<EmailListBean> newsLetterList = new ArrayList<EmailListBean>();
		EmailListBean  emailListBean;
		NewsLetterBean newsLetterBean;
		int counterOnlyNewsletter = 0;
		int counterTalker = 0;
		for (DBObject newsletterDBObject : newsletterDBList) {
			newsLetterBean = new NewsLetterBean();
			newsLetterBean.parseBasicFromDB(newsletterDBObject);
			emailListBean = new EmailListBean("TAH-Newsletter",newsLetterBean.getEmail());
			newsLetterList.add(emailListBean);
			counterOnlyNewsletter++;
			String[] subNewsLetters = newsLetterBean.getNewsLetterType();
			if(subNewsLetters != null && subNewsLetters.length > 0) {
				for(int index = 0 ; index < subNewsLetters.length; index++) {
					String newsLetterType = subNewsLetters[index];
					if(StringUtils.isNotBlank(newsLetterType) && newsLetterType.equalsIgnoreCase("workshop")) {
						newsLetterType = "TAH Workshop Notification";
					} else if(StringUtils.isNotBlank(newsLetterType) && newsLetterType.equalsIgnoreCase("Workshop summery")) {
						newsLetterType = "TAH Workshop Summary";
					}
					newsLetterType = newsLetterType.replaceAll(",", "");
					newsLetterType = newsLetterType.replaceAll(" ", "-");
					emailListBean = new EmailListBean(newsLetterType,newsLetterBean.getEmail());
					newsLetterList.add(emailListBean);
				}
			} else {
				Map<String, Boolean> lists = new HashMap<String, Boolean>();
				lists.put("Best-of-TalkAboutHealth", true);
				lists.put("TalkAboutHealth-Rewards", true);
				lists.put("TAH-Workshop-Summary", true);
				lists.put("TAH-Workshop-Notification", true);
				EmailUtil.setEmail(lists, newsLetterBean.getEmail());
			}
		}
		EmailUtil.setEmail(newsLetterList);
		
		List<TalkerBean> talkerBeans = TalkerDAO.loadAllTalkers();
		for (Iterator<TalkerBean> iterator = talkerBeans.iterator(); iterator.hasNext();) {
			counterTalker++;
			TalkerBean talkerBean = (TalkerBean) iterator.next();
			/*
			  	- Daily Workshop Announcement Update
				- Daily Q&A workshop summary
				- TAH Benefits
				- Best of TAH
			*/
			Map<String, Boolean> lists = new HashMap<String, Boolean>();
			lists.put("Best-of-TalkAboutHealth", true);
			lists.put("TalkAboutHealth-Rewards", true);
			lists.put("TAH-Workshop-Summary", true);
			lists.put("TAH-Workshop-Notification", true);
			EmailUtil.setEmail(lists, talkerBean.getEmail());
		}
		System.out.println("Only Newsletter : " + counterOnlyNewsletter);
		System.out.println("Talker Newsletter : " + counterTalker);
		return true;
	}
}
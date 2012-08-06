package scheduler;

import static util.DBUtil.getCollection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
		runEmailJob();
	}

	private boolean runEmailJob(){
		String NEWSLETTER_COLLECTION = "newsletter";
		DBCollection newsLetterCol = getCollection(NEWSLETTER_COLLECTION);
		List<DBObject> newsletterDBList = null;
		newsletterDBList = newsLetterCol.find().toArray();
		ArrayList<EmailListBean> newsLetterList = new ArrayList<EmailListBean>();
		EmailListBean  emailListBean;
		NewsLetterBean newsLetterBean;
		for (DBObject newsletterDBObject : newsletterDBList) {
			newsLetterBean = new NewsLetterBean();
			newsLetterBean.parseBasicFromDB(newsletterDBObject);
			emailListBean = new EmailListBean("TAH-Newsletter",newsLetterBean.getEmail());
			newsLetterList.add(emailListBean);
			
			String[] subNewsLetters = newsLetterBean.getNewsLetterType();
			if(subNewsLetters != null && subNewsLetters.length > 0){
				for(int index = 0 ; index < subNewsLetters.length; index++){
					String newsLetterType = subNewsLetters[index];
					newsLetterType = newsLetterType.replaceAll(",", "");
					newsLetterType = newsLetterType.replaceAll(" ", "-");
					emailListBean = new EmailListBean(newsLetterType,newsLetterBean.getEmail());
					newsLetterList.add(emailListBean);
				}
			}
		}

		List<TalkerBean> talkerBeans = TalkerDAO.loadAllTalkers();
		for (Iterator<TalkerBean> iterator = talkerBeans.iterator(); iterator.hasNext();) {
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
		return true;
	}
}
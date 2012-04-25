package scheduler;

import static util.DBUtil.getCollection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import models.EmailListBean;
import models.NewsLetterBean;
import models.TalkerBean;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import dao.TalkerDAO;
import play.jobs.*;
import util.EmailUtil;

/** Fire at 12pm (noon) every day **/ 
//@On("0 * 14 * * ?")
//@Every("1m")]

//@OnApplicationStart
@Every("12h")
public class EmailListJob extends Job {

	public void doJob() {
		System.out.println("Job : Email list population on sailthrou");
		runEmailJob();
	}

	private boolean runEmailJob(){
		/*
		- TAH Newsletter
		- TAH Member Verified Emails
		- TAH Member Unverified Emails
		- Patients - not done
		- Experts - not done
		- Organizations - not done
		 * */
		//System.out.println("Job : Email list population on sailthrou");
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
			//System.out.println("Job : TAH Newsletter : " + newsLetterBean.getEmail());
			newsLetterList.add(emailListBean);
			
			//add all subscribed newsletters in the email sending list.
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
		
		List<String> pationtList = Arrays.asList("Just Diagnosed","Current Patient","Former Patients");
		List<String> expertList = TalkerBean.PROFESSIONAL_CONNECTIONS_LIST;
		List<String> orgList = TalkerBean.ORGANIZATIONS_CONNECTIONS_LIST;
		
		List<TalkerBean> talkerBeans = TalkerDAO.loadAllTalkers();
		for (Iterator<TalkerBean> iterator = talkerBeans.iterator(); iterator.hasNext();) {
			TalkerBean talkerBean = (TalkerBean) iterator.next();
			if (talkerBean.getVerifyCode() != null) {
				emailListBean = new EmailListBean("TAH-Member-Unverified-Emails",talkerBean.getEmail());
				//System.out.println("Job : TAH Member Unverified Emails : " + talkerBean.getEmail());
				newsLetterList.add(emailListBean);
			} else {
				emailListBean = new EmailListBean("TAH-Member-Verified-Emails",talkerBean.getEmail());
				//System.out.println("Job : TAH Member Verified Emails : " + talkerBean.getEmail());
				newsLetterList.add(emailListBean);
			}
			
			if(pationtList.contains(talkerBean.getConnection())){
				emailListBean = new EmailListBean("Patients",talkerBean.getEmail());
				//System.out.println("Job : Patients : " + talkerBean.getEmail());
				newsLetterList.add(emailListBean);
			}
			
			if(expertList.contains(talkerBean.getConnection())){
				emailListBean = new EmailListBean("Experts",talkerBean.getEmail());
				//System.out.println("Job : Experts : " + talkerBean.getEmail());
				newsLetterList.add(emailListBean);
			}
			
			if(orgList.contains(talkerBean.getConnection())){
				emailListBean = new EmailListBean("Organizations",talkerBean.getEmail());
				//System.out.println("Job : Organizations : " + talkerBean.getEmail());
				newsLetterList.add(emailListBean);
			}
		}
		return EmailUtil.setEmail(newsLetterList);
		//return true;
	}
}
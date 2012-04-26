package util.jobs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import play.jobs.Every;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

import logic.TalkerLogic;
import models.CommentBean;
import models.ConversationBean;
import models.ServiceAccountBean;
import models.ServiceAccountBean.ServiceType;
import models.TalkerBean;
import models.ServicePost;
import util.CommonUtil;
import util.FacebookUtil;
import util.TwitterUtil;
import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;

/**
 * Imports new thoughts from Twitter/Facebook for users with checked "SHARE_TO_THOUGHTS" option.
 *
 */
@OnApplicationStart
public class ThoughtsFromServicesJob{
	
	public void doJob() throws Exception{
		System.out.println("----------ThoughtsFromServicesJob Started------------"+ new Date());
		List<TalkerBean> talkerList = TalkerDAO.loadAllActiveTalker(true);
		List<ServicePost> postsList = new ArrayList<ServicePost>();
		CommentBean thought;
		boolean isDuplicate;
		String lastPostId;
		for (TalkerBean talker : talkerList) {
			if (talker.isSuspended() || talker.isDeactivated()) {
				continue;
			}
			
			for (ServiceAccountBean serviceAccount : talker.getServiceAccounts()) {
				if (!serviceAccount.isTrue("SHARE_TO_THOUGHTS")) {
					continue;
				}

				if (serviceAccount.getType() == ServiceType.TWITTER) {
			    	postsList = TwitterUtil.importTweets(serviceAccount, serviceAccount.getLastPostId());
				}
				else if (serviceAccount.getType() == ServiceType.FACEBOOK) {
					postsList = FacebookUtil.importPosts(serviceAccount, serviceAccount.getLastPostId());
				}
				
				if (postsList.size() > 0) {
		    		Collections.sort(postsList);
			    	for (ServicePost post : postsList) {
			    		//check if we've already imported it
			    		thought = CommentsDAO.getThoughtByFromInfo(serviceAccount.getType().toString(), post.getId());
			    		
			    		// check duplicates by full-text + sender + some-days-back, and reject if found 
			    		isDuplicate = CommentsDAO.getThoughtDuplicates(talker.getId(),post.getText(),3);
			    		
			    		if (thought == null && !isDuplicate) {
			    			//Now we do no store html tags in the db
			    			//String htmlText = prepareText(serviceAccount.getType(), post.getText());
			    			//String htmlText = post.getText();
			    			TalkerLogic.saveProfileComment(talker, talker.getId(), null, 
			    					post.getText(), post.getText(), 
									serviceAccount.getType().toString(), post.getId(), null, null,null);
			    		}
					}
			    	
			    	lastPostId = postsList.get(postsList.size()-1).getId();
			    	serviceAccount.setLastPostId(lastPostId);
			    	TalkerDAO.updateTalker(talker);
		    	}
			}
		}
		System.out.println("----------ThoughtsFromServicesJob Completed------------"+ new Date());
		ThoughtsFromServicesJob thoughtJob = new ThoughtsFromServicesJob();
		try {
			thoughtJob.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	protected void finalize() throws Throwable {
		super.finalize();
	}
}

package util.jobs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import play.jobs.Every;
import play.jobs.Job;

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
@Every("10min")
public class ThoughtsFromServicesJob extends Job {
	
	@Override
	public void doJob() throws Exception {
		for (TalkerBean talker : TalkerDAO.loadAllTalkers()) {
			if (talker.isSuspended() || talker.isDeactivated()) {
				continue;
			}
			
			for (ServiceAccountBean serviceAccount : talker.getServiceAccounts()) {
				if (!serviceAccount.isTrue("SHARE_TO_THOUGHTS")) {
					continue;
				}
				
				List<ServicePost> postsList = new ArrayList<ServicePost>();
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
			    		CommentBean thought = 
			    			CommentsDAO.getThoughtByFromInfo(serviceAccount.getType().toString(), post.getId());
			    		
			    		// check duplicates by full-text + sender + some-days-back, and reject if found 
			    		boolean isDuplicate = CommentsDAO.getThoughtDuplicates(talker.getId(),post.getText(),3);
			    		
			    		if (thought == null && !isDuplicate) {
			    			//Now we do no store html tags in the db
//			    			String htmlText = prepareText(serviceAccount.getType(), post.getText());
			    			String htmlText = post.getText();
			    			TalkerLogic.saveProfileComment(talker, talker.getId(), null, 
									htmlText, post.getText(), 
									serviceAccount.getType().toString(), post.getId(), null, null,null);
			    		}
					}
			    	
			    	String lastPostId = postsList.get(postsList.size()-1).getId();
			    	serviceAccount.setLastPostId(lastPostId);
			    	TalkerDAO.updateTalker(talker);
		    	}
			}
		}
	}
}

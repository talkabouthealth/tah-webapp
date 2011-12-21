package util.jobs;

import java.util.List;

import play.Logger;
import play.jobs.Every;
import play.jobs.Job;

import dao.ConversationDAO;
import dao.TalkerDAO;

import logic.ConversationLogic;
import models.ConversationBean;
import models.ConversationBean.ConvoType;
import models.ServiceAccountBean.ServiceType;
import models.TalkerBean;
import models.ServicePost;

import util.TwitterUtil;

/**
 * Checks Twitter every minute for possible convo creations (tweets with 'talkabouthealth' and '?')
 *
 */
@Every("3min")
public class ConvoFromTwitterJob extends Job {
	
	@Override
	public void doJob() throws Exception {
		//get 10 last tweets with '@talkabouthealth'
		List<ServicePost> mentionTweets = TwitterUtil.loadMentions();
		for (ServicePost tweet : mentionTweets) {
			String text = tweet.getText().trim();
			if ( (text.endsWith("@talkabouthealth") || text.endsWith("@TalkAboutHealth"))
					&& text.contains("?") && !text.contains("RT")) {
				//check if we've already created this convo
				ConversationBean convo = ConversationDAO.getByFromInfo("twitter", tweet.getId());
				if (convo == null) {
					createConvoFromTweet(tweet);
				}
			}
		}
	}

	/**
	 * Creates conversation from given tweet and notifies user with DM.
	 */
	private void createConvoFromTweet(ServicePost tweet) {
		//check if creator is TAH user
		TalkerBean talker = TalkerDAO.getByAccount(ServiceType.TWITTER, tweet.getUserId());
		if (talker == null) {
			return;
		}
		
		String title = tweet.getText();
		title = title.replaceAll("@talkabouthealth", "").replaceAll("@TalkAboutHealth", "").trim();
		
		ConversationBean convo = 
			ConversationLogic.createConvo(ConvoType.QUESTION, title, talker, null, null, true, null, null, null, null);
		convo.setFrom("twitter");
		convo.setFromId(tweet.getId());
		ConversationDAO.updateConvo(convo);
		
		//Notify user, sent DM:
		//Thanks for your question. We will notify you via Twitter and email when you receive answers. 
		//View answers at: http://bit.ly/lksa
		StringBuilder dmText = new StringBuilder();
		dmText.append("Thanks for your question. We will notify you via Twitter and email when you receive answers. ");
		dmText.append("\nView answers at: ");
		dmText.append(convo.getBitly());
		TwitterUtil.sendDirect(tweet.getUserId(), dmText.toString());
	}

}

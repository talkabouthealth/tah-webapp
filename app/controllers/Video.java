package controllers;

import models.ConversationBean;
import models.NewsLetterBean;
import models.TalkerBean;
import models.VideoBean;
import play.data.validation.Email;
import play.data.validation.Error;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;
import dao.ConversationDAO;
import dao.NewsLetterDAO;
import dao.TalkerDAO;
import dao.VideoDAO;

@With( LoggerController.class )
public class Video extends Controller {

	@Check("admin")
	public static void addNewVideo(String videoId, String talkerId, String convoId) {
		String message = "Added video successfully.Please refresh page";
		boolean returnPath = false;
		ConversationBean convoBean = null;
		if(videoId == null)
			message = "Error:Please provide value for video";
		else if(videoId.trim().equals("")) 
			message = "Error:Please provide value for video";
		else {
			VideoBean videoBean = new VideoBean();
			videoBean.setVideoId(videoId);
			/*if(talkerId != null && !talkerId.trim().equals("")){
				TalkerBean talkerBean = TalkerDAO.getById(talkerId);
				videoBean.setTalkerBean(talkerBean);
			}*/
			convoBean = ConversationDAO.getConvoById(convoId);
			videoBean.setConvoBean(convoBean);
			
			videoBean.setTopics(convoBean.getTopics());

			if(VideoDAO.save(videoBean))
				returnPath = true;	
			
		}
		System.out.println("videoId :" + videoId);
		System.out.println("talkerId :" + talkerId);
		System.out.println("convoId :" + convoId);
		if(returnPath){
			renderText(message);
		}else
			renderText(message);
    }
	
	public static void topicVideo(String name){
		render();
	}
}
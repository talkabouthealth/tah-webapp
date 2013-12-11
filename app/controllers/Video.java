package controllers;

import org.apache.commons.lang.StringUtils;

import antlr.collections.List;
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

@Check("admin")
@With(Secure.class)
public class Video extends Controller {

	@Check("admin")
	public static void homePageVideo() {
		java.util.List<VideoBean> list = VideoDAO.loadHomeVideo("All Cancers");
		java.util.List<VideoBean> listBC = VideoDAO.loadHomeVideo("Breast Cancer");
		render("Dashboard/homeVideo.html",list,listBC);
	}

	public static void updateAll() {
		List<VideoBean> vList = VideoDAO.loadAllVideo();
		for (VideoBean videoBean : vList) {
			System.out.println("D: " + videoBean.getVideoId());
			VideoDAO.saveOrUpdate(videoBean);
		}
		homePageVideo();
	}
	
	public static void removeHomeVideo(String videoId,String cancerType) {
		VideoDAO.removeHomeVideo(videoId,cancerType);
		homePageVideo();
	}
	
	public static void addHomeVideo(String videoId, String videoTitle, String videoLink, String cancerType) {
		System.out.println(videoId);
		System.out.println(videoTitle);
		System.out.println(videoLink);
		System.out.println(cancerType);
		if(videoId != null) {
			VideoDAO.addHomeVideo(videoId,videoTitle,videoLink,cancerType);
		}
		homePageVideo();
	}

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
			/*if(StringUtils.isNotBlank(videoTitle)) {
				videoBean.setVideoTitle(videoTitle);
			}
			videoBean.setHomeVideoFlag(homeVideoFlag);
			*/
			 
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
		if(returnPath) {
			renderText(message);
		} else
			renderText(message);
    }

	public static void deleteVideo(String videoId) {
		/* 
		if(Security.isConnected()){
			TalkerBean talker =  CommonUtil.loadCachedTalker(session);
			if(!talker.isAdmin())
				notFound();
		}else
			notFound();
		*/
		if(VideoDAO.deleteVideo(videoId))
			renderText("Deleted video successfully.Please refresh page");
		else
			renderText("Internal error: Please try again");
	}
}
package controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import logic.TopicLogic;
import models.CommentBean;
import models.TalkerBean;
import models.ConversationBean;
import models.TalkerTopicInfo;
import models.URLName;
import models.TalkerBean.EmailSetting;
import models.TopicBean;
import models.actions.Action;
import models.actions.FollowConvoAction;
import models.actions.StartConvoAction;
import play.cache.Cache;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;
import play.templates.JavaExtensions;
import util.CommonUtil;
import util.DBUtil;
import util.EmailUtil;
import util.EmailUtil.EmailTemplate;
import util.NotificationUtils;
import dao.ActionDAO;
import dao.ApplicationDAO;
import dao.CommentsDAO;
import dao.TalkerDAO;
import dao.ConversationDAO;
import dao.TopicDAO;

@With(Secure.class)
public class Topics extends Controller {
	
	//follow or unfollow topic
    public static void follow(String topicId) {
    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	TopicBean topic = new TopicBean(topicId);
    	
    	String nextAction = null;
    	if (talker.getFollowingTopicsList().contains(topic)) {
    		//unfollow action
    		talker.getFollowingTopicsList().remove(topic);
    		nextAction = "follow";
    	}
    	else {
    		talker.getFollowingTopicsList().add(topic);
    		nextAction = "unfollow";
    	}
    	
    	CommonUtil.updateTalker(talker, session);
    	renderText(nextAction);
    }
    
    public static void manage(String name) {
    	TopicBean topic = TopicDAO.getByURL(name);
    	if (topic != null) {
			if (!topic.getMainURL().equals(name)) {
				//we come here by old url - redirect to main
				redirect("/"+topic.getMainURL()+"/manage");
			}
    	}
    	notFoundIfNull(topic);

    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	System.out.println(talker.getHiddenHelps());
    	
		render(talker, topic);
    }
    
    public static void updateField(String topicId, String name, String value) {
    	TopicBean topic = TopicDAO.getById(topicId);
    	notFoundIfNull(topic);
    	
    	if (name.equalsIgnoreCase("title")) {
    		if (topic.isFixed() && !Security.connected().equalsIgnoreCase("admin")) {
    			forbidden();
    			return;
    		}
    		URLName currentName = new URLName(topic.getTitle(), topic.getMainURL());
    		URLName newName = new URLName(value, null);
    		
    		//find old name with the same title
    		URLName oldName = topic.getOldNameByTitle(newName.getTitle());
    		if (oldName != null) {
    			//topic has already had this title, return it to main title/url
    			topic.setTitle(oldName.getTitle());
    			topic.setMainURL(oldName.getUrl());
    			topic.getOldNames().remove(oldName);
    		}
    		else {
    			//new title for this topic - create it
    			String newURL = ApplicationDAO.createURLName(newName.getTitle());
    			topic.setTitle(newName.getTitle());
    			topic.setMainURL(newURL);
    		}
    		topic.getOldNames().add(currentName);
    		TopicDAO.updateTopic(topic);
    	}
    	else if (name.equalsIgnoreCase("summary")) {
    		topic.setSummary(value);
    		TopicDAO.updateTopic(topic);
    	}
    	else if (name.equalsIgnoreCase("freeze")) {
    		TalkerBean talker = CommonUtil.loadCachedTalker(session);
    		if (!talker.getUserName().equalsIgnoreCase("admin")) {
    			forbidden();
    		}
    		
    		boolean newValue = Boolean.parseBoolean(value);
    		topic.setFixed(newValue);
    		TopicDAO.updateTopic(topic);
    	}
    }
    
    public static void delete(String topicId) {
    	TopicBean topic = TopicDAO.getById(topicId);
    	notFoundIfNull(topic);

    	topic.setDeleted(true);
    	TopicDAO.updateTopic(topic);
    	
    	//remove from parent topic
    	if (!topic.getParents().isEmpty()) {
    		TopicBean oldParent = topic.getParents().iterator().next();
    		TopicBean oldParentFull = TopicDAO.getById(oldParent.getId());
    		oldParentFull.getChildren().remove(topic);
    		TopicDAO.updateTopic(oldParentFull);
    	}
    }
    
    public static void manageAliases(String topicId, String todo, String alias) {
    	TopicBean topic = TopicDAO.getById(topicId);
    	notFoundIfNull(topic);

    	if (todo.equalsIgnoreCase("add")) {
    		topic.getAliases().add(alias);
    	}
    	else {
    		topic.getAliases().remove(alias);
    	}
    	TopicDAO.updateTopic(topic);
    	
    	renderText("<div class='topictxt1'>"+alias+"&nbsp;&nbsp;<a href='#' rel='"+
    			alias+"' class='removeAliasLink style2'>remove</a></div>");
    }
    
    public static void manageParents(String topicId, String todo, String parentId) {
    	TopicBean topic = TopicDAO.getById(topicId);
    	notFoundIfNull(topic);
    	if (topic.isFixed()) {
    		forbidden();
    		return;
    	}
    	
    	TopicBean parentTopic = null;
    	if (todo.equalsIgnoreCase("add")) {
    		parentId = JavaExtensions.capitalizeWords(parentId);
    		//in order to be a Parent topic, the topic must exist.
    		parentTopic = TopicDAO.getOrRestoreByTitle(parentId);
    		if (parentTopic == null || parentTopic.equals(topic)) {
    			forbidden();
        		return;
    		}
        	parentTopic.getChildren().add(topic);
        	
        	//remove topic from previous parent
        	if (!topic.getParents().isEmpty()) {
        		TopicBean oldParent = topic.getParents().iterator().next();
        		TopicBean oldParentFull = TopicDAO.getById(oldParent.getId());
        		oldParentFull.getChildren().remove(topic);
        		TopicDAO.updateTopic(oldParentFull);
        	}
        	TopicDAO.updateTopic(parentTopic);
    	}
    	else {
    		parentTopic = TopicDAO.getById(parentId);
        	notFoundIfNull(parentTopic);
        	
    		parentTopic.getChildren().remove(topic);
    		TopicDAO.updateTopic(parentTopic);
    		
    		TopicLogic.addToDefaultParent(topic);
    	}
    	
    	renderText("<div class='topictxtz'>"+parentTopic.getTitle()+"&nbsp;&nbsp;<a href='#' rel='"+
    			parentTopic.getId()+"' class='removeParentLink style2'>remove</a></div>");
    }
    
    public static void manageChildren(String topicId, String todo, String childId) {
    	TopicBean topic = TopicDAO.getById(topicId);
    	notFoundIfNull(topic);
    	
    	TopicBean childTopic = null;
    	if (todo.equalsIgnoreCase("add")) {
    		childId = JavaExtensions.capitalizeWords(childId);
    		childTopic = TopicDAO.getOrRestoreByTitle(childId);
    		if (childTopic == null || childTopic.equals(topic)) {
    			forbidden();
        		return;
    		}
    		
    		//check if child topic already has a parent
    		if (childTopic.getParents() != null && childTopic.getParents().size() > 0) {
    			TopicBean oldParent = childTopic.getParents().iterator().next();
    			if (oldParent.getTitle().equals(TopicLogic.DEFAULT_TOPIC)) {
    				TopicBean oldParentFull = TopicDAO.getById(oldParent.getId());
            		oldParentFull.getChildren().remove(childTopic);
            		TopicDAO.updateTopic(oldParentFull);
    			}
    			else {
    				renderText("Sorry, this topic cannot be a sub-topic. It already has a parent topic.");
        			return;
    			}
    			
    			topic.getChildren().add(childTopic);
    		}
    		else {
    			//top level topic (should be without parents)
    			renderText("Sorry, this topic cannot be a sub-topic.");
    		}
    	}
    	else {
    		if (topic.isFixed()) {
        		forbidden();
        		return;
        	}
    		childTopic = TopicDAO.getById(childId);
        	notFoundIfNull(childTopic);
        	
    		topic.getChildren().remove(childTopic);
    	}
    	TopicDAO.updateTopic(topic);
    	
    	StringBuilder htmlResponse = new StringBuilder();
    	htmlResponse.append("<div class='topictxtz'>");
    	htmlResponse.append("<a href='/"+childTopic.getMainURL()+"'>"+childTopic.getTitle()+"</a>&nbsp;");
    	if (!topic.isFixed()) {
    		htmlResponse.append("<a href='#' rel='"+
    			childTopic.getId()+"' class='removeChildLink style2'>remove</a>");
    	}
    	htmlResponse.append("</div>");
    	
    	renderText(htmlResponse.toString());
    }
    
	public static void updateTopicExperience(String topicId, String newValue) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		TopicBean topic = TopicDAO.getById(topicId);
		
		if (topic == null) {
			notFound();
			return;
		}
		
		TalkerTopicInfo talkerTopicInfo = talker.getTopicsInfoMap().get(topic);
		if (talkerTopicInfo == null) {
			talkerTopicInfo = new TalkerTopicInfo();
			talker.getTopicsInfoMap().put(topic, talkerTopicInfo);
		}
		talkerTopicInfo.setExperience(newValue);
		TalkerDAO.updateTalker(talker);
	}
	
	public static void endorse(String topicId, String toTalker) {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		TalkerBean toTalkerBean = TalkerDAO.getById(toTalker);
		TopicBean topic = TopicDAO.getById(topicId);
		
		if (topic == null || toTalkerBean == null) {
			notFound();
			return;
		}
		
		if (talker.equals(toTalkerBean)) {
			forbidden();
			return;
		}
		
		TalkerTopicInfo talkerTopicInfo = toTalkerBean.getTopicsInfoMap().get(topic);
		if (talkerTopicInfo == null) {
			talkerTopicInfo = new TalkerTopicInfo();
			toTalkerBean.getTopicsInfoMap().put(topic, talkerTopicInfo);
		}
		talkerTopicInfo.getEndorsements().add(talker);
		TalkerDAO.updateTalker(toTalkerBean);
	}
    
}

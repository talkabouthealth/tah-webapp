package controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logic.TopicLogic;
import models.CommentBean;
import models.TalkerBean;
import models.ConversationBean;
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
import webapp.LiveConversationsSingleton;
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
    	
    	if (talker.getFollowingTopicsList().contains(topic)) {
    		//unfollow action
    		talker.getFollowingTopicsList().remove(topic);
    	}
    	else {
    		talker.getFollowingTopicsList().add(topic);
    	}
    	
    	CommonUtil.updateTalker(talker, session);
    	renderText("Ok!");
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
    	
    	List<TopicBean> topicsList = TopicDAO.getTopics();
    	//we can't add this topic as parent/children to itself
    	topicsList.remove(topic);
		
		render(talker, topic, topicsList);
    }
    
    public static void updateField(String topicId, String name, String value) {
    	//TODO: list of allowed fields?
    	//TODO: hide edits from not-logined users
    	
//    	TalkerBean talker = CommonUtil.loadCachedTalker(session);
    	TopicBean topic = TopicDAO.getById(topicId);
    	notFoundIfNull(topic);
    	
    	if (name.equalsIgnoreCase("title")) {
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
    }
    
    public static void delete(String topicId) {
    	TopicBean topic = TopicDAO.getById(topicId);
    	notFoundIfNull(topic);

    	topic.setDeleted(true);
    	TopicDAO.updateTopic(topic);
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
    	
    	renderText("<li>"+alias+"&nbsp;<a href='#' rel='"+alias+"' class='removeAliasLink'>remove</a></li>");
    }
    
    //TODO: check valid add/remove? (Topic can't be children and parent at the same time)
    public static void manageParents(String topicId, String todo, String parentId) {
    	TopicBean topic = new TopicBean(topicId);
    	
    	TopicBean parentTopic = null;
    	if (todo.equalsIgnoreCase("add")) {
    		parentTopic = TopicLogic.findOrCreateTopic(parentId);
        	parentTopic.getChildren().add(topic);
    	}
    	else {
    		parentTopic = TopicDAO.getById(parentId);
        	notFoundIfNull(parentTopic);
        	
    		parentTopic.getChildren().remove(topic);
    	}
    	TopicDAO.updateTopic(parentTopic);
    	
    	renderText("<li>"+parentTopic.getTitle()+"&nbsp;<a href='#' rel='"+
    			parentTopic.getId()+"' class='removeParentLink'>remove</a></li>");
    }
    
    public static void manageChildren(String topicId, String todo, String childId) {
    	TopicBean topic = TopicDAO.getById(topicId);
    	notFoundIfNull(topic);
    	
    	TopicBean childTopic = null;
    	if (todo.equalsIgnoreCase("add")) {
    		childTopic = TopicLogic.findOrCreateTopic(childId);
    		topic.getChildren().add(childTopic);
    	}
    	else {
    		childTopic = TopicDAO.getById(childId);
        	notFoundIfNull(childTopic);
        	
    		topic.getChildren().remove(childTopic);
    	}
    	TopicDAO.updateTopic(topic);
    	
    	renderText("<li>"+childTopic.getTitle()+"&nbsp;<a href='#' rel='"+
    			childTopic.getId()+"' class='removeChildrenLink'>remove</a></li>");
    }
    
}

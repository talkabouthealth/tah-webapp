package controllers;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logic.ConversationLogic;
import logic.TopicLogic;
import models.ConversationBean;
import models.DiseaseBean;
import models.MessageBean;
import models.TalkerBean;
import models.TopicBean;
import models.actions.Action;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import play.mvc.Controller;
import util.CommonUtil;
import util.SearchUtil;
import dao.ConversationDAO;
import dao.MessagingDAO;
import dao.DiseaseDAO;
import dao.TalkerDAO;
import dao.TopicDAO;

public class Search extends Controller {
	
	/**
	 * Back-end for header autocomplete
	 * @param term String entered by user
	 */
	public static void ajaxSearch(String term) throws Exception {
		
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		List<String> allowedTypes = Arrays.asList("User", "Conversation", "Question", "Topic");
		List<Map<String, String>> results = makeSearch(term, allowedTypes, null);
		
		//add result entry which leads to the full search
		Map<String, String> result = new HashMap<String, String>();
		result.put("label", "Search for <b>"+term+"</b>");
		result.put("value", term);
		result.put("url", "#fullsearch");
		result.put("type", "");
		results.add(result);
		
		renderJSON(results);
	}
	
	/**
	 * Back-end for conversation autocomplete
	 * @param term
	 */
	public static void ajaxConvoSearch(String term) throws Exception {
		List<String> allowedTypes = Arrays.asList("Conversation", "Question");
		List<Map<String, String>> results = makeSearch(term, allowedTypes, null);
		
		renderJSON(results);
	}
	
	/**
	 * Back-end for topic autocomplete
	 * @param term
	 * @param parent If not null - search topic only in children of the 'parent'
	 */
	public static void ajaxTopicSearch(String term, String parent) throws Exception {
		List<String> allowedTypes = Arrays.asList("Topic");
		
		List<String> filterIds = null;
		if (parent != null) {
			TopicBean parentTopic = TopicDAO.getOrRestoreByTitle(parent);
			filterIds = TopicLogic.getSubTopics(parentTopic);
		}
		List<Map<String, String>> results = makeSearch(term, allowedTypes, filterIds);
		
		renderJSON(results);
	}
	
	/**
	 * Back-end for user autocomplete
	 * @param term
	 */
	public static void ajaxUserSearch(String term) throws Exception {
		
		List<String> allowedTypes = Arrays.asList("User");
		List<Map<String, String>> results = null;
		results = makeSearch(term, allowedTypes, null);
		renderJSON(results);
	}
	
	/**
	 * Back-end for disease auto-complete
	 * @param term
	 * @param convoId
	 */
	public static void ajaxDiseaseSearch(String term, String convoId) throws Exception {
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		//ConversationBean convo = ConversationDAO.getConvoById(convoId);
		String[] diseaseArr = new String[14];
		
		diseaseArr = talker.getOtherCategories();
		
		List<String> diseaseList = new ArrayList<String>();
		if(diseaseArr != null){
			for(int index = 0; index < diseaseArr.length; index++){
				if(term == null)
					diseaseList.add(diseaseArr[index]);
				else
					if(diseaseArr[index].toLowerCase().contains(term))
						diseaseList.add(diseaseArr[index]);
			}
		}
		
		//Add talker category in list. If user is admin user then add the convo talkers category
		if(term == null){
			diseaseList.add(talker.getCategory());
		}else{
			if(talker.getCategory().toLowerCase().contains(term))
			    diseaseList.add(talker.getCategory());
		}
		
		//Add All Cancer in category
		if(term == null)
			diseaseList.add(ConversationBean.ALL_CANCERS);
		else{
			if(ConversationBean.ALL_CANCERS.toLowerCase().contains(term))
				diseaseList.add(ConversationBean.ALL_CANCERS);
		}
		
		renderJSON(diseaseList);
	}
     
	 /**
	 * Back-end for message autocomplete
	 * @param term
	 */
	public static void ajaxMessageSearch(String term) throws Exception {
		
		List<String> allowedTypes = Arrays.asList("Message");
		List<Map<String, String>> results = null;
		results = makeSearch(term, allowedTypes, null);
		renderJSON(results);
	}
	
	/**
	 * Performs search in 'autocomplete' index
	 * @param term
	 * @param allowedTypes Types of items to search, e.g. 'User', 'Conversation', 'Topic', etc.
	 * @param filterIds Return only results with these ids
	 * @return
	 * @throws Exception
	 */
	private static List<Map<String, String>> makeSearch(String term, 
			List<String> allowedTypes, List<String> filterIds) throws Exception {
		IndexSearcher is = new IndexSearcher(SearchUtil.SEARCH_INDEX_PATH+"autocomplete");
				
		Analyzer analyzer = new StandardAnalyzer();
		Query searchQuery = SearchUtil.prepareSearchQuery(term, new String[] {"uname", "title"}, analyzer);
		
		Hits hits = is.search(searchQuery);
		
		/*TopDocs hits = is.search(searchQuery, null, 10);
		ScoreDoc [] docs = hits.scoreDocs;
		for (int i = 0; i <docs.length ; i++) {
			Document doc = is.doc(docs[i].doc);*/
		
		List<Map<String, String>> results = new ArrayList<Map<String, String>>();
		for (int i = 0; i < hits.length(); i++) {
			Document doc = hits.doc(i);
			
						
			//filter by id or type
			String type = doc.get("type");
			if (!allowedTypes.contains(type)) {
				continue;
			}

			if(type.equals("Message"))
				if(!(doc.get("rootid")).equals(""))
					continue;
			
			String id = doc.get("id");
			if (id != null && filterIds != null) {
				if (!filterIds.contains(id)) {
					continue;
				}
			}
			
			Map<String, String> result = new HashMap<String, String>();
			
			//prepare url and label for different types
			String label = null;
			String url = null;
			if (type.equalsIgnoreCase("User")) {
				label = doc.get("uname");
				url = "/"+label;
			}
			else {
				label = doc.get("title");
				url = "/"+doc.get("url");
			}
			result.put("label", label.replaceAll(term, "<b>"+term+"</b>"));
			result.put("value", label);
			result.put("url", url);
			result.put("type", type);
			results.add(result);
			
			if (results.size() == 10) {
				break;
			}
		}
		is.close();
		return results;
	}
	
	public static void allSearch(String query) throws Exception {
		TalkerBean _talker = CommonUtil.loadCachedTalker(session);
		int limit=10;
		int totalCount = 0;
		List<TopicBean> topicResults = null;
		List<Action> convoResults = null;
		if (query != null) {
			topicResults = topicsSearch(query);
			totalCount = SearchUtil.searchConvo(query,200000,_talker) == null ? 0 : SearchUtil.searchConvo(query,200000,_talker).size();
			List<ConversationBean> convoList = SearchUtil.searchConvo(query,limit,_talker);
			convoResults = 
				ConversationLogic.convosToFeed(convoList);
		}
		render(topicResults, convoResults, totalCount);
	}
	
	public static void allSearchAjaxLoad(String query,int limit) throws Exception {
		
		TalkerBean _talker = CommonUtil.loadCachedTalker(session);
		if(limit==0)
			limit=10;
		List<Action> convoResults = null;
		if (query != null) {
			List<ConversationBean> convoList = SearchUtil.searchConvo(query,limit,_talker);
			convoResults = 
				ConversationLogic.convosToFeed(convoList);
		}
		
		List<Action> _feedItems = convoResults;
		
		render("tags/feed/feedList.html", _feedItems, _talker);
	}
	
	private static List<TopicBean> topicsSearch(String query) throws Exception {
		IndexSearcher is = new IndexSearcher(SearchUtil.SEARCH_INDEX_PATH+"autocomplete");
		
		Analyzer analyzer = new StandardAnalyzer();
		Query searchQuery = SearchUtil.prepareSearchQuery(query, new String[] {"title"}, analyzer);
		Hits hits = is.search(searchQuery);
		
		List<TopicBean> results = new ArrayList<TopicBean>();
		for (int i = 0; i < hits.length(); i++) {
			Document doc = hits.doc(i);
			
			//get only topics
			String type = doc.get("type");
			if (type == null || !type.equals("Topic")) {
				continue;
			}
			
			String topicId = doc.get("id");
			TopicBean topic = TopicDAO.getById(topicId);
			//load info for 130 Followers | 200 Conversations
			topic.setConversations(ConversationDAO.loadConversationsByTopic(topic.getId()));
	    	topic.setFollowers(TopicDAO.getTopicFollowers(topic));
			results.add(topic);
			
			/*if (results.size() == 3) {
				break;
			}*/
		}
		is.close();
		return results;
	}
	
	public static void messageSearch(String mailSubject){
		String result = "";
		MessageBean message = MessagingDAO.getMessageBySubject(mailSubject);
		if(message != null){
			System.out.println("Message : "+message.getId() + " - " +message.getSubject());
			
			result = message.getId();
		}
		renderText(result);
	}
}

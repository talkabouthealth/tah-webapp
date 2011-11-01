package controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logic.ConversationLogic;
import logic.FeedsLogic;
import logic.TalkerLogic;
import logic.TopicLogic;
import models.TalkerBean;
import models.TopicBean;
import models.actions.Action;
import models.actions.Action.ActionType;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RangeFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopDocs;

import dao.ConversationDAO;
import dao.TopicDAO;

import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;
import util.SearchUtil;

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
		 
		//Hits hits = is.search(searchQuery);
		
		TopDocs hits = is.search(searchQuery, null, 10);
		ScoreDoc [] docs = hits.scoreDocs;

		List<Map<String, String>> results = new ArrayList<Map<String, String>>();
		for (int i = 0; i <docs.length ; i++) {
			Document doc = is.doc(docs[i].doc);
			
			//filter by id or type
			String type = doc.get("type");
			if (!allowedTypes.contains(type)) {
				continue;
			}
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
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		List<TopicBean> topicResults = null;
		List<Action> convoResults = null;
		if (query != null) {
			topicResults = topicsSearch(query);
			convoResults = 
				ConversationLogic.convosToFeed(SearchUtil.searchConvo(query, 5,talker));
		}
		
		render(topicResults, convoResults);
	}
	
	private static List<TopicBean> topicsSearch(String query) throws Exception {
		IndexSearcher is = new IndexSearcher(SearchUtil.SEARCH_INDEX_PATH+"autocomplete");
		
		Analyzer analyzer = new StandardAnalyzer();
		Query searchQuery = SearchUtil.prepareSearchQuery(query, new String[] {"title"}, analyzer);
		//Hits hits = is.search(searchQuery);
		
		TopDocs hits = is.search(searchQuery, null, 5);
		ScoreDoc [] docs = hits.scoreDocs;
		
		
		List<TopicBean> results = new ArrayList<TopicBean>();
		for (int i = 0; i < docs.length; i++) {
			Document doc = is.doc(docs[i].doc);
			
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
	
}

package controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;

import play.mvc.Controller;
import play.mvc.With;
import util.SearchUtil;

@With(Secure.class)
public class Search extends Controller {
	
	public static void search() throws Exception {
		render();
	}
	
	public static void ajaxSearch(String term) throws Exception {
		List<String> allowedTypes = Arrays.asList("User", "Conversation", "Question", "Topic");
		List<Map<String, String>> results = makeSearch(term, allowedTypes);
		
		//add link to full search
		Map<String, String> result = new HashMap<String, String>();
		result.put("label", "Search for <b>"+term+"</b>");
		result.put("value", term);
		result.put("url", "#fullsearch");
		result.put("type", "");
		results.add(result);
		
		renderJSON(results);
	}
		
	public static void ajaxConvoSearch(String term) throws Exception {
		List<String> allowedTypes = Arrays.asList("Conversation", "Question");
		List<Map<String, String>> results = makeSearch(term, allowedTypes);
		
		renderJSON(results);
	}
	
	private static List<Map<String, String>> makeSearch(String term, List<String> allowedTypes) throws Exception {
		IndexSearcher is = new IndexSearcher(SearchUtil.SEARCH_INDEX_PATH+"autocomplete");
		
//		Query searchQuery = new PrefixQuery(new Term("uname", term));
//		Hits hits = is.search(searchQuery);
		
		Analyzer analyzer = new StandardAnalyzer();
//		QueryParser parser = new QueryParser("uname", analyzer);
		QueryParser parser = new MultiFieldQueryParser(new String[] {"uname", "title"}, analyzer);
		parser.setAllowLeadingWildcard(true);
		Query searchQuery = parser.parse("*"+term+"*");
		Hits hits = is.search(searchQuery);
		
//		Scorer scorer = new QueryScorer(searchQuery);
//		Highlighter highlighter = new Highlighter(scorer);

		List<Map<String, String>> results = new ArrayList<Map<String, String>>();
		for (int i = 0; i < hits.length(); i++) {
			Document doc = hits.doc(i);
			
			String type = doc.get("type");
			if (allowedTypes.contains(type)) {
				Map<String, String> result = new HashMap<String, String>();
				
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
				result.put("value", "");
				result.put("url", url);
				result.put("type", type);
				
				results.add(result);
			}
			
			if (results.size() == 10) {
				break;
			}
		}
		
		is.close();
		
		return results;
	}
	
	public static void main(String[] args) throws Exception {
//		test();
		
//		Hits hits = performSearch("test");
//		for (int i = 0; i < hits.length(); i++) {
//			Document doc = hits.doc(i);
//			System.out.println(hits.score(i));
//			System.out.println(doc.get("uname")+" : "+doc.get("id"));
//		}
	}


}

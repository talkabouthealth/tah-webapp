package controllers;

import java.util.ArrayList;
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
	
	public static void search(String query) throws Exception {
		if (query == null) {
			render();
			return;
		}
		
//		ParallelMultiSearcher pms = new ParallelMultiSearcher(new i);
		System.out.println(SearchUtil.SEARCH_INDEX_PATH+"talker");
		
		Analyzer analyzer = new StandardAnalyzer();
		IndexSearcher is = new IndexSearcher(SearchUtil.SEARCH_INDEX_PATH+"talker");
//		QueryParser parser = new QueryParser("uname", analyzer);
		QueryParser parser = new MultiFieldQueryParser(new String[] {"uname", "bio"}, analyzer);
		parser.setAllowLeadingWildcard(true);
		Query searchQuery = parser.parse(query);
		Hits hits = is.search(searchQuery);
		
		List<String> results = new ArrayList<String>();
		for (int i = 0; i < hits.length(); i++) {
			Document doc = hits.doc(i);
//			System.out.println(hits.score(i));
//			System.out.println(doc.get("uname")+" : "+doc.get("id"));
			results.add(doc.get("uname"));
		}
		
		is.close();
		
		params.flash("query");
		render(results);
	}
	
	public static void ajaxSearch(String term) throws Exception {
		IndexSearcher is = new IndexSearcher(SearchUtil.SEARCH_INDEX_PATH+"autocomplete");
		
//		Query searchQuery = new PrefixQuery(new Term("uname", term));
//		Hits hits = is.search(searchQuery);
		
		Analyzer analyzer = new StandardAnalyzer();
//		QueryParser parser = new QueryParser("uname", analyzer);
		QueryParser parser = new MultiFieldQueryParser(new String[] {"uname", "title"}, analyzer);
		parser.setAllowLeadingWildcard(true);
		Query searchQuery = parser.parse("*"+term+"*");
		Hits hits = is.search(searchQuery);
		
		is.close();
		
//		Scorer scorer = new QueryScorer(searchQuery);
//		Highlighter highlighter = new Highlighter(scorer);

		List<Map<String, String>> results = new ArrayList<Map<String, String>>();
		for (int i = 0; i < hits.length(); i++) {
			Document doc = hits.doc(i);
			
			Map<String, String> result = new HashMap<String, String>();
			String type = doc.get("type");
			String label = null;
			String url = null;
			if (type.equalsIgnoreCase("User")) {
				label = doc.get("uname");
				url = "/"+label;
			}
			else {
				label = doc.get("title");
				url = "/topic/"+doc.get("url");
			}
			
			result.put("label", label.replaceAll(term, "<b>"+term+"</b>"));
			result.put("value", "");
			result.put("url", url);
			result.put("type", type);
			
			results.add(result);
			
			if (i == 10) {
				break;
			}
		}
		
		renderJSON(results);
	}
	
//	public static String doHighlighting(String text,String query){
//	    String delims="/ ;,:\\+-*";
//	    HashSet<String> queryWords=new HashSet<String>();
//	    
//	    StringTokenizer st=new StringTokenizer(query,delims);
//	    
//	    /*iterate over words in the query*/
//	    while(st.hasMoreTokens()){
//	      String token=st.nextToken().toLowerCase();
//	      queryWords.add(token);
//	    }
//	    
//	    for(String queryWord:queryWords){
//	     text=text.replaceAll(queryWord, "<b>"+queryWord+"</b>");
//	     if(!queryWord.equals(capitalizeFirstChar(queryWord)))
//	       text=text.replaceAll(capitalizeFirstChar(queryWord), "<b>"+capitalizeFirstChar(queryWord)+"</b>");
//	    }
//	    System.out.println(text);
//	    return text;
//	  }
	
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

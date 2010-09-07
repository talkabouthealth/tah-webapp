package util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

import play.Play;
import play.Play.Mode;

public class SearchUtil {
	
	public static final String SEARCH_INDEX_PATH = Play.configuration.getProperty("search.index");

	public static List<String> searchTalker(String query) throws CorruptIndexException, IOException, ParseException {
		IndexSearcher is = new IndexSearcher(SearchUtil.SEARCH_INDEX_PATH+"talker");
		Hits hits = getHits(is, new String[] {"uname", "bio"}, "*"+query+"*");
		List<String> results = new ArrayList<String>();
		for (int i = 0; i < hits.length(); i++) {
			Document doc = hits.doc(i);
//			System.out.println(hits.score(i));
//			System.out.println(doc.get("uname")+" : "+doc.get("id"));
			
			//TODO hide bio and health details
			String result = doc.get("uname");
			String bio = doc.get("bio");
			if (bio != null) {
				result += ("<br/>" + doc.get("bio"));
			}
			result = result.replaceAll(query, "<b>"+query+"</b>");
			results.add(result);
		}
		
		is.close();
		return results;
	}
	
	public static List<String> searchConvo(String query) throws CorruptIndexException, IOException, ParseException {
		IndexSearcher is = new IndexSearcher(SearchUtil.SEARCH_INDEX_PATH+"conversations");
		Hits hits = getHits(is, new String[] {"title", "answers"}, "*"+query+"*");
		List<String> results = new ArrayList<String>();
		for (int i = 0; i < hits.length(); i++) {
			Document doc = hits.doc(i);
			
			String result = doc.get("title");
			result = result.replaceAll(query, "<b>"+query+"</b>");
			results.add(result);
		}
		
		is.close();
		return results;
	}
	
	private static Hits getHits(IndexSearcher is, String[] fields, String query) 
			throws CorruptIndexException, IOException, ParseException {
		Analyzer analyzer = new StandardAnalyzer();
		//TODO: recommended to use only one searcher? Open after reindex?
		
		QueryParser parser = new MultiFieldQueryParser(fields, analyzer);
		parser.setAllowLeadingWildcard(true);
		Query searchQuery = parser.parse(query);
		
		Hits hits = is.search(searchQuery);
		return hits;
	}
	
//	ParallelMultiSearcher pms = new ParallelMultiSearcher(new i);
//		QueryParser parser = new QueryParser("uname", analyzer);
}

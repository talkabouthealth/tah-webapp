package controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import models.TalkerBean;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.ParallelMultiSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.LockObtainFailedException;

import play.mvc.Controller;
import play.mvc.With;

import util.SearchUtil;

import dao.TalkerDAO;

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
		
		params.flash("query");
		render(results);
	}
	
	public static void ajaxSearch(String term) throws Exception {
		IndexSearcher is = new IndexSearcher(SearchUtil.SEARCH_INDEX_PATH+"talker");
		
		Query searchQuery = new PrefixQuery(new Term("uname", term));
		Hits hits = is.search(searchQuery);
		
		List<String> results = new ArrayList<String>();
		for (int i = 0; i < hits.length(); i++) {
			Document doc = hits.doc(i);
			results.add(doc.get("uname"));
		}
		
		renderJSON(results);
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

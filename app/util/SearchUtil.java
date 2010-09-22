package util;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import models.ConversationBean;
import models.TalkerBean;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similar.MoreLikeThis;

import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;

import play.Play;
import play.Play.Mode;

public class SearchUtil {
	
	public static final String SEARCH_INDEX_PATH = Play.configuration.getProperty("search.index");

	public static List<TalkerBean> searchTalker(String query) throws CorruptIndexException, IOException, ParseException {
		IndexSearcher is = new IndexSearcher(SearchUtil.SEARCH_INDEX_PATH+"talker");
		Hits hits = getHits(is, new String[] {"uname", "bio"}, "*"+query+"*");
		
//		List<String> results = new ArrayList<String>();
		List<TalkerBean> results = new ArrayList<TalkerBean>();
		for (int i = 0; i < hits.length(); i++) {
			Document doc = hits.doc(i);
//			System.out.println(hits.score(i));
//			System.out.println(doc.get("uname")+" : "+doc.get("id"));
			
			//TODO hide bio and health details
//			String result = doc.get("uname");
//			String bio = doc.get("bio");
//			if (bio != null) {
//				result += ("<br/>" + doc.get("bio"));
//			}
//			result = result.replaceAll(query, "<b>"+query+"</b>");
//			results.add(result);
			TalkerBean talker = TalkerDAO.getById(doc.get("id"));
			results.add(talker);
			
			if (i == 8) {
				break;
			}
		}
		
		is.close();
		return results;
	}
	
	public static List<ConversationBean> searchConvo(String query) throws CorruptIndexException, IOException, ParseException {
		IndexSearcher is = new IndexSearcher(SearchUtil.SEARCH_INDEX_PATH+"conversations");
		Hits hits = getHits(is, new String[] {"title", "answers"}, "*"+query+"*");
		
		List<ConversationBean> results = new ArrayList<ConversationBean>();
		for (int i = 0; i < hits.length(); i++) {
			Document doc = hits.doc(i);
			
			String convoId = doc.get("id");
			ConversationBean convo = ConversationDAO.getByConvoId(convoId);
			convo.setComments(CommentsDAO.loadConvoAnswers(convoId));
			
			results.add(convo);
			
			if (i == 9) {
				break;
			}
		}
		
		is.close();
		return results;
	}
	
	//TODO: update Lucene and queries to most recent version
	//TODO: check MoreLikeThis again
	public static List<ConversationBean> getRelatedConvos(ConversationBean searchedConvo) throws Exception {
		IndexSearcher is = new IndexSearcher(SearchUtil.SEARCH_INDEX_PATH+"conversations");
//		MoreLikeThis mlt = new MoreLikeThis(is.getIndexReader());
//		mlt.setAnalyzer(new StandardAnalyzer());
//		mlt.setFieldNames(new String[] {"title"});
//		Query searchQuery = mlt.like(new StringReader(searchedConvo.getTopic()));
//		Hits hits = is.search(searchQuery);
		
		Analyzer analyzer = new StandardAnalyzer();
		QueryParser parser = new MultiFieldQueryParser(new String[] {"title"}, analyzer);
		Query searchQuery = parser.parse(searchedConvo.getTopic());
		Hits hits = is.search(searchQuery);

		System.out.println("SEARCH FOR: "+searchedConvo.getTopic()+", "+hits.length());
		
		List<ConversationBean> results = new ArrayList<ConversationBean>();
		for (int i = 0; i < hits.length(); i++) {
			Document doc = hits.doc(i);
			
			String convoId = doc.get("id");
			if (searchedConvo.getId().equals(convoId)) {
				continue;
			}
			ConversationBean convo = ConversationDAO.getByConvoId(convoId);
//			convo.setComments(CommentsDAO.loadConvoAnswers(convoId));
			results.add(convo);
			
			if (results.size() == 3) {
				break;
			}
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

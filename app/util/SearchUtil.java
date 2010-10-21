package util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import models.CommentBean;
import models.ConversationBean;
import models.TalkerBean;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CachingTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.Scorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.search.spans.SpanScorer;

import play.Play;
import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;

public class SearchUtil {
	
	public static final String SEARCH_INDEX_PATH = Play.configuration.getProperty("search.index");

	public static List<TalkerBean> searchTalker(String query) throws CorruptIndexException, IOException, ParseException {
		IndexSearcher is = new IndexSearcher(SearchUtil.SEARCH_INDEX_PATH+"talker");
		Query searchQuery = getHits(is, new String[] {"uname", "bio"}, "*"+query+"*");
		Hits hits = is.search(searchQuery);
		
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
//		Query searchQuery = getHits(is, new String[] {"title", "answers"}, "*"+query+"*");
		Analyzer analyzer = new StandardAnalyzer();
		QueryParser parser = new MultiFieldQueryParser(new String[] {"title", "answers"}, analyzer);
		parser.setAllowLeadingWildcard(true);
		Query searchQuery = parser.parse("*"+query+"*");
		Hits hits = is.search(searchQuery);
		
		List<ConversationBean> results = new ArrayList<ConversationBean>();
		for (int i = 0; i < hits.length(); i++) {
			Document doc = hits.doc(i);
			
			String convoId = doc.get("id");
			ConversationBean convo = ConversationDAO.getByConvoId(convoId);
			convo.setComments(CommentsDAO.loadConvoAnswers(convoId));
			
			StringBuilder answersString = new StringBuilder();
			for (CommentBean answer : convo.getComments()) {
				answersString.append(answer.getText());
				answersString.append(" ... ");
			}
			
			searchQuery = searchQuery.rewrite(is.getIndexReader());
			Scorer scorer = new QueryScorer(searchQuery, "answers");
			Highlighter highlighter = new Highlighter(scorer);
			Fragmenter fragmenter = new SimpleFragmenter(60);
			highlighter.setTextFragmenter(fragmenter);
			String fr = highlighter.getBestFragment(analyzer, "answers", answersString.toString());
//			String[] fragments = highlighter.getBestFragments(new StandardAnalyzer(), 
//					"answers", answersString.toString(), 5);
//			System.out.println(answersString.toString()+",  FR: "+fr);
			convo.setSearchFragment(fr);
			
			results.add(convo);
			
			if (i == 9) {
				break;
			}
		}
		
		is.close();
		return results;
	}
//	hlu.getFragmentsWithHighlightedTerms(analyzer,
//            query, “contents”, contents, 5, 100);

	private static void getFragmentsWithHighlightedTerms(String fieldName, String text, Analyzer analyzer) {
//		TokenStream stream = TokenSources.getTokenStream(fieldName, fieldContents, analyzer);
//		Scorer scorer = new SpanScorer(query, fieldName,
//				new CachingTokenFilter(stream));
//		Scorer scorer = new QueryScorer(query, fieldName);
////		Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, 100);
//	
//		Highlighter highlighter = new Highlighter(scorer);
////		highlighter.setTextFragmenter(fragmenter);
//		String[] fragments = highlighter.getBestFragments(analyzer, fieldName, text, 5);
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

//		System.out.println("SEARCH FOR: "+searchedConvo.getTopic()+", "+hits.length());
		
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
	
	//TODO: recommended to use only one searcher? Open after reindex?
	private static Query getHits(IndexSearcher is, String[] fields, String query) 
			throws CorruptIndexException, IOException, ParseException {
		Analyzer analyzer = new StandardAnalyzer();
		QueryParser parser = new MultiFieldQueryParser(fields, analyzer);
		parser.setAllowLeadingWildcard(true);
		Query searchQuery = parser.parse(query);
		
		return searchQuery;
	}
	
//	ParallelMultiSearcher pms = new ParallelMultiSearcher(new i);
//		QueryParser parser = new QueryParser("uname", analyzer);
}

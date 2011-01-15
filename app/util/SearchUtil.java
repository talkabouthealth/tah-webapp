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

//TODO: check FB, Twitter, Search utils
public class SearchUtil {
	
	public static final String SEARCH_INDEX_PATH = Play.configuration.getProperty("search.index");

	public static List<TalkerBean> searchTalker(String query) throws CorruptIndexException, IOException, ParseException {
		IndexSearcher is = new IndexSearcher(SearchUtil.SEARCH_INDEX_PATH+"talker");
		Query searchQuery = getHits(is, new String[] {"uname", "bio"}, "*"+query+"*");
		Hits hits = is.search(searchQuery);
		
		List<TalkerBean> results = new ArrayList<TalkerBean>();
		for (int i = 0; i < hits.length(); i++) {
			Document doc = hits.doc(i);

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
			ConversationBean convo = ConversationDAO.getById(convoId);
			if (convo == null || convo.isDeleted()) {
				continue;
			}
			convo.setComments(CommentsDAO.loadConvoAnswersTree(convoId));
			
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

	
	//TODO: update Lucene and queries to most recent version
	public static List<ConversationBean> getRelatedConvos(ConversationBean searchedConvo) throws Exception {
		IndexSearcher is = new IndexSearcher(SearchUtil.SEARCH_INDEX_PATH+"conversations");
//		MoreLikeThis mlt = new MoreLikeThis(is.getIndexReader());
//		mlt.setAnalyzer(new StandardAnalyzer());
//		mlt.setFieldNames(new String[] {"title"});
//		Query searchQuery = mlt.like(new StringReader(searchedConvo.getTopic()));
//		Hits hits = is.search(searchQuery);
		
		//remove bad characters (special for search)
		String queryText = searchedConvo.getTopic();
		queryText = queryText.replaceAll("[\\W_]", " ");
		
		Analyzer analyzer = new StandardAnalyzer();
		QueryParser parser = new MultiFieldQueryParser(new String[] {"title"}, analyzer);
		Query searchQuery = parser.parse(queryText);
		Hits hits = is.search(searchQuery);

		List<ConversationBean> results = new ArrayList<ConversationBean>();
		for (int i = 0; i < hits.length(); i++) {
			Document doc = hits.doc(i);
			
			String convoId = doc.get("id");
			if (searchedConvo.getId().equals(convoId)) {
				continue;
			}
			ConversationBean convo = ConversationDAO.getById(convoId);
			results.add(convo);
			
			if (results.size() == 3) {
				break;
			}
		}
		
		is.close();
		return results;
	}
	
	//TODO: later - recommended to use only one searcher? Open after reindex?
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

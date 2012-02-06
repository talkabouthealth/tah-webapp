package util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import logic.FeedsLogic;
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

	/**
	 * Returns eight talkers for given query.
	 * Search by username and bio.
	 * 
	 * @param query
	 * @return
	 * @throws CorruptIndexException
	 * @throws IOException
	 * @throws ParseException
	 */
	public static List<TalkerBean> searchTalker(String query, TalkerBean talkerBean) throws CorruptIndexException, IOException, ParseException {
		IndexSearcher is = new IndexSearcher(SearchUtil.SEARCH_INDEX_PATH+"talker");
		
		Analyzer analyzer = new StandardAnalyzer();
		Query searchQuery = prepareSearchQuery(query, new String[] {"uname","fname","lname","bio"}, analyzer);
		Hits hits = is.search(searchQuery);
		
		List<String> cat = FeedsLogic.getCancerType(talkerBean);
		
		List<TalkerBean> results = new ArrayList<TalkerBean>();
		for (int i = 0; i < hits.length(); i++) {
			Document doc = hits.doc(i);

			TalkerBean talker = TalkerDAO.getById(doc.get("id"));
			
			//Commented code to display all category users in members page
			//if(talker.getName() != null && cat.contains(talker.getCategory()))
				results.add(talker);
			//TO DO : Must need to remove to show more users. 
			//if (i == 7) {
			//	break;
			//}
		}
		is.close();
		return results;
	}
	
	/**
	 * Search 10 conversations for given query.
	 * Searches in titles and answers.
	 * 
	 * @param query
	 * @return
	 * @throws CorruptIndexException
	 * @throws IOException
	 * @throws ParseException
	 */
	public static List<ConversationBean> searchConvo(String query, int numOfResults, TalkerBean talker) throws CorruptIndexException, IOException, ParseException {
		IndexSearcher is = new IndexSearcher(SearchUtil.SEARCH_INDEX_PATH+"conversations");
		
		//prepare query to search
		Analyzer analyzer = new StandardAnalyzer();
		Query searchQuery = prepareSearchQuery(query, new String[] {"title", "answers"}, analyzer);
		Hits hits = is.search(searchQuery);
		
		//List<String> cat = FeedsLogic.getCancerType(talker);
		
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
			
			//get result fragment with highlighted matching parts
			searchQuery = searchQuery.rewrite(is.getIndexReader());
			Scorer scorer = new QueryScorer(searchQuery, "answers");
			Highlighter highlighter = new Highlighter(scorer);
			Fragmenter fragmenter = new SimpleFragmenter(60);
			highlighter.setTextFragmenter(fragmenter);
			String fr = highlighter.getBestFragment(analyzer, "answers", answersString.toString());
			convo.setSearchFragment(fr);
			
			//if(cat.contains(convo.getCategory()) || cat.contains(convo.getOtherDiseaseCategories()))
				results.add(convo);
			if (results.size() == numOfResults) {
				break;
			}
		}
		
		is.close();
		return results;
	}

	
	//TODO: later - update Lucene and queries to most recent version
	/**
	 * Returns 3 conversations related to given.
	 */
	public static List<ConversationBean> getRelatedConvos(TalkerBean talker,ConversationBean searchedConvo) throws Exception {
		IndexSearcher is = new IndexSearcher(SearchUtil.SEARCH_INDEX_PATH+"conversations");
		
		//Possible implementation?
//		MoreLikeThis mlt = new MoreLikeThis(is.getIndexReader());
//		mlt.setAnalyzer(new StandardAnalyzer());
//		mlt.setFieldNames(new String[] {"title"});
//		Query searchQuery = mlt.like(new StringReader(searchedConvo.getTopic()));
//		Hits hits = is.search(searchQuery);
		
		//remove bad characters (special for search)
		String queryText = searchedConvo.getTopic();
		queryText = queryText.replaceAll("[\\W_]", " ");
		
		Analyzer analyzer = new StandardAnalyzer();
		Query searchQuery = prepareSearchQuery(queryText, new String[] {"title"}, analyzer);
		Hits hits = is.search(searchQuery);
		
		//List<String> cat = FeedsLogic.getCancerType(talker);
		List<ConversationBean> results = new ArrayList<ConversationBean>();
		for (int i = 0; i < hits.length(); i++) {
			Document doc = hits.doc(i);
			
			String convoId = doc.get("id");
			if (searchedConvo.getId().equals(convoId)) {
				continue;
			}
			ConversationBean convo = ConversationDAO.getById(convoId);
			//if(cat.contains(convo.getTalker().getCategory()))
				results.add(convo);
			if (results.size() == 3) {
				break;
			}
		}
		
		is.close();
		return results;
	}
	
	public static Query prepareSearchQuery(String term, String[] fields, Analyzer analyzer)
			throws ParseException {

		if(term != null && !term.equals(""))
			term = escapeString(term);
		
		QueryParser parser = new MultiFieldQueryParser(fields, analyzer);
		parser.setAllowLeadingWildcard(true);
		/* Updated to show all users if no search term entered. 
		 * Good to have change
		 * */
		String searchTerm = "";
		if (term != null) {
			searchTerm = term;
		}
		if (term != null && term.length() > 0) {
			//if term contains only one word (or part) - use wildcard search
			if (term.split(" ").length == 1) {
				searchTerm = "*"+term+"*";
			}
		}
		
		Query searchQuery = parser.parse(searchTerm);
		return searchQuery;
	}
	
	private static String escapeString(String searchTerm){
		if(searchTerm.contains("(")) searchTerm = searchTerm.replace("(", "\\(");
		if(searchTerm.contains(")")) searchTerm = searchTerm.replace(")", "\\)");
		if(searchTerm.contains("[")) searchTerm = searchTerm.replace("[", "\\[");
		if(searchTerm.contains("]")) searchTerm = searchTerm.replace("]", "\\]");
		if(searchTerm.contains("{")) searchTerm = searchTerm.replace("{", "\\{");
		if(searchTerm.contains("}")) searchTerm = searchTerm.replace("}", "\\}");
		if(searchTerm.contains("^")) searchTerm = searchTerm.replace("^", "\\^");
		if(searchTerm.contains(" ")) searchTerm = searchTerm.replace(" ", "\\ ");
		if(searchTerm.contains(":")) searchTerm = searchTerm.replace(":", "\\:");
		if(searchTerm.contains("+")) searchTerm = searchTerm.replace("+", "\\+");
		if(searchTerm.contains("*")) searchTerm = searchTerm.replace("*", "\\*");
		if(searchTerm.contains("?")) searchTerm = searchTerm.replace("?", "\\?");
		if(searchTerm.contains("!")) searchTerm = searchTerm.replace("!", "\\!");
		return(searchTerm);
	}
	
}

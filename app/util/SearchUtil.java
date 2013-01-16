package util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import logic.FeedsLogic;
import logic.TalkerLogic;
import models.CommentBean;
import models.ConversationBean;
import models.TalkerBean;
import models.TopicBean;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Fragmenter;

import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.Scorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.mongodb.QueryBuilder;

import play.Play;
import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;

public class SearchUtil {
	
	public static final String SEARCH_INDEX_PATH = Play.configuration.getProperty("search.index");
	private static String ALL_CANCERS = "All Cancers";
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
	public static List<TalkerBean> searchTalker(String query, TalkerBean talkerBean,String cancerType) throws CorruptIndexException, IOException, ParseException {
		File indexerFile = new File(SearchUtil.SEARCH_INDEX_PATH+"talker");
 		Directory indexDir = FSDirectory.open(indexerFile);
 		IndexReader indexReader = IndexReader.open(indexDir);
		IndexSearcher is = new IndexSearcher(indexReader);
		//prepare query to search
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_36);
		Query searchQuery = prepareSearchQuery(query, new String[] {"uname","fname","lname","bio"}, analyzer, true,cancerType);
		System.out.println(searchQuery.toString());
		TopDocs hits = is.search(searchQuery, 15);
		ScoreDoc [] docs = hits.scoreDocs;
		
		//List<String> cat = FeedsLogic.getCancerType(talkerBean);
		
		List<TalkerBean> results = new ArrayList<TalkerBean>();
		for (int i = 0; i < docs.length ; i++) {
			Document doc = is.doc(docs[i].doc);
			TalkerBean talker = TalkerDAO.getById(doc.get("id"));
			
			//Commented code to display all category users in members page
			//if(talker.getName() != null && cat.contains(talker.getCategory()))
				results.add(talker);
			//TO DO : Must need to remove to show more users. 
			//if (i == 7) {
			//	break;
			//}
		}
		indexReader.close();
		indexDir.close();
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
	public static List<ConversationBean> searchConvo(String query, int numOfResults, TalkerBean talker,String cancerType) throws CorruptIndexException, IOException, ParseException {
		List<ConversationBean> results = new ArrayList<ConversationBean>();
		
		File indexerFile = new File(SearchUtil.SEARCH_INDEX_PATH+"conversations");
 		Directory indexDir = FSDirectory.open(indexerFile);
 		IndexReader indexReader = IndexReader.open(indexDir);
		IndexSearcher is = new IndexSearcher(indexReader);
		//prepare query to search
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_36);
		Query searchQuery = prepareSearchQuery(query, new String[] {"title", "answers"}, analyzer, true,cancerType);
		TopDocs hits = is.search(searchQuery, numOfResults);
		ScoreDoc [] docs = hits.scoreDocs;

		for (int i = 0; i < docs.length ; i++) {
			Document doc = is.doc(docs[i].doc);
			
			String convoId = doc.get("id");
			ConversationBean convo = ConversationDAO.getByIdBasicQuestion(convoId);
			if (convo == null || convo.isDeleted()) {
				continue;
			}
			convo.setComments(CommentsDAO.loadConvoAnswersTree(convoId));
			
			StringBuilder answersString = new StringBuilder();
			for (CommentBean answer : convo.getComments()) {
				answersString.append(answer.getText());
				answersString.append(" ... ");
			}
			
			//TODO: Removed some of the code for funtionality working for now
			// Need to implement in future.
			//get result fragment with highlighted matching parts
			//searchQuery = searchQuery.rewrite(is.getIndexReader());
			
			//QueryScorer scorer = new QueryScorer(searchQuery, indexReader, "answers"); 
			//Highlighter highlighter = new Highlighter(scorer); 
			//String [] fragment = highlighter.getBestFragments(analyzer, "answers", answersString.toString(),10);

			//Scorer scorer = new QueryScorer(searchQuery, "answers");
			//Highlighter highlighter = new Highlighter(scorer);
			//Fragmenter fragmenter = new SimpleFragmenter(60);
			//highlighter.setTextFragmenter(fragmenter);
			//String fr = highlighter.getBestFragment(analyzer, "answers", answersString.toString());
			//(analyzer, "answers", answersString.toString());
			//convo.setSearchFragment(fragment[0]);
			
			//if(cat.contains(convo.getCategory()) || cat.contains(convo.getOtherDiseaseCategories()))
				results.add(convo);
			if (results.size() == numOfResults) {
				break;
			}
		}
		indexReader.close();
		indexDir.close();
		is.close();
		return results;
	}

	
	//TODO: later - update Lucene and queries to most recent version
	/**
	 * Returns 3 conversations related to given.
	 */
	public static List<ConversationBean> getRelatedConvos(TalkerBean talker,ConversationBean searchedConvo,String cancerType) throws Exception {
		File indexerFile = new File(SearchUtil.SEARCH_INDEX_PATH+"conversations");
 		Directory indexDir = FSDirectory.open(indexerFile);
 		IndexReader indexReader = IndexReader.open(indexDir);
		IndexSearcher is = new IndexSearcher(indexReader);
		//prepare query to search
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_36);

		//remove bad characters (special for search)
		String queryText = searchedConvo.getTopic();
		queryText = queryText.replaceAll("[\\W_]", " ");
		cancerType = searchedConvo.getCategory();
		
		String topic = "";
		if(searchedConvo.getTopics() != null && !searchedConvo.getTopics().isEmpty()) {
			for (TopicBean topicBean : searchedConvo.getTopics()) {
				topic = topic + topicBean.getTitle() + " ";
			}
	 	}
		
		Query searchQuery = prepareSearchQueryForRelatedQuestion(queryText, new String[] {"title"}, analyzer, true,cancerType,topic);
		
		TopDocs hits = is.search(searchQuery, 50);
		ScoreDoc [] docs = hits.scoreDocs;
		
		List<ConversationBean> results = new ArrayList<ConversationBean>();
		for (int i = 0; i < docs.length ; i++) {
			if (results.size() == 13) {
				break;
			} else {
				Document doc = is.doc(docs[i].doc);
				String convoId = doc.get("id");
				if (searchedConvo.getId().equals(convoId)) {
					continue;
				} else {
					ConversationBean convo = TalkerLogic.loadConvoFromCache(convoId);
					if(!convo.isOpened())
						results.add(convo);
				}
			}
		}
		indexReader.close();
		indexDir.close();
		is.close();
		return results;
	}

	public static Query prepareSearchQueryForRelatedQuestion(String term, String[] fields, Analyzer analyzer,boolean check,String cancerType,String topic) 
			throws ParseException {

		QueryParser parser = new MultiFieldQueryParser(Version.LUCENE_36,fields, analyzer);
		parser.setAllowLeadingWildcard(true);
		/* Updated to show all users if no search term entered.  Good to have change */

		if (StringUtils.isBlank(term))
			term = "";

		if(check)
			term = escapeString(term);

		term = removeChars(term);

		String[] cancerFields = new String[] {"category"};
		
		QueryParser cancerParser = new MultiFieldQueryParser(Version.LUCENE_36,cancerFields, analyzer);
		parser.setAllowLeadingWildcard(true);

		String[] topicFields = new String[] {"topics"};
		QueryParser topicParser = new MultiFieldQueryParser(Version.LUCENE_36,topicFields, analyzer);

		Query searchQuery;
		if(StringUtils.isNotEmpty(cancerType)) {
			if(StringUtils.isNotBlank(term))
				searchQuery = parser.parse(term  + " OR  ((" +cancerParser.parse("\"" + cancerType + "\"") + ") AND (" + topicParser.parse(topic) +  ") )");
			else
				searchQuery = cancerParser.parse("\"" + cancerType + "\"");
		} else {
			searchQuery = parser.parse(term);
		}
		
		//Query queries = topicParser.parse("cancer");
		//ArrayList<Query> arr = new ArrayList<Query>();
		//arr.add(queries);
		//Query [] qArray = (Query[]) arr.toArray();
		
		//searchQuery.combine(qArray);
		//System.out.println("The Updated Query: " + searchQuery);
		return searchQuery;
	}

	public static Query prepareSearchQuery(String term, String[] fields, Analyzer analyzer,boolean check,String cancerType)
			throws ParseException {
		
		if(check) {
			if(term != null && !term.equals(""))
				term = escapeString(term);
		}
		
		QueryParser parser = new MultiFieldQueryParser(Version.LUCENE_36,fields, analyzer);
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
		
		String[] cancerFields = new String[] {"category"};
		
		QueryParser cancerParser = new MultiFieldQueryParser(Version.LUCENE_36,cancerFields, analyzer);
		parser.setAllowLeadingWildcard(true);
		
		searchTerm = removeChars(searchTerm);
		Query searchQuery;
		if(StringUtils.isNotEmpty(cancerType)) {
			if(StringUtils.isNotBlank(searchTerm))
				searchQuery = parser.parse(searchTerm  + " AND  (" +cancerParser.parse("\"" + cancerType + "\",\"" + ALL_CANCERS  + "\"") + ")");
			else
				searchQuery = cancerParser.parse("\"" + cancerType + "\",\"" + ALL_CANCERS  + "\"");
		} else {
			searchQuery = parser.parse(searchTerm);
		}
		return searchQuery;
	}
	
	private static String removeChars(String searchTerm) {
		if(searchTerm.contains("(")) searchTerm = searchTerm.replace("(", "");
		if(searchTerm.contains(")")) searchTerm = searchTerm.replace(")", "");
		if(searchTerm.contains("[")) searchTerm = searchTerm.replace("[", "");
		if(searchTerm.contains("]")) searchTerm = searchTerm.replace("]", "");
		if(searchTerm.contains("{")) searchTerm = searchTerm.replace("{", "");
		if(searchTerm.contains("}")) searchTerm = searchTerm.replace("}", "");
		if(searchTerm.contains("^")) searchTerm = searchTerm.replace("^", "");
		if(searchTerm.contains("\"")) searchTerm = searchTerm.replace("\"", "");
		return(searchTerm);
	}
	
	public static String escapeString(String searchTerm){
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
		if(searchTerm.contains("#")) searchTerm = searchTerm.replace("#", "\\#");
		if(searchTerm.contains("\"")) searchTerm = searchTerm.replace("\"", "\\\"");
		return(searchTerm);
	}

	public static int searchConvoToGetTotalCount(String query, int i, TalkerBean talker,String cancerType) throws Exception {
		File indexerFile = new File(SearchUtil.SEARCH_INDEX_PATH+"conversations");
 		Directory indexDir = FSDirectory.open(indexerFile);
 		IndexReader indexReader = IndexReader.open(indexDir);
		IndexSearcher is = new IndexSearcher(indexReader);
		//prepare query to search
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_36);
		Query searchQuery = prepareSearchQuery(query, new String[] {"title", "answers"}, analyzer, true,cancerType);
		TopDocs hits = is.search(searchQuery, 1);

		indexReader.close();
		indexDir.close();
		is.close();
		return hits.totalHits;
	}
	
}

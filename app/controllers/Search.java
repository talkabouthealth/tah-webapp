package controllers;

import java.io.IOException;

import models.TalkerBean;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.ParallelMultiSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.LockObtainFailedException;

import dao.TalkerDAO;

public class Search {
	
	public static void test() throws CorruptIndexException, LockObtainFailedException, IOException {
		IndexWriter indexWriter = new IndexWriter("D:\\index", new StandardAnalyzer(), true);
		
		for (TalkerBean talker : TalkerDAO.loadAllTalkers()) {
			Document doc = new Document();
			  doc.add(new Field("id", talker.getId(), Field.Store.YES,
			                      Field.Index.NO));
			  doc.add(new Field("uname", talker.getUserName(), Field.Store.YES,
			                        Field.Index.TOKENIZED));
//			  doc.add(new Field("city", hotel.getCity(), Field.Store.YES,
//			                        Field.Index.UN_TOKENIZED));
//			  doc.add(new Field("description", hotel.getDescription(),
//			                   Field.Store.YES,
//			                   Field.Index.TOKENIZED));
//			  String fullSearchableText
//			        = hotel.getName()
//			         + " " + hotel.getCity() + " " + hotel.getDescription();
//
//			  doc.add(new Field("content", fullSearchableText,
//			                 Field.Store.NO,
//			                 Field.Index.TOKENIZED));
			  
			  indexWriter.addDocument(doc);
		}
		indexWriter.close();
		
	}
	
	public static Hits performSearch(String queryString)
		    throws IOException, ParseException {
		
//		ParallelMultiSearcher pms = new ParallelMultiSearcher(new i);
		
		Analyzer analyzer = new StandardAnalyzer();
		IndexSearcher is = new IndexSearcher("D:\\index");
		QueryParser parser = new QueryParser("uname", analyzer);
		Query query = parser.parse(queryString);
		Hits hits = is.search(query);
		return hits;
	}
	
	public static void main(String[] args) throws Exception {
//		test();
		
		Hits hits = performSearch("test");
		for (int i = 0; i < hits.length(); i++) {
			Document doc = hits.doc(i);
			System.out.println(hits.score(i));
			System.out.println(doc.get("uname")+" : "+doc.get("id"));
		}
	}


}

package util.jobs;

import java.io.IOException;

import models.TalkerBean;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.LockObtainFailedException;

import dao.TalkerDAO;
import play.jobs.Every;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import util.SearchUtil;

@OnApplicationStart
@Every("15min")
public class SeachIndexerJob extends Job {

	@Override
	public void doJob() throws Exception {
		IndexWriter indexWriter = new IndexWriter(SearchUtil.SEARCH_INDEX_PATH+"talker", new StandardAnalyzer(), true);
		
		for (TalkerBean talker : TalkerDAO.loadAllTalkers()) {
			Document doc = new Document();
			  doc.add(new Field("id", talker.getId(), Field.Store.YES,
			                      Field.Index.NO));
			  doc.add(new Field("uname", talker.getUserName(), Field.Store.YES,
			                        Field.Index.TOKENIZED));
			  
			  if (talker.getBio() != null) {
				  doc.add(new Field("bio", talker.getBio(), Field.Store.YES,
	                      Field.Index.TOKENIZED));
			  }
			  
//				  doc.add(new Field("city", hotel.getCity(), Field.Store.YES,
//				                        Field.Index.UN_TOKENIZED));
//				  doc.add(new Field("description", hotel.getDescription(),
//				                   Field.Store.YES,
//				                   Field.Index.TOKENIZED));
//				  String fullSearchableText
//				        = hotel.getName()
//				         + " " + hotel.getCity() + " " + hotel.getDescription();
//
//				  doc.add(new Field("content", fullSearchableText,
//				                 Field.Store.NO,
//				                 Field.Index.TOKENIZED));
			  
			  indexWriter.addDocument(doc);
		}
		
		indexWriter.close();
	}
	
//	public static void main(String[] args) throws Exception {
//		new SeachIndexerJob().doJob();
//		System.out.println("finished");
//	}

}

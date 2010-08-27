package util.jobs;

import java.io.IOException;

import models.TalkerBean;
import models.TalkerBean.ProfilePreference;
import models.TopicBean;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.LockObtainFailedException;

import dao.TalkerDAO;
import dao.TopicDAO;
import play.jobs.Every;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import util.SearchUtil;

@OnApplicationStart
@Every("5min")
public class SeachIndexerJob extends Job {

	@Override
	public void doJob() throws Exception {
		IndexWriter talkerIndexWriter = new IndexWriter(SearchUtil.SEARCH_INDEX_PATH+"talker", new StandardAnalyzer(), true);
		IndexWriter autocompleteIndexWriter = 
			new IndexWriter(SearchUtil.SEARCH_INDEX_PATH+"autocomplete", new StandardAnalyzer(), true);
		
		for (TalkerBean talker : TalkerDAO.loadAllTalkers()) {
			  Document doc = new Document();
			  doc.add(new Field("id", talker.getId(), Field.Store.YES,
			                      Field.Index.NO));
			  doc.add(new Field("uname", talker.getUserName(), Field.Store.YES,
			                        Field.Index.TOKENIZED));
			  
			  if (talker.isAllowed(ProfilePreference.BIO) && talker.getBio() != null) {
				  doc.add(new Field("bio", talker.getBio(), Field.Store.YES,
	                      Field.Index.TOKENIZED));
			  }
			  
			  //setBoost
			  
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
			  
			  talkerIndexWriter.addDocument(doc);
			  
			  //for autocomplete
			  Document doc2 = new Document();
			  doc2.add(new Field("uname", talker.getUserName(), Field.Store.YES,
                      Field.Index.TOKENIZED));
			  doc2.add(new Field("type", "User", Field.Store.YES, Field.Index.NO));
			  autocompleteIndexWriter.addDocument(doc2);
		}
		
		for (TopicBean topic : TopicDAO.loadAllTopics()) {
			//for autocomplete
			Document doc = new Document();
			doc.add(new Field("title", topic.getTopic(), Field.Store.YES,
					Field.Index.TOKENIZED));
			doc.add(new Field("type", "Conversation", Field.Store.YES, Field.Index.NO));
			doc.add(new Field("url", topic.getMainURL(), Field.Store.YES, Field.Index.NO));
			
			autocompleteIndexWriter.addDocument(doc);
		}
		
		talkerIndexWriter.close();
		autocompleteIndexWriter.close();
	}
	
	public static void main(String[] args) throws Exception {
		new SeachIndexerJob().doJob();
		System.out.println("finished");
	}

}
package util.jobs;


import models.TalkerBean;
import models.PrivacySetting.PrivacyType;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;

import dao.TalkerDAO;

import play.jobs.Every;
import play.jobs.Job;

import util.SearchUtil;

//@Every("10min")
public class SearchTalkerIndexerJob extends Job{
int limit=10;

	@Override
	public void doJob() throws Exception {
	 
		IndexWriter talkerIndexWriter=null;
		IndexWriter autocompleteIndexWriter=null;
		 talkerIndexWriter = new IndexWriter(SearchUtil.SEARCH_INDEX_PATH+"talker", new StandardAnalyzer(), false);
	  	 autocompleteIndexWriter = new IndexWriter(SearchUtil.SEARCH_INDEX_PATH+"autocomplete", new StandardAnalyzer(), false);
	   try {
			for (TalkerBean talker : TalkerDAO.loadUpdatedTalker(limit)) {
				  
				  Document doc = new Document();
				  doc.add(new Field("id", talker.getId(), Field.Store.YES, Field.Index.TOKENIZED));
				  doc.add(new Field("uname", talker.getUserName(), Field.Store.YES, Field.Index.TOKENIZED));
				  if (!talker.isPrivate(PrivacyType.PROFILE_INFO) && talker.getBio() != null) {
					  doc.add(new Field("bio", talker.getBio(), Field.Store.YES,
		                      Field.Index.TOKENIZED));
				  }
				  talkerIndexWriter.addDocument(doc);
				  
				  //for autocomplete
				  doc = new Document();
				  doc.add(new Field("id", talker.getId(), Field.Store.YES, Field.Index.TOKENIZED));
				  doc.add(new Field("uname", talker.getUserName(), Field.Store.YES, Field.Index.TOKENIZED));
				  doc.add(new Field("type", "User", Field.Store.YES, Field.Index.NO));
				  autocompleteIndexWriter.addDocument(doc);
			}
	   }
		finally {
					talkerIndexWriter.close();
					autocompleteIndexWriter.close();
	   }
	
	}
	
	public static void main(String[] args) throws Exception {
		new SearchTalkerIndexerJob().doJob();
	}
	 
}

package util.jobs;


import java.util.Date;

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

public class SearchTalkerIndexerJob extends Job{
	public static void main(String[] args) throws Throwable {
		int limit=10;
		IndexWriter talkerIndexWriter=null;
		IndexWriter autocompleteIndexWriter=null;
		System.out.println("SearchTalkerIndexerJob Started::::"+ new Date());
		talkerIndexWriter = new IndexWriter("/data/searchindex/talker", new StandardAnalyzer(), false);
	  	autocompleteIndexWriter = new IndexWriter("/data/searchindex/autocomplete", new StandardAnalyzer(), false);
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
			System.out.println("SearchTalkerIndexerJob Completed::::"+ new Date());
			SearchTalkerIndexerJob talkerJob = new SearchTalkerIndexerJob();
			talkerJob.finalize();
	   }
	}
	
	protected void finalize() throws Throwable {
		super.finalize();
	}
	 
}

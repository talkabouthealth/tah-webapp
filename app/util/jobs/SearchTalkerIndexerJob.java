package util.jobs;


import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.ConversationBean;
import models.TalkerBean;
import models.PrivacySetting.PrivacyType;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import play.jobs.Job;
import dao.TalkerDAO;

public class SearchTalkerIndexerJob extends Job{
	public static void main(String[] args) throws Throwable {
		int limit=10;
		
		File autocompleteIndexerFile = new File("/data/searchindex/autocomplete");
 		Directory autocompleteIndexDir = FSDirectory.open(autocompleteIndexerFile);
 		
 		File talkerIndexerFile = new File("/data/searchindex/talker");
 		Directory talkerIndexDir = FSDirectory.open(talkerIndexerFile);
 		
		System.out.println("SearchConvoIndexerJob Started::::"+ new Date());
		IndexWriter talkerIndexWriter = new IndexWriter(talkerIndexDir, new StandardAnalyzer(Version.LUCENE_36), false,MaxFieldLength.UNLIMITED) ;
		IndexWriter autocompleteIndexWriter = new IndexWriter(autocompleteIndexDir, new StandardAnalyzer(Version.LUCENE_36), false,MaxFieldLength.UNLIMITED);
		
		System.out.println("SearchTalkerIndexerJob Started::::"+ new Date());
	  	System.out.println("Creating talker indexes::::");
	  	try {
	  		List<TalkerBean> talkerList = TalkerDAO.loadUpdatedTalker(limit);
	  		ArrayList<Document> talkerIndex = new ArrayList<Document>();
	  		ArrayList<Document> autoTalkerIndex = new ArrayList<Document>();
			for (TalkerBean talker : talkerList) {

				  Document doc = new Document();
				  doc.add(new Field("id", talker.getId(), Field.Store.YES, Field.Index.ANALYZED));
				  doc.add(new Field("uname", talker.getUserName(), Field.Store.YES, Field.Index.ANALYZED));
				  if(StringUtils.isNotBlank(talker.getCategory())) {
					  doc.add(new Field("category", talker.getCategory(), Field.Store.YES, Field.Index.ANALYZED));
				  } else {
					  doc.add(new Field("category", ConversationBean.ALL_CANCERS, Field.Store.YES, Field.Index.ANALYZED));
				  }
				  if (!talker.isPrivate(PrivacyType.PROFILE_INFO) && talker.getBio() != null) {
					  doc.add(new Field("bio", talker.getBio(), Field.Store.YES, Field.Index.ANALYZED));
				  }
				  talkerIndex.add(doc);
				  //talkerIndexWriter.addDocument(doc);

				  //for autocomplete
				  doc = new Document();
				  doc.add(new Field("id", talker.getId(), Field.Store.YES, Field.Index.ANALYZED));
				  doc.add(new Field("uname", talker.getUserName(), Field.Store.YES, Field.Index.ANALYZED));
				  if(StringUtils.isNotBlank(talker.getCategory())) {
					  doc.add(new Field("category", talker.getCategory(), Field.Store.YES, Field.Index.ANALYZED));
				  } else {
					  doc.add(new Field("category", ConversationBean.ALL_CANCERS, Field.Store.YES, Field.Index.ANALYZED));
				  }
				  doc.add(new Field("type", "User", Field.Store.YES, Field.Index.NO));
				  //autocompleteIndexWriter.addDocument(doc);
				  autoTalkerIndex.add(doc);
			}
			if(talkerIndex.size()>0)
				talkerIndexWriter.addDocuments(talkerIndex);
			if(autoTalkerIndex.size()>0)
				autocompleteIndexWriter.addDocuments(autoTalkerIndex);

			System.out.println("Completed talker indexes::::");
			talkerList.clear();
			talkerIndex.clear();
			autoTalkerIndex.clear();
	  	} finally {
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

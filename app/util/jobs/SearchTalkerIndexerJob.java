package util.jobs;


import static util.DBUtil.getCollection;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import models.ConversationBean;
import models.TalkerBean;
import models.PrivacySetting.PrivacyType;
import models.PrivacySetting.PrivacyValue;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import play.jobs.Job;
import dao.NewsLetterDAO;
import dao.TalkerDAO;

@SuppressWarnings("deprecation")
public class SearchTalkerIndexerJob extends Job{
	public static void main(String[] args) throws Throwable {
		int limit=10;
		popupateDB();

		//For Local
		//String searchIndexPath = "/home/avibha/Documents/db/data/searchindex/";

		//For Live/QA
		String searchIndexPath = "/data/searchindex/";
		
		File autocompleteIndexerFile = new File(searchIndexPath + "autocomplete");
 		Directory autocompleteIndexDir = FSDirectory.open(autocompleteIndexerFile);
 		
 		File talkerIndexerFile = new File(searchIndexPath + "talker");
 		Directory talkerIndexDir = FSDirectory.open(talkerIndexerFile);
 		
		IndexWriter talkerIndexWriter = new IndexWriter(talkerIndexDir, new StandardAnalyzer(Version.LUCENE_36), false,MaxFieldLength.UNLIMITED) ;
		IndexWriter autocompleteIndexWriter = new IndexWriter(autocompleteIndexDir, new StandardAnalyzer(Version.LUCENE_36), false,MaxFieldLength.UNLIMITED);
		
		System.out.println("SearchTalkerIndexerJob Started: "+ new Date());
	  	try {
	  		List<TalkerBean> talkerList = TalkerDAO.loadUpdatedTalker(limit);
	  		ArrayList<Document> talkerIndex = new ArrayList<Document>();
	  		ArrayList<Document> autoTalkerIndex = new ArrayList<Document>();
	  		int count = 0;
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
				  if(PrivacyValue.PRIVATE.equals(talker.getPrivacyValue(PrivacyType.PROFILE_INFO))) {
					  doc.add(new Field("profile", "1", Field.Store.YES, Field.Index.ANALYZED));
				  } else {
					  doc.add(new Field("profile", "0", Field.Store.YES, Field.Index.ANALYZED));
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
				  if(PrivacyValue.PRIVATE.equals(talker.getPrivacyValue(PrivacyType.PROFILE_INFO))) {
					  doc.add(new Field("profile", "1", Field.Store.YES, Field.Index.ANALYZED));
				  } else {
					  doc.add(new Field("profile", "0", Field.Store.YES, Field.Index.ANALYZED));
				  }
				  doc.add(new Field("type", "User", Field.Store.YES, Field.Index.NO));
				  //autocompleteIndexWriter.addDocument(doc);
				  autoTalkerIndex.add(doc);
				  count++;
			}
			if(talkerIndex.size()>0)
				talkerIndexWriter.addDocuments(talkerIndex);
			if(autoTalkerIndex.size()>0)
				autocompleteIndexWriter.addDocuments(autoTalkerIndex);

			System.out.println("Completed talker indexes: " + count);
			talkerList.clear();
			talkerIndex.clear();
			autoTalkerIndex.clear();
	  	} finally {
			talkerIndexWriter.close();
			autocompleteIndexWriter.close();
			System.out.println("SearchTalkerIndexerJob Completed: "+ new Date());
			SearchTalkerIndexerJob talkerJob = new SearchTalkerIndexerJob();
			talkerJob.finalize();
	   }
	}

	private static void popupateDB() {
		DBCollection namesColl = getCollection("schedulerStat");
		DBObject waitingDBObject = BasicDBObjectBuilder.start()
				.add("scheduleType", "TALKER")
				.add("timestamp", Calendar.getInstance().getTime())
				.get();
		namesColl.save(waitingDBObject);
	}

	protected void finalize() throws Throwable {
		super.finalize();
	}
}

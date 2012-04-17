package util.jobs;

import java.io.File;
import java.util.Date;
import java.util.List;

import models.CommentBean;
import models.ConversationBean;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import dao.CommentsDAO;
import dao.ConversationDAO;

public class SearchConvoIndexerJob{
	
	public static void main(String[] args) throws Throwable {
		int limit=10;
		
		File autocompleteIndexerFile = new File("/data/searchindex/autocomplete");
 		Directory autocompleteIndexDir = FSDirectory.open(autocompleteIndexerFile);
 		File convoIndexerFile = new File("/data/searchindex/conversations");
 		Directory convoIndexDir = FSDirectory.open(convoIndexerFile);
		System.out.println("SearchConvoIndexerJob Started::::"+ new Date());
		IndexWriter convoIndexWriter = new IndexWriter(convoIndexDir, new StandardAnalyzer(Version.LUCENE_36), false,MaxFieldLength.UNLIMITED) ;
		//IndexWriter autocompleteIndexWriter = new IndexWriter(autocompleteIndexDir, autocompleteIndexWriterConfig);
		IndexWriter autocompleteIndexWriter = new IndexWriter(autocompleteIndexDir, new StandardAnalyzer(Version.LUCENE_36), false,MaxFieldLength.UNLIMITED);
			 
		 try{
			 System.out.println("Creating convo indexes::::");
			 for (ConversationBean convo : ConversationDAO.loadUpdatedConversations(limit)) {
				//possibly weight titles, conversation details, summaries, and answers more than the archived real-time conversations?
				
				Document doc = new Document();
				doc.add(new Field("id", convo.getId(), Field.Store.YES, Field.Index.ANALYZED));
				doc.add(new Field("title", convo.getTopic(), Field.Store.YES, Field.Index.ANALYZED));
				
				//add an answer, reply, or live conversation text ?
				List<CommentBean> answersList = CommentsDAO.loadConvoAnswersTreeForScheduler(convo.getId());
				StringBuilder answersString = new StringBuilder();
				for (CommentBean answer : answersList) {
					if (!answer.isDeleted()) {
						answersString.append(answer.getText());
					}
				}
				doc.add(new Field("answers", answersString.toString(), Field.Store.NO, Field.Index.ANALYZED));
				convoIndexWriter.addDocument(doc);
				  
						
				//for autocomplete
				doc = new Document();
				doc.add(new Field("id", convo.getId(), Field.Store.YES, Field.Index.ANALYZED));
				doc.add(new Field("title", convo.getTopic(), Field.Store.YES, Field.Index.ANALYZED));
				doc.add(new Field("type", "Conversation", Field.Store.YES, Field.Index.NO));
				if (convo.getMainURL() != null) {
					doc.add(new Field("url", convo.getMainURL(), Field.Store.YES, Field.Index.NO));
				}
				autocompleteIndexWriter.addDocument(doc);
				System.out.println("Completed convo indexes::::");
			}
		 }
		 finally{
			convoIndexWriter.close();
			autocompleteIndexWriter.close();
			System.out.println("SearchConvoIndexerJob Completed::::"+ new Date());
			SearchConvoIndexerJob convoJob = new SearchConvoIndexerJob();
			convoJob.finalize();
		 }
	}
	
	protected void finalize() throws Throwable {
		super.finalize();
	}
	
}

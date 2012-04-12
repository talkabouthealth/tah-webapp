package util.jobs;

import java.util.Date;
import java.util.List;

import models.CommentBean;
import models.ConversationBean;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;

import dao.CommentsDAO;
import dao.ConversationDAO;

import play.jobs.Every;
import play.jobs.Job;

import util.SearchUtil;

public class SearchConvoIndexerJob{
	
	public static void main(String[] args) throws Throwable {
		int limit=10;
		IndexWriter convoIndexWriter=null;
		IndexWriter autocompleteIndexWriter=null;
		System.out.println("SearchConvoIndexerJob Started::::"+ new Date());
		convoIndexWriter = new IndexWriter("/data/searchindex/conversations", new StandardAnalyzer(), false);
		autocompleteIndexWriter = new IndexWriter("/data/searchindex/autocomplete", new StandardAnalyzer(), false);
			 
		 try{
			 for (ConversationBean convo : ConversationDAO.loadUpdatedConversations(limit)) {
				//possibly weight titles, conversation details, summaries, and answers more than the archived real-time conversations?
				
				Document doc = new Document();
				doc.add(new Field("id", convo.getId(), Field.Store.YES, Field.Index.TOKENIZED));
				doc.add(new Field("title", convo.getTopic(), Field.Store.YES, Field.Index.TOKENIZED));
				
				//add an answer, reply, or live conversation text ?
				List<CommentBean> answersList = CommentsDAO.loadConvoAnswersTree(convo.getId());
				StringBuilder answersString = new StringBuilder();
				for (CommentBean answer : answersList) {
					if (!answer.isDeleted()) {
						answersString.append(answer.getText());
					}
				}
				doc.add(new Field("answers", answersString.toString(), Field.Store.NO, Field.Index.TOKENIZED));
				convoIndexWriter.addDocument(doc);
				  
						
				//for autocomplete
				doc = new Document();
				doc.add(new Field("id", convo.getId(), Field.Store.YES, Field.Index.TOKENIZED));
				doc.add(new Field("title", convo.getTopic(), Field.Store.YES, Field.Index.TOKENIZED));
				doc.add(new Field("type", "Conversation", Field.Store.YES, Field.Index.NO));
				if (convo.getMainURL() != null) {
					doc.add(new Field("url", convo.getMainURL(), Field.Store.YES, Field.Index.NO));
				}
				autocompleteIndexWriter.addDocument(doc);
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

package util.jobs;

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

@Every("13min")
public class SearchConvoIndexerJob extends Job{
	int limit=10;
	@Override
	public void doJob() throws Exception {
		
		IndexWriter convoIndexWriter=null;
		IndexWriter autocompleteIndexWriter=null;
		
		convoIndexWriter = new IndexWriter(SearchUtil.SEARCH_INDEX_PATH+"conversations", new StandardAnalyzer(), false);
		autocompleteIndexWriter = new IndexWriter(SearchUtil.SEARCH_INDEX_PATH+"autocomplete", new StandardAnalyzer(), false);
			 
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
		 }
			 
	}
	
	public static void main(String[] args) throws Exception {
		new SearchConvoIndexerJob().doJob();
	}
	
}

package util.jobs;

import java.io.IOException;
import java.util.List;

import models.CommentBean;
import models.TalkerBean;
import models.TalkerBean.ProfilePreference;
import models.ConversationBean;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.LockObtainFailedException;

import dao.CommentsDAO;
import dao.TalkerDAO;
import dao.ConversationDAO;
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
		IndexWriter convoIndexWriter = new IndexWriter(SearchUtil.SEARCH_INDEX_PATH+"conversations", new StandardAnalyzer(), true);
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
		
		for (ConversationBean convo : ConversationDAO.loadAllTopics()) {
//			possibly weight titles, conversation details, summaries, and answers more than the archived real-time conversations?
			
			//TODO: should check all tree? (not only top answers)
			List<CommentBean> answersList = CommentsDAO.loadConvoAnswers(convo.getId());
			
			Document doc = new Document();
			doc.add(new Field("id", convo.getId(), Field.Store.YES, Field.Index.NO));
			doc.add(new Field("title", convo.getTopic(), Field.Store.YES, Field.Index.TOKENIZED));
			StringBuilder answersString = new StringBuilder();
			for (CommentBean answer : answersList) {
				answersString.append(answer.getText());
			}
			doc.add(new Field("answers", answersString.toString(), Field.Store.NO, Field.Index.TOKENIZED));
			convoIndexWriter.addDocument(doc);
			  
					
			//for autocomplete
			Document doc2 = new Document();
			doc2.add(new Field("title", convo.getTopic(), Field.Store.YES, Field.Index.TOKENIZED));
			doc2.add(new Field("type", "Conversation", Field.Store.YES, Field.Index.NO));
			//TODO: url can be changed after indexing?
			doc2.add(new Field("url", convo.getMainURL(), Field.Store.YES, Field.Index.NO));
			
			autocompleteIndexWriter.addDocument(doc2);
		}
		
		talkerIndexWriter.close();
		convoIndexWriter.close();
		autocompleteIndexWriter.close();
	}
	
	public static void main(String[] args) throws Exception {
		new SeachIndexerJob().doJob();
		System.out.println("finished");
	}

}

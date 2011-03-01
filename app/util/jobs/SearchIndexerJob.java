package util.jobs;

import java.io.IOException;
import java.util.List;

import models.CommentBean;
import models.PrivacySetting.PrivacyType;
import models.TalkerBean;
import models.ConversationBean;
import models.TopicBean;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.LockObtainFailedException;

import dao.CommentsDAO;
import dao.TalkerDAO;
import dao.ConversationDAO;
import dao.TopicDAO;
import play.Logger;
import play.jobs.Every;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import util.SearchUtil;

/**
 * Updates all search indexes
 *
 */
@Every("10min")
public class SearchIndexerJob extends Job {

	@Override
	public void doJob() throws Exception {
		IndexWriter talkerIndexWriter = new IndexWriter(SearchUtil.SEARCH_INDEX_PATH+"talker", new StandardAnalyzer(), true);
		IndexWriter convoIndexWriter = new IndexWriter(SearchUtil.SEARCH_INDEX_PATH+"conversations", new StandardAnalyzer(), true);
		IndexWriter autocompleteIndexWriter = 
			new IndexWriter(SearchUtil.SEARCH_INDEX_PATH+"autocomplete", new StandardAnalyzer(), true);
		
		try {
			for (TalkerBean talker : TalkerDAO.loadAllTalkers()) {
				  if (talker.isSuspended()) {
					  continue;
				  }
				  
				  Document doc = new Document();
				  doc.add(new Field("id", talker.getId(), Field.Store.YES, Field.Index.NO));
				  doc.add(new Field("uname", talker.getUserName(), Field.Store.YES, Field.Index.TOKENIZED));
				  if (!talker.isPrivate(PrivacyType.PROFILE_INFO) && talker.getBio() != null) {
					  doc.add(new Field("bio", talker.getBio(), Field.Store.YES,
		                      Field.Index.TOKENIZED));
				  }
				  talkerIndexWriter.addDocument(doc);
				  
				  //for autocomplete
				  doc = new Document();
				  doc.add(new Field("uname", talker.getUserName(), Field.Store.YES, Field.Index.TOKENIZED));
				  doc.add(new Field("type", "User", Field.Store.YES, Field.Index.NO));
				  autocompleteIndexWriter.addDocument(doc);
			}
			
			for (ConversationBean convo : ConversationDAO.loadAllConversations()) {
	//			possibly weight titles, conversation details, summaries, and answers more than the archived real-time conversations?
				if (convo.isDeleted()) {
					continue;
				}
				
				Document doc = new Document();
				doc.add(new Field("id", convo.getId(), Field.Store.YES, Field.Index.NO));
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
				doc.add(new Field("title", convo.getTopic(), Field.Store.YES, Field.Index.TOKENIZED));
				doc.add(new Field("type", "Conversation", Field.Store.YES, Field.Index.NO));
				if (convo.getMainURL() != null) {
					doc.add(new Field("url", convo.getMainURL(), Field.Store.YES, Field.Index.NO));
				}
				autocompleteIndexWriter.addDocument(doc);
			}
			
			for (TopicBean topic : TopicDAO.loadAllTopics()) {
				if (topic.isDeleted()) {
					continue;
				}
				
				Document doc = new Document();
				doc.add(new Field("id", topic.getId(), Field.Store.YES, Field.Index.NO));
				doc.add(new Field("title", topic.getTitle(), Field.Store.YES, Field.Index.TOKENIZED));
				doc.add(new Field("type", "Topic", Field.Store.YES, Field.Index.NO));
				doc.add(new Field("url", topic.getMainURL(), Field.Store.YES, Field.Index.NO));
				autocompleteIndexWriter.addDocument(doc);
			}
		}
		finally {
			talkerIndexWriter.close();
			convoIndexWriter.close();
			autocompleteIndexWriter.close();
		}
	}
	
	public static void main(String[] args) throws Exception {
		new SearchIndexerJob().doJob();
	}

}

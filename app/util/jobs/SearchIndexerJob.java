package util.jobs;

import java.util.Date;
import java.util.List;
import java.util.Set;

import models.CommentBean;
import models.ConversationBean;
import models.TalkerBean;
import models.TopicBean;
import models.PrivacySetting.PrivacyType;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;

import play.jobs.Every;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import util.SearchUtil;
import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;
import dao.TopicDAO;

/**
 * Updates all search indexes
 *
 */
public class SearchIndexerJob extends Throwable{

	public static void main(String[] args) throws Throwable {
		System.out.println("SearchIndexerJob Started::::"+ new Date());
		IndexWriter talkerIndexWriter = new IndexWriter("/data/searchindex/talker", new StandardAnalyzer(), true);
		IndexWriter convoIndexWriter = new IndexWriter("/data/searchindex/conversations", new StandardAnalyzer(), true);
		IndexWriter autocompleteIndexWriter = new IndexWriter("/data/searchindex/autocomplete", new StandardAnalyzer(), true);
		
		try {
			List<TalkerBean> allTalkers = TalkerDAO.loadAllTalkers(true);
			for (TalkerBean talker : allTalkers) {
				  if (talker.isSuspended()) {
					  continue;
				  }
				  
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
			allTalkers.clear();
			List<ConversationBean> allConvos = ConversationDAO.getAllConvosForScheduler();
			for (ConversationBean convo : allConvos) {
	//			possibly weight titles, conversation details, summaries, and answers more than the archived real-time conversations?
				if (convo.isDeleted()) {
					continue;
				}
				
				Document doc = new Document();
				doc.add(new Field("id", convo.getId(), Field.Store.YES, Field.Index.TOKENIZED));
				doc.add(new Field("title", convo.getTopic(), Field.Store.YES, Field.Index.TOKENIZED));
				
				//add an answer, reply, or live conversation text ?
				List<CommentBean> answersList = CommentsDAO.loadConvoAnswersTreeForScheduler(convo.getId());
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
			allConvos.clear();
			Set<TopicBean> allTopics = TopicDAO.loadAllTopics(true);
			for (TopicBean topic : allTopics) {
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
			allTopics.clear();
		}
		finally {
			talkerIndexWriter.close();
			convoIndexWriter.close();
			autocompleteIndexWriter.close();
			System.out.println("SearchIndexerJob Completed::::"+new Date());
			SearchIndexerJob serachJob = new SearchIndexerJob();
			serachJob.finalize();
		}
		
	}
	
	protected void finalize() throws Throwable {
		super.finalize();
	}
}

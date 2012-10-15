package util.jobs;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Set;

import models.CommentBean;
import models.ConversationBean;
import models.TalkerBean;
import models.TopicBean;
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
		File autocompleteIndexerFile = new File("/data/searchindex/autocomplete");
 		Directory autocompleteIndexDir = FSDirectory.open(autocompleteIndexerFile);
		
 		File convoIndexerFile = new File("/data/searchindex/conversations");
 		Directory convoIndexDir = FSDirectory.open(convoIndexerFile);
 		File talkerIndexerFile = new File("/data/searchindex/talker");
 		Directory talkerIndexDir = FSDirectory.open(talkerIndexerFile);
		
		
		IndexWriter autocompleteIndexWriter = new IndexWriter(autocompleteIndexDir, new StandardAnalyzer(Version.LUCENE_36), true,MaxFieldLength.UNLIMITED);
		IndexWriter convoIndexWriter = new IndexWriter(convoIndexDir, new StandardAnalyzer(Version.LUCENE_36), true,MaxFieldLength.UNLIMITED) ;
		IndexWriter talkerIndexWriter = new IndexWriter(talkerIndexDir, new StandardAnalyzer(Version.LUCENE_36), true,MaxFieldLength.UNLIMITED) ;
 		
		try {
			List<TalkerBean> allTalkers = TalkerDAO.loadAllTalkers(true);
			System.out.println("Creating talker indexes::::");
			for (TalkerBean talker : allTalkers) {
				  if (talker.isSuspended()) {
					  continue;
				  }
				  if(talker.getUserName()== null)
					  continue;
				  Document doc = new Document();
				  doc.add(new Field("id", talker.getId(), Field.Store.YES, Field.Index.ANALYZED));
				  doc.add(new Field("uname", talker.getUserName(), Field.Store.YES, Field.Index.ANALYZED));
				  if(StringUtils.isNotBlank(talker.getCategory())){
					  doc.add(new Field("category", talker.getCategory(), Field.Store.YES, Field.Index.ANALYZED));
				  }else{
					  doc.add(new Field("category", "All Cancer", Field.Store.YES, Field.Index.ANALYZED));
				  }
					  
				  if (!talker.isPrivate(PrivacyType.PROFILE_INFO) && talker.getBio() != null) {
					  doc.add(new Field("bio", talker.getBio(), Field.Store.YES,
		                      Field.Index.ANALYZED));
				  }
				  talkerIndexWriter.addDocument(doc);
				  
				  //for autocomplete
				  doc = new Document();
				  doc.add(new Field("id", talker.getId(), Field.Store.YES, Field.Index.ANALYZED));
				  doc.add(new Field("uname", talker.getUserName(), Field.Store.YES, Field.Index.ANALYZED));
				  if(StringUtils.isNotBlank(talker.getCategory())){
					  doc.add(new Field("category", talker.getCategory(), Field.Store.YES, Field.Index.ANALYZED));
				  }else{
					  doc.add(new Field("category", ALL_CANCERS, Field.Store.YES, Field.Index.ANALYZED));
				  }
				  doc.add(new Field("type", "User", Field.Store.YES, Field.Index.NO));
				  autocompleteIndexWriter.addDocument(doc);
			}
			allTalkers.clear();
			System.out.println("Completed talker indexes::::");
			System.out.println("Creating convo indexes::::");
			List<ConversationBean> allConvos = ConversationDAO.getAllConvosForScheduler();
			for (ConversationBean convo : allConvos) {
	//			possibly weight titles, conversation details, summaries, and answers more than the archived real-time conversations?
				if (convo.isDeleted()) {
					continue;
				}
				
				Document doc = new Document();
				doc.add(new Field("id", convo.getId(), Field.Store.YES, Field.Index.ANALYZED));
				doc.add(new Field("title", convo.getTopic(), Field.Store.YES, Field.Index.ANALYZED));
				
				if(StringUtils.isNotBlank(convo.getCategory())){
					doc.add(new Field("category", convo.getCategory(), Field.Store.YES, Field.Index.ANALYZED));
				} else {
					doc.add(new Field("category", ALL_CANCERS, Field.Store.YES, Field.Index.ANALYZED));
				}
				
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
				if(StringUtils.isNotBlank(convo.getCategory())){
					doc.add(new Field("category", convo.getCategory(), Field.Store.YES, Field.Index.ANALYZED));
				} else {
					doc.add(new Field("category", ALL_CANCERS, Field.Store.YES, Field.Index.ANALYZED));
				}
				doc.add(new Field("type", "Conversation", Field.Store.YES, Field.Index.NO));
				if (convo.getMainURL() != null) {
					doc.add(new Field("url", convo.getMainURL(), Field.Store.YES, Field.Index.NO));
				}
				autocompleteIndexWriter.addDocument(doc);
			}
			allConvos.clear();
			System.out.println("Completed convo indexes::::");
			
			System.out.println("Creating topic indexes::::");
			Set<TopicBean> allTopics = TopicDAO.loadAllTopics(true);
			for (TopicBean topic : allTopics) {
				if (topic.isDeleted()) {
					continue;
				}
				
				Document doc = new Document();
				doc.add(new Field("id", topic.getId(), Field.Store.YES, Field.Index.NO));
				doc.add(new Field("title", topic.getTitle(), Field.Store.YES, Field.Index.ANALYZED));
				doc.add(new Field("type", "Topic", Field.Store.YES, Field.Index.NO));
				doc.add(new Field("url", topic.getMainURL(), Field.Store.YES, Field.Index.NO));
				autocompleteIndexWriter.addDocument(doc);
			}
			allTopics.clear();
			System.out.println("Completed topic indexes::::");
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
	
	private static String ALL_CANCERS = "All Cancers";
}

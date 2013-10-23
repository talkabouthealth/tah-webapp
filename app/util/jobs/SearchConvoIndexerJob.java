package util.jobs;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.CommentBean;
import models.ConversationBean;
import models.TopicBean;

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

public class SearchConvoIndexerJob{
	
	public static void main(String[] args) throws Throwable {
		int limit=10;
		
		File autocompleteIndexerFile = new File("/data/searchindex/autocomplete");
 		Directory autocompleteIndexDir = FSDirectory.open(autocompleteIndexerFile);
 		
 		File convoIndexerFile = new File("/data/searchindex/conversations");
 		Directory convoIndexDir = FSDirectory.open(convoIndexerFile);
 		
		System.out.println("SearchConvoIndexerJob Started::::"+ new Date());
		IndexWriter convoIndexWriter = new IndexWriter(convoIndexDir, new StandardAnalyzer(Version.LUCENE_36), false,MaxFieldLength.UNLIMITED) ;
		IndexWriter autocompleteIndexWriter = new IndexWriter(autocompleteIndexDir, new StandardAnalyzer(Version.LUCENE_36), false,MaxFieldLength.UNLIMITED);
			 
		 try {
			 System.out.println("Creating convo indexes::::");
			 List<ConversationBean> convoList =  ConversationDAO.loadUpdatedConversations(limit);
			 ArrayList<Document> convoIndex = new ArrayList<Document>();
		  	 ArrayList<Document> autoConvoIndex = new ArrayList<Document>();
			 for (ConversationBean convo : convoList) {
				//possibly weight titles, conversation details, summaries, and answers more than the archived real-time conversations?
				List<CommentBean> answersList = CommentsDAO.loadConvoAnswersTreeForScheduler(convo.getId());
				if (convo.isDeleted() || (answersList==null ||  answersList.size()==0)) {
					continue;
				}
				Document doc = new Document();
				doc.add(new Field("id", convo.getId(), Field.Store.YES, Field.Index.ANALYZED));
				doc.add(new Field("title", convo.getTopic(), Field.Store.YES, Field.Index.ANALYZED));
				if(StringUtils.isNotBlank(convo.getCategory())){
					doc.add(new Field("category", convo.getCategory(), Field.Store.YES, Field.Index.ANALYZED));
				} else {
					doc.add(new Field("category", ConversationBean.ALL_CANCERS, Field.Store.YES, Field.Index.ANALYZED));
				}
				//add an answer, reply, or live conversation text ?
				StringBuilder answersString = new StringBuilder();
				for (CommentBean answer : answersList) {
					if (!answer.isDeleted()) {
						answersString.append(answer.getText());
					}
				}
				doc.add(new Field("answers", answersString.toString(), Field.Store.NO, Field.Index.ANALYZED));
				String topic = "";
				if(convo.getTopics() != null && !convo.getTopics().isEmpty()) {
					for (TopicBean topicBean : convo.getTopics()) {
						topic = topic + topicBean.getTitle() + " ";
					}
			 	}
				doc.add(new Field("topics", topic, Field.Store.YES, Field.Index.ANALYZED));
				//convoIndexWriter.addDocument(doc);
				convoIndex.add(doc);
				//for autocomplete
				doc = new Document();
				doc.add(new Field("id", convo.getId(), Field.Store.YES, Field.Index.ANALYZED));
				doc.add(new Field("title", convo.getTopic(), Field.Store.YES, Field.Index.ANALYZED));
				if(StringUtils.isNotBlank(convo.getCategory())){
					doc.add(new Field("category", convo.getCategory(), Field.Store.YES, Field.Index.ANALYZED));
				} else {
					doc.add(new Field("category", ConversationBean.ALL_CANCERS, Field.Store.YES, Field.Index.ANALYZED));
				}
				doc.add(new Field("profile", "0", Field.Store.YES, Field.Index.ANALYZED));
				doc.add(new Field("topics", topic, Field.Store.YES, Field.Index.ANALYZED));
				
				doc.add(new Field("type", "Conversation", Field.Store.YES, Field.Index.NO));
				if (convo.getMainURL() != null) {
					doc.add(new Field("url", convo.getMainURL(), Field.Store.YES, Field.Index.NO));
				}
				//autocompleteIndexWriter.addDocument(doc);
				autoConvoIndex.add(doc);
				System.out.println("Completed convo indexes::::");
			}
			 if(convoIndex.size()>0)
				 convoIndexWriter.addDocuments(convoIndex);
			 if(autoConvoIndex.size()>0)
				 autocompleteIndexWriter.addDocuments(autoConvoIndex);
			 convoIndex.clear();
			 autoConvoIndex.clear();
			 convoList.clear();
		 } finally {
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

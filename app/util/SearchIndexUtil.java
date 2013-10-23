package util;

import java.io.File;
import java.io.IOException;

import models.TalkerBean;
import models.PrivacySetting.PrivacyType;
import models.PrivacySetting.PrivacyValue;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class SearchIndexUtil {

	/**
	 * Method will populate user in search index
	 * @return boolean
	 * @throws IOException 
	 * @throws CorruptIndexException 
	 */
	public static boolean populateTalkerSearchIndex(TalkerBean talker) {
		boolean returnFlag = true;
		Document doc = new Document();
		try {
			//For auto Complete
			File autocompleteIndexerFile = new File(SearchUtil.SEARCH_INDEX_PATH+"autocomplete");
			Directory autocompleteIndexDir = FSDirectory.open(autocompleteIndexerFile);
			IndexWriter autocompleteIndexWriter = new IndexWriter(autocompleteIndexDir, new IndexWriterConfig(Version.LUCENE_36,new StandardAnalyzer(Version.LUCENE_36)));
			doc.add(new Field("id", talker.getId(), Field.Store.YES, Field.Index.ANALYZED));
			doc.add(new Field("uname", talker.getUserName(), Field.Store.YES, Field.Index.ANALYZED));
			if(StringUtils.isNotBlank(talker.getCategory())){
				  doc.add(new Field("category", talker.getCategory(), Field.Store.YES, Field.Index.ANALYZED));
			} else {
				  doc.add(new Field("category", "All Cancer", Field.Store.YES, Field.Index.ANALYZED));
			}
			doc.add(new Field("type", "User", Field.Store.YES, Field.Index.NO));
			autocompleteIndexWriter.addDocument(doc);
			autocompleteIndexWriter.close();

			//For talker index
			File talkerIndexerFile = new File(SearchUtil.SEARCH_INDEX_PATH + "talker");
	 		Directory talkerIndexDir = FSDirectory.open(talkerIndexerFile);
			IndexWriter talkerIndexWriter = new IndexWriter(talkerIndexDir, new IndexWriterConfig(Version.LUCENE_36,new StandardAnalyzer(Version.LUCENE_36)));
			doc = new Document();
			doc.add(new Field("id", talker.getId(), Field.Store.YES, Field.Index.ANALYZED));
			doc.add(new Field("uname", talker.getUserName(), Field.Store.YES, Field.Index.ANALYZED));
			if(StringUtils.isNotBlank(talker.getCategory())){
				  doc.add(new Field("category", talker.getCategory(), Field.Store.YES, Field.Index.ANALYZED));
			} else {
				  doc.add(new Field("category", "All Cancer", Field.Store.YES, Field.Index.ANALYZED));
			}
			if (!talker.isPrivate(PrivacyType.PROFILE_INFO) && talker.getBio() != null) {
				doc.add(new Field("bio", talker.getBio(), Field.Store.YES,Field.Index.ANALYZED));
			}
			
			if(PrivacyValue.PRIVATE.equals(talker.getPrivacyValue(PrivacyType.PROFILE_INFO))) {
				  doc.add(new Field("profile", "1", Field.Store.YES, Field.Index.ANALYZED));
			  } else {
				  doc.add(new Field("profile", "0", Field.Store.YES, Field.Index.ANALYZED));
			  }
			talkerIndexWriter.addDocument(doc);
			talkerIndexWriter.close();

		} catch(Exception e) {
			e.printStackTrace();
			returnFlag = false;
		}
		return returnFlag;
	}
	
	public static boolean modifyTalkerSearchIndex(TalkerBean talker) {
		boolean returnFlag = true;
		try {
			File autocompleteIndexerFile = new File(SearchUtil.SEARCH_INDEX_PATH+"autocomplete");
			Directory autocompleteIndexDir = FSDirectory.open(autocompleteIndexerFile);
			IndexWriter autocompleteIndexWriter = new IndexWriter(autocompleteIndexDir, new IndexWriterConfig(Version.LUCENE_36,new StandardAnalyzer(Version.LUCENE_36)));
			//Delete Old index
			autocompleteIndexWriter.deleteDocuments(new Term("id", talker.getId()));

			Document doc = new Document();
			doc.add(new Field("id", talker.getId(), Field.Store.YES, Field.Index.ANALYZED));
			doc.add(new Field("uname", talker.getUserName(), Field.Store.YES, Field.Index.ANALYZED));
			if(StringUtils.isNotBlank(talker.getCategory())) {
				  doc.add(new Field("category", talker.getCategory(), Field.Store.YES, Field.Index.ANALYZED));
			} else {
				  doc.add(new Field("category", "All Cancer", Field.Store.YES, Field.Index.ANALYZED));
			}
			doc.add(new Field("type", "User", Field.Store.YES, Field.Index.NO));
			autocompleteIndexWriter.addDocument(doc);
			autocompleteIndexWriter.close();

			//For talker Index
			File talkerIndexerFile = new File(SearchUtil.SEARCH_INDEX_PATH + "talker");
			Directory talkerIndexDir = FSDirectory.open(talkerIndexerFile);
			IndexWriter talkerIndexWriter = new IndexWriter(talkerIndexDir, new IndexWriterConfig(Version.LUCENE_36,new StandardAnalyzer(Version.LUCENE_36)));
			//Delete Old index
			talkerIndexWriter.deleteDocuments(new Term("id", talker.getId()));

			doc = new Document();
			doc.add(new Field("id", talker.getId(), Field.Store.YES, Field.Index.ANALYZED));
			doc.add(new Field("uname", talker.getUserName(), Field.Store.YES, Field.Index.ANALYZED));
			if(StringUtils.isNotBlank(talker.getCategory())){
				  doc.add(new Field("category", talker.getCategory(), Field.Store.YES, Field.Index.ANALYZED));
			} else {
				  doc.add(new Field("category", "All Cancer", Field.Store.YES, Field.Index.ANALYZED));
			}
			if (!talker.isPrivate(PrivacyType.PROFILE_INFO) && talker.getBio() != null) {
				doc.add(new Field("bio", talker.getBio(), Field.Store.YES,Field.Index.ANALYZED));
			}
			
			if(PrivacyValue.PRIVATE.equals(talker.getPrivacyValue(PrivacyType.PROFILE_INFO))) {
				  doc.add(new Field("profile", "1", Field.Store.YES, Field.Index.ANALYZED));
			  } else {
				  doc.add(new Field("profile", "0", Field.Store.YES, Field.Index.ANALYZED));
			  }
			talkerIndexWriter.addDocument(doc);
			talkerIndexWriter.close();

		} catch(Exception e) {
			e.printStackTrace();
		}
		return returnFlag;
	}
}
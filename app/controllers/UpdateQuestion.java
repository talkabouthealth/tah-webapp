package controllers;

import java.util.List;

import models.CommentBean;
import models.ConversationBean;
import models.TalkerBean;
import models.PrivacySetting.PrivacyType;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;

import play.mvc.Controller;

import dao.CommentsDAO;
import dao.ConversationDAO;
import dao.TalkerDAO;

import util.SearchUtil;

public class UpdateQuestion extends Controller{

	public static void updateQuestionStatus(){
		
		for(ConversationBean conversation : ConversationDAO.loadAllConversations()){
			List<CommentBean> answerList= CommentsDAO.loadConvoAnswers(conversation.getId());
			int count = 0;
			if(answerList != null)
				count = answerList.size();
			//set question as Unanswered question
			if(count <= 0){
	    		ConversationBean convo = ConversationDAO.getConvoById(conversation.getId());
	    		convo.setOpened(true);
	    		ConversationDAO.updateConvo(convo);
			}
		}
		redirect("/home");
	}
}

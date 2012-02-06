package controllers;

import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.persistence.criteria.CriteriaBuilder.In;

import com.tah.im.model.UserMessage;

import dao.AnswerNotificationDAO;
import dao.MessagingDAO;
import dao.TalkerDAO;
import models.MessageBean;
import models.TalkerBean;
import play.mvc.Controller;
import play.mvc.With;
import play.mvc.Scope.Flash;
import util.CommonUtil;

@With(Secure.class)
public class Messaging  extends Controller {

	public static final int CONVO_PER_PAGE = 10;
	
	public static void inbox(String action, String user, String subject, String message, String page){
		//--------------For Paging-----------------------
		int startPage=0;	//initially start page is 0
		int endPage=0;		//initially end page is 0
		
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
		List<TalkerBean> talkerList = TalkerDAO.loadAllTalkers(); 
		
		page = (page == null || (page != null && page.equals(""))) ? "1" : page;	
		//no of pages
		int pageNo = Integer.parseInt(page) - 1;
		int convoCount= MessagingDAO.getInboxMessagesCount(talker.getId());
		
		List<MessageBean> messageList = MessagingDAO.getInboxMessagesById(talker.getId(),pageNo);
		
		if(messageList != null){
			for(int index = 0; index < messageList.size(); index++){
				MessageBean message1 = messageList.get(index);
				//TalkerBean fromTalker = TalkerDAO.getById(message1.getFromTalkerId());
				
				//for displaying date
				Date date = message1.getTime();
				DateFormat format = new SimpleDateFormat("MMM,dd, hh:mm aaa");
				message1.setDisplayDate(format.format(date));
				
				//for displaying 1st line of message
				String messageDisp = message1.getText();
				if(messageDisp != null && messageDisp.length() > 50){
					messageDisp = messageDisp.substring(0, 50);
					messageDisp = messageDisp + "...";
				}
				message1.setDisplayMessage(messageDisp);
				
				//message1.setFromTalker(fromTalker);
				messageList.set(index, message1);
			}
		}
		
		//Convocount used for total records
				
		int totalCount=convoCount;		//adding convocount into totalcount
		
		int pages = convoCount / CONVO_PER_PAGE;
		
		if(convoCount%CONVO_PER_PAGE > 0){
			pages=pages+1;						//for adding one page
		}
				
		if(startPage == 0){
			startPage=1;
					
			if(totalCount/CONVO_PER_PAGE >= CONVO_PER_PAGE/2)
				endPage=CONVO_PER_PAGE/2;					//Condition for 5 page limit
			else{
				endPage = totalCount / CONVO_PER_PAGE;	//if page less than 5
				if(totalCount % CONVO_PER_PAGE > 0)
					endPage = endPage + 1;
			}			
		}
		
		//increasing start and end page
		if(Integer.parseInt(page)>endPage){
			startPage=startPage+1;
			endPage=endPage+1;
		}else if(Integer.parseInt(page)<startPage){ 
			startPage=startPage-1;		//decreasing start and end page
			endPage=endPage-1;
		}
		
		if( Integer.parseInt(page) > CONVO_PER_PAGE/2 &&  Integer.parseInt(page) <= pages){
			endPage= Integer.parseInt(page);
			startPage=Integer.parseInt(page)-CONVO_PER_PAGE/2 + 1;
		}
		
		int prevPage =  Integer.parseInt(page) - 1 == 0 ? 1 : Integer.parseInt(page) - 1;
		int nextPage = 	Integer.parseInt(page)  == pages ? pages : Integer.parseInt(page) + 1;		
		int size = messageList == null ? 0 : messageList.size();
		String fromPage = "inbox";
		
		render(talker,talkerList,messageList,pages,page,prevPage,nextPage,size,startPage,endPage,fromPage);
	}
	
	public static void sentMail(String action, String user, String subject, String message,String page){
				
		//--------------For Paging------------------------------
		int startPage=0;	//initially start page is 0
		int endPage=0;		//initially end page is 0
		
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		page = (page == null || (page != null && page.equals(""))) ? "1" : page;	
		int pageNo = Integer.parseInt(page) - 1;
		int convoCount = MessagingDAO.getAllSentMessageCount(talker.getId());
				
		if(action != null && action.equalsIgnoreCase("sendMessage")){
			if(user != null){
			    String[] usrArray = user.split(",");	//comma separated user
			    String dummyId="";
			    for(int i=0;i<usrArray.length;i++){
			    	
			    	if(!usrArray[i].trim().equals("")){
			    		TalkerBean toTalker = new TalkerBean();
			    		if(usrArray[i].contains("@"))
			    			toTalker = TalkerDAO.getByEmail(usrArray[i].trim());
			    		else
			    			toTalker = TalkerDAO.getByUserName(usrArray[i].trim());
						if(toTalker != null){
							MessageBean messageBean = new MessageBean();
							messageBean.setFromTalkerId(talker.getId());
							messageBean.setToTalkerId(toTalker.getId());
							messageBean.setSubject(subject);
							messageBean.setText(message);
							messageBean.setRootId("");
							messageBean.setReadFlagSender(false);
							messageBean.setDeleteFlagSender(false);
							messageBean.setArchieveFlagSender(false);
							messageBean.setReplied(false);
							if(i==0){
								messageBean.setDummyId(dummyId);
					    		dummyId=MessagingDAO.saveMessage(messageBean);	
					    	}
							else{
							    messageBean.setDummyId(dummyId); 	  
							    MessagingDAO.saveMessage(messageBean);
							}   
						}else{
							flash.error("Sorry, The address '" + usrArray[i].trim() +"' in the 'To' field was not recognized. Please make sure that all addresses are properly formed.");
						}
			    	}
			    
			    }
			}
		}
		talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
		List<TalkerBean> talkerList = TalkerDAO.loadAllTalkers(); 
		List<MessageBean> sentMessageList = MessagingDAO.getSentMailMessagesById(talker.getId(), pageNo);
		for(int index = 0; index < sentMessageList.size(); index++){
			MessageBean message1 = sentMessageList.get(index);
			TalkerBean totalker = TalkerDAO.getById(message1.getToTalkerId());
			//for displaying date
					Date date = message1.getTime();
					DateFormat format = new SimpleDateFormat("MMM,dd, hh:mm aaa");
					message1.setDisplayDate(format.format(date));
					//for displaying 1st line of message
					String messageDisp = message1.getText();
					if(messageDisp != null && messageDisp.length() > 50){
						messageDisp = messageDisp.substring(0, 50);
						messageDisp = messageDisp + "...";
					}
			List<String> toTalkerNames=MessagingDAO.getToTalkerNamesByMessageId(message1.getId());
			message1.setToTalers(toTalkerNames);
			
			message1.setDisplayMessage(messageDisp);
			message1.setToTalker(totalker);
			sentMessageList.set(index, message1);
		}
		int totalCount=convoCount;		//adding convocount into totalcount
		int pages = convoCount / CONVO_PER_PAGE;
		if(convoCount%CONVO_PER_PAGE > 0){
			pages=pages+1;				//for adding one page
		}
				
		if(startPage==0){
			startPage=1;
					
			if(totalCount/CONVO_PER_PAGE >= CONVO_PER_PAGE)
				endPage=CONVO_PER_PAGE;					//Condition for 5 page limit
			else{
				endPage = totalCount / CONVO_PER_PAGE;	//if page less than 5
				if(totalCount % CONVO_PER_PAGE > 0)
					endPage = endPage + 1;
			}			
		}		
		//increasing start and end page
		if(Integer.parseInt(page)>endPage){
			startPage=startPage+1;
			endPage=endPage+1;
		}else if(Integer.parseInt(page)<startPage){ 
			startPage=startPage-1;		//decreasing start and end page
			endPage=endPage-1;
		}
		if( Integer.parseInt(page) > CONVO_PER_PAGE/2 &&  Integer.parseInt(page) <= pages){
			endPage= Integer.parseInt(page);
			startPage=Integer.parseInt(page)-CONVO_PER_PAGE/2 + 1;
		}		
		int prevPage =  Integer.parseInt(page) - 1 == 0 ? 1 : Integer.parseInt(page) - 1;
		int nextPage = Integer.parseInt(page)  == pages ? pages : Integer.parseInt(page) + 1;
		int size = sentMessageList == null ? 0 : sentMessageList.size();

		String fromPage = "sentmail";
		
		render(talker,talkerList,sentMessageList,pages,page,prevPage,nextPage,size,startPage,endPage,fromPage);
	}
	
	
	public static void email(String id, String action, String replyText, String _page, String fromPage){
		
		TalkerBean talker = CommonUtil.loadCachedTalker(session);		
		MessageBean userMessage = null;
		
		if(id == null || (id != null && id.equals(""))){
			flash.error("Sorry, This message does not exist...");
		}else{
			userMessage = MessagingDAO.getMessageById(id);
		}
		
		if(fromPage != null && fromPage.equalsIgnoreCase("inbox"))
			userMessage.setReadFlag(true);
		else if(fromPage != null && fromPage.equalsIgnoreCase("sentmail"))
			userMessage.setReadFlagSender(true);
		else if(fromPage !=null && fromPage.equalsIgnoreCase("archive")){
			String totalker =  userMessage.getToTalkerId();
			
			if(talker.getId().equals(totalker))
				userMessage.setReadFlag(true);
			else
				userMessage.setReadFlagSender(true);
		}
		
		if(userMessage != null)
			MessagingDAO.updateMessage(userMessage);
		
		if(action != null && action.equalsIgnoreCase("sendReply")){
			
			MessageBean messageBean = new MessageBean();
			messageBean.setFromTalkerId(talker.getId());		
						
			messageBean.setToTalkerId(userMessage.getFromTalkerId());
			messageBean.setSubject(userMessage.getSubject());
			messageBean.setText(replyText);
			
			//for updating new time
			
			if(userMessage!=null && userMessage.getId()!=null)
			{
				userMessage.setTime(new Date());
				MessagingDAO.updateMessage(userMessage);
			}
			
		    if(userMessage.getRootId().trim().equals("")){
				messageBean.setRootId(id);    //assign "id" of message to "rootid" of all reply's
				
				if(userMessage.isReplied()==false) {
					if(userMessage.getToTalkerId().equals(talker.getId()))
					userMessage.setReplied(true);
					userMessage.setTime(new Date());
					userMessage.setReadFlag(false);
					MessagingDAO.updateMessage(userMessage);
				}				
			}
			else{
				messageBean.setRootId(userMessage.getRootId());
			}		    			
			MessagingDAO.saveMessage(messageBean);
	    }
		
		if(userMessage != null){
			TalkerBean fromTalker = TalkerDAO.getById(userMessage.getFromTalkerId());
			userMessage.setFromTalker(fromTalker);
			//for displaying date
			Date date = userMessage.getTime();
			DateFormat format = new SimpleDateFormat("MMM,dd, hh:mm aaa");
			userMessage.setDisplayDate(format.format(date));			
			//MessageBean userReplyMessage
		}
		
		List<MessageBean> replyList = MessagingDAO.getMessageReplies(id);
		List<MessageBean> messageList = new ArrayList<MessageBean>();
			
		for(MessageBean message : replyList){
			TalkerBean fromTalker = TalkerDAO.getById(message.getFromTalkerId());
			message.setFromTalker(fromTalker);
			//for displaying date
			Date date = message.getTime();
			DateFormat format = new SimpleDateFormat("MMM,dd, hh:mm aaa");
			message.setDisplayDate(format.format(date));
			messageList.add(message);
		}
		
		talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
		List<TalkerBean> talkerList = TalkerDAO.loadAllTalkers();
		
		if(fromPage == null || (fromPage != null && fromPage.equals("")))
			fromPage = "inbox";
		if(userMessage != null)
			userMessage.setToTalers(MessagingDAO.getToTalkerNamesByMessageId(userMessage.getId()));
		render(talker,talkerList,userMessage,messageList,_page,fromPage);
	}
	
	public static void doAction(List<String> selectedMessageIds, String actionState, String page, String path){
		
		List<MessageBean> messages = MessagingDAO.loadAllMessages();
		selectedMessageIds = (selectedMessageIds == null ? Collections.EMPTY_LIST : selectedMessageIds);
		TalkerBean _talker = CommonUtil.loadCachedTalker(session);
		
		//Code for message set as unread
		if(actionState.equalsIgnoreCase("MARK_AS_UNREAD")){
			for (MessageBean messageBean : messages) {
				if (selectedMessageIds.contains(messageBean.getId())) {
					if(path != null && path.contains("inbox"))
						messageBean.setReadFlag(false);
					else if(path != null && path.contains("sentmail"))
						messageBean.setReadFlagSender(false);
					else if(path != null && path.contains("archive")){
						String totalker =  messageBean.getToTalkerId();
						
						if(_talker.getId().equals(totalker))
							messageBean.setReadFlag(false);
						else
							messageBean.setReadFlagSender(false);
					}
					MessagingDAO.updateMessage(messageBean);
				}
			}
		}else if(actionState.equalsIgnoreCase("DELETE")){
			for (MessageBean messageBean : messages) {
				if (selectedMessageIds.contains(messageBean.getId())) {
					if(path != null && path.contains("inbox"))
						messageBean.setDeleteFlag(true);
					else if(path != null && path.contains("sentmail"))
						messageBean.setDeleteFlagSender(true);
					else if(path != null && path.contains("archive")){
						String totalker =  messageBean.getToTalkerId();
						
						if(_talker.getId().equals(totalker))
							messageBean.setDeleteFlag(true);
						else
							messageBean.setDeleteFlagSender(true);
					   }
					MessagingDAO.updateMessage(messageBean);
				}
			}
		}else if(actionState.equalsIgnoreCase("ARCHIEVE")){
			for (MessageBean messageBean : messages) {
				if (selectedMessageIds.contains(messageBean.getId())) {
					if(path != null && path.contains("inbox"))
						messageBean.setArchieveFlag(true);
					else if(path != null && path.contains("sentmail"))
						messageBean.setArchieveFlagSender(true);
					MessagingDAO.updateMessage(messageBean);
					
				}
			}
		}
		
		//Render inbox page 
		
		_talker.setFollowerList(TalkerDAO.loadFollowers(_talker.getId()));
		int pageNo = Integer.parseInt(page) - 1;
		List<MessageBean> _messageList = null;
		if(path != null && path.contains("inbox")){
			 _messageList = MessagingDAO.getInboxMessagesById(_talker.getId(),pageNo);
			 if((!page.equals("1")) && (_messageList.size()==0)){
				 page=Integer.toString(pageNo);
				 pageNo--;
				 _messageList=MessagingDAO.getInboxMessagesById(_talker.getId(),pageNo);
			 }
		}else if(path != null && path.contains("archive")){
			_messageList = MessagingDAO.getArchiveMessagesById(_talker.getId(),pageNo);
			if((!page.equals("1")) && (_messageList.size()==0)){
				 page=Integer.toString(pageNo);
				 pageNo--;
				 _messageList=MessagingDAO.getArchiveMessagesById(_talker.getId(), pageNo);
			 }
			
		}else if(path != null && path.contains("sentmail")){
			_messageList = MessagingDAO.getSentMailMessagesById(_talker.getId(),pageNo);
			if((!page.equals("1")) && (_messageList.size()==0)){
				 page=Integer.toString(pageNo);
				 pageNo--;
				 _messageList=MessagingDAO.getSentMailMessagesById(_talker.getId(), pageNo);
			 }
			
		}
		if(_messageList != null){
			for(int index = 0; index < _messageList.size(); index++){
				MessageBean message1 = _messageList.get(index);
				TalkerBean fromTalker = TalkerDAO.getById(message1.getFromTalkerId());
				
				if(path != null && path.contains("sentmail")){
						fromTalker = TalkerDAO.getById(message1.getToTalkerId());
						List<String> toTalkerNames=MessagingDAO.getToTalkerNamesByMessageId(message1.getId());
						message1.setToTalers(toTalkerNames);
						message1.setToTalker(fromTalker);
				}
				//for displaying date
				Date date = message1.getTime();
				DateFormat format = new SimpleDateFormat("MMM,dd, hh:mm aaa");
				message1.setDisplayDate(format.format(date));
				
				//for displaying 1st line of message
				String messageDisp = message1.getText();
				if(messageDisp != null && messageDisp.length() > 50){
					messageDisp = messageDisp.substring(0, 50);
					messageDisp = messageDisp + "...";
				}
				message1.setDisplayMessage(messageDisp);
				
				message1.setFromTalker(fromTalker);
				_messageList.set(index, message1);
			}
		}
		String _page = page;
		int _listsize=_messageList.size();
		if(path != null && path.contains("inbox"))
			render("tags/Messaging/inboxMessage.html", _messageList,_page,_talker,_listsize);
		else if(path != null && path.contains("archive"))
			render("tags/Messaging/archiveMessage.html", _messageList,_page,_talker,_listsize);
		else if(path != null && path.contains("sentmail"))
			render("tags/Messaging/sentMessage.html", _messageList,_page,_listsize);
	}
	
	public static void archive(String action, String user, String subject, String message,String page){
		
		TalkerBean talker = CommonUtil.loadCachedTalker(session);		
		int startPage=0;	//initially start page is 0
		int endPage=0;		//initially end page is 0
		
		//	talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
		
		//List<TalkerBean> talkerList = TalkerDAO.loadAllTalkers(); 
		
		page = (page == null || (page != null && page.equals(""))) ? "1" : page;	
					
		//System.out.println("page:"+page);
		int pageNo = Integer.parseInt(page) - 1;
		//System.out.println("pageno:"+pageNo);
									
		talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
		List<TalkerBean> talkerList = TalkerDAO.loadAllTalkers(); 
		page = page==null?"1":page;

		int convoCount = MessagingDAO.getArchiveMessageCount(talker.getId());
		List<MessageBean> messageList = MessagingDAO.getArchiveMessagesById(talker.getId(),pageNo);
		
		if(messageList != null){
			for(int index = 0; index < messageList.size(); index++){
				MessageBean message1 = messageList.get(index);
				TalkerBean fromTalker = TalkerDAO.getById(message1.getFromTalkerId());
				
				//for displaying date
				Date date = message1.getTime();
				DateFormat format = new SimpleDateFormat("MMM,dd, hh:mm aaa");
				message1.setDisplayDate(format.format(date));
				
				//for displaying 1st line of message
				String messageDisp = message1.getText();
				if(messageDisp != null && messageDisp.length() > 50){
					messageDisp = messageDisp.substring(0, 50);
					messageDisp = messageDisp + "...";
				}
				message1.setDisplayMessage(messageDisp);
				
				message1.setFromTalker(fromTalker);
				messageList.set(index, message1);
			}
		}
		
			//	Convocount used for total records
		
		int totalCount=convoCount;		//adding convocount into totalcount		
		int pages = convoCount / CONVO_PER_PAGE;
		if(convoCount%CONVO_PER_PAGE > 0){
			pages=pages+1;				//for adding one page
		}
				
		if(startPage==0){
			startPage=1;
					
			if(totalCount/CONVO_PER_PAGE >= CONVO_PER_PAGE)
				endPage=CONVO_PER_PAGE;					//Condition for 5 page limit
			else{
				endPage = totalCount / CONVO_PER_PAGE;	//if page less than 5
				if(totalCount % CONVO_PER_PAGE > 0)
					endPage = endPage + 1;
			}
			
		}
		//increasing start and end page
		if(Integer.parseInt(page)>endPage){
			startPage=startPage+1;
			endPage=endPage+1;
		}else if(Integer.parseInt(page)<startPage){ 
			startPage=startPage-1;		//decreasing start and end page
			endPage=endPage-1;
		}
		System.out.println("page:"+page);
		System.out.println("totcount:"+totalCount +"-"+endPage);
		if( Integer.parseInt(page) > CONVO_PER_PAGE/2 &&  Integer.parseInt(page) <= pages){
			endPage= Integer.parseInt(page);
			startPage=Integer.parseInt(page)-CONVO_PER_PAGE/2 + 1;
		}		
		
		int prevPage =  Integer.parseInt(page) - 1 == 0 ? 1 : Integer.parseInt(page) - 1;
		int nextPage = Integer.parseInt(page)  == pages ? pages : Integer.parseInt(page) + 1;
		
		int size = messageList == null ? 0 : messageList.size();
		
		String fromPage = "archive";
		//render(talker,talkerList,messageList,convoCount,page,prevPage,nextPage,size);
		render(talker,talkerList,messageList,pages,page,prevPage,nextPage,size,startPage,endPage,fromPage);

	}
	public static void talkerImageLink(int size,String userName){
		int _size=size;
		String _userName=userName;
		render("tags/talker/talkerImageLink.html",_size,_userName);
	}
}

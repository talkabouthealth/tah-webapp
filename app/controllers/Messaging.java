package controllers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.MessageBean;
import models.TalkerBean;
import models.TalkerBean.EmailSetting;
import play.mvc.Controller;
import play.mvc.With;
import util.CommonUtil;
import util.EmailUtil;
import util.NotificationUtils;
import util.EmailUtil.EmailTemplate;
import dao.MessagingDAO;
import dao.TalkerDAO;

@With(Secure.class)
public class Messaging  extends Controller {

	public static final int CONVO_PER_PAGE = 10;
	
	public static void inbox(String action, String user, String subject, String message, String page){
		//--------------For Paging-----------------------
		int startPage=0;	//initially start page is 0
		int endPage=0;		//initially end page is 0
		
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
		
		page = (page == null || (page != null && page.equals(""))) ? "1" : page;	
		//no of pages
		int pageNo = Integer.parseInt(page) - 1;
		int convoCount = MessagingDAO.getInboxMessagesCount(talker.getId());
		
		List<MessageBean> messageList = MessagingDAO.getInboxMessagesById(talker.getId(),pageNo);
		if(messageList != null){
			for(int index = 0; index < messageList.size(); index++) {
				MessageBean message1 = messageList.get(index);
				//for displaying 1st line of message
				List<MessageBean> replyMessageList = null;
				if(message1.isReplied()) {
					replyMessageList = MessagingDAO.getMessageByRootId(message1.getId());
					if(replyMessageList != null && replyMessageList.size() > 0){
						message1.setDisplayMessage(replyMessageList.get(0).getText());
						if(message1.isReadFlag())
							message1.setReadFlag(replyMessageList.get(0).isReadFlag());
					}
				} else {
					message1.setDisplayMessage( message1.getText());
				}
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
		
		session.put("inboxUnreadCount",MessagingDAO.getUnreadMessageCount(talker.getId()));
		
		render(talker,messageList,pages,page,prevPage,nextPage,size,startPage,endPage,fromPage);
	}
	
	public static void sentMail(String action, String user, String subject, String message,String page){
			
		TalkerBean talker = CommonUtil.loadCachedTalker(session);
		
		if(action != null && action.equalsIgnoreCase("sendMessage")){
			if(user != null){
			    String[] usrArray = user.split(",");	//comma separated user
			    String dummyId="";
			    TalkerBean[] talkerArray = new TalkerBean[usrArray.length];
			    for(int index=0; index<usrArray.length; index++){
			    	if(!usrArray[index].trim().equals("")){
						TalkerBean toTalker = null;
						// Checking user name is valid or not
			    		toTalker = TalkerDAO.getByUserName(usrArray[index].trim());
						if(toTalker != null)
							talkerArray[index] = toTalker;
						else
							flash.put("errMsg", "Sorry, The address '" + usrArray[index].trim() +"' in the 'To' field was not recognized. Please make sure that all addresses are properly formed.");
							//flash.error("Sorry, The address '" + usrArray[index].trim() +"' in the 'To' field was not recognized. Please make sure that all addresses are properly formed.");
			    	}
			    }
			    
			    for(int index=0; index<talkerArray.length; index++){
					TalkerBean toTalker = talkerArray[index];
					
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
						if(index == 0){
							messageBean.setDummyId(dummyId);
				    		dummyId=MessagingDAO.saveMessage(messageBean);
				    		
				    		//sending mail to talker for direct message
				    		Map<String, String> vars = new HashMap<String, String>();
				    		vars.put("other_talker", talker.getUserName());
				    		vars.put("message_text", messageBean.getText());
				    		if(toTalker.getEmailSettings().toString().contains("RECEIVE_DIRECT")){
				    			NotificationUtils.sendEmailNotification(EmailSetting.RECEIVE_DIRECT, toTalker, vars);
				    		}
				    		//NotificationUtils.sendEmailNotification(EmailSetting.RECEIVE_DIRECT_MESSAGE, toTalker, vars);
				    		EmailUtil.sendEmail(EmailTemplate.NOTIFICATION_DIRECT_MESSAGE, EmailUtil.ADMIN_EMAIL, vars, null, false);
				    		
				    		//creating index for message
				    		try{
				    			MessagingDAO.populateMessageIndex(dummyId);
				    		}catch(Exception e){
				    			e.printStackTrace();
				    		}
				    	}
						else{
						    messageBean.setDummyId(dummyId); 	  
						    String messageId = MessagingDAO.saveMessage(messageBean);
						    
						    //sending mail to talker for direct message
						    Map<String, String> vars = new HashMap<String, String>();
				    		vars.put("other_talker", talker.getUserName());
				    		vars.put("message_text", messageBean.getText());
				    		if(toTalker.getEmailSettings().toString().contains("RECEIVE_DIRECT")){
				    			NotificationUtils.sendEmailNotification(EmailSetting.RECEIVE_DIRECT, toTalker, vars);
				    		}
				    		EmailUtil.sendEmail(EmailTemplate.NOTIFICATION_DIRECT_MESSAGE, EmailUtil.ADMIN_EMAIL, vars, null, false);
				    		
				    		//creating index for message
						    try{
						    	MessagingDAO.populateMessageIndex(messageId);
				    		}catch(Exception e){
				    			e.printStackTrace();
				    		}
						}   
					}
		    	}
			}
			redirect("/message/inbox");
		}else{
			//--------------For Paging------------------------------
			int startPage=0;	//initially start page is 0
			int endPage=0;		//initially end page is 0
			
			page = (page == null || (page != null && page.equals(""))) ? "1" : page;	
			int pageNo = Integer.parseInt(page) - 1;
			int convoCount = MessagingDAO.getAllSentMessageCount(talker.getId());
			
			talker.setFollowerList(TalkerDAO.loadFollowers(talker.getId()));
			List<MessageBean> sentMessageList = MessagingDAO.getSentMailMessagesById(talker.getId(), pageNo);
			for(int index = 0; index < sentMessageList.size(); index++){
				MessageBean message1 = sentMessageList.get(index);
				TalkerBean totalker = TalkerDAO.getById(message1.getToTalkerId());
				
				//for displaying date
				//Date date = message1.getTime();
				//DateFormat format = new SimpleDateFormat("MMM,dd, hh:mm aaa");
				//message1.setDisplayDate(format.format(date));
				
				//for displaying 1st line of message
				String messageDisp = "";
				List<MessageBean> replyMessageList = null;
				if(message1.isReplied()){
					replyMessageList = MessagingDAO.getMessageByRootId(message1.getId());
					if(replyMessageList != null && replyMessageList.size() > 0){
						messageDisp = replyMessageList.get(0).getText();
						message1.setDisplayMessage(messageDisp);
						if(message1.isReadFlag())
							message1.setReadFlag(replyMessageList.get(0).isReadFlag());
					}
				}else{
					messageDisp = message1.getText();
					message1.setDisplayMessage(messageDisp);
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
			render(talker,sentMessageList,pages,page,prevPage,nextPage,size,startPage,endPage,fromPage);
		}
	}
	
	
	public static void email(String id, String action, String replyText, String _page, String fromPage){
		
		TalkerBean talker = CommonUtil.loadCachedTalker(session);		
		MessageBean userMessage = null;
		if(_page == null || (_page != null && _page.equals("")))
			_page = "1";
		
		if(id == null || (id != null && id.equals(""))){
			flash.error("Sorry, This message does not exist...");
		}else{
			userMessage = MessagingDAO.getMessageById(id);
		}
		
		if(fromPage != null && fromPage.equalsIgnoreCase("inbox")){
			if(talker.getUserName().equalsIgnoreCase(userMessage.getFromTalker().getUserName()))
				userMessage.setReadFlagSender(true);
			else if(talker.getUserName().equalsIgnoreCase(userMessage.getToTalker().getUserName()))
				userMessage.setReadFlag(true);
			List<MessageBean> replyMessageList = null;
			replyMessageList = MessagingDAO.getMessageByRootId(userMessage.getId());
			for(int index = 0; index < replyMessageList.size(); index++){
				MessageBean message = replyMessageList.get(index);
				message.setReadFlag(true);
				MessagingDAO.updateMessage(message);
			}
		}else if(fromPage != null && fromPage.equalsIgnoreCase("sentmail"))
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
			messageBean.setReadFlag(false);
			messageBean.setReadFlagSender(false);
			messageBean.setDeleteFlag(false);
			messageBean.setDeleteFlagSender(false);
			
			
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
				}	
				userMessage.setReadFlag(false);
				userMessage.setReadFlagSender(false);
				userMessage.setDeleteFlag(false);
				userMessage.setDeleteFlagSender(false);
				MessagingDAO.updateMessage(userMessage);
			}
			else{
				messageBean.setRootId(userMessage.getRootId());
			}		    			
			MessagingDAO.saveMessage(messageBean);
			
			//sending mail to talker for direct message
			TalkerBean fromTalker = TalkerDAO.getById(userMessage.getFromTalkerId());
			TalkerBean toTalker = TalkerDAO.getById(userMessage.getToTalkerId());
		    Map<String, String> vars = new HashMap<String, String>();
    		vars.put("other_talker", talker.getUserName());
    		vars.put("message_text", messageBean.getText());
    		
    		if(fromTalker.getEmailSettings().toString().contains("RECEIVE_DIRECT")){
    			if(!talker.getUserName().equalsIgnoreCase(fromTalker.getUserName()))
    				NotificationUtils.sendEmailNotification(EmailSetting.RECEIVE_DIRECT, fromTalker, vars);
    		}
    		if(toTalker.getEmailSettings().toString().contains("RECEIVE_DIRECT")){
    			if(!talker.getUserName().equalsIgnoreCase(toTalker.getUserName()))
    				NotificationUtils.sendEmailNotification(EmailSetting.RECEIVE_DIRECT, toTalker, vars);
    		}
    		EmailUtil.sendEmail(EmailTemplate.NOTIFICATION_DIRECT_MESSAGE, EmailUtil.ADMIN_EMAIL, vars, null, false);
	    }
		
		if(userMessage != null){
			TalkerBean fromTalker = TalkerDAO.getById(userMessage.getFromTalkerId());
			userMessage.setFromTalker(fromTalker);
			//for displaying date
			//Date date = userMessage.getTime();
			//DateFormat format = new SimpleDateFormat("MMM,dd, hh:mm aaa");
			//userMessage.setDisplayDate(format.format(date));			
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
		
		if(fromPage == null || (fromPage != null && fromPage.equals("")))
			fromPage = "inbox";
		if(userMessage != null)
			userMessage.setToTalers(MessagingDAO.getToTalkerNamesByMessageId(userMessage.getId()));
		
		session.put("inboxUnreadCount",MessagingDAO.getUnreadMessageCount(talker.getId()));
		
		if(action != null && action.equalsIgnoreCase("sendReply")){
			List<MessageBean> _messageList = messageList;
			render("tags/Messaging/replyMessage.html", _messageList);
		}else
			render(talker,userMessage,messageList,_page,fromPage);
		
	}
	
	public static void doAction(List<String> selectedMessageIds, String actionState, String page, String path){
		
		List<MessageBean> messages = MessagingDAO.loadAllMessages();
		selectedMessageIds = (selectedMessageIds == null ? Collections.EMPTY_LIST : selectedMessageIds);
		TalkerBean _talker = CommonUtil.loadCachedTalker(session);
		 
		//Code for message set as unread
		if(actionState.equalsIgnoreCase("MARK_AS_UNREAD")){
			for (MessageBean messageBean : messages) {
				if (selectedMessageIds.contains(messageBean.getId())) {
					
					if(_talker.getUserName().equalsIgnoreCase(messageBean.getFromTalker().getUserName()))
						messageBean.setReadFlagSender(false);
					else if(_talker.getUserName().equalsIgnoreCase(messageBean.getToTalker().getUserName()))
						messageBean.setReadFlag(false);
					
					if(path != null && path.contains("archive")){
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
					
					if(_talker.getUserName().equalsIgnoreCase(messageBean.getFromTalker().getUserName()))
						messageBean.setDeleteFlagSender(true);
					else if(_talker.getUserName().equalsIgnoreCase(messageBean.getToTalker().getUserName()))
						messageBean.setDeleteFlag(true);
					
					if(path != null && path.contains("archive")){
						String totalker =  messageBean.getToTalkerId();
						
						if(_talker.getId().equals(totalker))
							messageBean.setDeleteFlag(true);
						else
							messageBean.setDeleteFlagSender(true);
					   }
					MessagingDAO.updateMessage(messageBean);
					try{
						if(messageBean.isDeleteFlag() == true && messageBean.isDeleteFlagSender() == true)
							MessagingDAO.deleteMessageIndex(messageBean.getId());
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		}else if(actionState.equalsIgnoreCase("ARCHIEVE")){
			for (MessageBean messageBean : messages) {
				if (selectedMessageIds.contains(messageBean.getId())) {
					
					if(_talker.getUserName().equalsIgnoreCase(messageBean.getFromTalker().getUserName()))
						messageBean.setArchieveFlagSender(true);
					else if(_talker.getUserName().equalsIgnoreCase(messageBean.getToTalker().getUserName()))
						messageBean.setArchieveFlag(true);
					
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
				//Date date = message1.getTime();
				//DateFormat format = new SimpleDateFormat("MMM,dd, hh:mm aaa");
				//message1.setDisplayDate(format.format(date));
				
				//for displaying 1st line of message
				String messageDisp = "";
				List<MessageBean> replyMessageList = null;
				if(message1.isReplied()){
					replyMessageList = MessagingDAO.getMessageByRootId(message1.getId());
					if(replyMessageList != null && replyMessageList.size() > 0){
						messageDisp = replyMessageList.get(0).getText();
						message1.setDisplayMessage(messageDisp);
						if(message1.isReadFlag())
							message1.setReadFlag(replyMessageList.get(0).isReadFlag());
					}
				}else{
					messageDisp = message1.getText();
					message1.setDisplayMessage(messageDisp);
				}
				
				message1.setFromTalker(fromTalker);
				_messageList.set(index, message1);
			}
		}
		String _page = page;
		int _listsize=_messageList.size();
		
		session.put("inboxUnreadCount",MessagingDAO.getUnreadMessageCount(_talker.getId()));
		if(path.contains("emailPage")){
		}else{
			if(path != null && path.contains("inbox"))
				render("tags/Messaging/inboxMessage.html", _messageList,_page,_talker,_listsize);
			else if(path != null && path.contains("archive"))
				render("tags/Messaging/archiveMessage.html", _messageList,_page,_talker,_listsize);
			else if(path != null && path.contains("sentmail"))
				render("tags/Messaging/sentMessage.html", _messageList,_page,_listsize);
		}
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
		page = page==null?"1":page;

		int convoCount = MessagingDAO.getArchiveMessageCount(talker.getId());
		List<MessageBean> messageList = MessagingDAO.getArchiveMessagesById(talker.getId(),pageNo);
		
		if(messageList != null){
			for(int index = 0; index < messageList.size(); index++){
				MessageBean message1 = messageList.get(index);
				TalkerBean fromTalker = TalkerDAO.getById(message1.getFromTalkerId());
				
				//for displaying 1st line of message
				String messageDisp = "";
				List<MessageBean> replyMessageList = null;
				if(message1.isReplied()){
					replyMessageList = MessagingDAO.getMessageByRootId(message1.getId());
					if(replyMessageList != null && replyMessageList.size() > 0){
						messageDisp = replyMessageList.get(0).getText();
						message1.setDisplayMessage(messageDisp);
						if(message1.isReadFlag())
							message1.setReadFlag(replyMessageList.get(0).isReadFlag());
					}
				}else{
					messageDisp = message1.getText();
					message1.setDisplayMessage(messageDisp);
				}
				
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

		if( Integer.parseInt(page) > CONVO_PER_PAGE/2 &&  Integer.parseInt(page) <= pages){
			endPage= Integer.parseInt(page);
			startPage=Integer.parseInt(page)-CONVO_PER_PAGE/2 + 1;
		}		
		
		int prevPage =  Integer.parseInt(page) - 1 == 0 ? 1 : Integer.parseInt(page) - 1;
		int nextPage = Integer.parseInt(page)  == pages ? pages : Integer.parseInt(page) + 1;
		
		int size = messageList == null ? 0 : messageList.size();
		
		String fromPage = "archive";
		//render(talker,talkerList,messageList,convoCount,page,prevPage,nextPage,size);
		render(talker,messageList,pages,page,prevPage,nextPage,size,startPage,endPage,fromPage);

	}
	public static void talkerImageLink(int size,String userName){
		int _size=size;
		String _userName=userName;
		render("tags/talker/talkerImageLink.html",_size,_userName);
	}
}

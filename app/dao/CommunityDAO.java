package dao;

import static util.DBUtil.getCollection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;

import logic.FeedsLogic;
import models.CommentBean;
import models.ConversationBean;
import models.TalkerBean;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

public class CommunityDAO {
	
	public static List <ConversationBean> loadExpertsAnswer(String afterActionId,String cancerType){
		List<ConversationBean> convosList = new ArrayList<ConversationBean>();
		while(true){
				List<DBObject> commentDBlist=getExpertsAnswerFromDB(afterActionId,cancerType);
				convosList.addAll(getConvoForExpAnswer(commentDBlist,convosList.size(),cancerType));
				if(convosList.size()>=20 || commentDBlist.size()<40)
					break;
				afterActionId=commentDBlist.get(commentDBlist.size()-1).get("_id").toString();
		}
		return convosList;
	}

	private static List<DBObject> getExpertsAnswerFromDB(String afterActionId,String cancerType) {
		DBCollection commentsColl = getCollection(CommentsDAO.CONVO_COMMENTS_COLLECTION);
		commentsColl.ensureIndex(new BasicDBObject("time", -1));
		
		BasicDBObjectBuilder queryBuilder =  BasicDBObjectBuilder.start()
		.add("deleted", new BasicDBObject("$ne", true))
		.add("answer",true);
	
		if (afterActionId != null && !afterActionId.equals("")) {
				DBObject fields=BasicDBObjectBuilder.start()		
					.add("time" , 1).get();
				Date firstActionTime = new Date();
				DBObject comment=commentsColl.findOne(new BasicDBObject("_id", new ObjectId(afterActionId)),fields);
				
				firstActionTime=(Date)comment.get("time");
				
				if(firstActionTime!=null){
					queryBuilder.add("time", new BasicDBObject("$lt", firstActionTime));
				}
		}
		DBObject fields=BasicDBObjectBuilder.start()		
		.add("_id" , 1)
		.add("convo" , 1)
		.add("from" , 1 )
		.add("time",1)
		.add("text",1)
		.get();
		 
		DBObject query = queryBuilder.get();
		DBCursor commentsCur=commentsColl.find(query,fields).sort(new BasicDBObject("time", -1)).limit(40);
		List <DBObject> commentObjList=new ArrayList<DBObject>();
		while(commentsCur.hasNext()){
			commentObjList.add(commentsCur.next());
		}
		return commentObjList;
	}
	
	private static List<ConversationBean> getConvoForExpAnswer(List<DBObject> commentDBlist,int convosize,String cancerType){
		DBCollection talkerColl = getCollection(TalkerDAO.TALKERS_COLLECTION);
		DBCollection ConvoColl = getCollection(ConversationDAO.CONVERSATIONS_COLLECTION);
		
		List<ConversationBean> convolist=new ArrayList<ConversationBean>();
		
		List<String> cat=new ArrayList<String>();
		cat.add(ConversationBean.ALL_CANCERS);
		cat.add(cancerType);
		BasicDBObjectBuilder queryBuilder =  BasicDBObjectBuilder.start();
		queryBuilder.add("deleted", new BasicDBObject("$ne", true));
		queryBuilder.add("category", new BasicDBObject("$in", cat));
		
		DBObject fields = getBasicConversationFields();
		for(DBObject obj:commentDBlist){
			DBObject query=new BasicDBObject("_id",new ObjectId(((DBRef)obj.get("from")).getId().toString()));
			String connection=talkerColl.findOne(query, new BasicDBObject("connection",1)).get("connection").toString();
			if(connection !=null && TalkerBean.PROFESSIONAL_CONNECTIONS_LIST.contains(connection)){
				/*queryBuilder.add("$or",Arrays.asList(
					new BasicDBObject("other_disease_categories", new BasicDBObject("$in", cat)),
					new BasicDBObject("category", new BasicDBObject("$in", cat))
				));*/
				queryBuilder.add("_id", new ObjectId(((DBRef)obj.get("convo")).getId().toString()));
				DBObject convoQuery = queryBuilder.get();
				
				DBObject convoObj=ConvoColl.findOne(convoQuery,fields);
				if(convoObj!=null){
						ConversationBean convoBean=new ConversationBean();
						convoBean.parseBasicFromDB(convoObj);
						CommentBean answer=new CommentBean();
							answer.parseFromDB(obj);
							List <CommentBean> answerList=new ArrayList<CommentBean>();
							answerList.add(answer);
							for(int i=1;i<CommentsDAO.getConvoAnswersCount(convoBean.getId());i++){
								answerList.add(null);
							}
						convoBean.setComments(answerList);
						convolist.add(convoBean);
						convosize++;
						if(convosize>=20)
								break;
				}
			}
		}
		return convolist;
	}
	
	/**
	 * Opened Questions - not answered, which are marked with 'opened' flag.
	 */
	public static List<ConversationBean> getOpenQuestions(String afterActionId, String cancerType) {
		DBCollection convosColl = getCollection(CONVERSATIONS_COLLECTION);
		
		List<String> cat=new ArrayList<String>();
		cat.add(ConversationBean.ALL_CANCERS);
		cat.add(cancerType);
		
		BasicDBObjectBuilder queryBuilder =  BasicDBObjectBuilder.start()
			.add("opened", true)
			.add("deleted", new BasicDBObject("$ne", true));
		queryBuilder.add("category", new BasicDBObject("$in", cat));
		
		if (afterActionId != null && !afterActionId.equals("")) {
			DBObject fields=BasicDBObjectBuilder.start()		
			.add("cr_date" , 1).get();
			Date firstActionTime = new Date();
			DBObject comment=convosColl.findOne(new BasicDBObject("_id", new ObjectId(afterActionId)),fields);
		
			firstActionTime=(Date)comment.get("cr_date");
		
			if(firstActionTime != null) {
				queryBuilder.add("cr_date", new BasicDBObject("$lt", firstActionTime));
			}
		}

		List<DBObject> convosDBList = new ArrayList<DBObject>();//convosColl.find(query).sort(new BasicDBObject("cr_date", -1)).toArray();
		DBCursor convoCur=convosColl.find(queryBuilder.get()).sort(new BasicDBObject("cr_date", -1)).limit(logic.FeedsLogic.FEEDS_PER_PAGE);
		int recCount = 0;
		while(convoCur.hasNext()) {
			if(recCount>=logic.FeedsLogic.FEEDS_PER_PAGE)
				break;
			recCount++;
			convosDBList.add(convoCur.next());
		}
		List<ConversationBean> convosList = new ArrayList<ConversationBean>();
		for (DBObject convoDBObject : convosDBList) {
			ConversationBean convo = new ConversationBean();
			convo.parseBasicFromDB(convoDBObject);
	    	convosList.add(convo);
		}
		//cat.clear();
		return convosList;
	}
	
	
	/**
	 * @return
	 */
	public static DBObject getBasicConversationFields() {
		DBObject fields = BasicDBObjectBuilder.start()
			.add("summary", 0)
			.add("sum_authors", 0)
			.add("related_convos", 0)
			.add("followup_convos", 0)
			.add("messages", 0)
			.add("summary", 0)
			.add("sum_authors", 0)
			.add("related_convos", 0)
			.add("followup_convos", 0)
			.add("category", 0)
			.get();
		return fields;
	}
	
	public static final String CONVERSATIONS_COLLECTION = "convos";
}

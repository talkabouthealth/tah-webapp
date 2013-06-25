package dao;

import static util.DBUtil.createRef;
import static util.DBUtil.getCollection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.QueryBuilder;
import com.mongodb.WriteResult;

import models.CommentBean;
import models.TalkerBean;
import models.VideoBean;

public class VideoDAO {

	public static boolean save(VideoBean bean) {
		DBCollection videoColl = getCollection(VIDEO_COLLECTION);
		videoColl.save(bean.toDBObject());
		return true;
		/*	id - default
			videoId
			talkerId
			convoId
			list topics
			date
		 */
	}

	public static List<VideoBean> loadConvoVideo(String convoId) {
		List<VideoBean> videoBeanList = null;
		VideoBean videoBean;
		DBCollection videoColl = getCollection(VIDEO_COLLECTION);
		DBRef convoRef = createRef(ConversationDAO.CONVERSATIONS_COLLECTION,convoId);
		DBObject videodbObject = BasicDBObjectBuilder.start().add("convo", convoRef).get();
		DBCursor convoCur=videoColl.find(videodbObject).sort(new BasicDBObject("timestamp", -1));
		if(convoCur.hasNext()){
			videoBeanList = new ArrayList<VideoBean>();
			do{
				videoBean = new VideoBean();
				videoBean.parseDBObject(convoCur.next());
				videoBeanList.add(videoBean);
			}while(convoCur.hasNext());
		}
		return videoBeanList;
	}

	public static List<VideoBean> loadAllVideo() {
		List<VideoBean> videoBeanList = null;
		VideoBean videoBean;
		DBCollection videoColl = getCollection(VIDEO_COLLECTION);
		DBObject videodbObject = BasicDBObjectBuilder.start().get();
		DBCursor convoCur=videoColl.find(videodbObject).sort(new BasicDBObject("timestamp", -1));//.limit(10);
		if(convoCur.hasNext()){
			videoBeanList = new ArrayList<VideoBean>();
			do {
				videoBean = new VideoBean();
				videoBean.parseDBObjectTopic(convoCur.next());
				videoBeanList.add(videoBean);
			}while(convoCur.hasNext());
		}
		return videoBeanList;
	}
	
	public static List<VideoBean> loadTopicVideo(String convoId,int limit){
		List<VideoBean> videoBeanList = null;
		VideoBean videoBean;
		DBCollection videoColl = getCollection(VIDEO_COLLECTION);

		Set<DBRef> topicDB = new HashSet<DBRef>();
		topicDB.add(createRef(TopicDAO.TOPICS_COLLECTION, convoId));
		
		BasicDBObjectBuilder queryBuilder = BasicDBObjectBuilder.start()
		.add("topics", new BasicDBObject("$in", topicDB));
		DBCursor convoCur= null;
		if(limit == 0)
			convoCur=videoColl.find(queryBuilder.get()).sort(new BasicDBObject("timestamp", -1));
		else
			convoCur=videoColl.find(queryBuilder.get()).sort(new BasicDBObject("timestamp", -1)).limit(limit);
		if(convoCur.hasNext()){
			videoBeanList = new ArrayList<VideoBean>();
			do {
				videoBean = new VideoBean();
				videoBean.parseDBObjectTopic(convoCur.next());
				videoBeanList.add(videoBean);
			}while(convoCur.hasNext());
		}
		return videoBeanList;
	}
	
	public static boolean deleteVideo(String videoId) {
		boolean returnFlag = true;
		DBCollection videoColl = getCollection(VIDEO_COLLECTION);
		DBObject query =  BasicDBObjectBuilder.start()
						  .add("videoId", videoId)
						  .get();
		videoColl.remove(query);
		return returnFlag;
	}

	public static List<VideoBean> loadVideoForHome(int limit, String cancerType){
		 List<VideoBean> videoBeanList = loadHomeVideo(cancerType);
		 boolean loadMore = false;
		 int newLimit = limit;
		 if(videoBeanList == null) {
			loadMore = true;
			newLimit = limit;
		 } else if(videoBeanList.size() < limit) {
			 newLimit =  (limit - videoBeanList.size()) + 1;
			 loadMore = true;
		 }
		 if(loadMore && "All Cancers".equals(cancerType)) {
				VideoBean videoBean;
				DBCollection videoColl = getCollection(VIDEO_COLLECTION);
				DBCursor convoCur= null;
				BasicDBObjectBuilder queryBuilder = BasicDBObjectBuilder.start().add("homeVideoFlag", false);
				convoCur=videoColl.find(queryBuilder.get()).limit(newLimit).sort(new BasicDBObject("timestamp", -1));
				if(convoCur.hasNext()) {
					if(videoBeanList == null)
						videoBeanList = new ArrayList<VideoBean>();
					do {
						videoBean = new VideoBean();
						videoBean.parseDBObjectTopic(convoCur.next());
						videoBeanList.add(videoBean);
					}while(convoCur.hasNext());
				}
		 } 
		 /* else {
			 System.out.println("Not for all cancer");
		 }*/
		 return videoBeanList;
	}
	
	public static boolean addHomeVideo(String videoId, String videoTitle, String videoLink,String cancerType){
		boolean addFlag = true;
		DBCollection videoColl = getCollection(HOME_VIDEO_COLLECTION);
		DBObject videoDBObject = BasicDBObjectBuilder.start()
			.add("videoId", videoId)
			.add("videoTitle", videoTitle)
			.add("videoLink", videoLink)
			.add("timestamp", Calendar.getInstance().getTime())
			.add("cancerType", cancerType)
			.get();
		videoColl.save(videoDBObject);
		return addFlag;
	}
	
	public static List<VideoBean> loadHomeVideo(String cancerType) {
		List<VideoBean> videoBeanList = null;
		VideoBean videoBean;
		DBCollection videoColl = getCollection(HOME_VIDEO_COLLECTION);
		DBCursor convoCur= null;

		//DBObject query =  BasicDBObjectBuilder.start().add("cancerType", cancerType).get();
		convoCur=videoColl.find().sort(new BasicDBObject("timestamp", -1)); //find(query)

		if(convoCur.hasNext()) {
			videoBeanList = new ArrayList<VideoBean>();
			do {
				videoBean = new VideoBean();
				videoBean.parseDBObjectHome(convoCur.next());
				videoBeanList.add(videoBean);
			} while(convoCur.hasNext());
		}
		return videoBeanList;
	}
	
	public static boolean removeHomeVideo(String videoId, String cancerType){
		boolean returnFlag = true;
		DBCollection videoColl = getCollection(HOME_VIDEO_COLLECTION);
		DBObject query =  BasicDBObjectBuilder.start().add("videoId", videoId).add("cancerType",cancerType).get();
		DBCursor convoCur = videoColl.find(query);
		if(convoCur.hasNext()) {
			do {
				DBObject dbObject =	convoCur.next();
				videoColl.remove(dbObject);
			}while(convoCur.hasNext());
		}
		return returnFlag;
	}

	public static final String VIDEO_COLLECTION = "video";
	public static final String HOME_VIDEO_COLLECTION = "homevideo";
}
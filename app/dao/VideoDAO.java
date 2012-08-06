package dao;

import static util.DBUtil.createRef;
import static util.DBUtil.getCollection;

import java.util.ArrayList;
import java.util.Arrays;
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
	public static final String VIDEO_COLLECTION = "video";
}
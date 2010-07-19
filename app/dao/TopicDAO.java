package dao;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import models.TalkerBean;
import models.TopicBean;

import org.bson.types.ObjectId;

import util.DBUtil;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

public class TopicDAO {
	
	public static final String TOPICS_COLLECTION = "topics";
	
	public static String save(TopicBean topic) {
		DBCollection topicsColl = DBUtil.getCollection(TOPICS_COLLECTION);
		
		DBRef talkerRef = new DBRef(DBUtil.getDB(), TalkerDAO.TALKERS_COLLECTION, new ObjectId(topic.getUid()));
		DBObject topicObject = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("topic", topic.getTopic())
			.add("cr_date", topic.getCreationDate())
			.add("disp_date", topic.getDisplayTime())
			.get();

		topicsColl.save(topicObject);
		return topicObject.get("_id").toString();
	}
	
	public static Map<String, TopicBean> queryTopics() {
		DBCollection topicsColl = DBUtil.getCollection(TOPICS_COLLECTION);
		List<DBObject> topicsList = 
			topicsColl.find().sort(new BasicDBObject("disp_date", -1)).limit(20).toArray();
		
		Map <String, TopicBean> topicsMap = new LinkedHashMap <String, TopicBean>(20);
		for (DBObject topicDBObject : topicsList) {
			TopicBean topic = new TopicBean();
	    	topic.setId(topicDBObject.get("_id").toString());
	    	topic.setTopic((String)topicDBObject.get("topic"));
	    	topic.setCreationDate((Date)topicDBObject.get("cr_date"));
	    	topic.setDisplayTime((Date)topicDBObject.get("disp_date"));
			
	    	DBObject talkerDBObject = ((DBRef)topicDBObject.get("uid")).fetch();
	    	TalkerBean talker = new TalkerBean();
	    	talker.parseFromDB(talkerDBObject);
	    	talker.setNumberOfTopics(getNumberOfTopics(talker.getId()));
	    	topic.setTalker(talker);
	    	
	    	topicsMap.put(topic.getId(), topic);
		}
		
		return topicsMap;
	}
	
	/**
	 * # of topics for given Talker ID
	 * TODO: store additional field - quicker access?
	 */
	public static int getNumberOfTopics(String talkerId) {
		DBCollection topicsColl = DBUtil.getCollection(TOPICS_COLLECTION);
		
		DBRef talkerRef = new DBRef(DBUtil.getDB(), TalkerDAO.TALKERS_COLLECTION, new ObjectId(talkerId));
		DBObject query = new BasicDBObject("uid", talkerRef);

		int numberOfTopics = topicsColl.find(query).count();
		return numberOfTopics;
	}
	
	public static String getLastTopicId() {
		DBCollection topicsColl = DBUtil.getDB().getCollection(TOPICS_COLLECTION);
		
		DBObject topicDBObject = topicsColl.find().sort(new BasicDBObject("cr_date", -1)).next();
		if (topicDBObject == null) {
			return null;
		}
		else {
			return topicDBObject.get("_id").toString();
		}
	}
	
	public static List<Map<String, String>> loadTopicsForDashboard(boolean withNotifications) {
		DBCollection topicsColl = DBUtil.getDB().getCollection(TOPICS_COLLECTION);
		DateFormat dateFormat = new SimpleDateFormat("MM.dd.yyyy HH:mm:ss");
		
		List<DBObject> topicsDBList = topicsColl.find().sort(new BasicDBObject("cr_date", 1)).toArray();
		
		List<Map<String, String>> topicsInfoList = new ArrayList<Map<String,String>>();
		for (DBObject topicDBObject : topicsDBList) {
			Map<String, String> topicInfoMap = new HashMap<String, String>();
			
			//noti_history.noti_time is null
			int numOfNotifications = NotificationDAO.getNotiNumByTopic(topicDBObject.get("_id").toString());
			if (withNotifications && numOfNotifications == 0) {
				continue;
			}
			else if (!withNotifications && numOfNotifications > 0) {
				continue;
			}
			
			//convert data to map
			DBRef talkerRef = (DBRef)topicDBObject.get("uid");
			DBObject talkerDBObject = talkerRef.fetch();
			
			topicInfoMap.put("topicId", topicDBObject.get("_id").toString());
			topicInfoMap.put("topic", (String)topicDBObject.get("topic"));
			Date creationDate = (Date)topicDBObject.get("cr_date");
			topicInfoMap.put("cr_date", dateFormat.format(creationDate));
			
			topicInfoMap.put("uid", talkerDBObject.get("_id").toString());
			topicInfoMap.put("uname", talkerDBObject.get("uname").toString());
			topicInfoMap.put("gender", talkerDBObject.get("gender").toString());
			
			if (withNotifications) {
				//String sqlStatement4 = 
				//	"SELECT COUNT(*) FROM topics 
				//	RIGHT JOIN noti_history ON topics.topic_id = noti_history.topic_id 
				//	WHERE topics.topic_id = " + con3.getRs().getInt("topics.topic_id");
				int notificationsNum = NotificationDAO.getNotiNumByTopic(topicInfoMap.get("topicId"));
				topicInfoMap.put("notificationsNum", ""+notificationsNum);
			}
			
			topicsInfoList.add(topicInfoMap);
		}
		
		return topicsInfoList;
	}
	
	public static void main(String[] args) {
		System.out.println(TopicDAO.getNumberOfTopics("4c2cb43160adf3055c97d061"));
		
//		TopicBean topic = new TopicBean();
//		topic.setTopic("test");
//		topic.setUid("4c2cb43160adf3055c97d061");
//		Date currentDate = Calendar.getInstance().getTime();
//		topic.setCreationDate(currentDate);
//		topic.setDisplayTime(currentDate);
//		TopicDAO.save(topic);
	}
	

	/* 
	 	Unused DB code - if we'll need it we convert it later.
	 	
	 	
	 	
	 	public static Map<Integer, TopicBean> queryOlderTopics(String sMaxDisplayTime){
		
		Connection conn = null;
		PreparedStatement ps = null;  // Or PreparedStatement if needed
		ResultSet rs = null;
		
		try {
		    Context initContext = new InitialContext();
		    Context envContext  = (Context)initContext.lookup("java:/comp/env");
		    DataSource ds = (DataSource)envContext.lookup(DATA_SOURCE_NAME);
		    conn = ds.getConnection();
		    //2009-02-25 22:48:22
		    String sqlStatement = "SELECT topic_id, topic, display_time FROM topics WHERE display_time BETWEEN '0' AND '" + sMaxDisplayTime + "' ORDER BY display_time DESC LIMIT 40";		    
			
		    ps = conn.prepareStatement(sqlStatement);
		    rs = ps.executeQuery();
		    Map <Integer, TopicBean> mapTopics = new LinkedHashMap<Integer, TopicBean>(40);
		    while (rs.next()){
		    	TopicBean tb = new TopicBean();
		    	tb.setTopicID(rs.getInt(1));
		    	tb.setTopic(rs.getString(2));
		    	
		    	String sDisplayTime = rs.getString(3);
		    	SimpleDateFormat SQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
				Date dDisplayTime = SQL_DATE_FORMAT.parse(sDisplayTime);
		    	tb.setDisplayTime(dDisplayTime);
				mapTopics.put(tb.getTopicID(), tb);
			}
		    rs.close();
		    rs = null;
		    ps.close();
		    ps = null;
		    conn.close(); // Return to connection pool
		    conn = null;  // Make sure we don't close it twice
		   
		    return mapTopics;
		} catch (SQLException ex) {
			    // handle any errors
			    ex.printStackTrace();
				return null;
		} catch (Exception ex) {
				ex.printStackTrace();
				return null;
		} finally {
		    // Always make sure result sets and statements are closed,
		    // and the connection is returned to the pool
		    if (rs != null) {
		      try { rs.close(); } catch (SQLException e) { ; }
		      rs = null;
		    }
		    if (ps != null) {
		      try { ps.close(); } catch (SQLException e) { ; }
		      ps = null;
		    }
		    if (conn != null) {
		      try { conn.close(); } catch (SQLException e) { ; }
		      conn = null;
		    }
		}
	}
	
	
	
	public static Map<Integer, TopicBean> queryNewerTopics(String sMinDisplayTime){
		
		Connection conn = null;
		PreparedStatement ps = null;  // Or PreparedStatement if needed
		ResultSet rs = null;
		
		Date dNow = Calendar.getInstance().getTime();
		SimpleDateFormat SQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String now = SQL_DATE_FORMAT.format(dNow);
		
		try {
		    Context initContext = new InitialContext();
		    Context envContext  = (Context)initContext.lookup("java:/comp/env");
		    DataSource ds = (DataSource)envContext.lookup(DATA_SOURCE_NAME);
		    conn = ds.getConnection();
		    //2009-02-25 22:48:22
		    String sqlStatement = "SELECT topic_id, topic, display_time FROM topics WHERE display_time BETWEEN '" + sMinDisplayTime + "' AND '" + now + "' ORDER BY display_time ASC LIMIT 40";		    
			
		    ps = conn.prepareStatement(sqlStatement);
		    rs = ps.executeQuery();
		    Map <Integer, TopicBean> mapTopics = new LinkedHashMap<Integer, TopicBean>(40);
		    
		    rs.setFetchDirection(ResultSet.FETCH_REVERSE);
		    rs.last();
		    while (rs.previous()){
		    	TopicBean tb = new TopicBean();
		    	tb.setTopicID(rs.getInt(1));
		    	tb.setTopic(rs.getString(2));
		    	
		    	String sDisplayTime = rs.getString(3);
		    	Date dDisplayTime = SQL_DATE_FORMAT.parse(sDisplayTime);
		    	tb.setDisplayTime(dDisplayTime);
				mapTopics.put(tb.getTopicID(), tb);
			}
		    rs.close();
		    rs = null;
		    ps.close();
		    ps = null;
		    conn.close(); // Return to connection pool
		    conn = null;  // Make sure we don't close it twice
		    
		    return mapTopics;
		} catch (SQLException ex) {
			    // handle any errors
			    ex.printStackTrace();
				return null;
		} catch (Exception ex) {
				ex.printStackTrace();
				return null;
		} finally {
		    // Always make sure result sets and statements are closed,
		    // and the connection is returned to the pool
		    if (rs != null) {
		      try { rs.close(); } catch (SQLException e) { ; }
		      rs = null;
		    }
		    if (ps != null) {
		      try { ps.close(); } catch (SQLException e) { ; }
		      ps = null;
		    }
		    if (conn != null) {
		      try { conn.close(); } catch (SQLException e) { ; }
		      conn = null;
		    }
		}
	}
	
	
	public static Map<Integer, TopicBean> queryUserCreatedTopics(String uid){
		
		Connection conn = null;
		PreparedStatement ps = null;  // Or PreparedStatement if needed
		ResultSet rs = null;
		
		try {
		    Context initContext = new InitialContext();
		    Context envContext  = (Context)initContext.lookup("java:/comp/env");
		    DataSource ds = (DataSource)envContext.lookup(DATA_SOURCE_NAME);
		    conn = ds.getConnection();
		    
		    String sqlStatement = "SELECT topic_id, topic, creation_date FROM topics WHERE uid = '" + uid + "' ORDER BY topic_id DESC LIMIT 40";
		    ps = conn.prepareStatement(sqlStatement);
		    rs = ps.executeQuery();
		    Map <Integer, TopicBean> mapTopics = new LinkedHashMap<Integer, TopicBean>(40);
		    while (rs.next()){
		    	TopicBean tb = new TopicBean();
		    	tb.setTopicID(rs.getInt(1));
		    	tb.setTopic(rs.getString(2));
		    	String sCreationDate = rs.getString(3);
		    	SimpleDateFormat SQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
				Date dCreationDate = SQL_DATE_FORMAT.parse(sCreationDate);
		    	tb.setCreationDate(dCreationDate);
				mapTopics.put(tb.getTopicID(), tb);
			}
		    rs.close();
		    rs = null;
		    ps.close();
		    ps = null;
		    conn.close(); // Return to connection pool
		    conn = null;  // Make sure we don't close it twice
		    return mapTopics;
		} catch (SQLException ex) {
			    // handle any errors
			    ex.printStackTrace();
				return null;
		} catch (Exception ex) {
				ex.printStackTrace();
				return null;
		} finally {
		    // Always make sure result sets and statements are closed,
		    // and the connection is returned to the pool
		    if (rs != null) {
		      try { rs.close(); } catch (SQLException e) { ; }
		      rs = null;
		    }
		    if (ps != null) {
		      try { ps.close(); } catch (SQLException e) { ; }
		      ps = null;
		    }
		    if (conn != null) {
		      try { conn.close(); } catch (SQLException e) { ; }
		      conn = null;
		    }
		}
	}
	public static String queryLastTalkTopic(String uid, String uid2){
	
		Connection conn = null;
		PreparedStatement ps = null;  // Or PreparedStatement if needed
		ResultSet rs = null;
		
		try {
		    Context initContext = new InitialContext();
		    Context envContext  = (Context)initContext.lookup("java:/comp/env");
		    DataSource ds = (DataSource)envContext.lookup(DATA_SOURCE_NAME);
		    conn = ds.getConnection();
		    
		    String sqlStatement = "SELECT conversations.topic_id FROM conversations INNER JOIN topics ON conversations.topic_id = topics.topic_id WHERE ((conversations.uid1 = '" + uid + "' AND conversations.uid2 = '" + uid2 + "') OR (conversations.uid2 = '" + uid + "' AND conversations.uid1 = '" + uid + "')) ORDER BY conversations.end_time DESC LIMIT 1";
		    ps = conn.prepareStatement(sqlStatement);
		    rs = ps.executeQuery();
		    String sTopicID = null;
		    while (rs.next()){
		    	sTopicID = rs.getString(1);
		    }
		    rs.close();
		    rs = null;
		    ps.close();
		    ps = null;
		    conn.close(); // Return to connection pool
		    conn = null;  // Make sure we don't close it twice
		    return sTopicID;
		} catch (SQLException ex) {
			    // handle any errors
			    ex.printStackTrace();
				return null;
		} catch (Exception ex) {
				ex.printStackTrace();
				return null;
		} finally {
		    // Always make sure result sets and statements are closed,
		    // and the connection is returned to the pool
		    if (rs != null) {
		      try { rs.close(); } catch (SQLException e) { ; }
		      rs = null;
		    }
		    if (ps != null) {
		      try { ps.close(); } catch (SQLException e) { ; }
		      ps = null;
		    }
		    if (conn != null) {
		      try { conn.close(); } catch (SQLException e) { ; }
		      conn = null;
		    }
		}
	}
	
	
	
	public Map<Integer, TopicBean> queryNextTopics(String sLatestTimeStamp){
		
	Connection conn = null;
	PreparedStatement ps = null;  // Or PreparedStatement if needed
	ResultSet rs = null;
	
	Date dNow = Calendar.getInstance().getTime();
	SimpleDateFormat SQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	String now = SQL_DATE_FORMAT.format(dNow);
	
	Date dLTS = dNow;
	try {
		dLTS = SQL_DATE_FORMAT.parse(sLatestTimeStamp);
	} catch (ParseException e1) {
		e1.printStackTrace();
	}
	////System.out.println("*** retrieveNextTopicServlet - Latest TimeStamp: Long: " + dLTS.getTime());
	long lLTS = dLTS.getTime() + 1000;
	////System.out.println("*** retrieveNextTopicServlet - Latest TimeStamp: " + lLTS);
	dLTS.setTime(lLTS);
	sLatestTimeStamp = SQL_DATE_FORMAT.format(dLTS);
	////System.out.println("*** retrieveNextTopicServlet - Latest TimeStamp: " + sLatestTimeStamp);
	
	try {
	    String sqlStatement = "SELECT topic_id, topic, display_time FROM topics WHERE display_time BETWEEN '" + sLatestTimeStamp + "' AND '" + now + "' ORDER BY display_time ASC LIMIT 40";		    
		
	    conn = ds.getConnection();
	    ps = conn.prepareStatement(sqlStatement);
	    rs = ps.executeQuery();
	    Map <Integer, TopicBean> mapNextTopics = new LinkedHashMap<Integer, TopicBean>(40);
	    
	    while (rs.next()){
	    	TopicBean tb = new TopicBean();
	    	tb.setTopicID(rs.getInt(1));
	    	tb.setTopic(rs.getString(2));
	    	
	    	String sDisplayTime = rs.getString(3);
	    	Date dDisplayTime = SQL_DATE_FORMAT.parse(sDisplayTime);
	    	tb.setDisplayTime(dDisplayTime);
			mapNextTopics.put(tb.getTopicID(), tb);
			////System.out.println("***queryNextTopics - queried: " + tb.getTopic());
	    }
	    rs.close();
	    rs = null;
	    ps.close();
	    ps = null;
	    conn.close(); // Return to connection pool
	    conn = null;  // Make sure we don't close it twice
	    
	    return mapNextTopics;
	} catch (SQLException ex) {
		    // handle any errors
		    ex.printStackTrace();
			return null;
	} catch (Exception ex) {
			ex.printStackTrace();
			return null;
	} finally {
	    // Always make sure result sets and statements are closed,
	    // and the connection is returned to the pool
	    if (rs != null) {
	      try { rs.close(); } catch (SQLException e) { ; }
	      rs = null;
	    }
	    if (ps != null) {
	      try { ps.close(); } catch (SQLException e) { ; }
	      ps = null;
	    }
	    if (conn != null) {
	      try { conn.close(); } catch (SQLException e) { ; }
	      conn = null;
	    }
	}
}



public Map<Integer, TopicBean> queryNextTopics(String sEarliestTimeStamp){
		
	Connection conn = null;
	PreparedStatement ps = null;  // Or PreparedStatement if needed
	ResultSet rs = null;
	
	SimpleDateFormat SQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	Date dBeginning;
	try {
		dBeginning = SQL_DATE_FORMAT.parse("2009-01-1 00:00:00");
	} catch (ParseException e2) {
		e2.printStackTrace();
		return null;
	}
	String sBeginning = SQL_DATE_FORMAT.format(dBeginning);
	Date dETS = dBeginning;
	try {
		dETS = SQL_DATE_FORMAT.parse(sEarliestTimeStamp);
	} catch (ParseException e1) {
		e1.printStackTrace();
	}
	////System.out.println("*** retrieveNextTopicServlet - Latest TimeStamp: Long: " + dLTS.getTime());
	//long lLTS = dLTS.getTime() + 1000;
	long lLTS = dETS.getTime();
	////System.out.println("*** retrieveNextTopicServlet - Latest TimeStamp: " + lLTS);
	dETS.setTime(lLTS);
	sEarliestTimeStamp = SQL_DATE_FORMAT.format(dETS);
	
	try {
	    String sqlStatement = "SELECT topic_id, topic, display_time FROM topics WHERE display_time BETWEEN '" + sBeginning + "' AND '" + sEarliestTimeStamp + "' ORDER BY display_time DESC LIMIT 40";		    
	    //System.out.println("***MoreOldTopicServlet:  SQL: " + sqlStatement);
		
	    conn = ds.getConnection();
	    ps = conn.prepareStatement(sqlStatement);
	    rs = ps.executeQuery();
	    Map <Integer, TopicBean> mapNextTopics = new LinkedHashMap<Integer, TopicBean>(40);
	    
	    while (rs.next()){
	    	TopicBean tb = new TopicBean();
	    	tb.setTopicID(rs.getInt(1));
	    	tb.setTopic(rs.getString(2));
	    	
	    	String sDisplayTime = rs.getString(3);
	    	Date dDisplayTime = SQL_DATE_FORMAT.parse(sDisplayTime);
	    	tb.setDisplayTime(dDisplayTime);
			mapNextTopics.put(tb.getTopicID(), tb);
			////System.out.println("***queryNextTopics - queried: " + tb.getTopic());
	    }
	    rs.close();
	    rs = null;
	    ps.close();
	    ps = null;
	    conn.close(); // Return to connection pool
	    conn = null;  // Make sure we don't close it twice
	    
	    return mapNextTopics;
	} catch (SQLException ex) {
		    // handle any errors
		    ex.printStackTrace();
			return null;
	} catch (Exception ex) {
			ex.printStackTrace();
			return null;
	} finally {
	    // Always make sure result sets and statements are closed,
	    // and the connection is returned to the pool
	    if (rs != null) {
	      try { rs.close(); } catch (SQLException e) { ; }
	      rs = null;
	    }
	    if (ps != null) {
	      try { ps.close(); } catch (SQLException e) { ; }
	      ps = null;
	    }
	    if (conn != null) {
	      try { conn.close(); } catch (SQLException e) { ; }
	      conn = null;
	    }
	}
}
	 
	 
	 */
	

}


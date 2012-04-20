package util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import models.ServiceAccountBean;
import models.ServicePost;
import models.TalkerBean;
import play.Logger;
import play.libs.WS;
import play.libs.WS.HttpResponse;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;

import dao.TalkerDAO;

public class FacebookUtil {
	
	/**
	 * Post wall message with given text from given account
	 * @param fullText
	 * @param fbAccount
	 */
	public static void post(String fullText, final ServiceAccountBean fbAccount) {
		final String text = fullText;
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				try {
					HttpResponse res = WS.url(
							"https://graph.facebook.com/me/feed?access_token=%s&message=%s", 
							fbAccount.getToken(), text).post();
					
					Logger.debug("FB update response: " + res.getStatus() + ", "+res.getString());
				} catch (Exception e) {
					Logger.error(e, "FacebookUtil.java : post");
				}
			}
		});
	}
	
	public static List<ServicePost> importPosts(ServiceAccountBean fbAccount, String sinceId) {
		List<ServicePost> postsList = new ArrayList<ServicePost>();
		try {
			String params = "&limit=500";
			if (sinceId != null) {
				//if sinceId not null - we don't need to load all posts, just the latest ones;
				//few hours would be enough, but to be sure we check all from last week
				params = params + "&since=last%%20week";
			}
			HttpResponse res = WS.url(
					"https://graph.facebook.com/me/posts?access_token=%s"+params, 
					URLEncoder.encode(fbAccount.getToken(), "UTF-8")).get();
			
			BasicDBObject data = (BasicDBObject)JSON.parse(res.getString());
			BasicDBList dataList = (BasicDBList)data.get("data");
			
//			[ { "id" : "100001842920779_198456163499404" , "from" : { "name" : "Osya Osezno" , "id" : "100001842920779"} ,
//				 "message" : "great site - http://google.com" , "type" : "status" , "created_time" : "2011-02-09T17:02:27+0000
//				" , "updated_time" : "2011-02-09T17:02:27+0000" , "attribution" : "test"}
			
			//2011-02-09T17:02:27+0000
			Locale.setDefault(Locale.US);
			DateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
			if(dataList != null && !dataList.isEmpty()){
				for (Object objectData : dataList) {
					BasicDBObject postData = (BasicDBObject)objectData;
					
					ServicePost post = new ServicePost();
					post.setId(postData.getString("id"));
					post.setText(postData.getString("message"));
					
					BasicDBObject userData = (BasicDBObject)postData.get("from");
					post.setUserId(userData.getString("id"));
					
					String timeString = postData.getString("created_time");
					post.setTime(timeFormat.parse(timeString));
					
					postsList.add(post);
				}
			}
		} catch (Exception e) {
			Logger.error(e, "Wasn't able to import Facebook posts");
		}
		return postsList;
	}
	
	public static void useFacebookImage(TalkerBean talker, String accountId) {
		try {
			URL pictureURL = new URL("http://graph.facebook.com/"+accountId+"/picture?type=normal");
			BufferedImage bsrc = ImageIO.read(pictureURL);
			ByteArrayOutputStream baos = ImageUtil.createThumbnailFromFacebook(bsrc);
	    	TalkerDAO.updateTalkerImage(talker, baos.toByteArray());
		}
		catch (Exception e) {
			Logger.error(e, "FacebookUtil : userFacebookImage ");
		}
		
	}

}

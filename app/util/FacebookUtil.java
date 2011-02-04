package util;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import models.ServiceAccountBean;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;


import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import play.Logger;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import util.oauth.TwitterOAuthProvider;

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
					Logger.error(e, "FB post error");
				}
			}
		});
	}
	
	public static void importPosts(final String token) {
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				try {
					HttpResponse res = WS.url(
							"https://graph.facebook.com/me/posts?access_token=%s&limit=3", token).get();
					
					Logger.error("FB update response: " + res.getStatus());
					
//					GsonBuilder gson = new GsonBuilder();
//					Map<String, Object> data = gson.create().fromJson(res.getString(), Map.class);
					
//					Object data = JSON.fromJSON(res.getString(), List.class);
//					System.out.println(data);
					
					BasicDBObject data = (BasicDBObject)JSON.parse(res.getString());
					BasicDBList dataList = (BasicDBList)data.get("data");
					System.out.println(dataList);
					
//					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//					DocumentBuilder db = dbf.newDocumentBuilder();
//					Document doc = db.parse(conn.getInputStream());
//					
//					NodeList statusNodeList = doc.getFirstChild().getChildNodes();
//					for (int i=0; i<statusNodeList.getLength(); i++) {
//						Node statusNode = statusNodeList.item(i);
//						NodeList childNodes = statusNode.getChildNodes();
////						  <created_at>Tue Mar 10 14:17:11 +0000 2009</created_at>
////						  <id>1305504773</id>
////						  <text>hi kan!</text>
//						for (int j=0; j<childNodes.getLength(); j++) {
//							Node child = childNodes.item(j);
//							if (child.getNodeName().equals("text")) {
//								System.out.println(":"+child.getFirstChild().getNodeValue());
//							}
//						}
//						
//
//					}
					
				} catch (Exception e) {
					//Logger.error(e, "Wasn't able to follow Twitter user!");
					e.printStackTrace();
				}
			}
		});
	}

}

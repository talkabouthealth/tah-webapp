package util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.GsonBuilder;

import fr.zenexity.json.JSON;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.http.HttpParameters;
import oauth.signpost.http.HttpRequest;
import play.Logger;
import util.oauth.TwitterOAuthProvider;

//TODO: rewrite it with new Play OAuth API
public class TwitterUtil {
	/* Account @talkforhealth */
	private static final String CONSUMER_KEY = "dntMSxZl859YGyAeKcTFcg";
	private static final String CONSUMER_SECRET = "gP8XNqM8bpnuYzBSfJDZLMXrDowE58znsZJuwjfAsQ";
	
	private static final String ACCESS_TOKEN = "136322338-kNsQxRvvPQBHYp1EBU8F6CJAKEm9R3FXtT7S19ua";
	private static final String ACCESS_TOKEN_SECRET = "bROleD290VFQCjtTeSfhPm3IBn6sM69uCID1hHJz44";
	
	public static final String TALKFORHEALTH_ID = "136322338";
	
	/* Operation from TAH account */
	public static void followUser(final String userAccountId) {
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				OAuthConsumer consumer = new DefaultOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
				consumer.setTokenWithSecret(ACCESS_TOKEN, ACCESS_TOKEN_SECRET);
				
				try {
					URL url = new URL("http://api.twitter.com/1/friendships/create/"+userAccountId+".xml");
					
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("POST");
					consumer.sign(conn);
					
					conn.connect();
					
					Logger.debug("Twitter follow response: " + conn.getResponseCode() + " "
			                + conn.getResponseMessage());
				} catch (Exception e) {
					Logger.error(e, "Wasn't able to follow Twitter user!");
				}
			}
		});
	}
	
	public static void sendDirect(final String userAccountId, final String text) {
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				OAuthConsumer consumer = new DefaultOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
				consumer.setTokenWithSecret(ACCESS_TOKEN, ACCESS_TOKEN_SECRET);
				
				try {
					String urlParameters =
				        "user_id=" + userAccountId +
				        "&text=" + URLEncoder.encode(text, "UTF-8");
					URL url = new URL("http://api.twitter.com/1/direct_messages/new.xml?"+urlParameters);
					
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("POST");
					
					consumer.sign(conn);
					conn.connect();
					
					System.out.println("Twitter DM response: " + conn.getResponseCode() + " "
			                + conn.getResponseMessage());
				} catch (Exception e) {
					e.printStackTrace();
					Logger.error(e, "Wasn't able to follow Twitter user!");
				}
			}
		});
	}
	
	
	/* Operations from user's account */
	public static void followTAH(final String token, final String tokenSecret) {
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				OAuthConsumer consumer = new DefaultOAuthConsumer(TwitterOAuthProvider.CONSUMER_KEY, TwitterOAuthProvider.CONSUMER_SECRET);
				consumer.setTokenWithSecret(token, tokenSecret);
				
				try {
					URL url = new URL("http://api.twitter.com/1/friendships/create/"+TALKFORHEALTH_ID+".xml");
					
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("POST");
					consumer.sign(conn);
					
					conn.connect();
					System.out.println("Twitter update response: " + conn.getResponseCode() + " "
			                + conn.getResponseMessage());
				} catch (Exception e) {
					Logger.error(e, "Wasn't able to follow Twitter user!");
				}
			}
		});
	}
	
	
	public static void makeUserTwit(String fullText, final String token, final String tokenSecret) {
		if (fullText.length() > 135) {
			fullText = fullText.substring(0, 135)+"...";
		}
		final String text = fullText;
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				OAuthConsumer consumer = new DefaultOAuthConsumer(TwitterOAuthProvider.CONSUMER_KEY, TwitterOAuthProvider.CONSUMER_SECRET);
				consumer.setTokenWithSecret(token, tokenSecret);
				
				try {
					URL url = new URL("http://api.twitter.com/1/statuses/update.xml?status="+URLEncoder.encode(text, "UTF-8"));
					
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("POST");
					consumer.sign(conn);
					
					conn.connect();
					Logger.error("Twitter update response: " + conn.getResponseCode() + " "
			                + conn.getResponseMessage());
				} catch (Exception e) {
					Logger.error(e, "Wasn't able to follow Twitter user!");
				}
			}
		});
	}
	
	public static void importTweets(final String token, final String tokenSecret) {
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				OAuthConsumer consumer = new DefaultOAuthConsumer(TwitterOAuthProvider.CONSUMER_KEY, TwitterOAuthProvider.CONSUMER_SECRET);
				consumer.setTokenWithSecret(token, tokenSecret);
				
				try {
					URL url = new URL("http://api.twitter.com/1/statuses/user_timeline.xml?count=200");
					
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET");
					consumer.sign(conn);
					
					conn.connect();
					
					System.out.println("Twitter update response: " + conn.getResponseCode() + " "
			                + conn.getResponseMessage());
					
					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
					DocumentBuilder db = dbf.newDocumentBuilder();
					Document doc = db.parse(conn.getInputStream());
					
					NodeList statusNodeList = doc.getFirstChild().getChildNodes();
					for (int i=0; i<statusNodeList.getLength(); i++) {
						Node statusNode = statusNodeList.item(i);
						NodeList childNodes = statusNode.getChildNodes();
//						  <created_at>Tue Mar 10 14:17:11 +0000 2009</created_at>
//						  <id>1305504773</id>
//						  <text>hi kan!</text>
						for (int j=0; j<childNodes.getLength(); j++) {
							Node child = childNodes.item(j);
							if (child.getNodeName().equals("text")) {
								System.out.println(":"+child.getFirstChild().getNodeValue());
							}
						}
						

					}
					
				} catch (Exception e) {
					Logger.error(e, "Wasn't able to follow Twitter user!");
				}
			}
		});
	}
	
	public static void loadMentions() {
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				OAuthConsumer consumer = new DefaultOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
				consumer.setTokenWithSecret(ACCESS_TOKEN, ACCESS_TOKEN_SECRET);
				
				try {
					URL url = new URL("http://api.twitter.com/1/statuses/mentions.xml?count=200");
					
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET");
					consumer.sign(conn);
					
					conn.connect();
					
					System.out.println("Twitter update response: " + conn.getResponseCode() + " "
			                + conn.getResponseMessage());
					
					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
					DocumentBuilder db = dbf.newDocumentBuilder();
					Document doc = db.parse(conn.getInputStream());
					
					NodeList statusNodeList = doc.getFirstChild().getChildNodes();
					for (int i=0; i<statusNodeList.getLength(); i++) {
						Node statusNode = statusNodeList.item(i);
						NodeList childNodes = statusNode.getChildNodes();
//						  <created_at>Tue Mar 10 14:17:11 +0000 2009</created_at>
//						  <id>1305504773</id>
//						  <text>hi kan!</text>
						for (int j=0; j<childNodes.getLength(); j++) {
							Node child = childNodes.item(j);
							if (child.getNodeName().equals("text")) {
								System.out.println(":"+child.getFirstChild().getNodeValue());
							}
						}
						

					}
					
				} catch (Exception e) {
					Logger.error(e, "Wasn't able to follow Twitter user!");
				}
			}
		});
	}

	//Input: "Just started <PARAM> "
	//Output: Param truncated according to Twitter limit
	public static String prepareTwit(String twitText, String param) {
		if (param == null) {
			if (twitText.length() > 135) {
				twitText = twitText.substring(0, 131)+" ...";
			}
			return twitText;
		}
		else {
			//all text without param
			int textLength = twitText.length() - 6;
			//135 is better than 140
			int forParam = 135 - textLength;
			
			if (param.length() > forParam) {
				param = param.substring(0, forParam-5)+" ...";
			}
			return twitText.replace("<PARAM>", param);
		}
	}
	
	public static void main(String[] args) {
		//"osezno" account id
//		followUser("23594406");
//		String res = prepareTwit("Hello suasdfsdf asld <PARAM> fjsaldkf jsasad;lfjasdl;kfj sadlfjs daflks adjf supersuperHello super", "COOOOO1000000002000000000300000000000400000000000000000005L");
//		System.out.println(res.length());
//		System.out.println(res);
	}

}

package util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import models.ServiceAccountBean;
import models.Tweet;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.GsonBuilder;

import fr.zenexity.json.JSON;
import groovy.time.BaseDuration.From;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.http.HttpParameters;
import oauth.signpost.http.HttpRequest;
import play.Logger;
import sun.security.krb5.internal.crypto.dk.ArcFourCrypto;
import util.oauth.TwitterOAuthProvider;

/**
 * Implemented using http://code.google.com/p/oauth-signpost/ library
 */
//TODO: later - rewrite it with new Play OAuth API
public class TwitterUtil {
	/* Account @talkabouthealth */
	private static final String CONSUMER_KEY = "dntMSxZl859YGyAeKcTFcg";
	private static final String CONSUMER_SECRET = "gP8XNqM8bpnuYzBSfJDZLMXrDowE58znsZJuwjfAsQ";
	
	private static final String ACCESS_TOKEN = "136322338-kNsQxRvvPQBHYp1EBU8F6CJAKEm9R3FXtT7S19ua";
	private static final String ACCESS_TOKEN_SECRET = "bROleD290VFQCjtTeSfhPm3IBn6sM69uCID1hHJz44";
	
	public static final String TALKABOUTHEALTH_ID = "136322338";
	
	/*------------------ Operations from TAH account ------------------- */
	
	/**
	 * Follow given user by @talkabouthealth
	 * @param userAccountId Twitter Id of user to follow
	 */
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
	
	/**
	 * Send DirectMessage from @talkabouthealth to given user
	 * @param userAccountId
	 * @param text
	 */
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
					
					Logger.debug("Twitter DM response: " + conn.getResponseCode() + " "
			                + conn.getResponseMessage());
				} catch (Exception e) {
					Logger.error(e, "DM sending error");
				}
			}
		});
	}
	
	/**
	 * Load last 10 tweets that contain '@talkabouthealth' in it
	 * @return
	 */
	public static List<Tweet> loadMentions() {
		OAuthConsumer consumer = new DefaultOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
		consumer.setTokenWithSecret(ACCESS_TOKEN, ACCESS_TOKEN_SECRET);
		
		List<Tweet> mentionTweets = new ArrayList<Tweet>();
		try {
			URL url = new URL("http://api.twitter.com/1/statuses/mentions.xml?count=10&trim_user=1");
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			consumer.sign(conn);
			conn.connect();
			
			//parse returned xml to Document
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(conn.getInputStream());
			
			//list of <status> objects
			NodeList statusNodeList = doc.getFirstChild().getChildNodes();
			for (int i=0; i<statusNodeList.getLength(); i++) {
				Node statusNode = statusNodeList.item(i);
				NodeList childNodes = statusNode.getChildNodes();
				if (childNodes.getLength() > 0) {
					//parse Tweet data
					Tweet tweet = new Tweet();
					for (int j=0; j<childNodes.getLength(); j++) {
						Node child = childNodes.item(j);
						String nodeName = child.getNodeName();
						if (nodeName.equals("id")) {
							tweet.setId(child.getFirstChild().getNodeValue());
						}
						else if (nodeName.equals("text")) {
							tweet.setText(child.getFirstChild().getNodeValue());
						}
						else if (nodeName.equals("user")) {
							tweet.setUserId(child.getFirstChild().getNextSibling().getFirstChild().getNodeValue());
						}
					}
					mentionTweets.add(tweet);
				}
			}
		} catch (Exception e) {
			Logger.error(e, "Error loading mentions");
		}
		
		return mentionTweets;
	}
	
	
	/*---------------------- Operations from user's account --------------------- */
	
	/**
	 * Follow @talkabouthealth by given user
	 */
	public static void followTAH(final ServiceAccountBean twitterAccount) {
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				OAuthConsumer consumer = new DefaultOAuthConsumer(TwitterOAuthProvider.CONSUMER_KEY, TwitterOAuthProvider.CONSUMER_SECRET);
				consumer.setTokenWithSecret(twitterAccount.getToken(), twitterAccount.getTokenSecret());
				
				try {
					URL url = new URL("http://api.twitter.com/1/friendships/create/"+TALKABOUTHEALTH_ID+".xml");
					
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("POST");
					consumer.sign(conn);
					conn.connect();
					
					Logger.debug("Twitter update response: " + conn.getResponseCode() + " "
			                + conn.getResponseMessage());
				} catch (Exception e) {
					Logger.error(e, "Wasn't able to follow Twitter user!");
				}
			}
		});
	}
	
	/**
	 * Create tweet with given text from given account
	 * @param fullText
	 * @param twitterAccount
	 */
	public static void tweet(String fullText, final ServiceAccountBean twitterAccount) {
		if (fullText.length() > 140) {
			fullText = fullText.substring(0, 135)+"...";
		}
		final String text = fullText;
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				OAuthConsumer consumer = new DefaultOAuthConsumer(TwitterOAuthProvider.CONSUMER_KEY, TwitterOAuthProvider.CONSUMER_SECRET);
				consumer.setTokenWithSecret(twitterAccount.getToken(), twitterAccount.getTokenSecret());
				
				try {
					URL url = new URL("http://api.twitter.com/1/statuses/update.xml?status="+URLEncoder.encode(text, "UTF-8"));
					
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("POST");
					consumer.sign(conn);
					conn.connect();
					
					Logger.debug("Twitter update response: " + conn.getResponseCode() + " "
			                + conn.getResponseMessage());
				} catch (Exception e) {
					Logger.error(e, "Wasn't able to follow Twitter user!");
				}
			}
		});
	}
	
	/**
	 * Prepares given text for tweet or DM
	 * Input text: "Just started <PARAM> "
	 * Output text: "Just started Param truncated if necess... "
	 */
	public static String prepareTwit(String twitText, String param) {
		if (param == null) {
			if (twitText.length() > 140) {
				twitText = twitText.substring(0, 135)+" ...";
			}
			return twitText;
		}
		else {
			//all text without param
			int textLength = twitText.length() - 7;
			//138 (not 140) to be sure
			int forParam = 138 - textLength;
			
			if (param.length() > forParam) {
				param = param.substring(0, forParam-3)+"...";
			}
			return twitText.replace("<PARAM>", param);
		}
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
					
					Logger.info("Twitter response: " + conn.getResponseCode() + " "
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
	
}

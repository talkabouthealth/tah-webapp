package util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.http.HttpRequest;
import play.Logger;

public class TwitterUtil {
	/* Account @talkforhealth */
	private static final String CONSUMER_KEY = "dntMSxZl859YGyAeKcTFcg";
	private static final String CONSUMER_SECRET = "gP8XNqM8bpnuYzBSfJDZLMXrDowE58znsZJuwjfAsQ";
	
	private static final String ACCESS_TOKEN = "136322338-kNsQxRvvPQBHYp1EBU8F6CJAKEm9R3FXtT7S19ua";
	private static final String ACCESS_TOKEN_SECRET = "bROleD290VFQCjtTeSfhPm3IBn6sM69uCID1hHJz44";
	
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
	
//	public static void main(String[] args) {
//		//"osezno" account id
//		followUser("23594406");
//	}

}
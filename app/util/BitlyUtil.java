package util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import play.libs.WS;
import play.libs.WS.WSRequest;

public class BitlyUtil {
	
	private final static String BITLY_LOGIN = "talkabouthealth";
	private final static String BITLY_APIKEY = "R_3a1665a7d37bf1561af856d0da3523b1";
	
	public static String shortLink(String longURL) {
		String result = WS.url("http://api.bit.ly/v3/shorten?login=%s&apiKey=%s&longUrl=%s&format=txt",
				BITLY_LOGIN, BITLY_APIKEY, longURL).get().getString();
		
		if (result != null && result.length() > 5) {
			//remove carriage return at the end
			result = result.substring(0, result.length()-1);
		}
		
		return result;
	}
}

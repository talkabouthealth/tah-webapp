package util;

import play.Play;
import play.Play.Mode;

public class SearchUtil {
	
	//TODO: move to configurations?
	public static String getTalkerIndex() {
		if (Play.mode == Mode.PROD) {
			return "/data/talkerindex";
		}
		else {
			return "C:\\data\\talkerindex";
		}
	}
	

}

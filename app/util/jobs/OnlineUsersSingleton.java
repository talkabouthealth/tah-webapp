package util.jobs;

import com.tah.im.singleton.googleSingleton;
import com.tah.im.singleton.msnSingleton;
import com.tah.im.singleton.onlineUsersSingleton;
import com.tah.im.singleton.yahooSingleton;

public class OnlineUsersSingleton implements Runnable {
	
	public void run() {
		System.out.println("***Preparing Online User List");
		System.out.println("Initiating Live Conversation Data Structure!");
		googleSingleton.getInstance();
		yahooSingleton.getInstance();
		msnSingleton.getInstance();
		onlineUsersSingleton.getInstance();
		System.out.println("***OnlineUser List Ready");

		boolean run = true;
		while (run) {
			try {
				Thread.sleep(600000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
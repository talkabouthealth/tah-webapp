package util.importers;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logic.TalkerLogic;
import util.CommonUtil;

/**
 * Imports information for matching Health Info with Topics.
 * Used for recommending Topics based on Health Info
 * Format:
 
 <HEALTH_ITEM>|<TOPIC1>,<TOPIC2>,...
 ....
 
 */
public class HealthItems2Topics {
	
	public static void main(String[] args) throws Exception {
		importData("healthitems2topics.dat");
	}
	
	public static void importData(String fileName) throws Exception {
		BufferedReader br = CommonUtil.createImportReader(fileName);
		String line = null;
		
		Map<String, List<String>> healthItems2TopicsMap = new HashMap<String, List<String>>();
		while ((line = br.readLine()) != null) {
			line = line.trim();
			
			String[] lineArr = line.split("\\|");
			String healthItem = lineArr[0];
			String[] topicsArr = lineArr[1].split(",");
			
			healthItems2TopicsMap.put(healthItem, Arrays.asList(topicsArr));
		}
		
		TalkerLogic.setHealthItems2TopicsMap(healthItems2TopicsMap);
	}

}

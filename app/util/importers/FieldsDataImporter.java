package util.importers;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import play.jobs.Job;
import play.jobs.OnApplicationStart;

import logic.TalkerLogic;
import models.TopicBean;
import util.CommonUtil;
import dao.ApplicationDAO;

/**
 * Import field choices (options in <select>) for different profiles (i.e. Physician, Nurse, etc.)
 * Format:

---<FIELD_NAME>|<PROFILE_TYPE>
<OPTION1>
<OPTION2>
....
<EMPTY LINE>

 *
 */
@OnApplicationStart
public class FieldsDataImporter extends Job {
	
	public void doJob() throws Exception {
		importData("fields.dat");
	}
	
	public static void importData(String fileName) throws Exception {
		BufferedReader br = CommonUtil.createImportReader(fileName);
		String line = null;
		
		Map<String, List<String>> fieldsDataMap = new HashMap<String, List<String>>();
		String currentField = null;
		List<String> currentList = new ArrayList<String>();
		currentList.add("");
		while ((line = br.readLine()) != null) {
			line = line.trim();
			//System.out.println(line);
			if (line.length() == 0) {
				if (currentField != null) {
					fieldsDataMap.put(currentField, currentList);

					currentField = null;
					currentList = new ArrayList<String>();
					currentList.add("");
				}
				continue;
			}
			
			if (line.startsWith("---")) {
				currentField = line.substring(3);
				continue;
			}
			currentList.add(line);
		}
		
		TalkerLogic.setFieldsDataMap(fieldsDataMap);
	}

}

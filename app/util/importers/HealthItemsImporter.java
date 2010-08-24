package util.importers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.gson.GsonBuilder;

import dao.HealthItemDAO;

import models.HealthItemBean;

/**
 * Format of the input file is:
   --TopLevel1
   healthitem1
   healthitem2
   healthitem3
   --TopLevel2
   -SubLevel1
   healthitem4
   -SubLevel3
   healthitem5
 *
 */
public class HealthItemsImporter {
	
	//Breast Cancer
	private static final String DEFAULT_DISEASE_ID = "4c2ddd873846000000001f4b";
	
	public static void main(String[] args) throws Exception {
		importHealthItems("D:\\healthitems.txt");
	}
	
	private static void importHealthItems(String fileName) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = null;
		
		HealthItemBean topLevel = null;
		Set<HealthItemBean> topLevelChildren = null;
		HealthItemBean subLevel = null;
		Set<HealthItemBean> subLevelChildren = null;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			
			if (line.length() == 0) {
				continue;
			}
			
			if (line.startsWith("--")) {
				if (topLevel != null) {
					HealthItemDAO.saveTree(topLevel, null, DEFAULT_DISEASE_ID);
				}
				topLevel = new HealthItemBean(line.substring(2));
				topLevelChildren = new LinkedHashSet<HealthItemBean>();
				topLevel.setChildren(topLevelChildren);
				
				subLevel = null;
				subLevelChildren = null;
			}
			else if (line.startsWith("-")) {
				subLevel = new HealthItemBean(line.substring(1));
				topLevelChildren.add(subLevel);
				
				subLevelChildren = new LinkedHashSet<HealthItemBean>();
				subLevel.setChildren(subLevelChildren);
			}
			else {
				HealthItemBean healthItem = new HealthItemBean(line);
				if (subLevelChildren == null) {
					topLevelChildren.add(healthItem);
				}
				else {
					subLevelChildren.add(healthItem);
				}
			}
		}
		
		if (topLevel != null) {
			HealthItemDAO.saveTree(topLevel, null, DEFAULT_DISEASE_ID);
		}
	}


}

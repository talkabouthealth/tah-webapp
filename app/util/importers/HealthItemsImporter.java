package util.importers;

import java.io.BufferedReader;
import java.util.LinkedHashSet;
import java.util.Set;

import util.CommonUtil;
import dao.DiseaseDAO;
import dao.HealthItemDAO;

import models.HealthItemBean;

/**
 * Imports HealthItems for particular disease (described by DEFAULT_DISEASE_ID)
 * 
 * Format of the input file is:

   --<TOP_LEVEL1>
   <HEALTH_ITEM1>
   <HEALTH_ITEM2>
   ...
   --<TOP_LEVEL2>
   -<SUB_LEVEL1>
   <HEALTH_ITEM4>
   -<SUB_LEVEL2>
   <HEALTH_ITEM5>
   ..
   
 *
 * TOP_LEVEL_NAME - symptoms, tests
 * SUB_LEVEL - Screening, Diagnosis
 *
 */
public class HealthItemsImporter {
	
	//Breast Cancer
	private static final String DEFAULT_DISEASE_ID = "4c2ddd873846000000001f4b";
	
	public static void main(String[] args) throws Exception {
		importHealthItems("healthitems.dat");
	}
	
	public static void importHealthItems(String fileName) throws Exception {
		BufferedReader br = CommonUtil.createImportReader(fileName);
		String line = null;
		String diseaseName = null;
		String diseaseId = null;
		String old_diseaseName = null;
		
		HealthItemBean topLevel = null;
		Set<HealthItemBean> topLevelChildren = null;
		HealthItemBean subLevel = null;
		Set<HealthItemBean> subLevelChildren = null;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			if (line.length() == 0) {
				continue;
			}
			if (line.startsWith("---")) {
				diseaseName = null;
				diseaseName = line.substring(3);
				if(old_diseaseName != null && !old_diseaseName.equalsIgnoreCase(diseaseName)){
					if (topLevel != null) 
						if(diseaseId != null)
							HealthItemDAO.saveTree(topLevel, null, diseaseId);
				}
				diseaseId = null;
				topLevel = null;
				topLevelChildren = null;
				subLevel = null;
				subLevelChildren = null;
					
				old_diseaseName = diseaseName;
				diseaseId = DiseaseDAO.getDiseaseByName(diseaseName);
			}else if (line.startsWith("--")) {
				if (topLevel != null) {
					HealthItemDAO.saveTree(topLevel, null, diseaseId);
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
			if(diseaseId != null)
				HealthItemDAO.saveTree(topLevel, null, diseaseId);
			else
				HealthItemDAO.saveTree(topLevel, null, DEFAULT_DISEASE_ID);
		}
	}


}

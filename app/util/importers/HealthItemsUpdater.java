package util.importers;

import java.io.BufferedReader;
import java.util.LinkedHashSet;
import java.util.Set;

import models.HealthItemBean;
import util.CommonUtil;
import dao.HealthItemDAO;

/**
 * Updates names of HealthItems based on give file
 * Format:

<OLD_NAME>|<NEW_NAME>
...

 */
public class HealthItemsUpdater {
	
	public static void main(String[] args) throws Exception {
		updateHealthItems("healthitemsupd.dat");
	}

	public static void updateHealthItems(String fileName) throws Exception {
		BufferedReader br = CommonUtil.createImportReader(fileName);
		String line = null;
		
		while ((line = br.readLine()) != null) {
			line = line.trim();
			
			if (line.length() == 0) {
				continue;
			}
			String[] lineArr = line.split("\\|");
			if (lineArr.length == 2) {
				String current = lineArr[0];
				String after = lineArr[1].trim();
				
				HealthItemBean healthItem = HealthItemDAO.getHealthItemByName(current, null);
				if (healthItem != null) {
					healthItem.setName(after);
					HealthItemDAO.update(healthItem);
				}
			}
		}
	}
}

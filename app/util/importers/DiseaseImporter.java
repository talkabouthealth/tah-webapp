package util.importers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.GsonBuilder;

import models.HealthItemBean;

public class DiseaseImporter {
	
	//Breast Cancer
	private static final String DEFAULT_DISEASE_ID = "4c2ddd873846000000001f4b";
	
	public static void main(String[] args) throws Exception {
		importDiseases("D:\\diseases.txt");
	}
	
	private static void importDiseases(String fileName) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = null;
		
		GsonBuilder gson = new GsonBuilder();
		Map<String, String> m = gson.create().fromJson(br, HashMap.class);
		System.out.println(m);
		
//		while ((line = br.readLine()) != null) {
//			line = line.trim();
//			
//			if (line.length() == 0) {
//				continue;
//			}
//		}
	}

}

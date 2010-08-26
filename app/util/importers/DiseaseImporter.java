package util.importers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.GsonBuilder;

import dao.DiseaseDAO;

import models.DiseaseBean;
import models.HealthItemBean;
import models.DiseaseBean.DiseaseQuestion;
import models.DiseaseBean.DiseaseQuestion.DiseaseQuestionType;

public class DiseaseImporter {
	
	//Breast Cancer
	private static final String DEFAULT_DISEASE_ID = "4c2ddd873846000000001f4b";
	
	public static void main(String[] args) throws Exception {
		importDiseases("D:\\diseases.txt");
	}
	
	private static void importDiseases(String fileName) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = null;
		
		DiseaseBean disease = null;
		DiseaseQuestion currentQuestion = null;
		while ((line = br.readLine()) != null && line.length() != 0) {
			line = line.trim();
			String[] lineArr = line.split(";");
//			System.out.println(Arrays.toString(lineArr));
			if (lineArr.length == 0) {
				continue;
			}
			else if (lineArr[0].equalsIgnoreCase("Disease")) {
				if (disease != null) {
					//save disease to DB
					DiseaseDAO.save(disease);
					disease = null;
				}
				disease = new DiseaseBean();
				disease.setName(lineArr[1]);
				disease.setQuestions(new ArrayList<DiseaseQuestion>());
			}
			else if (lineArr[0].equalsIgnoreCase("choices")) {
				Set<String> choices = new LinkedHashSet<String>();
				for (int i=1; i<lineArr.length; i++) {
					if (lineArr[i].length() != 0) {
						choices.add(lineArr[i]);
					}
				}
				currentQuestion.setChoices(choices);
				
				//for other types (without choices) we should save in other place
				disease.getQuestions().add(currentQuestion);
			}
			else {
				//field info
				//stage;Stage of disease;Stage of disease;select
				currentQuestion = new DiseaseQuestion();
				currentQuestion.setName(lineArr[0]);
				currentQuestion.setText(lineArr[1]);
				currentQuestion.setDisplayName(lineArr[2]);
				
				if (lineArr[3].equalsIgnoreCase("select")) {
					currentQuestion.setType(DiseaseQuestionType.SELECT);
				}
				else {
					currentQuestion.setType(DiseaseQuestionType.MULTI_SELECT);
				}
			}
		}
		
		if (disease != null) {
			//save disease to DB
			DiseaseDAO.save(disease);
			disease = null;
		}
	}

}

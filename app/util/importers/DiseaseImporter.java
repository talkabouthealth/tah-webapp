package util.importers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import models.DiseaseBean;
import models.DiseaseBean.DiseaseQuestion;
import models.DiseaseBean.DiseaseQuestion.DiseaseQuestionType;
import play.Play;
import util.CommonUtil;
import dao.DiseaseDAO;

/**
 * Importer for diseases info and related questions.
 * 
 * Format:

Disease;<NAME_OF_DISEASE>;;;;;
;;;;;;
<QUESTION_NAME>;<QUESTION_TEXT>;<QUESTION_PROFILE_TEXT>;<QUESTION_TYPE(SELECT/MULTISELECT)>;;;
choices;<LIST OF CHOICES FOR COMBOBOX>
;;;;;;
.... other questions

 *
 */
public class DiseaseImporter {
	
	public static void main(String[] args) throws Exception {
		importDiseases("diseases.dat");
	}
	
	public static void importDiseases(String fileName) throws Exception {
		
		BufferedReader br = CommonUtil.createImportReader(fileName);
		
	
		String buffer="";
		String line = null;
		List<String> fileids=new ArrayList<String>();
		
		DiseaseBean disease = null;
		DiseaseQuestion currentQuestion = null;
		while ((line = br.readLine()) != null && line.length() != 0) {
			line = line.trim();
			buffer=buffer+line+"\n";
			String[] lineArr = line.split(";");
			
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
				disease.setFileId(lineArr[6]);
				disease.setUpdateFlag(lineArr[7]);
				fileids.add(lineArr[6]);
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
		 br.close();
		
		 buffer=buffer.replaceAll("true","false");
		 buffer=buffer.replaceAll("insert","false");
		 
		  FileWriter fstream = new FileWriter("/opt/tah-webapp/app/util/importers/data/"+fileName);
		  BufferedWriter out = new BufferedWriter(fstream);
		  out.write(buffer);
		  out.close();
		  DiseaseDAO.removeDisease(fileids);
		  
	}

}

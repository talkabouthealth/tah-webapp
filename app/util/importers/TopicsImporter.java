package util.importers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import play.templates.JavaExtensions;
import util.CommonUtil;

import models.DiseaseBean;
import models.DiseaseBean.DiseaseQuestion;
import models.DiseaseBean.DiseaseQuestion.DiseaseQuestionType;
import models.TopicBean;
import dao.ApplicationDAO;
import dao.DiseaseDAO;
import dao.TopicDAO;

public class TopicsImporter {
	
	public static void main(String[] args) throws Exception {
		importTopics("topics.dat");
	}
	
	public static void importTopics(String fileName) throws Exception {
		BufferedReader br = CommonUtil.createImportReader(fileName);
		String line = null;
		
		List<TopicBean> topics = new ArrayList<TopicBean>();
		
		//parent topics at each level
		TopicBean[] levels = new TopicBean[4];
		while ((line = br.readLine()) != null && line.length() != 0) {
			line = line.trim();
			String[] lineArr = line.split(";");
		
			if (lineArr.length == 0 || lineArr[0].equals("Parent")) {
				continue;
			}
			
//			System.out.println(Arrays.toString(lineArr));
		
			//check all levels
			TopicBean topic = null;
			for (int i=0; i<lineArr.length && i<4; i++) {
				if (!lineArr[i].isEmpty()) {
					//we use 'name' as topic id, because topic aren't saved in the DB yet
					String name = lineArr[i].trim();
					topic = new TopicBean(name);
					topic.setChildren(new LinkedHashSet<TopicBean>());
					topic.setTitle(name);
					topic.setMainURL(ApplicationDAO.createURLName(name));
					
					if (i == 0) {
						topics.add(topic);
					}
					else {
						//not-top topic
						levels[i-1].getChildren().add(topic);
					}
					
					levels[i] = topic;
				}
			}
			
			//check aliases
			if (lineArr.length >= 5) {
				String[] aliases = lineArr[4].split(", ");
				topic.setAliases(new LinkedHashSet<String>(Arrays.asList(aliases)));
			}
		}
		
		for (TopicBean topic : topics) {
			//printTopic(topic, "");
			saveTopic(topic);
		}
	}
	
	private static void saveTopic(TopicBean parent) {
		Set<TopicBean> savedChildren = new LinkedHashSet<TopicBean>();
		for (TopicBean child : parent.getChildren()) {
			saveTopic(child);
			savedChildren.add(child);
		}
		parent.setChildren(savedChildren);
		TopicDAO.save(parent);
	}
	
//	private static void printTopic(TopicBean parent, String prefix) {
//		System.out.println(prefix+parent);
//		for (TopicBean child : parent.getChildren()) {
//			printTopic(child, prefix+"___");
//		}
//	}
}

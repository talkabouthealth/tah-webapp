package util;

import java.util.List;

import logic.TalkerLogic;
import models.ConversationBean;
import models.TalkerBean;
import models.TalkerDiseaseBean;
import dao.ConversationDAO;
import dao.TalkerDAO;
import dao.TalkerDiseaseDAO;

public class DiseaseChangeUtil {

	public static void main(String[] args){
		/*String oldName = "";
		String newName = "";
		String method = "";
		if (args.length == 3){
			oldName = args[0];
			newName = args[1];
			method = args[2];
			if(method.equalsIgnoreCase("talker")){
				updateDiseaseNameInTalker(oldName, newName);
			}else if(method.equalsIgnoreCase("convo")){
				updateDiseaseNameInConvo(oldName, newName);
			}
		}*/
		System.out.println("---------started DiseaseChangeUtility--------------");
		updateDiseaseNameInTalker("Uterine and Corpus Cancer","Uterine and Endometrial Cancer");
		System.out.println("---------completed DiseaseChangeUtility--------------");
	}
	
	/**
	 * Method added for change the diseaseName
	 * @param oldName
	 * @param newName
	 */
	public static void updateDiseaseNameInTalker(String oldName, String newName){
		System.out.println("----------Updateing disease name is talkers---------------------");
		List<TalkerBean> talkersList = TalkerLogic.loadAllTalkersFromCache();
		if(talkersList != null && talkersList.size() > 0){
			for(int index = 0; index < talkersList.size(); index++){
				TalkerBean talker = talkersList.get(index);
				boolean flag = false;
				if(talker != null){
					//update talker category
					String category = talker.getCategory();
					if(category != null && !category.equals("")){
						if(category.equalsIgnoreCase(oldName)){
							flag = true;
							category = newName;
							talker.setCategory(category);
						}
					}
					
					//update talker other categories
					String[] otherCategories = talker.getOtherCategories();
					if(otherCategories != null && otherCategories.length > 0){
						for(int i = 0; i < otherCategories.length; i++){
							String otherCat = otherCategories[i];
							if(otherCat.equalsIgnoreCase(oldName)){
								flag = true;
								otherCat = newName;
								otherCategories[i] = otherCat;
								talker.setOtherCategories(otherCategories);
							}
						}
					}
					
					if(flag == true)
						TalkerDAO.updateTalkerForDisease(talker);
					
					//update talkers disease information
					List<TalkerDiseaseBean> talkerDiseaseList = TalkerDiseaseDAO.getListByTalkerId(talker.getId());
					if(talkerDiseaseList != null && talkerDiseaseList.size() > 0){
						for(int i = 0; i < talkerDiseaseList.size(); i++){
							TalkerDiseaseBean talkerDiseaseBean = talkerDiseaseList.get(i);
							if(talkerDiseaseBean != null){
								String diseaseName = talkerDiseaseBean.getDiseaseName();
								if(diseaseName.equalsIgnoreCase(oldName)){
									diseaseName = newName;
									talkerDiseaseBean.setDiseaseName(diseaseName);
									TalkerDiseaseDAO.saveTalkerDisease(talkerDiseaseBean, talker.getId());
								}
							}
						}
					}
					
				}
			}
		}
		System.out.println("----------Completed updating disease name is talkers---------------------");
	}
	
	public static void updateDiseaseNameInConvo(String oldName, String newName){
		List<ConversationBean> convoList = ConversationDAO.loadAllConversations();
		if(convoList != null && convoList.size() > 0){
			for(int index = 0; index < convoList.size(); index++){
				ConversationBean convo = convoList.get(index);
				if(convo != null){
					//update talker category
					String category = convo.getCategory();
					if(category != null && !category.equals("")){
						if(category.equalsIgnoreCase(oldName)){
							category = newName;
							convo.setCategory(category);
						}
					}
					
					//update talker other categories
					String[] otherCategories = convo.getOtherDiseaseCategories();
					if(otherCategories != null && otherCategories.length > 0){
						for(int i = 0; i < otherCategories.length; i++){
							String otherCat = otherCategories[i];
							if(otherCat.equalsIgnoreCase(oldName)){
								otherCat = newName;
								otherCategories[i] = otherCat;
								convo.setOtherDiseaseCategories(otherCategories);
							}
						}
					}
					
					ConversationDAO.updateConvo(convo);
				}
			}
		}
	}
}

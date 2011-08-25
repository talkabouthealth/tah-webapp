package controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dao.QuestionDAO;
import dao.TalkerDAO;
import dao.UserListDAO;

import models.NotificationBean;
import models.TalkerBean;
import play.mvc.Controller;
import util.CommonUtil;
import util.EmailUtil;
import util.EmailUtil.EmailTemplate;

public class UserList extends Controller{
	public static void index(String id, String password, String action) {
		
		if(action != null && action.equalsIgnoreCase("passwordEditDisplay")){
			if(id != null && !id.equals("")){
				TalkerBean bean = TalkerDAO.getById(id);
				//generate new password
			    String newPassword = CommonUtil.generateRandomPassword();
			    bean.setPassword(newPassword);
				render(bean);
			}
		}else if(action != null && action.equalsIgnoreCase("passwordEdit")){
			TalkerBean bean = TalkerDAO.getById(id);
			if(password != null && !password.equals("")){
				password = CommonUtil.hashPassword(password);
				UserListDAO.updatePassword(id, password);
			}
			
			//Updated password send to user
			Map<String, String> vars = new HashMap<String, String>();
			vars.put("username", bean.getUserName());
			vars.put("newpassword", password);
			EmailUtil.sendEmail(EmailTemplate.FORGOT_PASSWORD, bean.getEmail(), vars, null, false);
			
			List<TalkerBean> list = TalkerDAO.loadAllTalkers(true);
			render(list);
		}else{
			List<TalkerBean> list = TalkerDAO.loadAllTalkers(true);
			render(list);
		}
	}
}

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
import play.mvc.With;
import util.CommonUtil;
import util.EmailUtil;
import util.EmailUtil.EmailTemplate;

@With( { Secure.class, LoggerController.class } )
public class UserList extends Controller{
	
	public static final String ADMIN = "admin";
	
	public static void index(String id, String password, String action, String searchString) {
		
		//check admin user 
		if(!session.get("username").equalsIgnoreCase(ADMIN)){
			redirect("/home");
		}
		
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
				String hashPassword = CommonUtil.hashPassword(password);
				UserListDAO.updatePassword(id, hashPassword);
			}
			
			//Updated password send to user
			Map<String, String> vars = new HashMap<String, String>();
			vars.put("username", bean.getUserName());
			vars.put("newpassword", password);
			EmailUtil.sendEmail(EmailTemplate.FORGOT_PASSWORD, bean.getEmail(), vars, null, false);
			
			List<TalkerBean> list = TalkerDAO.loadAllTalkers(true);
			//removing admin from user list
			for(int index = 0; index < list.size(); index++){
				if(list.get(index).getUserName().equalsIgnoreCase(ADMIN)){
					list.remove(index);
				}
			}
			render(list);
		}else if(action != null && action.equalsIgnoreCase("searchUser")){
			List<TalkerBean> list = TalkerDAO.searchTalkers(searchString);
			for(int index = 0; index < list.size(); index++){
				if(list.get(index).getUserName().equalsIgnoreCase(ADMIN)){
					list.remove(index);
				}
			}
			render(list);
		}else{
			List<TalkerBean> list = TalkerDAO.loadAllTalkers(true);
			//removing admin from user list
			for(int index = 0; index < list.size(); index++){
				if(list.get(index).getUserName().equalsIgnoreCase(ADMIN)){
					list.remove(index);
				}
			}
			render(list);
		}
	}
	
	/**
	 * Use for login as another user from admin. 
	 * @param userName
	 * @throws Throwable
	 */
	public static void loginAsAnotherUser(String userName) throws Throwable{
		
		if(userName != null && !userName.equals("")){
			TalkerBean talkerBean = TalkerDAO.getByUserName(userName);
			if(talkerBean != null){
				Cache.clear();
		        session.clear();
		        response.removeCookie("rememberme");
				Security.authenticate(userName, talkerBean.getPassword(),true);
				session.put("username", talkerBean.getUserName());
				Application.index();
			}
		}
		
	}
	
}

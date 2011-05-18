import java.util.List;
import java.util.Random;

import org.junit.*;

import controllers.Application;

import dao.TalkerDAO;
import play.Logger;
import play.test.*;
import play.data.validation.Valid;
import play.mvc.*;
import play.mvc.Before;
import play.mvc.Http.*;
import logic.TalkerLogic;
import models.*;

public class ApplicationTest extends FunctionalTest {
	private static final String USER_ID="ID_";
	private static final String USER_NAME="member";
	private static final String USER_PASSWORD="pass";
	private static final String USER_EMAIL="fake_email";
	private static final String EMAIL_SUFFIX= "@aim.com";
	private static final int NUM_USERS=12;
	private static Random r= new Random();
	
	@Before
	public void setUp() {
	    Fixtures.deleteAll();
	}
	
    public void testThatIndexPageWorks() {
        Response response = GET("/");
        assertIsOk(response);
        assertContentType("text/html", response);
        assertCharset("utf-8", response);
    }
   
    
    @Test
    public void testRegisterUser(){
    	for(int i=0;i<NUM_USERS;i++){
    		TalkerBean talker=this.createRandomTalker(i);
    		this.assertNotNull(talker);
    		boolean created=Application.registerUser(talker);
    		this.assertTrue("created user "+talker.getUserName(), created);
    		
    	}
    }
    
    @Test
    public void testLoadAllTalkers(){
    	List<TalkerBean> allTalkers=TalkerDAO.loadAllTalkers();
    	this.assertNotNull(allTalkers);
    	for (TalkerBean t : allTalkers){
    		System.out.println(t.getUserName());
    	}
    	
    }
    
    private TalkerBean createRandomTalker(int index){
    	TalkerBean talker=new TalkerBean();
    	talker.setUserName(USER_NAME+index);
    	talker.setPassword(USER_PASSWORD+index);
    	talker.setEmail(USER_EMAIL+index+EMAIL_SUFFIX);
    	talker.setConnection(TalkerBean.CONNECTIONS_ARRAY[r.nextInt(TalkerBean.CONNECTIONS_ARRAY.length)]);
    	return talker;
    	
    }
    
    private void deleteAllTestUsers(){
    	List<TalkerBean> allTalkers=TalkerDAO.loadAllTalkers();
    	for (TalkerBean t : allTalkers){
    		if(t.getUserName().startsWith(USER_NAME) &&
    				t.getPassword().startsWith(USER_PASSWORD) &&
    				t.getEmail().startsWith(USER_EMAIL)){
    			
    		}
    	}
    }
   
    
}
package plugin;

import org.apache.commons.lang.StringUtils;

import com.skype.Application;

import dao.DiseaseDAO;
import play.*;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Scope.Session;

public class TAHGlobalSettings  extends Controller  {

	@Before
    static void checkValidSession() {
		String cancerType = "";
		String[] arr = request.host.split("\\.");
		if (arr != null && arr.length > 0) {
			if(arr.length == 3) {
				//cancerType= arr[0];
				//cancerType = "Breast Cancer";
			} else {
				cancerType = params.get("cancerType");
				if(StringUtils.isBlank(cancerType) ){
					cancerType = session.get("cancerType");
				}
			}
    	} else {
    		cancerType = params.get("cancerType");
    		if(StringUtils.isBlank(cancerType) ){
				cancerType = session.get("cancerType");
			}
    	}
		if(StringUtils.isBlank(cancerType) || DiseaseDAO.getDiseaseByName(cancerType) == null)
			controllers.Application.index();
			//cancerType = null;
		else
			session.put("cancerType", cancerType);

        /*if(session.get("cancerType") != null) {
        	System.out.println("CancerType New: " + session.get("cancerType"));
        }*/
    }
}

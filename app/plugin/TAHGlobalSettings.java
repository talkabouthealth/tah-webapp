package plugin;

import org.apache.commons.lang.StringUtils;

import play.mvc.Before;
import play.mvc.Controller;
import dao.DiseaseDAO;

public class TAHGlobalSettings  extends Controller  {

	@Before
    static void checkValidSession() {
		String cancerType = "";
		String[] arr = request.host.split("\\.");
		if( request.host.contains("talkbreastcancer.com")) {
    		cancerType = "breastcancer";
    	} else if(request.host.contains("173.203.101.101")) {
    		cancerType = params.get("cancerType");
    	} else if (arr != null && arr.length > 0) {
			if(arr.length == 3) {
				cancerType= arr[0];
			} else if(arr.length == 4) {
				cancerType= arr[1];
			} else {
				cancerType = params.get("cancerType");
				if(StringUtils.isBlank(cancerType) ) {
					cancerType = session.get("cancerType");
				}
			}
    	} else {
    		cancerType = params.get("cancerType");
    		if(StringUtils.isBlank(cancerType) ) {
				cancerType = session.get("cancerType");
			}
    	}

		if(StringUtils.isNotBlank(cancerType) && cancerType.equals("breastcancer")) {
			cancerType = "Breast Cancer";
		} else if(StringUtils.isNotBlank(cancerType) && cancerType.equals("lungcancer")) {
			cancerType = "Lung Cancer";
		}

		if(StringUtils.isBlank(cancerType) || DiseaseDAO.getDiseaseByName(cancerType) == null) {
			session.put("def", "1");
			controllers.Application.index();
		} else {
			session.put("def", "0");
			session.put("cancerType", cancerType);
		}
        /*if(session.get("cancerType") != null) {
        	System.out.println("CancerType New: " + session.get("cancerType"));
        }*/
    }
}

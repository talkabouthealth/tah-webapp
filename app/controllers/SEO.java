package controllers;

import org.apache.commons.lang.StringUtils;

import play.mvc.Controller;
import play.mvc.Router.Route;

public class SEO extends Controller  {

	public static void sitemap() {
		String cancerType = session.get("cancerType");;
		if(StringUtils.isNotBlank(cancerType) && cancerType.equals("Breast Cancer")) {
			render("SEO/sitemap_bc.xml");			
		} else {
			render("SEO/sitemap.xml");	
		}
	}
}
package controllers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import dao.TalkerDAO;

import play.Play;
import play.mvc.Controller;

public class Image extends Controller {
	
	public static final String DEFAULT_IMAGE = "public/images/img1.gif"; 
	
	public static void show(String userName) {
//		if (!Security.isConnected()) {
//			renderBinary(Play.getFile(DEFAULT_IMAGE));
//			return;
//		}
		
		byte[] imageArray = TalkerDAO.loadTalkerImage(userName, Security.connected());
		
		response.setHeader("Content-Type", "image/gif");
		if (imageArray == null) {
			//render default
			//TODO: cache it? or redirect?
			renderBinary(Play.getFile(DEFAULT_IMAGE));
		}
		else {
			renderBinary(new ByteArrayInputStream(imageArray));
		}
		
	}
	
}

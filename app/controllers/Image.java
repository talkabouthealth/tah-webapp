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
	
	public static final File DEFAULT_IMAGE_FILE = Play.getFile("public/images/avatar.png");
	
	/**
	 * Renders image of given talker (or returns default image)
	 */
	public static void show(String userName) {
		byte[] imageArray = TalkerDAO.loadTalkerImage(userName, Security.connected());
		
		response.setHeader("Content-Type", "image/gif");
                response.setHeader("Cache-Control", "no-cache");
		if (imageArray == null) {
			//render default
			renderBinary(DEFAULT_IMAGE_FILE);
		}
		else {
			renderBinary(new ByteArrayInputStream(imageArray));
		}
	}
}
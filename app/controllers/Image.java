package controllers;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import play.Play;
import play.mvc.Controller;
import dao.TalkerDAO;

public class Image extends Controller {
	
	public static final File DEFAULT_IMAGE_FILE = Play.getFile("public/images/avatar.png");
	public static final File DEFAULT_SOCIAL_IMAGE_FILE = Play.getFile("public/images/tahfblike.jpg");
	private static final int IMG_WIDTH = 202;
	private static final int IMG_HEIGHT = 202;

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
		} else {
			renderBinary(new ByteArrayInputStream(imageArray));
		}
	}

	public static void showSocialSiteImage(String userName) {
		byte[] imageArray = TalkerDAO.loadTalkerImage(userName, Security.connected());
		byte[] imageArrayOut;
		response.setHeader("Content-Type", "image/gif");
        response.setHeader("Cache-Control", "no-cache");
        if (imageArray == null) {
        	renderBinary(DEFAULT_SOCIAL_IMAGE_FILE);
        } else {
        	imageArrayOut = resizeImageWithHint(imageArray);
        	if(imageArrayOut != null)
        		renderBinary(new ByteArrayInputStream(imageArrayOut));
        	else
        		renderBinary(new ByteArrayInputStream(imageArray));
		}
	}
	
	 private static byte[] resizeImageWithHint(byte[] imageArray) {
		 try {
		 	InputStream in = new ByteArrayInputStream(imageArray);
		 	BufferedImage originalImage = ImageIO.read(in);
		 	
		 	int type = (originalImage.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
		 	
		 	if(originalImage.getHeight() >= IMG_HEIGHT && originalImage.getWidth() >=  IMG_WIDTH) {
		 		return null;
		 	} else {
				BufferedImage resizedImage = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, type);
				Graphics2D g = resizedImage.createGraphics();
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

				g.drawImage(originalImage, 0, 0, IMG_WIDTH, IMG_HEIGHT, null);
				g.dispose();	

				g.setComposite(AlphaComposite.Src);

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write( resizedImage, "jpg", baos );
				baos.flush();
				byte[] imageInByte = baos.toByteArray();

				return imageInByte;
		 	}
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		return null;
	}
}
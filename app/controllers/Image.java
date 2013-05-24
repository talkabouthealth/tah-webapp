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

import models.TalkerImageBean;

import play.Play;
import play.mvc.Controller;
import util.ImageUtil;
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
		TalkerImageBean imageBean = TalkerDAO.loadTalkerImageBean(userName, Security.connected());
		String contentType = "image/" + imageBean.getImageType();

		response.setHeader("Content-Type", contentType);
		response.setHeader("Cache-Control", "no-cache");
		if (imageBean.getImageArray() == null) {
			//render default
			renderBinary(DEFAULT_IMAGE_FILE);
		} else {
			try{
				InputStream in = new ByteArrayInputStream(imageBean.getImageArray());
			 	BufferedImage originalImage = ImageIO.read(in);
				String [] coords = imageBean.getCoords();
		        int xPos = 0;
				int yPos =  0;
				int width =  100;
				int height =  100;
				if(coords != null && coords.length == 4) {
			    	xPos = Integer.parseInt(coords[0]);
			    	yPos = Integer.parseInt(coords[1]);
			    	width = Integer.parseInt(coords[2]);
			    	height = Integer.parseInt(coords[3]);
			    }
				ByteArrayOutputStream baos = null;
				if(coords == null || (originalImage.getWidth() < xPos + width || originalImage.getHeight() < yPos + height)) {
			 		//baos = ImageUtil.createCropedThumbnail(0, 0, originalImage.getWidth(), originalImage.getHeight(), originalImage,imageBean.getImageType());
					//baos = new ByteArrayOutputStream(in);
					renderBinary(new ByteArrayInputStream(imageBean.getImageArray()));
			 	} else {
			 		width = width + xPos;
			 		height = height + yPos;
			 		baos = ImageUtil.crop(xPos, yPos, width, height, originalImage,imageBean.getImageType());
			 		renderBinary(new ByteArrayInputStream(baos.toByteArray()));
			 	}
			}catch ( Exception e ) {
				e.printStackTrace();
				renderBinary(DEFAULT_IMAGE_FILE);
			}
		}
	}

	public static void showForEdit(String userName) {
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
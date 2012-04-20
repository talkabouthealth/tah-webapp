package util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import play.Logger;

public class ImageUtil {
	
	/**
	 * Creates 100x100 thumbnail from given image
	 * @param bsrc
	 * @return
	 * @throws IOException
	 */
	public static ByteArrayOutputStream createThumbnail(BufferedImage bsrc) throws IOException {
		int width = 100;
		int height = 100;
		
		int srcWidth = bsrc.getWidth();
		int srcHeight = bsrc.getHeight();
		int scaleToWidth = 0;
		int scaleToHeight = 0;
                
                if (srcWidth <= width && srcHeight <= height) {
                    width = srcWidth;
                    height = srcHeight;
                }
                if (srcHeight > srcWidth) {
			scaleToWidth = width;
			scaleToHeight = (srcHeight*width)/srcWidth;
		}
		else {
			scaleToWidth = (srcWidth*height)/srcHeight;
			scaleToHeight = height;
		}
		
		BufferedImage bdest = ImageUtil.getScaledInstance(bsrc, scaleToWidth, scaleToHeight, 
				RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
		bdest = bdest.getSubimage(0, 0, width, height);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	ImageIO.write(bdest, "GIF", baos);
    	return baos;
	}
	
	/**
	 * Truncates width and height of given image to 100px max
	 * @param bsrc
	 * @return
	 * @throws IOException
	 */
	public static ByteArrayOutputStream createThumbnailFromFacebook(BufferedImage bsrc) throws IOException {
		int width = bsrc.getWidth();
		int height = bsrc.getHeight();
		if (width > 100) {
			width = 100;
		}
		if (height > 100) {
			height = 100;
		}
		bsrc = bsrc.getSubimage(0, 0, width, height);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	ImageIO.write(bsrc, "GIF", baos);
    	return baos;
	}

	/**
	  * 
	  * Example from article:
	  * http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
	  * 
	  * 
	  * 
    * Convenience method that returns a scaled instance of the
    * provided {@code BufferedImage}.
    *
    * @param img the original image to be scaled
    * @param targetWidth the desired width of the scaled instance,
    *    in pixels
    * @param targetHeight the desired height of the scaled instance,
    *    in pixels
    * @param hint one of the rendering hints that corresponds to
    *    {@code RenderingHints.KEY_INTERPOLATION} (e.g.
    *    {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
    *    {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
    *    {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
    * @param higherQuality if true, this method will use a multi-step
    *    scaling technique that provides higher quality than the usual
    *    one-step technique (only useful in downscaling cases, where
    *    {@code targetWidth} or {@code targetHeight} is
    *    smaller than the original dimensions, and generally only when
    *    the {@code BILINEAR} hint is specified)
    * @return a scaled version of the original {@code BufferedImage}
    */
   public static BufferedImage getScaledInstance(BufferedImage img,
                                          int targetWidth,
                                          int targetHeight,
                                          Object hint,
                                          boolean higherQuality)
   {
       int type = (img.getTransparency() == Transparency.OPAQUE) ?
           BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
       BufferedImage ret = (BufferedImage)img;
       int w, h;
       if (higherQuality) {
           // Use multi-step technique: start with original size, then
           // scale down in multiple passes with drawImage()
           // until the target size is reached
           w = img.getWidth();
           h = img.getHeight();
       } else {
           // Use one-step technique: scale directly from original
           // size to target size with a single drawImage() call
           w = targetWidth;
           h = targetHeight;
       }
       
       int count = 0;
       do {
           if (higherQuality && w > targetWidth) {
               w /= 2;
               if (w < targetWidth) {
                   w = targetWidth;
               }
           }

           if (higherQuality && h > targetHeight) {
               h /= 2;
               if (h < targetHeight) {
                   h = targetHeight;
               }
           }

           BufferedImage tmp = new BufferedImage(w, h, type);
           Graphics2D g2 = tmp.createGraphics();
           g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
           g2.drawImage(ret, 0, 0, w, h, null);
           g2.dispose();

           ret = tmp;
           count++;
       } while (w != targetWidth || h != targetHeight);
       Logger.info("getScaledInstance : " + count);

       return ret;
   }
}

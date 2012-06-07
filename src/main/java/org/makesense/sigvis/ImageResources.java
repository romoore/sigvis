package org.makesense.sigvis;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

public class ImageResources {

  public static BufferedImage IMG_PLAY = null;

  public static BufferedImage IMG_PAUSE = null;
  
  public static BufferedImage IMG_UP_ARROW = null;
  
  public static BufferedImage IMG_DOWN_ARROW = null;
  
  public static BufferedImage IMG_TRANSMITTER = null;
  
  public static BufferedImage IMG_RECEIVER = null;

  static {

    try {
      IMG_PLAY = toCompatibleImage(ImageIO.read(ImageResources.class
          .getResourceAsStream("/images/play_green.png")));
      IMG_PAUSE = toCompatibleImage(ImageIO.read(ImageResources.class
          .getResourceAsStream("/images/pause_yellow.png")));
      IMG_DOWN_ARROW = toCompatibleImage(ImageIO.read(ImageResources.class.getResourceAsStream(
          "/images/arrow_down.blue.png")));
      IMG_UP_ARROW = toCompatibleImage(ImageIO.read(ImageResources.class.getResourceAsStream(
          "/images/arrow_up.red.png")));
      IMG_RECEIVER = toCompatibleImage(ImageIO.read(ImageResources.class.getResourceAsStream(
          "/images/receiver.png")));
      IMG_TRANSMITTER = toCompatibleImage(ImageIO.read(ImageResources.class.getResourceAsStream(
          "/images/fiduciary_transmitter.png")));
    } catch (Exception e) {
      System.err.println("Unable to load image resources!");
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * From http://stackoverflow.com/questions/196890/java2d-performance-issues
   * 
   */
  public static BufferedImage toCompatibleImage(BufferedImage image) {
    GraphicsConfiguration gfx_config = GraphicsEnvironment
        .getLocalGraphicsEnvironment().getDefaultScreenDevice()
        .getDefaultConfiguration();

    if (image.getColorModel().equals(gfx_config.getColorModel())) {
      return image;
    }

    BufferedImage new_image = gfx_config.createCompatibleImage(
        image.getWidth(), image.getHeight(), image.getTransparency());

    Graphics2D g2d = (Graphics2D) new_image.getGraphics();

    g2d.drawImage(image, 0, 0, null);
    g2d.dispose();

    return new_image;
  }

}

/*
 * Signal Visualization Tools for the Owl Platform
 * Copyright (C) 2012 Robert Moore
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.owlplatform.sigvis;

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
  
  public static BufferedImage IMG_ISLAND = null;
  
  public static BufferedImage IMG_KANGAROO_ISLAND = null;

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
      IMG_ISLAND = toCompatibleImage(ImageIO.read(ImageResources.class.getResource("/images/trop_isle.jpg")));
      IMG_KANGAROO_ISLAND = toCompatibleImage(ImageIO.read(ImageResources.class.getResource("/images/kangaroo_isle.jpg")));
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

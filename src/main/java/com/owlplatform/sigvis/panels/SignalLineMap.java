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

package com.owlplatform.sigvis.panels;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.sigvis.DataCache2;
import com.owlplatform.sigvis.DataCache2.ValueType;

public class SignalLineMap extends JComponent implements DisplayPanel {

  private static final Logger log = LoggerFactory
      .getLogger(SignalLineMap.class);

  protected Image backgroundImage = null;

  protected boolean enableAlpha = false;

  protected long lastRepaint = System.currentTimeMillis();

  protected float minValue = 0f;

  protected float maxValue = 10f;

  protected long maxAge = 3000l;

  protected DataCache2 cache;

  protected final ValueType type;
  
  protected long timeOffset = 0l;

  public float getMinValue() {
    return minValue;
  }

  public void setMinValue(float minValue) {
    this.minValue = minValue;
  }

  public Image getBackgroundImage() {
    return backgroundImage;
  }

  public void setBackgroundImage(Image backgroundImage) {
    this.backgroundImage = backgroundImage;
  }

  public SignalLineMap(final ValueType type, final DataCache2 cache) {
    super();
    this.type = type;
    this.cache = cache;
    this.setToolTipText("Signal Line Map");
  }

  @Override
  public String getToolTipText(MouseEvent me) {
    if (this.cache.getRegionBounds() == null) {
      return null;
    }

    Dimension panelDims = this.getSize();

    double mX = me.getPoint().getX();
    double mY = panelDims.getHeight() - me.getPoint().getY();

    // X scale for Screen->Region conversion
    double xS2R = this.cache.getRegionBounds().getMaxX() / panelDims.getWidth();
    double yS2R = this.cache.getRegionBounds().getMaxY()
        / panelDims.getHeight();

    double rX = mX * xS2R;
    double rY = mY * yS2R;
    
    float minCombined = Float.MAX_VALUE;
    String minDevice = null;
    
    for(String rxer : this.cache.getReceiverIds()){
      Point2D location = this.cache.getDeviceLocation(rxer);
      float dist = (float)(Math.abs(location.getX()-rX) + Math.abs(location.getY()-rY));
      if(dist < 10 && dist < minCombined){
        minCombined = dist;
        minDevice = rxer;
      }
    }
    
    for(String txer : this.cache.getFiduciaryTransmitterIds()){
      Point2D location = this.cache.getDeviceLocation(txer);
      float dist = (float)(Math.abs(location.getX()-rX) + Math.abs(location.getY()-rY));
      if(dist < 10 && dist < minCombined){
        minCombined = dist;
        minDevice = txer;
      }
    }
    if(minDevice != null){
      return minDevice;
    }

    return this.cache.getRegionUri() + String.format(" (%.1f, %.1f)", rX, rY);
  }

  // public String

  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    this.lastRepaint = System.currentTimeMillis();
    Graphics2D g2 = (Graphics2D) g;

    g2.setColor(Color.BLACK);
    int screenWidth = this.getWidth();
    int screenHeight = this.getHeight();

    // Nasty assignment hack in if statement
    if (this.backgroundImage == null && (this.backgroundImage = this.cache.getRegionImage()) == null) {
      
      g2.fillRect(0, 0, screenWidth, screenHeight);
    } else {
      g2.drawImage(this.backgroundImage, 0, 0, screenWidth, screenHeight, 0, 0,
          this.backgroundImage.getWidth(null), this.backgroundImage.getHeight(null),
          null);
    }

    Rectangle2D regionBounds = this.cache.getRegionBounds();

    if (regionBounds == null) {
      return;
    }

    float regionWidth = (float) regionBounds.getWidth();
    float regionHeight = (float) regionBounds.getHeight();

    double xScale = screenWidth / regionWidth;
    double yScale = screenHeight / regionHeight;

    float valueRange = this.maxValue - this.minValue;

    Color origColor = g2.getColor();
    Composite origComposite = g2.getComposite();

    Color drawColor = Color.GREEN;

   

    for (String receiverId : this.cache.getReceiverIds()) {
      for (String transmitterId : this.cache.getFiduciaryTransmitterIds()) {
        float value = this.type == ValueType.RSSI ? this.cache
            .getRssiAt(transmitterId, receiverId,this.timeOffset,this.maxAge) : this.cache
            .getVarianceAt(transmitterId, receiverId,this.timeOffset,this.maxAge);
        if (!(value >= this.minValue)) {
          continue;
        }
        
        Point2D receiverLocation = this.cache.getDeviceLocation(receiverId);
        if (receiverLocation == null) {
          continue;
        }
        Point2D transmitterLocation = this.cache
            .getDeviceLocation(transmitterId);
        if (transmitterLocation == null) {
          continue;
        }

        // Random coloring if nothing is set
        float adjusted = 0.9f * (value - this.minValue) / valueRange;
        float hue = adjusted;
        float sat = 0.95f;
        float bright = 0.95f;
        drawColor = new Color(Color.HSBtoRGB(hue, sat, bright));
        g2.setColor(drawColor);

        adjusted += 0.25f;
        if (adjusted > 1) {
          adjusted = 1f;
        }
        if (this.enableAlpha) {
          g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
              adjusted));
        }

        g2.drawLine((int) (receiverLocation.getX() * xScale), screenHeight
            - (int) (receiverLocation.getY() * yScale),
            (int) (transmitterLocation.getX() * xScale), screenHeight
                - (int) (transmitterLocation.getY() * yScale));
      }
    }
    
    this.drawTimestamp(g2, screenWidth, screenHeight);

    g2.setComposite(origComposite);

    g2.setColor(origColor);

  }
  
  protected void drawTimestamp(final Graphics2D g2, int screenWidth, int screenHeight){
    FontRenderContext frc = g2.getFontRenderContext();
    Font currentFont = g2.getFont();
    // Draw current timestamp
    long timestamp = this.cache.isClone() ? this.cache.getCreationTs() : System.currentTimeMillis() ;
    timestamp -= this.timeOffset;
    
    String dateString = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.MEDIUM).format(new Date(timestamp));
    Rectangle2D bounds = currentFont.getStringBounds(dateString, frc);
    Color origColor = g2.getColor();
    g2.setColor(Color.BLACK);
    Rectangle2D background = new Rectangle2D.Float(screenWidth - (float)bounds.getWidth()-9, 5, screenWidth-5, 9+(float)bounds.getHeight() );
    g2.fill(background);
    g2.setColor(Color.WHITE);
    g2.drawString(dateString,screenWidth-(float)bounds.getWidth()-2,(float)bounds.getHeight()+7);
    g2.setColor(origColor);
  }

  private AlphaComposite makeComposite(float alpha) {
    int type = AlphaComposite.SRC_OVER;
    return (AlphaComposite.getInstance(type, alpha));
  }

  public boolean isEnableAlpha() {
    return enableAlpha;
  }

  public void setEnableAlpha(boolean enableAlpha) {
    this.enableAlpha = enableAlpha;
  }

  public float getMaxValue() {
    return maxValue;
  }

  public void setMaxValue(float maxValue) {
    this.maxValue = maxValue;
  }

  @Override
  public void setDisplayedId(String deviceId) {
    // TODO Auto-generated method stub

  }

  @Override
  public String getDisplayedId() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setDeviceIsTransmitter(boolean isTransmitter) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setSelfAdjustMin(boolean selfAdjustMin) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setSelfAdjustMax(boolean selfAdjustMax) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setMaxAge(long maxAge) {
    this.maxAge = maxAge;
  }

  @Override
  public void setDisplayLegend(boolean displayLegend) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setDeviceIcon(BufferedImage icon) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setAntiAlias(boolean antiAlias) {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean isAntiAlias() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean supportsAntiAlias() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void setTransparency(boolean transparency) {
    this.enableAlpha = transparency;
  }

  @Override
  public boolean isTransparency() {
    return this.enableAlpha;
  }

  @Override
  public boolean supportsTransparency() {
    return true;
  }

  public long getTimeOffset() {
    return timeOffset;
  }

  public void setTimeOffset(long timeOffset) {
    this.timeOffset = timeOffset;
  }

  public void setCache(DataCache2 cache) {
    this.cache = cache;
  }
}

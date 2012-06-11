/*
 * Signal Visualization Tools for Make Sense Platform
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

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.sigvis.DataCache2;
import com.owlplatform.sigvis.DataCache2.ValueType;

public class BarChart extends JComponent implements DisplayPanel {

  private static final Logger log = LoggerFactory.getLogger(BarChart.class);

  protected static final int DEFAULT_MARGIN = 40;

  protected static final int MARGIN_TOP = 0;
  protected static final int MARGIN_RIGHT = 1;
  protected static final int MARGIN_BOTTOM = 2;
  protected static final int MARGIN_LEFT = 3;

  protected int[] margins = { DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
      DEFAULT_MARGIN };

  private final Font barFont = new Font("Serif", Font.PLAIN, 14);
  private final Font rssiFont = new Font("Serif", Font.PLAIN, 10);

  protected Color backgroundColor = Color.BLACK;

  protected boolean enableAntiAliasing = true;

  protected float minValue = 0;

  protected float maxValue = 100;

  protected boolean drawHorizontalGrid = true;

  protected Color gridColor = Color.LIGHT_GRAY;

  private String horizontalAxisLabel = "";
  private String verticalAxisLabel = "";

  protected int interBarMargin = 4;

  protected long lastRepaint = 0l;

  protected long maxAge = 3000l;

  protected Color barColor = Color.BLUE;

  protected Color labelColor = Color.WHITE;

  protected float minFps = 8;

  public float getMinFps() {
    return minFps;
  }

  public void setMinFps(float minFps) {
    this.minFps = minFps;
  }

  protected float currFps = 30;

  protected int slowFrames = 0;

  protected DataCache2 cache;

  protected String displayedId = null;

  protected boolean deviceIsTransmitter = true;

  protected ValueType type;
  
  protected long timeOffset = 0l;

  public BarChart(final ValueType type, final DataCache2 cache) {
    this.type = type;
    this.cache = cache;
  }

  public long getMaxAge() {
    return maxAge;
  }

  public void setMaxAge(long maxAge) {
    this.maxAge = maxAge;
  }

  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);

    
    
    this.lastRepaint = System.currentTimeMillis();

    Graphics2D g2 = (Graphics2D) g;
    if (this.enableAntiAliasing) {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON);
    }
    
    Color origColor = g2.getColor();
    Composite origComposite = g2.getComposite();
    Font origFont = g2.getFont();

    int screenWidth = this.getWidth();
    int screenHeight = this.getHeight();

    float valueRange = this.maxValue - this.minValue;

    int usableScreenWidth = screenWidth - this.margins[MARGIN_LEFT]
        - this.margins[MARGIN_RIGHT];
    int usableScreenHeight = screenHeight - this.margins[MARGIN_TOP]
        - this.margins[MARGIN_BOTTOM];

    float valueScale = usableScreenHeight / valueRange;

    float unitHeight = (float) usableScreenHeight / valueRange;

    FontRenderContext frc = g2.getFontRenderContext();

    // Draw background
    g2.setColor(this.backgroundColor);
    g2.fillRect(0, 0, screenWidth, screenHeight);

    // Draw the frame
    this.drawFrames(g2, screenWidth, screenHeight, unitHeight);
    
    long oldestTs = this.lastRepaint - this.maxAge;
    if(this.cache.isClone()){
      oldestTs = this.cache.getCreationTs() - this.maxAge;
    }

    if ((this.type == ValueType.RSSI)
        && (this.cache.getLastRssiUpdate() < oldestTs)) {
      return;
    }
    if ((this.type == ValueType.VARIANCE)
        && (this.cache.getLastRssiUpdate() < oldestTs)) {
      return;
    }
    TreeMap<String, Float> displayedInfo = this.generateDisplayedValues();
    if (displayedInfo == null) {
      log.debug("No data to display.");
      return;
    }

    int numValues = displayedInfo.keySet().size();
    if (numValues == 0) {
      return;
    }

    // Keep 2 px on each side of bar to space values for stroking
    float barWidth = (float) usableScreenWidth / numValues
        - this.interBarMargin;

    int barNumber = 0;
    for (String key = displayedInfo.firstKey(); displayedInfo.higherKey(key) != null; key = displayedInfo
        .higherKey(key)) {

      Float item = displayedInfo.get(key);
      if (!(item > Float.NEGATIVE_INFINITY)) {
        ++barNumber;
        continue;
      }

      float barHeight = (item - this.minValue) * valueScale;
      float barTop = screenHeight - this.margins[MARGIN_BOTTOM] - barHeight;
      if (barTop < this.margins[MARGIN_TOP]) {
        barTop = this.margins[MARGIN_TOP];
        barHeight = screenHeight - this.margins[MARGIN_BOTTOM]
            - this.margins[MARGIN_TOP];

      }
      float barLeft = this.margins[MARGIN_LEFT] + barNumber * (barWidth)
          + this.interBarMargin / 2f;
      if (barNumber > 0)
        barLeft += barNumber * this.interBarMargin;

      Rectangle2D.Float valueBar = new Rectangle2D.Float(barLeft, barTop,
          barWidth, barHeight);

      g2.setColor(this.barColor);
      g2.fill(valueBar);

      g2.setColor(Color.WHITE);
      g2.draw(valueBar);

      ++barNumber;

      String keyAsString = key.toString();

      Rectangle2D labelBounds = this.barFont.getStringBounds(
          String.format("%1.1f", item), frc);

      // Draw RSSI value at top of bar
      g2.setColor(this.labelColor);
      g2.setFont(this.rssiFont);
      g2.drawString(String.format("%1.1f", item), barLeft, barTop);

      labelBounds = this.barFont.getStringBounds(keyAsString, frc);

      AffineTransform textTransform = new AffineTransform();
      Font textFont = g2.getFont();
      textTransform.rotate(Math.PI / 2.0);
      Font vertFont = this.barFont.deriveFont(textTransform);
      g2.setColor(this.labelColor);
      g2.setFont(vertFont);
      g2.drawString(keyAsString, barLeft + 2, barTop + 5);
      g2.setFont(textFont);
      
      
    }
    
    g2.setFont(origFont);

    this.drawTimestamp(g2, screenWidth, screenHeight);
    g2.setColor(origColor);
    g2.setComposite(origComposite);
   
    
    long renderTime = System.currentTimeMillis() - this.lastRepaint;
//    log.debug("Rendered in {}ms", renderTime);
    this.currFps = this.currFps * 0.875f + (1000f / renderTime) * 0.125f;
    
    // if (this.enableAntiAliasing && (this.currFps < this.minFps * 0.9f)) {
    // ++this.slowFrames;
    // if (this.slowFrames > 3) {
    // this.enableAntiAliasing = false;
    // log.warn("FPS: {} Disabling Anti-Aliasing.", this.currFps);
    // }
    // } else if (this.enableAntiAliasing) {
    // this.slowFrames = 0;
    // }
  }
  
  protected void drawTimestamp(final Graphics2D g2, int screenWidth, int screenHeight){
    FontRenderContext frc = g2.getFontRenderContext();
    Font currentFont = g2.getFont();
    
    Color origColor = g2.getColor();
    g2.setColor(this.labelColor);
    // Draw current timestamp
    long timestamp = this.cache.isClone() ? this.cache.getCreationTs() : System.currentTimeMillis() ;
    timestamp -= this.timeOffset;
    
    String dateString = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.MEDIUM).format(new Date(timestamp));
    Rectangle2D bounds = currentFont.getStringBounds(dateString, frc);
    
    g2.drawString(dateString,screenWidth-this.margins[MARGIN_RIGHT]-(float)bounds.getWidth(),this.margins[MARGIN_TOP]-(float)bounds.getHeight()-2);
    g2.setColor(origColor);
  }

  protected void drawFrames(final Graphics g, final int screenWidth,
      final int screenHeight, final float unitHeight) {
    Graphics2D g2 = (Graphics2D) g;
    g2.setColor(this.gridColor);
    FontRenderContext frc = g2.getFontRenderContext();
    Font currentFont = g2.getFont();
    // Draw horizontal lines
    float ticSize = (this.maxValue - this.minValue) / 10;
    for (float tic = this.minValue; tic <= this.maxValue + 0.01f; tic += ticSize) {
      float lineYPos = screenHeight - this.margins[MARGIN_BOTTOM]
          - (tic - this.minValue) * unitHeight;

      Line2D.Float ticLine = new Line2D.Float(this.margins[MARGIN_LEFT],
          lineYPos, screenWidth - this.margins[MARGIN_RIGHT], lineYPos);
      g2.draw(ticLine);

      String ticString = String.format("%03.1f", tic);
      
      Rectangle2D ticStringBounds = currentFont.getStringBounds(ticString, frc);
      g2.drawString(ticString, 0f,
          (float) (lineYPos + (ticStringBounds.getHeight() / 2) - 3));
    }

    // Draw vertical sides
    g2.drawLine(this.margins[MARGIN_LEFT], this.margins[MARGIN_TOP],
        this.margins[MARGIN_LEFT], screenHeight - this.margins[MARGIN_BOTTOM]);
    g2.drawLine(screenWidth - this.margins[MARGIN_RIGHT],
        this.margins[MARGIN_TOP], screenWidth - this.margins[MARGIN_RIGHT],
        screenHeight - this.margins[MARGIN_BOTTOM]);
    
  }

  public float getMinValue() {
    return minValue;
  }

  public void setMinValue(float minValue) {
    this.minValue = minValue;
  }

  public float getMaxValue() {
    return maxValue;
  }

  public void setMaxValue(float maxValue) {
    this.maxValue = maxValue;
  }

  public Color getBackgroundColor() {
    return backgroundColor;
  }

  public void setBackgroundColor(Color backgroundColor) {
    this.backgroundColor = backgroundColor;
  }

  @Override
  public boolean isAntiAlias() {
    return enableAntiAliasing;
  }

  @Override
  public void setAntiAlias(boolean useAntiAlias) {
    this.enableAntiAliasing = useAntiAlias;
  }

  public boolean supportsAntiAlias() {
    return true;
  }

  @Override
  public void setTransparency(boolean transparent) {
    // Does nothing
  }

  @Override
  public boolean isTransparency() {
    // Doesn't use transparency right now.
    return false;
  }

  @Override
  public boolean supportsTransparency() {
    return false;
  }

  public boolean isDrawHorizontalGrid() {
    return drawHorizontalGrid;
  }

  public void setDrawHorizontalGrid(boolean drawHorizontalGrid) {
    this.drawHorizontalGrid = drawHorizontalGrid;
  }

  public Color getGridColor() {
    return gridColor;
  }

  public void setGridColor(Color gridColor) {
    this.gridColor = gridColor;
  }

  public String getHorizontalAxisLabel() {
    return horizontalAxisLabel;
  }

  public void setHorizontalAxisLabel(String horizontalAxisLabel) {
    this.horizontalAxisLabel = horizontalAxisLabel;
  }

  public String getVerticalAxisLabel() {
    return verticalAxisLabel;
  }

  public void setVerticalAxisLabel(String verticalAxisLabel) {
    this.verticalAxisLabel = verticalAxisLabel;
  }

  public int getInterBarMargin() {
    return interBarMargin;
  }

  public void setInterBarMargin(int interBarMargin) {
    this.interBarMargin = interBarMargin;
  }

  protected TreeMap<String, Float> generateDisplayedValues() {
    if (this.displayedId == null) {
      return null;
    }
    TreeMap<String, Float> returnedMap = new TreeMap<String, Float>();
    ValueType currType = this.type;
    List<String> deviceList = this.deviceIsTransmitter ? this.cache
        .getReceiverIds() : this.cache.getFiduciaryTransmitterIds();

    for (String device : deviceList) {
      if (currType == ValueType.RSSI) {
        float value = this.deviceIsTransmitter ? this.cache.getRssiAt(
            this.displayedId, device, this.timeOffset,this.maxAge) : this.cache.getRssiAt(device,
            this.displayedId,this.timeOffset,this.maxAge);
        returnedMap.put(device, Float.valueOf(value));
      } else if (currType == ValueType.VARIANCE) {
        float value = this.deviceIsTransmitter ? this.cache.getVarianceAt(
            this.displayedId, device, this.timeOffset,this.maxAge) : this.cache.getVarianceAt(device,
            this.displayedId,this.timeOffset,this.maxAge);
        returnedMap.put(device, Float.valueOf(value));
      }
    }
    return returnedMap;
  }

  @Override
  public String getDisplayedId() {
    return displayedId;
  }

  public void setDisplayedId(String displayedId) {
    this.displayedId = displayedId;
  }

  public boolean isDeviceTransmitter() {
    return deviceIsTransmitter;
  }

  public void setDeviceIsTransmitter(boolean deviceIsTransmitter) {
    this.deviceIsTransmitter = deviceIsTransmitter;
  }

  @Override
  public void setSelfAdjustMax(boolean selfAdjustMax) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setSelfAdjustMin(boolean selfAdjustMin) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setDisplayLegend(boolean displayLegend) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setDeviceIcon(BufferedImage icon) {
    // TODO Auto-generated method stub

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

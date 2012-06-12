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
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.ToolTipManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.common.util.Pair;
import com.owlplatform.sigvis.DataCache2;
import com.owlplatform.sigvis.structs.SignalToDistanceItem;

/**
 * Draws a map for a single transmitter and multiple receivers, where the
 * signal-to-distance plot of recent data is used to draw distance "rings"
 * around the receivers.
 * 
 * @author Robert Moore
 * 
 */
public class SignalToDistanceMap extends JComponent implements DisplayPanel {

  private static final Logger log = LoggerFactory
      .getLogger(SignalToDistanceMap.class);

  protected long maxAge = 3000;

  protected long maxVarianceAge = 3000;

  public long getMaxAge() {
    return maxAge;
  }

  public void setMaxAge(long maxAge) {
    this.maxAge = maxAge;
  }

  protected String displayedDeviceName = "";

  protected float minValue = -100f;

  protected float maxValue = -30f;

  protected String displayedId = null;

  @Override
  public String getDisplayedId() {
    return displayedId;
  }

  @Override
  public void setDisplayedId(String deviceId) {
    this.displayedId = deviceId;
  }

  protected BufferedImage deviceIcon = null;

  protected long lastRepaint = System.currentTimeMillis();

  protected boolean transparency = true;

  protected float minFps = 8;

  protected float currFps = 30;

  protected int slowFrames = 0;

  protected DataCache2 cache;

  protected boolean isDeviceTransmitter = false;
  
  protected long timeOffset = 0l;

  public boolean isDeviceTransmitter() {
    return isDeviceTransmitter;
  }

  public void setDeviceIsTransmitter(boolean isDeviceTransmitter) {
    this.isDeviceTransmitter = isDeviceTransmitter;
  }

  public SignalToDistanceMap(final DataCache2 cache) {
    super();
    this.cache = cache;
    ToolTipManager.sharedInstance().registerComponent(this);
  }

  public void paintComponent(Graphics g) {
    super.paintComponent(g);

    if (this.cache.getRegionBounds() == null) {
      return;
    }

    this.lastRepaint = System.currentTimeMillis();
    Graphics2D g2 = (Graphics2D) g;

    int screenWidth = this.getWidth();
    int screenHeight = this.getHeight();

    float xScale = screenWidth
        / (float) this.cache.getRegionBounds().getWidth();
    float yScale = screenHeight
        / (float) this.cache.getRegionBounds().getHeight();
    Ellipse2D drawPoint = null;

    float valueRange = this.maxValue - this.minValue;

    Color origColor = g2.getColor();
    Composite origComposite = g2.getComposite();

    g2.setColor(Color.BLACK);
    if (this.cache.getRegionImage() == null) {
      g2.fillRect(0, 0, screenWidth, screenHeight);
    } else {
      g2.drawImage(this.cache.getRegionImage(), 0, 0, screenWidth,
          screenHeight, 0, 0, this.cache.getRegionImage().getWidth(),
          this.cache.getRegionImage().getHeight(), null);
    }

    if (this.displayedId == null) {
      return;
    }

    // Draw icon last if no alpha transparency is used
    // if (this.transparency) {
    // this.drawDeviceIcon(g2, screenWidth, screenHeight);
    // }

    String[] receivers = this.isDeviceTransmitter ? this.cache.getReceiverIds()
        .toArray(new String[] {}) : this.cache.getFiduciaryTransmitterIds()
        .toArray(new String[] {});

    int receiverIndex = 0;

    // Do drawing here
    for (String receiverId : receivers) {
      Float signal = this.isDeviceTransmitter ? this.cache.getRssiAt(
          displayedId, receiverId,this.timeOffset, this.maxAge) : this.cache.getRssiAt(receiverId,
          this.displayedId,this.timeOffset, this.maxAge);
      if (!(signal >= this.minValue) ||  !(signal <= this.maxValue)) {
        continue;
      }

      float variance = this.isDeviceTransmitter ? this.cache
          .getVarianceAt(displayedId, receiverId,this.timeOffset,this.maxAge) : this.cache
          .getVarianceAt(receiverId, this.displayedId,this.timeOffset,this.maxAge);
      // Watch out for Float.NaN, it's NOT less than minValue
      if (!(variance >= this.minValue)) {
        continue;
      }

      Point2D receiverLocation = this.cache.getDeviceLocation(receiverId);
      if (receiverLocation == null) {
        continue;
      }

      float stdDev = (float) Math.sqrt(variance);
      Pair<Float, Float> distanceRange = this.isDeviceTransmitter ? this
          .getDistanceRange(receiverId, this.displayedId, signal.floatValue(),
              stdDev) : this.getDistanceRange(this.displayedId, receiverId,
          signal.floatValue(), stdDev);

      if (distanceRange == null) {

        continue;
      }

      float distanceGap = distanceRange.getValue2() - distanceRange.getValue1();

      // if (distanceGap > this.regionBounds.getWidth()
      // && distanceGap > this.regionBounds.getHeight()) {
      if (distanceGap > 9999f) {
        continue;
      }

      float alphaValue = Math.abs((signal - this.minValue) / valueRange);
      if (alphaValue > 1) {
        alphaValue = 1.0f;
      }

      if (this.transparency) {
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
            alphaValue * .65f));
      }

      Ellipse2D outerCircle = new Ellipse2D.Float();
      float screenX = (float) receiverLocation.getX() * xScale;
      float screenY = screenHeight - (float) receiverLocation.getY() * yScale;
      float outerWidth = distanceRange.getValue2() * xScale;
      float innerWidth = distanceRange.getValue1() * xScale;
      if (distanceGap < 0.001f) {
        outerWidth += 1f;
      }

      float outerHeight = distanceRange.getValue2() * yScale;
      float innerHeight = distanceRange.getValue1() * yScale;

      outerCircle.setFrame(screenX - outerWidth, screenY - outerHeight,
          outerWidth * 2, outerHeight * 2);

      Shape innerCircle = new Ellipse2D.Float();
      ((Ellipse2D) innerCircle).setFrame(screenX - innerWidth, screenY
          - innerHeight, innerWidth * 2, innerHeight * 2);

      Area ring = new Area(outerCircle);
      ring.subtract(new Area(innerCircle));

      // g2.setColor(Color.getHSBColor((float) receiverIndex
      // / receivers.length, 0.95f, .95f));
      g2.setColor(Color.getHSBColor(alphaValue * .66f, 0.95f, .95f));

      g2.fill(ring);

      g2.setColor(Color.BLACK);
      g2.setComposite(origComposite);

      g2.draw(ring);
      // g2.draw(outerCircle);
      // g2.draw(innerCircle);

      ++receiverIndex;
    }
    g2.setComposite(origComposite);
    // Draw icon last if no alpha transparency is used
    // if (!this.transparency) {
    this.drawDeviceIcon(g2, screenWidth, screenHeight);
    
    this.drawTimestamp(g2, screenWidth, screenHeight);
    // }

    long renderTime = System.currentTimeMillis() - this.lastRepaint;
    // log.info("Rendered in {}ms", renderTime);
    this.currFps = this.currFps * 0.875f + (1000f / renderTime) * 0.125f;

    // if (this.transparency && (this.currFps < this.minFps * 0.9f)) {
    // ++this.slowFrames;
    // if (this.slowFrames > 3) {
    // this.transparency = false;
    // log.warn("FPS: {} Disabling Alpha Tranparency.", this.currFps);
    // }
    // } else if (this.transparency) {
    // this.slowFrames = 0;
    // }
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

  protected void drawDeviceIcon(final Graphics g, final int screenWidth,
      final int screenHeight) {
    if (this.deviceIcon == null || this.displayedId == null)
      return;

    Point2D deviceLocation = this.cache.getDeviceLocation(this.displayedId);
    if (deviceLocation == null) {
      return;
    }

    Graphics2D g2 = (Graphics2D) g;
    int imageWidth = this.deviceIcon.getWidth();
    int imageHeight = this.deviceIcon.getHeight();

    float xScale = screenWidth
        / (float) this.cache.getRegionBounds().getWidth();
    float yScale = screenHeight
        / (float) this.cache.getRegionBounds().getHeight();

    g2.drawImage(
        this.deviceIcon,
        (int) (deviceLocation.getX() * xScale - (imageWidth / 2f)),
        (int) (screenHeight - (deviceLocation.getY() * yScale) - (imageHeight / 2f)),
        (int) (deviceLocation.getX() * xScale + (imageWidth / 2f)),
        (int) (screenHeight - (deviceLocation.getY() * yScale) + (imageHeight / 2f)),
        0, 0, imageWidth, imageHeight, null);
  }

  /**
   * 
   * @param receiverId
   * @param signal
   * @param signalDelta
   * @return a Pair<Float,Float> that contains the min distance in value1 and
   *         the max distance in value2
   */
  protected Pair<Float, Float> getDistanceRange(final String receiverId,
      final String excludedId, final float signal, final float signalDelta) {
    List<SignalToDistanceItem> receiverSigToDist = this.cache
        .getSignalToDistance(receiverId);

    if (receiverSigToDist == null) {
      log.warn("No signal-to-distance data for {}", receiverId);
      return null;
    }

    log.debug("Searching for RSSI: {}", signal);

    long oldestTime = System.currentTimeMillis() - this.timeOffset - this.maxAge;
    if(this.cache.isClone()){
      oldestTime = this.cache.getCreationTs() - this.timeOffset - this.maxAge;
    }

    LinkedList<SignalToDistanceItem> points = new LinkedList<SignalToDistanceItem>();

    // Need to get 4 values: next value ABOVE and BELOW signal,
    // max signal <= than signal+signalDelta,
    // and min signal >= than signal-signalDelta
    for (Iterator<SignalToDistanceItem> iter = receiverSigToDist.iterator(); iter
        .hasNext();) {

      SignalToDistanceItem currItem = iter.next();

      if (currItem.getCreationTime() < oldestTime) {
        iter.remove();
        continue;
      }
      // Skip this object in guessing the distance. It's too easy.
      if (currItem.getRxer().equals(excludedId)
          || currItem.getTxer().equals(excludedId)) {
        iter.remove();
        continue;
      }

      final float currSignal = currItem.getSignal();

      if (currSignal <= (signal + signalDelta)
          && currSignal >= (signal - signalDelta)) {
        points.add(currItem);
      }

    }

    SignalToDistanceItem maxDistanceItem = null;
    SignalToDistanceItem minDistanceItem = null;
    // No points found in range, so search for "Next" values
    if (points.size() == 0) {
      SignalToDistanceItem tempMinRssi = new SignalToDistanceItem("", "",
          10000f, -100f);
      SignalToDistanceItem tempMaxRssi = new SignalToDistanceItem("", "", 0f,
          0f);
      for (SignalToDistanceItem currItem : receiverSigToDist) {
        if (currItem.getSignal() > signal
            && currItem.getSignal() < tempMaxRssi.getSignal()) {
          tempMaxRssi = currItem;
        }
        if (currItem.getSignal() < signal
            && currItem.getSignal() > tempMinRssi.getSignal()) {
          tempMinRssi = currItem;
        }
      }

      Pair<Float, Float> distanceRange = new Pair<Float, Float>();
      distanceRange.setValue1(tempMaxRssi.getDistance());
      distanceRange.setValue2(tempMinRssi.getDistance());
      return distanceRange;
    }

    maxDistanceItem = points.getFirst();
    minDistanceItem = points.getFirst();

    for (SignalToDistanceItem currItem : points) {
      if (currItem.getDistance() > maxDistanceItem.getDistance()) {
        maxDistanceItem = currItem;
      }
      if (currItem.getDistance() < minDistanceItem.getDistance()) {
        minDistanceItem = currItem;
      }
    }

    // Min distance will be max RSSI
    Pair<Float, Float> distanceRange = new Pair<Float, Float>();
    distanceRange.setValue1(minDistanceItem.getDistance());
    distanceRange.setValue2(maxDistanceItem.getDistance());

    return distanceRange;
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

  public void clear() {
    this.displayedId = null;
  }

  public String getDisplayedDeviceName() {
    return displayedDeviceName;
  }

  public void setDisplayedDeviceName(String displayedDeviceName) {
    this.displayedDeviceName = displayedDeviceName;
  }

  public BufferedImage getDeviceIcon() {
    return deviceIcon;
  }

  public void setDeviceIcon(BufferedImage deviceImage) {
    this.deviceIcon = deviceImage;
  }

  @Override
  public boolean isTransparency() {
    return transparency;
  }

  @Override
  public void setTransparency(boolean transparency) {
    this.transparency = transparency;
  }

  public float getMinFps() {
    return minFps;
  }

  public void setMinFps(float minFps) {
    this.minFps = minFps;
  }

  public long getMaxVarianceAge() {
    return maxVarianceAge;
  }

  public void setMaxVarianceAge(long maxVarianceAge) {
    this.maxVarianceAge = maxVarianceAge;
  }

  public void setDisplayLegend(final boolean displayLegend) {
    // Nothing.
  }

  public void setSelfAdjustMin(final boolean selfAdjustMin) {
    // Nothing!
  }

  public void setSelfAdjustMax(final boolean selfAdjustMax) {
    // Nothing...
  }

  @Override
  public String getToolTipText(MouseEvent me) {
    if (this.cache.getRegionBounds() == null) {
      return null;
    }

    Dimension panelDims = this.getSize();

    double mX = me.getPoint().getX();
    double mY = panelDims.getHeight() - me.getPoint().getY();

    // Scale for Screen->Region conversion
    double xS2R = this.cache.getRegionBounds().getMaxX() / panelDims.getWidth();
    double yS2R = this.cache.getRegionBounds().getMaxY()
        / panelDims.getHeight();

    double rX = mX * xS2R;
    double rY = mY * yS2R;

    return this.cache.getRegionUri() + String.format(" (%.1f, %.1f)", rX, rY);
  }

  @Override
  public void setAntiAlias(boolean antiAlias) {

  }

  @Override
  public boolean isAntiAlias() {
    return false;
  }

  @Override
  public boolean supportsAntiAlias() {
    return false;
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

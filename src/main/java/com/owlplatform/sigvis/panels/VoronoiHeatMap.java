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
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JComponent;
import javax.swing.ToolTipManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.sigvis.DataCache2;
import com.owlplatform.sigvis.DataCache2.ValueType;
import com.owlplatform.sigvis.structs.Item2DPoint;

import delaunay.Pnt;
import delaunay.Triangle;
import delaunay.Triangulation;

public class VoronoiHeatMap extends JComponent implements DisplayPanel {

  private static final Logger log = LoggerFactory
      .getLogger(VoronoiHeatMap.class);

  protected long maxAge = 3000;

  private static int initialSize = 10000; // Size of initial triangle

  protected String displayedId = null;

  @Override
  public String getDisplayedId() {
    return displayedId;
  }

  @Override
  public void setDisplayedId(String displayedId) {
    this.displayedId = displayedId;
  }

  protected boolean deviceIsTransmitter = false;

  protected ValueType type;

  protected Triangle initialTriangle = null;

  protected float minValue = -100f;

  protected float maxValue = -30f;

  protected BufferedImage deviceImage;

  protected long lastRepaint = 0l;

  protected boolean transparency = true;

  protected float minFps = 8;

  protected float currFps = 30;

  protected int slowFrames = 0;

  protected DataCache2 cache;

  protected long timeOffset;

  public VoronoiHeatMap(final ValueType type, final DataCache2 cache) {
    super();
    this.cache = cache;
    this.type = type;
    initialTriangle = new Triangle(new Pnt(-initialSize, -initialSize),
        new Pnt(initialSize, -initialSize), new Pnt(0, initialSize));
    ToolTipManager.sharedInstance().registerComponent(this);
    this.setToolTipText("plot");
  }

  protected boolean shouldRepaint() {
    if (this.lastRepaint == 0l) {
      return true;
    }
    if (this.type == ValueType.RSSI) {
      return this.cache.getLastRssiUpdate() > this.lastRepaint;
    } else if (this.type == ValueType.VARIANCE) {
      return this.cache.getLastVarianceUpdate() > this.lastRepaint;
    }
    return false;
  }

  public void paintComponent(Graphics g) {
    
    if(this.cache.getRegionBounds() == null){
      return;
    }

    // if(!this.shouldRepaint()){
    // return;
    // }

    super.paintComponent(g);

    this.lastRepaint = System.currentTimeMillis();

    Graphics2D g2 = (Graphics2D) g;

    int screenWidth = this.getWidth();
    int screenHeight = this.getHeight();

    float xScale = screenWidth
        / (float) this.cache.getRegionBounds().getWidth();
    float yScale = screenHeight
        / (float) this.cache.getRegionBounds().getHeight();

    float valueRange = this.maxValue - this.minValue;
    Ellipse2D drawPoint = null;

    Color origColor = g2.getColor();
    Composite origComposite = g2.getComposite();

    g2.setColor(Color.BLACK);
    BufferedImage regionImage = this.cache.getRegionImage();
    if (regionImage == null) {
      g2.fillRect(0, 0, screenWidth, screenHeight);
    } else {
      g2.drawImage(regionImage, 0, 0, screenWidth, screenHeight, 0, 0,
          regionImage.getWidth(), regionImage.getHeight(), null);
    }

    if (this.displayedId == null) {
      return;
    }

    long oldestTs = this.lastRepaint - this.timeOffset - this.maxAge;
    if (this.cache.isClone()) {
      oldestTs = this.cache.getCreationTs() - this.timeOffset - this.maxAge;
    }

    // Draw first if Alpha is enabled as we should still be able to see it
    // With no alpha, draw it last (see below).
    // if (this.transparency) {
    // this.drawDeviceIcon(g2, screenWidth, screenHeight);
    // }

    Composite fillVoronoiComposite = origComposite;

    // g2.setColor(Color.RED);
    if (this.transparency) {
      fillVoronoiComposite = AlphaComposite.getInstance(
          AlphaComposite.SRC_OVER, 0.7f);
    }

    Triangulation dt = new Triangulation(this.initialTriangle);

    HashSet<Point2D> voronoiPoints = new HashSet<Point2D>();
    HashMap<Pnt, Item2DPoint> pntToItem = new HashMap<Pnt, Item2DPoint>();
    // long oldestItem = this.lastRepaint - this.maxAge;

    Map<String, Item2DPoint> heatPoints = this.generateDisplayedValues();

    for (String hash : heatPoints.keySet()) {
      Item2DPoint point = heatPoints.get(hash);
      if (point == null) {
        continue;
      }
      if (point.getUpdateTime() < oldestTs) {
        point.setValue(this.minValue - 1);
        point.setUpdateTime(this.lastRepaint);
      }

      voronoiPoints.add(point.getPoint());
      Pnt newPnt = new Pnt(point.getPoint().getX() * xScale, screenHeight
          - point.getPoint().getY() * yScale);
      pntToItem.put(newPnt, point);
      dt.delaunayPlace(newPnt);
    }

    for (Point2D point : voronoiPoints) {
      Pnt newPnt = new Pnt(point.getX() * xScale, screenHeight - point.getY()
          * yScale);
      dt.delaunayPlace(newPnt);
    }

    // Keep track of sites done; no drawing for initial triangles sites
    HashSet<Pnt> done = new HashSet<Pnt>(initialTriangle);
    for (Triangle triangle : dt) {
      for (Pnt site : triangle) {
        if (done.contains(site))
          continue;
        done.add(site);
        List<Triangle> list = dt.surroundingTriangles(site, triangle);
        Pnt[] vertices = new Pnt[list.size()];
        int i = 0;
        for (Triangle tri : list)
          vertices[i++] = tri.getCircumcenter();
        Item2DPoint item = pntToItem.get(site);
        // if (item.getValue() < this.minValue) {
        // continue;
        // }

        float normalRssi = (float) ((Math.abs(item.getValue() - this.minValue)) / valueRange);
        if (normalRssi < 0) {
          normalRssi = 0;
        } else if (normalRssi > 1.0) {
          normalRssi = 1.0f;
        }

        if (item.getValue() >= this.minValue || normalRssi < 0.01f) {
          this.draw(g, vertices,
              Color.getHSBColor(normalRssi * .66f, 0.9f, 0.8f),
              fillVoronoiComposite);
        } else {
          this.draw(g, vertices, null, null);
        }
        // Draw the point
        this.draw(g, site);
      }
    }

    g2.setComposite(origComposite);

    // Draw icon last if no alpha transparency is used
    // if (!this.transparency) {
    this.drawDeviceIcon(g2, screenWidth, screenHeight);

    // }

    g2.setColor(origColor);

    this.postDraw(g2, screenWidth, screenHeight, heatPoints);
    this.drawTimestamp(g2, screenWidth, screenHeight);
    long renderTime = System.currentTimeMillis() - this.lastRepaint;
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

  protected void postDraw(Graphics2D g2, int screenWidth, int screenHeight,
      Map<String, Item2DPoint> drawnPoints) {
    // Reserved for subclasses
  }

  protected Map<String, Item2DPoint> generateDisplayedValues() {
    if (this.displayedId == null) {
      return null;
    }
    TreeMap<String, Item2DPoint> returnedMap = new TreeMap<String, Item2DPoint>();
    ValueType currType = this.type;
    List<String> deviceList = this.deviceIsTransmitter ? this.cache
        .getReceiverIds() : this.cache.getFiduciaryTransmitterIds();

    for (String device : deviceList) {
      if (currType == ValueType.RSSI) {
        float value = this.deviceIsTransmitter ? this.cache.getRssiAt(
            this.displayedId, device, this.timeOffset, this.maxAge)
            : this.cache.getRssiAt(device, this.displayedId, this.timeOffset,
                this.maxAge);
        Point2D location = this.cache.getDeviceLocation(device);
        if (location == null) {
          continue;
        }
        Item2DPoint newItem = new Item2DPoint((float) location.getX(),
            (float) location.getY(), value);
        returnedMap.put(device, newItem);
      } else if (currType == ValueType.VARIANCE) {
        float value = this.deviceIsTransmitter ? this.cache.getVarianceAt(
            this.displayedId, device, this.timeOffset, this.maxAge)
            : this.cache.getVarianceAt(device, this.displayedId,
                this.timeOffset, this.maxAge);
        Point2D location = this.cache.getDeviceLocation(device);
        if (location == null) {
          continue;
        }
        Item2DPoint newItem = new Item2DPoint((float) location.getX(),
            (float) location.getY(), value);
        returnedMap.put(device, newItem);
      }
    }
    return returnedMap;
  }

  protected void drawTimestamp(final Graphics2D g2, int screenWidth,
      int screenHeight) {
    FontRenderContext frc = g2.getFontRenderContext();
    Font currentFont = g2.getFont();
    // Draw current timestamp
    long timestamp = this.cache.isClone() ? this.cache.getCreationTs() : System
        .currentTimeMillis();
    timestamp -= this.timeOffset;

    String dateString = SimpleDateFormat.getDateTimeInstance(
        SimpleDateFormat.MEDIUM, SimpleDateFormat.MEDIUM).format(
        new Date(timestamp));
    Rectangle2D bounds = currentFont.getStringBounds(dateString, frc);
    Color origColor = g2.getColor();
    g2.setColor(Color.BLACK);
    Rectangle2D background = new Rectangle2D.Float(screenWidth
        - (float) bounds.getWidth() - 9, 5, screenWidth - 5,
        9 + (float) bounds.getHeight());
    g2.fill(background);
    g2.setColor(Color.WHITE);
    g2.drawString(dateString, screenWidth - (float) bounds.getWidth() - 2,
        (float) bounds.getHeight() + 7);
    g2.setColor(origColor);
  }

  protected void drawDeviceIcon(final Graphics g, final int screenWidth,
      final int screenHeight) {
    if (this.deviceImage == null || this.displayedId == null)
      return;

    Point2D deviceLocation = this.cache.getDeviceLocation(this.displayedId);
    if (deviceLocation == null) {
      return;
    }

    Graphics2D g2 = (Graphics2D) g;
    int imageWidth = this.deviceImage.getWidth();
    int imageHeight = this.deviceImage.getHeight();

    float xScale = screenWidth
        / (float) this.cache.getRegionBounds().getWidth();
    float yScale = screenHeight
        / (float) this.cache.getRegionBounds().getHeight();

    g2.drawImage(
        this.deviceImage,
        (int) (deviceLocation.getX() * xScale - (imageWidth / 2f)),
        (int) (screenHeight - (deviceLocation.getY() * yScale) - (imageHeight / 2f)),
        (int) (deviceLocation.getX() * xScale + (imageWidth / 2f)),
        (int) (screenHeight - (deviceLocation.getY() * yScale) + (imageHeight / 2f)),
        0, 0, imageWidth, imageHeight, null);
  }

  /**
   * Draw a point.
   * 
   * @param point
   *          the Pnt to draw
   */
  public void draw(Graphics g, Pnt point) {
    int r = 1;
    int x = (int) point.coord(0);
    int y = (int) point.coord(1);
    g.fillOval(x - r, y - r, r + r, r + r);
  }

  /**
   * Draw a circle.
   * 
   * @param center
   *          the center of the circle
   * @param radius
   *          the circle's radius
   * @param fillColor
   *          null implies no fill
   */
  public void draw(Graphics g, Pnt center, double radius, Color fillColor) {
    int x = (int) center.coord(0);
    int y = (int) center.coord(1);
    int r = (int) radius;
    if (fillColor != null) {
      Color temp = g.getColor();
      g.setColor(fillColor);
      // g.fillOval(x - r, y - r, r + r, r + r);
      g.setColor(temp);
    }
    g.drawOval(x - r, y - r, r + r, r + r);
  }

  /**
   * Draw a polygon.
   * 
   * @param polygon
   *          an array of polygon vertices
   * @param fillColor
   *          null implies no fill
   */
  public void draw(final Graphics g, Pnt[] polygon, Color fillColor,
      Composite fillComposite) {
    Graphics2D g2 = (Graphics2D) g;
    int[] x = new int[polygon.length];
    int[] y = new int[polygon.length];
    for (int i = 0; i < polygon.length; i++) {
      x[i] = (int) polygon[i].coord(0);
      y[i] = (int) polygon[i].coord(1);
    }
    if (fillColor != null) {
      Color temp = g.getColor();
      g.setColor(fillColor);
      Composite origComposite = g2.getComposite();
      g2.setComposite(fillComposite);

      g.fillPolygon(x, y, polygon.length);
      g.setColor(temp);
      g2.setComposite(origComposite);
    }
    g.drawPolygon(x, y, polygon.length);
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

  @Override
  public boolean isTransparency() {
    return transparency;
  }

  @Override
  public void setTransparency(boolean transparency) {
    this.transparency = transparency;
  }

  @Override
  public boolean supportsTransparency() {
    return true;
  }

  public float getMinFps() {
    return minFps;
  }

  public void setMinFps(float minFps) {
    this.minFps = minFps;
  }

  public long getMaxAge() {
    return maxAge;
  }

  public void setMaxAge(long maxAge) {
    this.maxAge = maxAge;
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
    this.deviceImage = icon;
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

    return this.cache.getRegionUri() + String.format(" (%.1f, %.1f)", rX, rY);
  }

  @Override
  public void setAntiAlias(boolean antiAlias) {
    // Not supported
  }

  @Override
  public boolean isAntiAlias() {
    return false;
  }

  @Override
  public boolean supportsAntiAlias() {

    return false;
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

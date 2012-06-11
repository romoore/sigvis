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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JPanel;

import org.grailrtls.libcommon.SampleMessage;
import org.grailrtls.libcommon.util.HashableByteArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.sigvis.DataCache2;
import com.owlplatform.sigvis.DataCache2.ValueType;
import com.owlplatform.sigvis.structs.ChartItem;
import com.owlplatform.sigvis.structs.RSSILine;

import compgeom.RLine2D;
import compgeom.RLineSegment2D;
import compgeom.RPoint2D;
import compgeom.Rational;
import compgeom.algorithms.BentleyOttmann;
import compgeom.algorithms.ShamosHoey;

public class IntersectionLineMap extends SignalLineMap {

  private static final Logger log = LoggerFactory
      .getLogger(IntersectionLineMap.class);

  protected Collection<Point2D> intersections = null;

  protected boolean dirty = true;

  public IntersectionLineMap(final ValueType type, final DataCache2 cache) {
    super(type, cache);
  }

  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);

    Graphics2D g2 = (Graphics2D) g;

    Rectangle2D regionBounds = this.cache.getRegionBounds();
    if (regionBounds == null) {
      return;
    }

    int screenWidth = this.getWidth();
    int screenHeight = this.getHeight();

    float regionWidth = (float) regionBounds.getWidth();
    float regionHeight = (float) regionBounds.getHeight();

    double xScale = screenWidth / regionWidth;
    double yScale = screenHeight / regionHeight;

    float valueRange = this.maxValue - this.minValue;

    
    Set<RSSILine> validLines = new HashSet<RSSILine>();
    for (String receiverId : this.cache.getReceiverIds()) {
      for (String transmitterId : this.cache.getFiduciaryTransmitterIds()) {
        float value = this.type == ValueType.RSSI ? this.cache
            .getRssiAt(transmitterId, receiverId,this.timeOffset,this.maxAge) : this.cache
            .getVarianceAt(transmitterId, receiverId,this.timeOffset,this.maxAge);
        if (!(value>= this.minValue)) {
          continue;
        }

        RSSILine newLine = new RSSILine();
        newLine.setReceiver(receiverId);
        newLine.setTransmitter(transmitterId);
        newLine.setValue(value);
        newLine.setLine(new Line2D.Float(this.cache
            .getDeviceLocation(receiverId), this.cache
            .getDeviceLocation(transmitterId)));
        validLines.add(newLine);

      }
    }

    this.intersections = this.generateIntersections(validLines);
    log.debug("Generated {} intersections.", this.intersections.size());

    Color origColor = g2.getColor();

    g2.setColor(Color.RED);

    Ellipse2D drawEllipse = null;

    for (Point2D intersectPoint : intersections) {
      drawEllipse = new Ellipse2D.Float(
          (float) (intersectPoint.getX() * xScale - 1),
          (float) (screenHeight - intersectPoint.getY() * yScale) - 1, 2, 2);
      g2.fill(drawEllipse);
    }

    g2.setColor(origColor);
  }

  protected Collection<Point2D> generateIntersections(
      final Collection<RSSILine> rssiLines) {
    Set<RLineSegment2D> compLines = new HashSet<RLineSegment2D>();
    RLineSegment2D newLine = null;
    RPoint2D newPoint1 = null;
    Rational x1, x2, y1, y2;
    RPoint2D newPoint2 = null;
    for (RSSILine line : rssiLines) {
      if (line.getLine() == null) {
        Point2D recLoc = this.cache.getDeviceLocation(line.getReceiver());
        Point2D devLoc = this.cache.getDeviceLocation(line.getTransmitter());
        if (recLoc == null || devLoc == null) {
          continue;
        }

        line.setLine(new Line2D.Float(recLoc, devLoc));
      }
      x1 = new Rational(Float.toString(line.getLine().x1));
      x2 = new Rational(Float.toString(line.getLine().x2));

      y1 = new Rational(Float.toString(line.getLine().y1));
      y2 = new Rational(Float.toString(line.getLine().y2));

      newPoint1 = new RPoint2D(x1, y1);
      newPoint2 = new RPoint2D(x2, y2);
      newLine = new RLineSegment2D(newPoint1, newPoint2);
      compLines.add(newLine);
    }

    for (RLineSegment2D compLine : compLines) {
      log.debug("Created line: {}", compLine);
    }

    // Set<RPoint2D> interCompPoints = BentleyOttmann.intersections(compLines);
    Set<RPoint2D> interCompPoints = BentleyOttmann.intersections(compLines);

    LinkedList<Point2D> returnPoints = new LinkedList<Point2D>();

    Point2D point = null;

    for (RPoint2D compPoint : interCompPoints) {
      point = new Point2D.Float(compPoint.x.floatValue(),
          compPoint.y.floatValue());
      returnPoints.add(point);
    }

    for (Point2D aPoint : returnPoints) {
      log.debug("Generating intersection @ {}", aPoint);
    }

    return returnPoints;
  }
}

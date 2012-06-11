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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import com.owlplatform.sigvis.DataCache2;
import com.owlplatform.sigvis.DataCache2.ValueType;
import com.owlplatform.sigvis.structs.Item2DPoint;
import com.owlplatform.sigvis.structs.RSSILine;

public class VoronoiRSSIQualityMap extends VoronoiHeatMap {

  public VoronoiRSSIQualityMap(ValueType type, DataCache2 cache) {
    super(type, cache);
    // Don't need a device, but parent will expect a non-null value.
this.displayedId = "";
  }
  
  @Override
  public void setDisplayedId(String displayedId){
    // Ignored
  }

  protected Map<String, RSSILine> maxLines = new ConcurrentHashMap<String, RSSILine>();

  @Override
  protected Map<String, Item2DPoint> generateDisplayedValues() {
    this.maxLines.clear();

    // if (this.displayedId == null) {
    // return null;
    // }
    TreeMap<String, Item2DPoint> returnedMap = new TreeMap<String, Item2DPoint>();
    ValueType currType = this.type;
    List<String> receiverList = this.cache.getReceiverIds();
    List<String> transmitterList = this.cache.getFiduciaryTransmitterIds();

    for (String receiver : receiverList) {
      Point2D rxPoint = this.cache.getDeviceLocation(receiver);
      if (rxPoint == null) {
        continue;
      }
      Item2DPoint newPoint = new Item2DPoint((float) rxPoint.getX(),
          (float) rxPoint.getY(), Float.NaN);
      returnedMap.put(receiver,newPoint);
      float maxTxValue = this.minValue - 1;
      String maxTx = null;
      Point2D maxPoint = null;
      for (String transmitter : transmitterList) {
        float value = (this.type == ValueType.RSSI ? this.cache.getRssiAt(
            transmitter, receiver, this.timeOffset, this.maxAge) : this.cache
            .getVarianceAt(transmitter, receiver, this.timeOffset, this.maxAge));
        if (value < maxTxValue) {
          continue;
        }
        Point2D point = this.cache.getDeviceLocation(transmitter);
        if (point == null) {
          continue;
        }
        if (value > maxTxValue) {
          maxPoint = point;
          maxTxValue = value;
          maxTx = transmitter;
        }
      }
      newPoint.setValue(maxTxValue);
      if (maxTxValue > this.minValue - .5) {
        RSSILine newLine = new RSSILine();
        newLine.setReceiver(receiver);
        newLine.setTransmitter(maxTx);
        newLine.setValue(maxTxValue);
        newLine.setLine(new Line2D.Float(rxPoint, maxPoint));
        this.maxLines.put(receiver, newLine);
      }
    }
    
    for (String transmitter : transmitterList) {
      Point2D txPoint = this.cache.getDeviceLocation(transmitter);
      if (txPoint == null) {
        continue;
      }
      Item2DPoint newPoint =new Item2DPoint((float)txPoint.getX(), (float)txPoint.getY(), Float.NaN);
      returnedMap.put(transmitter,newPoint);
      float maxRxValue = this.minValue - 1;
      String maxRx = null;
      Point2D maxPoint = null;
      for (String receiver : receiverList) {
        float value = (this.type == ValueType.RSSI ? this.cache.getRssiAt(
            transmitter, receiver, this.timeOffset, this.maxAge) : this.cache
            .getVarianceAt(transmitter, receiver, this.timeOffset, this.maxAge));
        if (value < maxRxValue) {
          continue;
        }
        Point2D point = this.cache.getDeviceLocation(receiver);
        if (point == null) {
          continue;
        }
        if (value > maxRxValue) {
          maxPoint = point;
          maxRxValue = value;
          maxRx = receiver;
        }
      }
      
        newPoint.setValue(maxRxValue);
        if (maxRxValue > this.minValue - .5) {
          RSSILine newLine = new RSSILine();
          newLine.setReceiver(transmitter);
          newLine.setTransmitter(maxRx);
          newLine.setValue(maxRxValue);
          newLine.setLine(new Line2D.Float(txPoint, maxPoint));
          this.maxLines.put(transmitter, newLine);
        }
      
//        returnedMap.put(transmitter, newPoint);
        
//      }
    }
    return returnedMap;
  }

  @Override
  protected void postDraw(Graphics2D g2, int screenWidth, int screenHeight,
      Map<String, Item2DPoint> drawnItems) {

    Stroke origStroke = g2.getStroke();
    
    Stroke borderStroke = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    Color borderColor = Color.BLACK;
    
    Stroke lineStroke = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    
    float xScale = screenWidth
        / (float) this.cache.getRegionBounds().getWidth();
    float yScale = screenHeight
        / (float) this.cache.getRegionBounds().getHeight();
    Composite origComposite = g2.getComposite();
    
    for (String receiverId : this.maxLines.keySet()) {
      
      RSSILine line = this.maxLines.get(receiverId);
      float valueRange = this.maxValue - this.minValue;
      // Random coloring if nothing is set
      float normalRssi = (float) ((Math.abs(line.getValue() - this.minValue)) / valueRange);
      if (normalRssi < 0) {
        normalRssi = 0;
      } else if (normalRssi > 1.0) {
        normalRssi = 1.0f;
      }
      
      
      if (this.transparency) {
        
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
            normalRssi));
      }
      Color drawColor =  Color.getHSBColor(normalRssi * .66f, 1f, 1f);
      
      Line2D drawLine = new Line2D.Float((line.getLine().x1 * xScale), screenHeight
          - (line.getLine().y1 * yScale),
          (line.getLine().x2 * xScale),
          screenHeight - (line.getLine().y2 * yScale));
      
//      g2.setStroke(borderStroke);
//      g2.setColor(borderColor);
//      g2.draw(drawLine);
//      
      g2.setColor(drawColor);
//      g2.setStroke(lineStroke);
      
      g2.draw(drawLine);
    }
    g2.setComposite(origComposite);
    g2.setStroke(origStroke);
  }
  
}

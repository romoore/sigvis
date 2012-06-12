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

import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

import com.owlplatform.sigvis.DataCache2;
import com.owlplatform.sigvis.DataCache2.ValueType;
import com.owlplatform.sigvis.structs.ChartItem;

public class RssiStDvLineChart extends LineChart {

  public RssiStDvLineChart(DataCache2 cache) {
    super(ValueType.RSSI, cache);

  }

  @Override
  protected float drawStream(final Graphics g, final String rxer,
      final String txer, final Collection<ChartItem<Float>> streamValues,
      final int screenWidth, final int screenHeight) {

    if (streamValues == null) {

      return this.minValue;
    }
    boolean containsMax = false;

    // ChartItem<Float>[] items = streamValues.toArray(new ChartItem[] {});
    if (streamValues.size() == 0) {

      return this.minValue;
    }

    // Always called with RSSI, need to figure-out the variance at each point

    int baseYLevel = screenHeight - this.margins[MARGIN_BOTTOM];

    float usableWidth = screenWidth - this.margins[MARGIN_LEFT]
        - this.margins[MARGIN_RIGHT];

    float timeScale = usableWidth / this.maxAge;

    float valueRange = this.maxValue - this.minValue;
    if (valueRange < 0.01f) {
      valueRange = 1;
    }

    float valueScale = (baseYLevel - this.margins[MARGIN_TOP] - this.legendHeight)
        / valueRange;

    float itemXLocation = this.margins[MARGIN_LEFT];
    float previousXLocation = -1;
    float previousYLocation = -1;

    ChartItem<Float> previousItem = null;
    Graphics2D g2 = (Graphics2D) g;

    Composite origComposite = g2.getComposite();

    ArrayList<Polygon> fillPolys = new ArrayList<Polygon>();

    ArrayList<Integer> xValues = new ArrayList<Integer>();
    ArrayList<Integer> yValues = new ArrayList<Integer>();

    Stack<Integer> revXValues = new Stack<Integer>();
    Stack<Integer> revYValues = new Stack<Integer>();

    // boolean isFirst = true;
    boolean previousGap = true;
    float maxValue = this.minValue;

    long currentTime = this.lastRepaint;

    long youngestItem = this.lastRepaint - this.timeOffset;
    if (this.cache.isClone()) {
      youngestItem = this.cache.getCreationTs() - this.timeOffset;
      currentTime = this.cache.getCreationTs();
    }
    long oldestItem = youngestItem - this.maxAge;

    long lastItemTime = 0l;
    GeneralPath itemPath = new GeneralPath();
    for (ChartItem<Float> item : streamValues) {

      if (item.getCreationTime() < oldestItem) {
        continue;
      }
      if (item.getCreationTime() > youngestItem) {
        break;
      }
      float value = item.getValue().floatValue();
      if (value > maxValue) {
        maxValue = value;
      }

      float variance = this.cache.getVarianceAt(txer, rxer,
          currentTime - item.getCreationTime(), 1000l);

      if (!(variance >= 1f)) {
        variance = 0f;
      } else {
        variance = (float) Math.sqrt(variance);
      }

      itemXLocation = screenWidth - this.margins[MARGIN_RIGHT]
          - (youngestItem - item.getCreationTime()) * timeScale;
      float itemYLocation = (baseYLevel - (value - this.minValue) * valueScale);

      boolean finished = false;
      
      // First check to see if we should "close" an existing polygon
      // 1. A gap in the data means shut-down the polygon back at the previous
      // location
      long gap = item.getCreationTime() - lastItemTime;
      if (gap > MAX_TIME_GAP) {

        // POLY: If we have a previous polygon, finish it off... (end "]")
        if (xValues.size() > 0) {
          xValues.add(Integer.valueOf((int) previousXLocation));
          yValues.add(Integer.valueOf((int) previousYLocation));
          Polygon poly = this.finishAndBuildPoly(xValues, yValues, revXValues,
              revYValues);
          fillPolys.add(poly);
          finished = true;
        }
        // POLY: Don't keep old data around after a gap
        xValues.clear();
        yValues.clear();
        revXValues.clear();
        revYValues.clear();

        // LINE&POLY: Don't use old location data after a gap (includes lines)
        previousXLocation = -1;
        previousYLocation = -1;
      }
      // When variance goes to 0, finish-off any polygons
      else if (variance < 0.01f) {
        // POLY: If we have a previous polygon, finish it off... (end ">")
        if (xValues.size() > 0) {
          xValues.add(Integer.valueOf((int) itemXLocation));
          yValues.add(Integer.valueOf((int) itemYLocation));
          Polygon poly = this.finishAndBuildPoly(xValues, yValues, revXValues,
              revYValues);
          fillPolys.add(poly);
          finished = true;
        }

        // POLY: Don't keep old data around once we get to 0
        xValues.clear();
        yValues.clear();
        revXValues.clear();
        revYValues.clear();
      }

      // POLY: We drawing polygons and we have a non-zero variance.
      // POLY: We should either start or continue a polygon
      if (variance > 0.01f) {
        // POLY: New polygon
        if (xValues.isEmpty()) {
          // We can use the previous value ("<" start)
          if (previousXLocation > 0) {
            xValues.add(Integer.valueOf((int) previousXLocation));
            yValues.add(Integer.valueOf((int) previousYLocation));
          }
          // Most likely a gap before this, no previous value ("[" start)
          else {
            xValues.add(Integer.valueOf((int) itemXLocation));
            yValues.add(Integer.valueOf((int) itemYLocation));
          }
        }

        // POLY: New or continuation
        // Add the +/- variance values to the polygon
        xValues.add(Integer.valueOf((int) itemXLocation));
        yValues.add(Integer.valueOf((int) (itemYLocation + variance
            * valueScale)));

        revXValues.push(Integer.valueOf((int) itemXLocation));
        revYValues.push(Integer.valueOf((int) (itemYLocation - variance
            * valueScale)));

      }

      // LINE: Draw a line from the previous point
      if (previousXLocation >= 0) {
        if (xValues.isEmpty() && !finished) {
          itemPath.lineTo(itemXLocation, itemYLocation);
        }else{
          itemPath.moveTo(itemXLocation,itemYLocation);
        }

      } else {
        itemPath.moveTo(itemXLocation, itemYLocation);
      }
      
      // Prepare for next iteration
      previousXLocation = itemXLocation;
      previousYLocation = itemYLocation;
      lastItemTime = item.getCreationTime();

      previousItem = item;

    }

    g2.draw(itemPath);
 // POLY: We've reached the end of the data, and haven't closed the poly
    // (end with "]")
    if (xValues.size() > 0) {
      xValues.add(Integer.valueOf((int) previousXLocation));
      yValues.add(Integer.valueOf((int) previousYLocation));

      fillPolys.add(this.finishAndBuildPoly(xValues, yValues, revXValues,
          revYValues));
    }
    if (fillPolys.size() > 0) {

      

      for (Polygon p : fillPolys) {
        g2.draw(p);
        if (this.useTransparency) {
          g2.setComposite(this.fillUnderAlpha);
        }
        g2.fill(p);
        g2.setComposite(origComposite);
      }

    }
    g2.setComposite(origComposite);
    // if (this.selfAdjustMax
    // && previousItem != null
    // && previousItem.getValue() < (this.maxValue - valueRange * .35f)) {
    //
    // this.adjustMaxY(previousItem.getValue(), valueRange);
    // }

    return maxValue;
  }

  private Polygon finishAndBuildPoly(List<Integer> xValues,
      List<Integer> yValues, Stack<Integer> xRev, Stack<Integer> yRev) {

    while (!xRev.isEmpty()) {
      xValues.add(xRev.pop());
      yValues.add(yRev.pop());
    }

    return this.buildPoly(xValues, yValues);
  }

  private Polygon buildPoly(List<Integer> xValues, List<Integer> yValues) {
    Integer[] xIntegers = xValues.toArray(new Integer[] {});
    Integer[] yIntegers = yValues.toArray(new Integer[] {});

    int[] xInts = new int[xIntegers.length];
    int[] yInts = new int[yIntegers.length];
    for (int i = 0; i < xIntegers.length; ++i) {
      xInts[i] = xIntegers[i].intValue();
    }
    for (int i = 0; i < yIntegers.length; ++i) {
      yInts[i] = yIntegers[i].intValue();
    }

    return new Polygon(xInts, yInts, xInts.length);
  }

  @Override
  protected Collection<ChartItem<Float>> generateDisplayedData(String txer,
      String rxer) {
    return this.cache.getRssiList(rxer, txer);
  }

  @Override
  public boolean supportsTransparency() {
    return true;
  }
}

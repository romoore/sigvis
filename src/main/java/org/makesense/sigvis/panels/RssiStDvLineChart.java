package org.makesense.sigvis.panels;

import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Stack;

import org.makesense.sigvis.DataCache2;
import org.makesense.sigvis.DataCache2.ValueType;
import org.makesense.sigvis.structs.ChartItem;

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
    float previousVariance = 0f;
    ChartItem<Float> previousItem = null;
    Graphics2D g2 = (Graphics2D) g;

    Composite origComposite = g2.getComposite();

    ArrayList<Polygon> fillPolys = new ArrayList<Polygon>();

    ArrayList<Integer> xValues = new ArrayList<Integer>();
    ArrayList<Integer> yValues = new ArrayList<Integer>();

    Stack<Integer> revXValues = new Stack<Integer>();
    Stack<Integer> revYValues = new Stack<Integer>();

    boolean isFirst = true;

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

      long gap = item.getCreationTime() - lastItemTime;
      if (gap > MAX_TIME_GAP || variance < 0.01f) {
        isFirst = true;
        // If we have a previous polygon, finish it off...
        if (xValues.size() > 0) {
          xValues.add(Integer.valueOf((int) itemXLocation));
          yValues.add(Integer.valueOf((int) itemYLocation));

          while (!revXValues.isEmpty()) {
            xValues.add(revXValues.pop());
            yValues.add(revYValues.pop());
          }

          fillPolys.add(this.buildPoly(xValues, yValues));
        }
        xValues.clear();
        yValues.clear();
        if (gap > MAX_TIME_GAP) {

          previousXLocation = -1;
          previousYLocation = -1;
          previousVariance = 0f;
        }
      }
      lastItemTime = item.getCreationTime();

      if (this.useTransparency && variance > 0.01f) {
        if (isFirst) {

          isFirst = false;
          if (previousXLocation >= 0) {
            
            xValues.add(Integer.valueOf((int) previousXLocation));
            yValues.add(Integer.valueOf((int) previousYLocation));
          } else {
            
            xValues.add(Integer.valueOf((int) itemXLocation));
            yValues.add(Integer.valueOf((int) itemYLocation));
          }
        } 
          xValues.add(Integer.valueOf((int) itemXLocation));
          yValues.add(Integer.valueOf((int) (itemYLocation + variance
              * valueScale)));

          revXValues.push(Integer.valueOf((int) itemXLocation));
          revYValues.push(Integer.valueOf((int) (itemYLocation - variance
              * valueScale)));

        

      }

      // Draw a line from the previous point
      if (previousXLocation >= 0) {

        itemPath.lineTo(itemXLocation, itemYLocation);

      } else {
        itemPath.moveTo(itemXLocation, itemYLocation);
      }

      // Prepare for next iteration
      previousXLocation = itemXLocation;
      previousYLocation = itemYLocation;
      previousVariance = variance;

      previousItem = item;

    }

    g2.draw(itemPath);

    if (this.useTransparency && fillPolys.size() > 0) {

      if (xValues.size() > 0) {
        xValues.add(Integer.valueOf((int) previousXLocation));
        yValues.add(Integer.valueOf((int) previousYLocation));

        while (!revXValues.isEmpty()) {
          xValues.add(revXValues.pop());
          yValues.add(revYValues.pop());
        }

        fillPolys.add(this.buildPoly(xValues, yValues));
      }

      for (Polygon p : fillPolys) {
        g2.draw(p);
        g2.setComposite(this.fillUnderAlpha);
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

  private Polygon buildPoly(ArrayList<Integer> xValues,
      ArrayList<Integer> yValues) {
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

}

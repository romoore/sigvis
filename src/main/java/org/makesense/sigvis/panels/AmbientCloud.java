package org.makesense.sigvis.panels;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Stack;

import org.grailrtls.libcommon.util.Pair;
import org.makesense.sigvis.DataCache2;
import org.makesense.sigvis.ImageResources;
import org.makesense.sigvis.structs.ChartItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmbientCloud extends RssiStDvLineChart {
  private static final Logger log = LoggerFactory.getLogger(AmbientCloud.class);

  public AmbientCloud(DataCache2 cache) {
    super(cache);
    this.displayedId = "";
    this.fillUnderAlpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
        .5f);
    this.margins[MARGIN_TOP] = 0;
    this.margins[MARGIN_BOTTOM] = 0;
    this.margins[MARGIN_LEFT] = 0;
    this.margins[MARGIN_RIGHT] = 0;
  }

  public void paintComponent(Graphics graphics) {
    super.paintComponent(graphics);

    if (this.scrolling) {
      this.lastRepaint = System.currentTimeMillis();
    }

    Graphics2D g2 = (Graphics2D) graphics;
    if (this.enableAntiAliasing) {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON);
    }

    int screenWidth = this.getWidth();
    int screenHeight = this.getHeight();

    this.margins[MARGIN_BOTTOM] = (int) (screenHeight * .45f);

    Color origColor = g2.getColor();
    Composite origComposite = g2.getComposite();

    this.drawBackground(g2, screenWidth, screenHeight);

    Color drawColor = Color.WHITE;

    for (String rxId : this.cache.getReceiverIds()) {

      for (String txId : this.cache.getFiduciaryTransmitterIds()) {

        // TODO: Grab variance list instead
        Collection<ChartItem<Float>> sampleList = this.generateDisplayedData(
            txId, rxId);

        if (sampleList == null || sampleList.isEmpty()) {

          continue;
        }

        float tempMax = this.drawStream(g2, rxId, txId, sampleList,
            screenWidth, screenHeight);

      }
    }
    long timestamp = this.cache.isClone() ? this.cache.getCreationTs()
        - this.timeOffset : System.currentTimeMillis() - this.timeOffset;
    this.drawTimeOfDay(g2, screenWidth, screenHeight, timestamp);
    this.drawTimestamp(g2, screenWidth, screenHeight);

  }

  @Override
  protected Collection<ChartItem<Float>> generateDisplayedData(String txer,
      String rxer) {
    return this.cache.getRssiList(rxer, txer);
  }

  @Override
  protected void drawBackground(Graphics2D g2, int screenWidth, int screenHeight) {
    if (ImageResources.IMG_ISLAND != null) {
      g2.drawImage(ImageResources.IMG_ISLAND, 0, 0, screenWidth, screenHeight,
          0, 0, ImageResources.IMG_ISLAND.getWidth(),
          ImageResources.IMG_ISLAND.getHeight(), null);
    } else {
      g2.setColor(Color.BLUE);
      g2.fillRect(0, 0, screenWidth, screenHeight);

    }
  }

  protected void drawTimeOfDay(Graphics2D g2, int screenWidth,
      int screenHeight, long timestamp) {
    // 0 - 23
    Calendar currCal = Calendar.getInstance();
    currCal.setTimeInMillis(timestamp);
    float hourOfDay = (currCal.get(Calendar.HOUR_OF_DAY) + 4) % 24;
    // Add 4 hours, we want to dim when between 8pm and 7am
    // hourOfDay = (hourOfDay + 4) % 24;

    // Default to no dimming
    float alpha = 0f;

    if (hourOfDay < 11) {
      // Dim from 8pm-10pm, lighten from 5am-7am, dark between

      if (hourOfDay < 2 || hourOfDay > 9) {
        int minutes = ((int) hourOfDay % 2) * 60;
        minutes += currCal.get(Calendar.MINUTE);
        if (hourOfDay < 2) {
          alpha = (minutes / 120f) * .7f;
        } else {
          alpha = .7f - (minutes / 120f) * .7f;
        }

      } else {
        alpha = .7f;
      }

    }

    if (alpha > 0.001f) {
      Composite origComposite = g2.getComposite();
      Color origColor = g2.getColor();
      g2.setColor(Color.BLACK);
      g2.setComposite(AlphaComposite
          .getInstance(AlphaComposite.SRC_OVER, alpha));
      g2.fillRect(0, 0, screenWidth, screenHeight);
      g2.setColor(origColor);
      g2.setComposite(origComposite);
    }
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

    Graphics2D g2 = (Graphics2D) g;

    Composite origComposite = g2.getComposite();

    ArrayList<Polygon> fillPolys = new ArrayList<Polygon>();

    ArrayList<Integer> xValues = new ArrayList<Integer>();
    ArrayList<Integer> yValues = new ArrayList<Integer>();

    Stack<Integer> revXValues = new Stack<Integer>();
    Stack<Integer> revYValues = new Stack<Integer>();

    float maxValue = this.minValue;

    long currentTime = System.currentTimeMillis();

    // Bump 5 seconds to right side
    long youngestItem = currentTime - this.timeOffset;
    if (this.cache.isClone()) {
      currentTime = this.cache.getCreationTs();
      youngestItem = currentTime - this.timeOffset;

    }
    // Bump 5 seconds to left side
    long oldestItem = youngestItem - this.maxAge - 5000l;

    boolean startPoly = true;

    long lastItemTime = 0l;

    for (ChartItem<Float> item : streamValues) {

      if (item.getCreationTime() < oldestItem) {
        continue;
      }
      if (item.getCreationTime() > (youngestItem + 5000l)) {
        break;
      }
      float value = item.getValue().floatValue();
      if (value > maxValue) {
        maxValue = value;
      }

      float variance = this.cache.getVarianceAt(txer, rxer,
          currentTime - item.getCreationTime(), 1000l);

      if (!(variance >= 15f)) {
        variance = 0f;
      } else {
        variance = (float) Math.sqrt(variance);
      }

      itemXLocation = screenWidth - this.margins[MARGIN_RIGHT]
          - (youngestItem - item.getCreationTime()) * timeScale;

      float itemYLocation = (value - this.minValue) * valueScale;
      if (itemYLocation > baseYLevel) {
        itemYLocation = baseYLevel;
      }

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
        }
        // POLY: Don't keep old data around after a gap
        xValues.clear();
        yValues.clear();
        revXValues.clear();
        revYValues.clear();

        // LINE&POLY: Don't use old location data after a gap (includes lines)
        previousXLocation = -1;
        previousYLocation = -1;
        startPoly = true;
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
        }

        // POLY: Don't keep old data around once we get to 0
        xValues.clear();
        yValues.clear();
        revXValues.clear();
        revYValues.clear();
        startPoly = true;
      }

      // POLY: We drawing polygons and we have a non-zero variance.
      // POLY: We should either start or continue a polygon
      if (variance > 0.01f) {
        // POLY: New polygon
        if (xValues.isEmpty()) {
          // We can use the previous value ("<" start)
          if (!startPoly) {
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
        startPoly = false;
      }

      // Prepare for next iteration
      previousXLocation = itemXLocation;
      previousYLocation = itemYLocation;
      lastItemTime = item.getCreationTime();

    }

    // g2.draw(itemPath);
    g2.setColor(Color.WHITE);
    // POLY: We've reached the end of the data, and haven't closed the poly
    // (end with "]")
    if (xValues.size() > 0) {
      xValues.add(Integer.valueOf((int) previousXLocation));
      yValues.add(Integer.valueOf((int) previousYLocation));

      fillPolys.add(this.finishAndBuildPoly(xValues, yValues, revXValues,
          revYValues));
    }
    if (fillPolys.size() > 0) {

      for (Polygon poly : fillPolys) {
        // g2.draw(p);
        if (this.useTransparency) {
          g2.setComposite(this.fillUnderAlpha);
        }
        g2.fill(poly);
        Collection<Pair<Arc2D, ArrayList<Float>>> arcs = this
            .generateArcs(poly);
        AffineTransform origTrans = g2.getTransform();
        for (Pair<Arc2D, ArrayList<Float>> pair : arcs) {
          Arc2D arc = pair.getValue1();
          ArrayList<Float> coords = pair.getValue2();
          float phi = coords.get(0), cornerX = coords.get(1), cornerY = coords
              .get(2);

          g2.translate(cornerX, cornerY);
          g2.rotate(phi);
          g2.fill(arc);
          g2.setTransform(origTrans);
        }
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

  protected Collection<Pair<Arc2D, ArrayList<Float>>> generateArcs(Polygon poly) {
    ArrayList<Pair<Arc2D, ArrayList<Float>>> arcs = new ArrayList<Pair<Arc2D, ArrayList<Float>>>();

    int currX;
    int currY;

    int prevX = poly.xpoints[poly.npoints - 1];
    int prevY = poly.ypoints[poly.npoints - 1];

    for (int i = 0; i < poly.npoints; ++i) {
      currX = poly.xpoints[i];
      currY = poly.ypoints[i];

      Point2D.Float leftPoint = new Point2D.Float(currX, currY);
      Point2D.Float rightPoint = new Point2D.Float(prevX, prevY);

      if (leftPoint.getX() > rightPoint.getX()) {
        Point2D.Float tmp = leftPoint;
        leftPoint = rightPoint;
        rightPoint = tmp;
      }

      float midX = (float) (leftPoint.getX() + rightPoint.getX()) / 2;
      float midY = (float) (leftPoint.getY() + rightPoint.getY()) / 2;
      float width = (float) Math.sqrt(Math.pow(
          leftPoint.getX() - rightPoint.getX(), 2)
          + Math.pow(leftPoint.getY() - rightPoint.getY(), 2));
      float height = width;

      float cornerX = midX - width / 2;
      float cornerY = midY - width / 2;

      float phi = 0f;

      float rise = (float) (rightPoint.getY() - leftPoint.getY());
      float run = (float) (rightPoint.getX() - leftPoint.getX());

      if (run == 0) {
        if (i >= poly.npoints / 2) {
          phi = (float) Math.PI / 2f;
        } else {
          phi = 3 * (float) Math.PI / 2f;
        }
      } else if (rise == 0) {
        if (i >= poly.npoints / 2) {
          phi = 0;
        } else {
          phi = (float) Math.PI;
        }
      } else {
        float slope = rise / run;
        phi = (float) Math.atan(slope);

        if (i <= poly.npoints / 2) {
          phi += Math.PI;
        }

        if (i == 0 && slope > 0) {
          System.out.println("First point...");
          phi += 3 * Math.PI / 2;
        }

      }

      Arc2D arc = new Arc2D.Float(-width / 2, -height / 2, width, height, 0,
          180, Arc2D.PIE);
      Pair<Arc2D, ArrayList<Float>> pair = new Pair<Arc2D, ArrayList<Float>>();
      ArrayList<Float> coords = new ArrayList<Float>(3);
      coords.add(phi);
      coords.add(cornerX + width / 2);
      coords.add(cornerY + height / 2);
      pair.setValue1(arc);
      pair.setValue2(coords);

      arcs.add(pair);

      prevX = currX;
      prevY = currY;
    }

    return arcs;
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
  protected void drawPauseInfo(Graphics g, int screenWidth, int screenHeight) {
  }

  @Override
  protected void adjustMaxY(float tempMax, float yRange) {
  }

  @Override
  protected void drawLegend(final Graphics g, final List<String> devices,
      final int screenWidth, final int screenHeight) {
  }

  @Override
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

    Composite origComposite = g2.getComposite();
    if (this.useTransparency) {
      g2.setComposite(this.fillUnderAlpha);
    }

    g2.fill(background);
    g2.setColor(Color.WHITE);
    g2.drawString(dateString, screenWidth - (float) bounds.getWidth() - 2,
        (float) bounds.getHeight() + 7);
    g2.setColor(origColor);
    g2.setComposite(origComposite);
  }

  @Override
  protected void drawChartBorders(Graphics g, final int screenWidth,
      final int screenHeight) {
  }

  @Override
  protected void drawChartGrid(Graphics g, final int screenWidth,
      final int screenHeight, float xStep, float yStep) {
  }

  @Override
  public void setDisplayedId(String id) {
    // Ignored
  }
}

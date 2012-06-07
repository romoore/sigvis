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
package org.makesense.sigvis.panels;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.grailrtls.libcommon.util.LRUCache;
import org.makesense.sigvis.ImageResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScatterPlotPanel extends JComponent {

	private static final Logger log = LoggerFactory
			.getLogger(ScatterPlotPanel.class);

	protected static final int DEFAULT_MARGIN = 30;

	protected static final int MARGIN_TOP = 0;
	protected static final int MARGIN_RIGHT = 1;
	protected static final int MARGIN_BOTTOM = 2;
	protected static final int MARGIN_LEFT = 3;

	protected int[] margins = { DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
			DEFAULT_MARGIN };

	protected int maxElements = 2000;

	protected final ConcurrentLinkedQueue<Point2D> dataPoints = new ConcurrentLinkedQueue<Point2D>();

	protected volatile int numPoints = 0;

	protected float minXValue = 0;

	protected float maxXValue = 100;

	protected float minYValue = -100;

	protected float maxYValue = 0;

	protected String displayedInfo = null;

	protected long lastRepaint = 0l;

	protected boolean enableAntiAliasing = true;

	protected long minFps = 15;

	protected boolean selfAdjustMax = false;

	protected boolean selfAdjustMin = false;

	protected float yAdjustValue = 0f;
	
	protected float adjustInterval = 1.0f;
	
	protected BufferedImage downArrowImg = null;
	
	protected BufferedImage upArrowImg = null;
	
	public ScatterPlotPanel(){
		super();
		 this.upArrowImg = ImageResources.IMG_UP_ARROW;
	    this.downArrowImg = ImageResources.IMG_DOWN_ARROW;
	}

	public long getMinFps() {
		return minFps;
	}

	public void setMinFps(long minFps) {
		this.minFps = minFps;
	}

	protected float currFps = 30;

	protected int slowFrames = 0;
	
	protected boolean amRaiseMaxY = false;
	
	protected boolean amLowerMaxY = false;

	public boolean isEnableAntiAliasing() {
		return enableAntiAliasing;
	}

	public void setEnableAntiAliasing(boolean enableAntiAlias) {
		this.enableAntiAliasing = enableAntiAlias;
	}

	public void setMaxYValue(float maxYValue) {
		this.maxYValue = maxYValue;
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

		int screenWidth = this.getWidth();
		int screenHeight = this.getHeight();

		g2.setColor(Color.BLACK);
		g2.fillRect(0, 0, screenWidth, screenHeight);

		float xRange = this.maxXValue - this.minXValue;
		float yRange = this.maxYValue - this.minYValue;

		int usableWidth = screenWidth - this.margins[MARGIN_LEFT]
				- this.margins[MARGIN_RIGHT];
		int usableHeight = screenHeight - this.margins[MARGIN_TOP]
				- this.margins[MARGIN_BOTTOM];

		float xScale = usableWidth / xRange;
		float yScale = usableHeight / yRange;

		Ellipse2D drawPoint = new Ellipse2D.Float();

		this.drawChartBorders(g2, screenWidth, screenHeight);
		this.drawChartGrid(g2, screenWidth, screenHeight, xRange / 5f,
				yRange / 5f);
		this.drawDisplayedInfo(g2, screenWidth, screenHeight);

		g2.setColor(Color.WHITE);

		float tempMax = Float.MIN_VALUE;

		for (Point2D point : this.dataPoints) {
			float xOnScreen = this.margins[MARGIN_LEFT]
					+ (float) (point.getX() - this.minXValue) * xScale;
			float yOnScreen = screenHeight - this.margins[MARGIN_BOTTOM]
					- (float) (point.getY() - this.minYValue) * yScale;

			if (point.getY() > tempMax) {
				tempMax = (float) point.getY();
			}

			drawPoint.setFrame(xOnScreen - 1.5f, yOnScreen - 1.5f, 3f, 3f);

			g2.fill(drawPoint);

		}
		if (this.selfAdjustMax) {
			this.adjustMaxY(tempMax, yRange);
		}
		g2.setColor(origColor);
		g2.setComposite(origComposite);
		
		this.drawAdjustInfo(g2, screenWidth, screenHeight);

		long renderTime = System.currentTimeMillis() - this.lastRepaint;
		this.currFps = this.currFps * 0.875f + (1000f / renderTime) * 0.125f;

		if (this.enableAntiAliasing && (this.currFps < this.minFps * 0.9f)) {
			++this.slowFrames;
			if (this.slowFrames > 3) {
				this.enableAntiAliasing = false;
				log.warn("FPS: {} Disabling Anti-Aliasing.", this.currFps);
			}
		} else if (this.enableAntiAliasing) {
			this.slowFrames = 0;
		}
	}
	
	protected void drawAdjustInfo(Graphics g, int screenWidth, int screenHeight){
		if(this.amLowerMaxY && this.downArrowImg != null){
			g.drawImage(this.downArrowImg, screenWidth - this.margins[MARGIN_RIGHT], this.margins[MARGIN_TOP], screenWidth,this.margins[MARGIN_TOP]+this.margins[MARGIN_RIGHT],0,0,this.downArrowImg.getWidth(), this.downArrowImg.getHeight(),null);
		} else if(this.amRaiseMaxY && this.upArrowImg != null){
			g.drawImage(this.upArrowImg, screenWidth - this.margins[MARGIN_RIGHT], this.margins[MARGIN_TOP], screenWidth,this.margins[MARGIN_TOP]+this.margins[MARGIN_RIGHT],0,0,this.upArrowImg.getWidth(), this.upArrowImg.getHeight(),null);
		}
	}

	protected void adjustMaxY(float tempMax, float yRange) {

		float adjustBy = 0f;
		// Check to see if we need to adjust...
		if (tempMax < (this.maxYValue - yRange * 0.15)) {
			float diff = this.maxYValue - tempMax;
			
			if(this.yAdjustValue < diff){
				this.yAdjustValue = diff;
			}
			
			// If not adjusting, then set up the adjustment interval
//			if (!this.amLowerMaxY) {
//				this.yAdjustValue = this.maxYValue - tempMax;
				this.amLowerMaxY = true;
				this.amRaiseMaxY = false;
//			}
			adjustBy = this.yAdjustValue * (1f / (this.adjustInterval*this.minFps));
			
			if(adjustBy > diff){
				adjustBy = diff;
			}
		
			this.maxYValue -= adjustBy;
			if (this.maxYValue < this.minYValue + 1) {
				this.maxYValue = this.minYValue + 1;
			}
		} else if(tempMax > this.maxYValue){
			float diff = tempMax*1.1f - this.maxYValue;
			
			if(diff > this.yAdjustValue){
				this.yAdjustValue = diff;
			}
//			
//			if(!this.amRaiseMaxY){
//				this.yAdjustValue = tempMax*1.1f - this.maxYValue;
				this.amRaiseMaxY = true;
				this.amLowerMaxY = false;
//			}
			adjustBy = this.yAdjustValue * (1f/(this.adjustInterval*this.minFps));
			
			if(adjustBy > diff){
				adjustBy = diff;
			}
			this.maxYValue += adjustBy;
		}
		
		else {
			this.yAdjustValue = 0f;
			this.amRaiseMaxY = false;
			this.amLowerMaxY = false;
			return;
		}

		
		/*
		 * else if (tempMax > this.maxYValue - yRange * 0.1f) { float increaseBy
		 * = (tempMax - this.maxYValue) (1f / this.minFps);
		 * log.info("MFPS: {}, Increase by {}", this.minFps, increaseBy);
		 * this.maxYValue += increaseBy; }
		 */
	}

	protected void drawDisplayedInfo(Graphics g, final int screenWidth,
			final int screenHeight) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setColor(Color.WHITE);
		String actualDisplay = this.displayedInfo;
		if (actualDisplay == null) {
			actualDisplay = "[UNAVAILABLE]";
			g2.setColor(Color.RED);
		}

		FontMetrics metrics = g2.getFontMetrics();
		int stringWidth = metrics.stringWidth(actualDisplay);
		g2.drawString(actualDisplay, screenWidth - this.margins[MARGIN_RIGHT]
				- stringWidth, this.margins[MARGIN_TOP]);

	}

	protected void drawChartBorders(Graphics g, final int screenWidth,
			final int screenHeight) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setColor(Color.LIGHT_GRAY);
		// Draw left border
		Line2D.Float borderLine = new Line2D.Float();
		borderLine.setLine(this.margins[MARGIN_LEFT], this.margins[MARGIN_TOP],
				this.margins[MARGIN_LEFT], screenHeight
						- this.margins[MARGIN_BOTTOM]);
		g2.draw(borderLine);

		// Draw right border
		borderLine.setLine(screenWidth - this.margins[MARGIN_RIGHT],
				this.margins[MARGIN_TOP], screenWidth
						- this.margins[MARGIN_RIGHT], screenHeight
						- this.margins[MARGIN_BOTTOM]);
		g2.draw(borderLine);

		// Draw the bottom border
		borderLine.setLine(this.margins[MARGIN_LEFT], screenHeight
				- this.margins[MARGIN_BOTTOM], screenWidth
				- this.margins[MARGIN_RIGHT], screenHeight
				- this.margins[MARGIN_BOTTOM]);
		g2.draw(borderLine);
	}

	protected void drawChartGrid(Graphics g, final int screenWidth,
			final int screenHeight, float xStep, float yStep) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setColor(Color.LIGHT_GRAY);
		float xScale = (screenWidth - this.margins[MARGIN_LEFT] - this.margins[MARGIN_RIGHT])
				/ (this.maxXValue - this.minXValue);
		float yScale = (screenHeight - this.margins[MARGIN_TOP] - this.margins[MARGIN_BOTTOM])
				/ (this.maxYValue - this.minYValue);

		float horizontalLine = this.minYValue;
		float screenY = 0;

		for (; horizontalLine < this.maxYValue + 0.01f; horizontalLine += yStep) {
			screenY = this.margins[MARGIN_TOP]
					+ (this.maxYValue - horizontalLine) * yScale;
			g2.drawLine(this.margins[MARGIN_LEFT], (int) screenY, screenWidth
					- this.margins[MARGIN_RIGHT], (int) screenY);
			g2.drawString(String.format("%03.1f", horizontalLine), 0, screenY);
		}

		float verticalLine = this.minXValue;
		float screenX = 0;
		for (; verticalLine < this.maxXValue + 0.01f; verticalLine += xStep) {
			screenX = this.margins[MARGIN_LEFT]
					+ (verticalLine - this.minXValue) * xScale;
			g2.drawLine((int) screenX, screenHeight
					- this.margins[MARGIN_BOTTOM], (int) screenX,
					this.margins[MARGIN_TOP]);
			g2.drawString(String.format("%03.1f", verticalLine), screenX,
					screenHeight - this.margins[MARGIN_BOTTOM] / 2f);
		}
	}

	public void addPoint(float xValue, float yValue) {
		++this.numPoints;

		this.dataPoints.offer(new Point2D.Float(xValue, yValue));

		while (this.dataPoints.size() > this.maxElements) {
			--this.numPoints;
			this.dataPoints.poll();
		}

		if (this.selfAdjustMax) {
			if (yValue > this.maxYValue) {
				this.maxYValue += (1f / this.minFps)
						* (yValue - this.maxYValue);
			}
		}

		if (this.selfAdjustMin) {
			if (yValue < this.minYValue) {
				this.minYValue = yValue;
			}
		}

	}

	public void clear() {
		this.numPoints = 0;
		this.dataPoints.clear();
	}

	public int getMaxElements() {
		return maxElements;
	}

	public void setMaxElements(int maxElements) {
		this.maxElements = maxElements;
	}

	public float getMinXValue() {
		return minXValue;
	}

	public void setMinXValue(float minXValue) {
		this.minXValue = minXValue;
	}

	public float getMaxXValue() {
		return maxXValue;
	}

	public void setMaxXValue(float maxXValue) {
		this.maxXValue = maxXValue;
	}

	public float getMinYValue() {
		return minYValue;
	}

	public void setMinYValue(float minYValue) {
		this.minYValue = minYValue;
	}

	public float getMaxYValue() {
		return maxYValue;
	}

	public String getDisplayedInfo() {
		return displayedInfo;
	}

	public void setDisplayedInfo(String displayedInfo) {
		this.displayedInfo = displayedInfo;
	}

	public boolean isSelfAdjustMax() {
		return selfAdjustMax;
	}

	public void setSelfAdjustMax(boolean selfAdjustMax) {
		this.selfAdjustMax = selfAdjustMax;
	}

	public boolean isSelfAdjustMin() {
		return selfAdjustMin;
	}

	public void setSelfAdjustMin(boolean selfAdjustMin) {
		this.selfAdjustMin = selfAdjustMin;
	}
}

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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import org.grailrtls.libcommon.util.HashableByteArray;
import org.grailrtls.libcommon.util.NumericUtils;
import org.makesense.sigvis.structs.Item2DPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleHeatMap extends JComponent {

	private static final Logger log = LoggerFactory
			.getLogger(SimpleHeatMap.class);

	protected Rectangle2D regionBounds;

	protected long maxAge = 3000;

	protected String displayedDeviceName = "";

	protected final ConcurrentHashMap<HashableByteArray, Item2DPoint> heatPoints = new ConcurrentHashMap<HashableByteArray, Item2DPoint>();

	protected float minValue = -100f;

	protected float maxValue = -30f;

	protected BufferedImage deviceImage;

	protected Point2D deviceLocation = null;

	protected BufferedImage backgroundImage = null;
	
	protected long lastRepaint = System.currentTimeMillis();
	
	protected boolean enableAlpha = true;
	
	protected long minFps = 15;

	protected float currFps = 30;

	protected int slowFrames = 0;

	public BufferedImage getBackgroundImage() {
		return backgroundImage;
	}

	public void setBackgroundImage(BufferedImage backgroundImage) {
		this.backgroundImage = backgroundImage;
	}

	public SimpleHeatMap() {
		super();
		ToolTipManager.sharedInstance().registerComponent(this);
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		this.lastRepaint = System.currentTimeMillis();
		Graphics2D g2 = (Graphics2D) g;

		int screenWidth = this.getWidth();
		int screenHeight = this.getHeight();

		float xScale = screenWidth / (float) this.regionBounds.getWidth();
		float yScale = screenHeight / (float) this.regionBounds.getHeight();
		Ellipse2D drawPoint = null;

		Color origColor = g2.getColor();
		Composite origComposite = g2.getComposite();

		g2.setColor(Color.BLACK);
		if (this.backgroundImage == null) {
			g2.fillRect(0, 0, screenWidth, screenHeight);
		} else {
			g2.drawImage(this.backgroundImage, 0, 0, screenWidth, screenHeight,
					0, 0, this.backgroundImage.getWidth(), this.backgroundImage
							.getHeight(), null);
		}

		// Draw first if Alpha is enabled as we should still be able to see it
		// With no alpha, draw it last (see below).
		if(this.enableAlpha)
		{
			this.drawDeviceIcon(g2, screenWidth, screenHeight);
		}
		
		// g2.setColor(Color.RED);
		if(this.enableAlpha)
		{
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
				0.7f));
		}
		boolean isFirst = true;
		for (HashableByteArray identifier : this.heatPoints.keySet()) {

			if (isFirst) {
				isFirst = false;
				g2.setColor(Color.WHITE);
				g2.drawString(this.displayedDeviceName, 10, screenHeight - 10);
			}
			Item2DPoint item = this.heatPoints.get(identifier);

			if (item == null) {
				// Oops?
				continue;
			}
			if (item.getUpdateTime() < this.lastRepaint - this.maxAge) {
				item.setValue(0f);
				continue;
			}
			if (item.getPoint().getX() < 0 || item.getPoint().getY() < 0) {
				continue;
			}

			float valueRange = this.maxValue - this.minValue;

			float normalRssi = (float) ((Math.abs(item.getValue()
					- this.minValue)) / valueRange);
			if (normalRssi < 0) {
				normalRssi = 0;
			} else if (normalRssi > 1.0) {
				normalRssi = 1.0f;
			}
			if (normalRssi < 0.01f) {
				continue;
			}

			g2.setColor(Color.getHSBColor(normalRssi * .66f, 0.9f, 0.9f));

			// g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
			// normalRssi));
			drawPoint = new Ellipse2D.Float(((float) item.getPoint().getX())
					* xScale - 50 * normalRssi, screenHeight
					- (float) (item.getPoint().getY()) * yScale - 50
					* normalRssi, 90 * normalRssi + 10, 90 * normalRssi + 10);
			g2.fill(drawPoint);
		}
		
		// Draw icon last if no alpha transparency is used
		if(!this.enableAlpha)
		{
			this.drawDeviceIcon(g2, screenWidth, screenHeight);
		}
		
		g2.setComposite(origComposite);
		g2.setColor(origColor);
		
		long renderTime = System.currentTimeMillis() - this.lastRepaint;
		this.currFps = this.currFps * 0.875f + (1000f / renderTime)*0.125f;

		if (this.enableAlpha && (this.currFps < this.minFps * 0.9f)) {
			++this.slowFrames;
			if (this.slowFrames > 3) {
				this.enableAlpha = false;
				log.warn("FPS: {} Disabling Alpha Tranparency.", this.currFps);
			}
		} else if (this.enableAlpha) {
			this.slowFrames = 0;
		}
	}

	protected void drawDeviceIcon(final Graphics g, final int screenWidth,
			final int screenHeight) {
		if (this.deviceImage == null || this.deviceLocation == null)
			return;

		Graphics2D g2 = (Graphics2D) g;
		int imageWidth = this.deviceImage.getWidth();
		int imageHeight = this.deviceImage.getHeight();

		float xScale = screenWidth / (float) this.regionBounds.getWidth();
		float yScale = screenHeight / (float) this.regionBounds.getHeight();

		g2.drawImage(this.deviceImage, (int)(this.deviceLocation.getX() * xScale
				- (imageWidth / 2f)), (int)(screenHeight
				- (this.deviceLocation.getY() * yScale) - (imageHeight / 2f)),
				(int)(this.deviceLocation.getX() * xScale + (imageWidth / 2f)),
				(int)(screenHeight - (this.deviceLocation.getY() * yScale)
						+ (imageHeight / 2f)), 0, 0, imageWidth, imageHeight,
				null);
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

	public void setLocationOfItem(HashableByteArray item, Point2D location) {
		Item2DPoint currPoint = this.heatPoints.get(item);
		if (currPoint == null) {
			currPoint = new Item2DPoint((float) location.getX(),
					(float) location.getY(), -100);
			this.heatPoints.put(item, currPoint);
		} else {
			currPoint.setPoint(location);
		}
	}

	public void setValueOfItem(HashableByteArray item, float value) {
		Item2DPoint currPoint = this.heatPoints.get(item);
		if (currPoint == null) {
			currPoint = new Item2DPoint(-1, -1, value);
			this.heatPoints.put(item, currPoint);
		} else {
			currPoint.setValue(value);
			currPoint.setUpdateTime(System.currentTimeMillis());
		}
		
		// Limit fps to 30
//		if(System.currentTimeMillis() - this.lastRepaint > (1000/30))
//		{
//			this.repaint(10);
//		}
	}

	public void clear() {
		synchronized (this.heatPoints) {
			for (Item2DPoint item : this.heatPoints.values()) {
				item.setValue(this.minValue);
			}
		}
	}

	public Rectangle2D getRegionBounds() {
		return regionBounds;
	}

	public void setRegionBounds(Rectangle2D regionBounds) {
		this.regionBounds = regionBounds;
	}

	public String getDisplayedDeviceName() {
		return displayedDeviceName;
	}

	public void setDisplayedDeviceName(String displayedDeviceName) {
		this.displayedDeviceName = displayedDeviceName;
	}

	public BufferedImage getDeviceImage() {
		return deviceImage;
	}

	public void setDeviceImage(BufferedImage deviceImage) {
		this.deviceImage = deviceImage;
	}

	public Point2D getDeviceLocation() {
		return deviceLocation;
	}

	public void setDeviceLocation(Point2D deviceLocation) {
		this.deviceLocation = deviceLocation;
	}

	public boolean isEnableAlpha() {
		return enableAlpha;
	}

	public void setEnableAlpha(boolean enableAlpha) {
		this.enableAlpha = enableAlpha;
	}

	public long getMinFps() {
		return minFps;
	}

	public void setMinFps(long minFps) {
		this.minFps = minFps;
	}
	
	@Override
	public String getToolTipText(MouseEvent me) {
		if(this.regionBounds == null){
			return null;
		}
		
		Dimension panelDims = this.getSize();
		
		double mX = me.getPoint().getX();
		double mY = panelDims.getHeight() - me.getPoint().getY();
		
		
		// X scale for Screen->Region conversion
		double xS2R = this.regionBounds.getMaxX()/panelDims.getWidth();
		double yS2R = this.regionBounds.getMaxY()/panelDims.getHeight();
		
		double rX = mX*xS2R;
		double rY = mY*yS2R;
		
		return String.format("(%.1f, %.1f)",rX,rY);
	}
}

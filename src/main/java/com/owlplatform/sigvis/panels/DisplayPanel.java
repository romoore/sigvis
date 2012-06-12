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

import java.awt.image.BufferedImage;


import com.owlplatform.sigvis.DataCache2;

public interface DisplayPanel {
	
	public void setDisplayedId(String deviceId);
	
	public String getDisplayedId();
	
	public void setDeviceIsTransmitter(boolean isTransmitter);
	

	
	public void setMinValue(float minValue);
	
	public void setMaxValue(float maxValue);
	
	public void setSelfAdjustMin(boolean selfAdjustMin);
	
	public void setSelfAdjustMax(boolean selfAdjustMax);
	
	public void setMaxAge(long maxAge);
	
	public void setDisplayLegend(boolean displayLegend);
	
	public void setDeviceIcon(BufferedImage icon);
	
	public void setAntiAlias(boolean antiAlias);
	
	public boolean isAntiAlias();
	
	public boolean supportsAntiAlias();
	
	public void setTransparency(boolean transparency);
	
	public boolean isTransparency();
	
	public boolean supportsTransparency();
	
	 public long getTimeOffset();
	 
	 public void setTimeOffset(long offset);
	 
	 public void setCache(DataCache2 cache);
}

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

package com.owlplatform.sigvis.structs;

import java.awt.geom.Point2D;

public class Item2DPoint implements ChartItem<Float> {

	protected Point2D point;
	protected float value;
	protected long updateTime;
	
	protected final long creationTime;

	public Item2DPoint(float x, float y, float value) {
		this.point = new Point2D.Float(x, y);
		this.value = value;
		this.creationTime = System.currentTimeMillis();
		this.updateTime = this.creationTime;
	}

	@Override
	public Float getValue() {
		return Float.valueOf(this.value);
	}

	public long getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(final long updateTime) {
		this.updateTime = updateTime;
	}

	public Point2D getPoint() {
		return point;
	}

	public void setPoint(Point2D point) {
		this.point = point;
	}

	public void setValue(float value) {
		this.value = value;
	}

	@Override
	public long getCreationTime() {
		return this.creationTime;
	}

}

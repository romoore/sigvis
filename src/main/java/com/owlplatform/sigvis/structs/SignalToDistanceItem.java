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
package com.owlplatform.sigvis.structs;

import java.util.Comparator;

public class SignalToDistanceItem implements ChartItem<Float> {

	protected final float distance;
	
	protected final float signal;
	
	protected final long creationTimestamp;
	
	protected final String rxer;
	protected final String txer;
	
	public SignalToDistanceItem(String rxer, String txer, final float distance, final float signal){
		this.distance = distance;
		this.signal = signal;
		this.creationTimestamp = System.currentTimeMillis();
		this.rxer = rxer;
		this.txer = txer;
	}
	
	@Override
	public long getCreationTime() {
		return this.creationTimestamp;
	}

	@Override
	public Float getValue() {
		return Float.valueOf(this.signal);
	}

	public float getDistance() {
		return distance;
	}

	public float getSignal() {
		return signal;
	}
	
	public static class SignalComparator implements Comparator<SignalToDistanceItem>{

		protected float delta = 0.0001f;
		
		public SignalComparator(){
			super();
		}
		
		public SignalComparator(final float delta){
			this.delta = delta < 0f ? Math.abs(delta) : delta;
		}
		
		@Override
		public int compare(SignalToDistanceItem o1, SignalToDistanceItem o2) {
			float diff = o1.signal - o2.signal;
			if(Math.abs(diff) < this.delta){
				return 0;
			}
			if(diff < 0){
				return -1;
			}
			return 1;
		}
		
	}
	
	public static class DistanceComparator implements Comparator<SignalToDistanceItem>{

		protected float delta = 0.0001f;
		
		public DistanceComparator(){
			super();
		}
		
		public DistanceComparator(final float delta){
			this.delta = delta < 0f ? Math.abs(delta) : delta;
		}
		
		@Override
		public int compare(SignalToDistanceItem o1, SignalToDistanceItem o2) {
			float diff = o1.distance - o2.distance;
			if(Math.abs(diff) < this.delta){
				return 0;
			}
			if(diff < 0f){
				return -1;
			}
			return 1;
		}
		
	}
	
	@Override
	public String toString(){
		return "Signal: " + this.signal + ", Distance: " + this.distance;
	}

  public String getRxer() {
    return rxer;
  }

  public String getTxer() {
    return txer;
  }

}

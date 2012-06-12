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

/**
 * Compares two ChartItem objects by their creation timestamps, ordering later
 * (newer) objects before earlier (older) objects.
 * 
 * @author Robert Moore
 * 
 */
public class ChartItemTimeComparator implements Comparator<ChartItem> {

  private final boolean earlierFirst ;
  
  public ChartItemTimeComparator(final boolean earlierFirst){
    super();
    this.earlierFirst = earlierFirst;
  }
  
  @Override
  public int compare(ChartItem arg0, ChartItem arg1) {
    // Get diff of first - second
    long diff = arg0.getCreationTime() - arg1.getCreationTime();
    
    // If 1st is newer (higher ts), return -1 (earlier in ordering)
    if (diff > 0) {
      return this.earlierFirst ? -1 : 1;
    }
    // If 1st is older (lower ts), return 1 (later in ordering)
    if (diff < 0) {
      return this.earlierFirst ? 1 : -1;
    }
    // Same time
    return 0;
  }

}

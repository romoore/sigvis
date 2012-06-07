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

package org.makesense.sigvis;

import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class CacheStatsPanel extends JPanel {
  private final DataCache2 cache;
  
  private static final String[] BYTE_UNITS = {"B", "KiB", "MiB", "GiB"};
  
  // Float value, long timestamp
  private static final int RSSI_OBJ_SIZE = 12;
  // Float value, float distance, long timestamp, String txer (?), String rxer (?)
  private static final int SIG_DIST_OBJ_SIZE = 32;
  
  private final JLabel cacheFidTxers = new JLabel("   0");
  private final JLabel cacheRxers = new JLabel("   0");
  private final JLabel cacheRssiPoints = new JLabel("   0");
  private final JLabel cacheVarPoints = new JLabel("   0");
  private final JLabel cacheSigDistPoints = new JLabel("   0");
  
  public CacheStatsPanel(final DataCache2 cache){
    super();
    this.cache = cache;
    
    this.setLayout(new GridLayout(5,2));
    
    JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    flowPanel.add(new JLabel("No. Fid. Txers:"));
    this.add(flowPanel);
    
    flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    flowPanel.add(this.cacheFidTxers);
    this.add(flowPanel);
    
    flowPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    flowPanel.add(new JLabel("No. Receivers:"));
    this.add(flowPanel);
    
    flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    flowPanel.add(this.cacheRxers);
    this.add(flowPanel);
    
    flowPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    flowPanel.add(new JLabel("No. RSSI Data Points:"));
    this.add(flowPanel);
    
    flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    flowPanel.add(this.cacheRssiPoints);
    this.add(flowPanel);
    
    flowPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    flowPanel.add(new JLabel("No. Var. Data Points:"));
    this.add(flowPanel);
    
    flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    flowPanel.add(this.cacheVarPoints);
    this.add(flowPanel);
    
    flowPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    flowPanel.add(new JLabel("No. Sig-to-Dist. Points:"));
    this.add(flowPanel);
    
    flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    flowPanel.add(this.cacheSigDistPoints);
    this.add(flowPanel);
    
    this.validate();
  }
  
  public void setNumFidTxers(int numTxers){
    this.cacheFidTxers.setText(Integer.toString(numTxers));
  }
  
  public void setNumRxers(int numRxers){
    this.cacheRxers.setText(Integer.toString(numRxers));
  }
  
  public void setNumRssiPoints(int numRssi){
    int unit = 0;
    float byteValue = numRssi*RSSI_OBJ_SIZE;
    while(byteValue > 1024){
      byteValue /= 1024f;
      ++unit;
    }
    this.cacheRssiPoints.setText(String.format("%,d (~%.1f %s)",numRssi,byteValue, BYTE_UNITS[unit]));
  }
  
  public void setNumVarPoints(int numVar){
    int unit = 0;
    float byteValue = numVar*RSSI_OBJ_SIZE;
    while(byteValue > 1024){
      byteValue /= 1024f;
      ++unit;
    }
    this.cacheVarPoints.setText(String.format("%,d (~%.1f %s)",numVar,byteValue, BYTE_UNITS[unit]));
  }
  
  public void setNumSigDistPoints(int numSigDist){
    int unit = 0;
    float byteValue = numSigDist*SIG_DIST_OBJ_SIZE;
    while(byteValue > 1024){
      byteValue /= 1024f;
      ++unit;
    }
    this.cacheSigDistPoints.setText(String.format("%,d (~%.1f %s)",numSigDist,byteValue, BYTE_UNITS[unit]));
  }
}


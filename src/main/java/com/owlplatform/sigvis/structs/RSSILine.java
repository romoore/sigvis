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

import java.awt.geom.Line2D;

import org.grailrtls.libcommon.util.HashableByteArray;
import org.grailrtls.libcommon.util.NumericUtils;

public class RSSILine
{
    private float value;
    
    private Line2D.Float line;
    
    private String transmitterHash;
    
    private String receiverHash;
    
    private long updateTime = 0l;

    public long getUpdateTime() {
		return updateTime;
	}

	public float getValue()
    {
        return value;
    }

    public void setValue(float value)
    {
        this.value = value;
        this.updateTime = System.currentTimeMillis();
    }

    public Line2D.Float getLine()
    {
        return line;
    }

    public void setLine(Line2D.Float line)
    {
        this.line = line;
    }

    public String getTransmitter()
    {
        return this.transmitterHash;
    }

    public void setTransmitter(String transmitter)
    {
        this.transmitterHash = transmitter;
    }

    public String getReceiver()
    {
        return this.receiverHash;
    }

    public void setReceiver(String receiver)
    {
        this.receiverHash = receiver;
    }
    
    @Override
    public String toString()
    {
    	StringBuffer sb = new StringBuffer();
    	
    	sb.append("RSSI Line (").append(this.receiverHash).append(" <-> ").append(this.transmitterHash).append("): ").append(this.value);
    	
    	return sb.toString();
    }
}

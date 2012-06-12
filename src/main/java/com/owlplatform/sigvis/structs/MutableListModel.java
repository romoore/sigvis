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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractListModel;

import org.apache.mina.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.common.util.HashableByteArray;

public class MutableListModel extends AbstractListModel {
	private static final Logger log = LoggerFactory.getLogger(MutableListModel.class);
	
	protected ConcurrentHashSet<HashableByteArray> transmitterSet = new ConcurrentHashSet<HashableByteArray>();

	protected List<HashableByteArray> sortedList = Collections
			.synchronizedList(new ArrayList<HashableByteArray>());
	
	@Override
	public Object getElementAt(int arg0) {
		return this.sortedList.get(arg0);
	}

	@Override
	public int getSize() {
		return this.transmitterSet.size();
	}

	public boolean addElement(HashableByteArray elem) {
		boolean returnValue = false;
		if (!this.transmitterSet.contains(elem)) {
			synchronized(this.sortedList)
			{
				// Double-checking inside synchronized block because
				// don't want to synchronize on every incoming HashableByteArray
				if(!this.transmitterSet.contains(elem))
				{
					this.transmitterSet.add(elem);
					returnValue = this.sortedList.add(elem);
					Collections.sort(this.sortedList);
					int insertPoint = this.sortedList.indexOf(elem);
					this.fireIntervalAdded(this, insertPoint, insertPoint);
				}
			}
		}
		return returnValue;
	}

}

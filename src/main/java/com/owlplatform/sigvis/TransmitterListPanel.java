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

package com.owlplatform.sigvis;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListModel;
import javax.swing.table.TableModel;

import org.apache.mina.filter.codec.statemachine.SkippingState;
import org.apache.mina.util.ConcurrentHashSet;

import com.owlplatform.common.SampleMessage;
import com.owlplatform.common.util.HashableByteArray;
import com.owlplatform.sigvis.structs.MutableListModel;
import com.owlplatform.solver.SolverAggregatorInterface;
import com.owlplatform.solver.listeners.SampleListener;

public class TransmitterListPanel extends JPanel implements SampleListener {
	
	
	protected MutableListModel listModel = new MutableListModel();
	
	protected JList list = new JList(this.listModel);
	
	public TransmitterListPanel()
	{
		super();
		this.setLayout(new BorderLayout());
		this.add(new JScrollPane(this.list), BorderLayout.CENTER);
	}
	
	@Override
	public void sampleReceived(SolverAggregatorInterface aggregator,
			SampleMessage sample) {
		HashableByteArray hash = new HashableByteArray(sample.getDeviceId());
		
		this.listModel.addElement(hash);
	}
	
	public HashableByteArray getSelectedValue()
	{
		return (HashableByteArray)this.list.getSelectedValue();
	}

}

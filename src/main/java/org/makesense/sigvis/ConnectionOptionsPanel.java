/*
 * GRAIL Real Time Localization System
 * Copyright (C) 2011 Rutgers University and Robert Moore
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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.grailrtls.libworldmodel.client.ClientWorldModelInterface;

public class ConnectionOptionsPanel extends JPanel {


	protected JTextField worldModelHost = new JTextField(20);
	protected JTextField worldModelPort = new JTextField(6);
	protected JTextField regionName = new JTextField(20);
	
	

	public ConnectionOptionsPanel() {
		super();
		
		this.setLayout(new GridLayout(3, 2));
		
		
		JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		
		flowPanel.add(new JLabel("World Model Host:"));
		this.add(flowPanel);
		flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		flowPanel.add(this.worldModelHost);
		this.add(flowPanel);
		
		
		
		flowPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		flowPanel.add(new JLabel("World Model Port:"));
		this.add(flowPanel);
		flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		flowPanel.add(this.worldModelPort);
		this.add(flowPanel);
		
		flowPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		flowPanel.add(new JLabel("Region:"));
		this.add(flowPanel);
		flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		flowPanel.add(this.regionName);
		
		this.add(flowPanel);
	}
	
	public String getWorldModelHost(){
	  return this.worldModelHost.getText();
	}
	
	public void setWorldModelHost(String host){
	  this.worldModelHost.setText(host);
	}
	
	public String getWorldModelPort(){
	  return this.worldModelPort.getText();
	}
	
	public void setWorldModelPort(String port){
	  this.worldModelPort.setText(port);
	}
	
	public String getRegion(){
	  return this.regionName.getText();
	}
	
	public void setRegion(String region){
	  this.regionName.setText(region);
	}
}

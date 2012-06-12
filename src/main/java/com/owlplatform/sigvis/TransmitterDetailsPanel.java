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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.common.SampleMessage;
import com.owlplatform.common.util.HashableByteArray;
import com.owlplatform.common.util.OnlineVariance;
import com.owlplatform.sigvis.panels.BarChart;
import com.owlplatform.sigvis.panels.LineChart;
import com.owlplatform.sigvis.panels.ScatterPlotPanel;
import com.owlplatform.sigvis.panels.SignalToDistanceMap;
import com.owlplatform.sigvis.panels.SimpleHeatMap;
import com.owlplatform.sigvis.panels.VoronoiHeatMap;
import com.owlplatform.sigvis.structs.MutableListModel;
import com.owlplatform.solver.SolverAggregatorInterface;
import com.owlplatform.solver.listeners.SampleListener;
import com.owlplatform.worldmodel.client.ClientWorldModelInterface;
import com.owlplatform.worldmodel.client.listeners.DataListener;
import com.owlplatform.worldmodel.client.protocol.messages.AbstractRequestMessage;
import com.owlplatform.worldmodel.client.protocol.messages.Attribute;
import com.owlplatform.worldmodel.client.protocol.messages.AttributeAliasMessage;
import com.owlplatform.worldmodel.client.protocol.messages.DataResponseMessage;
import com.owlplatform.worldmodel.client.protocol.messages.OriginAliasMessage;
import com.owlplatform.worldmodel.client.protocol.messages.OriginPreferenceMessage;
import com.owlplatform.worldmodel.client.protocol.messages.SnapshotRequestMessage;
import com.owlplatform.worldmodel.client.protocol.messages.URISearchResponseMessage;
import com.owlplatform.worldmodel.types.DataConverter;

public class TransmitterDetailsPanel extends JPanel implements SampleListener,
		DataListener, ListSelectionListener,
		MouseListener {

	private static final Logger log = LoggerFactory
			.getLogger(TransmitterDetailsPanel.class);

	protected SolverAggregatorInterface solver;

	protected ClientWorldModelInterface worldServer;

	protected ScatterPlotPanel signalPlot = new ScatterPlotPanel();
	
	protected static final byte[] DEFAULT_DEVICE = new byte[] { 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	protected volatile byte[] currentDevice = DEFAULT_DEVICE;

	protected float smoothedSPS = 0;

	protected float smoothingWeight = 0.875f;

	protected final ConcurrentHashMap<HashableByteArray, Point2D> transmitterLocations = new ConcurrentHashMap<HashableByteArray, Point2D>();

	protected final ConcurrentHashMap<HashableByteArray, Point2D> receiverLocations = new ConcurrentHashMap<HashableByteArray, Point2D>();

	protected final ConcurrentHashMap<HashableByteArray, OnlineVariance> currentTransmitterVariance = new ConcurrentHashMap<HashableByteArray, OnlineVariance>();

	protected BufferedImage transmitterIcon = null;

	/**
	 * Formatter for printing floating point numbers with 4 decimal places of
	 * precision.
	 */
	private static final DecimalFormat samplesPerSecFmt = new DecimalFormat(
			"###,##0.0");

	/**
	 * Number of samples received since the statistics were last generated.
	 */
	private int samplesReceived = 0;

	/**
	 * Mean latency of samples received from the aggregator. This value assumes
	 * that the sensors are sending valid timestamps to the aggregator.
	 */
	private float meanReceiveLatency = 0;

	/**
	 * The last time that statistics were generated.
	 */
	private long lastReportTime = System.currentTimeMillis();

	/**
	 * Number of bytes read from the aggregator since the last time statistics
	 * were generated.
	 */
	private long bytesRead = 0;

	/**
	 * Number of bytes written to the aggregator since the last time statistics
	 * were generated.
	 */
	private long bytesWritten = 0;

	private final Timer updateTimer = new Timer();

	private TimerTask updateTask = null;

	protected MutableListModel transmitterListModel = new MutableListModel();

	protected JList transmitterList = new JList(this.transmitterListModel);

	protected long statsUpdatePeriod = 50;

	protected long statsTimeHistory = 1000 * 60;

	protected long spsRefreshPeriod = 100;

	protected String regionImageUri = null;

	protected int desiredFps = 25;

	protected int minFps = 20;

	protected final ExecutorService workers = Executors
			.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	public TransmitterDetailsPanel(final SolverAggregatorInterface solver,
			final ClientWorldModelInterface worldServer) {
		this.solver = solver;
		this.worldServer = worldServer;

		this.solver.addSampleListener(this);
		this.worldServer.addDataListener(this);

		this.worldServer.registerSearchRequest("*.transmitter.*");
		this.worldServer.registerSearchRequest("*.receiver.*");
		this.worldServer.registerSearchRequest("winlab");


		try {
			this.transmitterIcon = ImageIO.read(getClass().getResourceAsStream(
					"/images/fiduciary_transmitter.png"));
		} catch (IOException e) {
			log.warn("Unable to load fiduciary transmitter image.");
		}

		// Need to really get region info from World Model
		// TODO: Get region dimensions from world model
		Rectangle2D fakeRegionBounds = new Rectangle2D.Float(0, 0, 1, 1);

		this.transmitterList.addListSelectionListener(this);
		this.transmitterList
				.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.transmitterList
				.setPrototypeCellValue("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");

		this.signalPlot.setMinYValue(-100f);
		this.signalPlot.setMaxYValue(-20f);
		this.signalPlot.setMinXValue(0f);
		this.signalPlot.setMaxXValue(150f);
		this.signalPlot.setMaxElements(100);
		this.signalPlot.addMouseListener(this);
		this.updateFps();

		this.setLayout(new BorderLayout());

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setOneTouchExpandable(true);

		JTabbedPane deviceDetailPanel = new JTabbedPane();

		// Device signal map
		// tempPanel = new JPanel();
		// //this.createTitledPanel("Device Signal Map");
		// tempPanel.setLayout(new GridLayout(1,1));
		// tempPanel.setBorder(BorderFactory.createTitledBorder("Device Signal Map"));
		// tempPanel.add(this.deviceSignalMap);
		// this.add(tempPanel, BorderLayout.CENTER);

		// Signal to Distance scatterplot
		// tempPanel = new JPanel();
		// tempPanel.setLayout(new GridLayout(1,1));
		// tempPanel.setBorder(BorderFactory.createTitledBorder("Signal to Distance"));
		// tempPanel.add(this.signalPlot);
		deviceDetailPanel.add(this.signalPlot, "Signal to Distance");
		splitPane.setRightComponent(deviceDetailPanel);

		// Transmitter list
		JPanel tempPanel = new JPanel();
		tempPanel.setLayout(new GridLayout(1, 1));
		tempPanel.setBorder(BorderFactory
				.createTitledBorder("Observed Devices"));
		tempPanel.add(new JScrollPane(this.transmitterList));
		splitPane.setLeftComponent(tempPanel);

		this.add(splitPane, BorderLayout.CENTER);
		this.setPreferredSize(new Dimension(800, 600));
	}

	protected JPanel createTitledPanel(String title) {
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createTitledBorder(title));
		return panel;
	}

	protected void updateFps() {
		this.signalPlot.setMinFps(this.minFps);
	}

	public void startUpdates() {
		this.updateTask = new TimerTask() {

			@Override
			public void run() {
				TransmitterDetailsPanel.this.updateInfo();
				TransmitterDetailsPanel.this.repaintMembers();
			}
		};
		this.updateTimer.schedule(this.updateTask, 1000 / this.desiredFps,
				1000 / this.desiredFps);
	}

	@Override
	public void sampleReceived(SolverAggregatorInterface aggregator,
			final SampleMessage sample) {
		this.workers.execute(new Runnable() {

			@Override
			public void run() {
				TransmitterDetailsPanel.this.processSample(sample);
			}
		});
	}

	protected void processSample(final SampleMessage sample) {
		++this.samplesReceived;
		HashableByteArray receiverHash = new HashableByteArray(sample
				.getReceiverId());
		if (Arrays.equals(this.currentDevice, sample.getDeviceId())) {

			this.updateSignalScatter(sample);
			this.updateVariances(sample);
		}

		HashableByteArray hash = new HashableByteArray(sample.getDeviceId());
		this.transmitterListModel.addElement(hash);
	}

	protected void updateVariances(final SampleMessage sample) {
		HashableByteArray hash = new HashableByteArray(sample.getReceiverId());
		OnlineVariance variance = this.currentTransmitterVariance.get(hash);
		if (variance == null) {
			variance = new OnlineVariance();
			this.currentTransmitterVariance.put(hash, variance);
		}

		float v = variance.addValue(sample.getRssi());
	}

	protected void updateSignalScatter(SampleMessage sample) {
		if (sample == null)
			return;
		HashableByteArray receiverHash = new HashableByteArray(sample
				.getReceiverId());
		HashableByteArray transmitterHash = new HashableByteArray(sample
				.getDeviceId());

		Point2D receiverPoint = this.receiverLocations.get(receiverHash);
		if (receiverPoint == null) {
			return;
		}
		Point2D transmitterPoint = this.transmitterLocations
				.get(transmitterHash);
		if (transmitterPoint == null) {
			return;
		}

		double xDiff = receiverPoint.getX() - transmitterPoint.getX();
		double yDiff = receiverPoint.getY() - transmitterPoint.getY();

		double distance = Math.sqrt(Math.pow(xDiff, 2) + Math.pow(yDiff, 2));
		this.signalPlot.addPoint((float) distance, sample.getRssi());
	}

	public void updateInfo() {
		long now = System.currentTimeMillis();
		float sps = this.samplesReceived
				/ ((now - this.lastReportTime) / 1000f);

		this.smoothedSPS = sps * (1 - this.smoothingWeight) + this.smoothedSPS
				* this.smoothingWeight;
		if (this.smoothedSPS < 0.01f) {
			this.smoothedSPS = 0f;
		}

		if (this.solver.getSession() != null) {
			long newBytesRead = this.solver.getSession().getReadBytes();
			long newBytesWritten = this.solver.getSession().getWrittenBytes();
			this.bytesRead = newBytesRead;
			this.bytesWritten = newBytesWritten;
		}

		this.samplesReceived = 0;
		this.meanReceiveLatency = 0;
		this.lastReportTime = now;

	}

	protected void repaintMembers() {
		this.signalPlot.repaint();
	}

	public static byte[] makeIdFromPipString(String uri) {
		int lastDot = uri.lastIndexOf(".");
		if (lastDot < 0)
			return null;
		String pipIdString = uri.substring(lastDot + 1, uri.length());
		try {
			Integer fullValue = Integer.valueOf(pipIdString);
			byte[] fullBytes = new byte[SampleMessage.DEVICE_ID_SIZE];
			fullBytes[15] = (byte) (fullValue.intValue() & 0xFF);
			fullBytes[14] = (byte) ((fullValue.intValue() >> 8) & 0xFF);
			return fullBytes;
		} catch (NumberFormatException nfe) {
			log.error("Error while parsing {}", uri, nfe);
			return null;
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (e.getSource() == null || e.getValueIsAdjusting())
			return;

		if (e.getSource() == this.transmitterList) {
			HashableByteArray hash = (HashableByteArray) this.transmitterList
					.getSelectedValue();
			if (hash == null) {
				return;
			}

			if (!Arrays.equals(this.currentDevice, hash.getData())) {
				this.currentDevice = hash.getData();
				this.currentTransmitterVariance.clear();
								// TODO: Set legend here for LineChart members

				this.signalPlot.clear();

				if (this.transmitterLocations.containsKey(hash)) {
					this.signalPlot.setDisplayedInfo(hash.toString());

				} else {
					this.signalPlot.setDisplayedInfo(null);
				}

				this.repaintMembers();
			}
		}
	}

	protected void setRegionImageUri(final String regionImageUri) {
		this.regionImageUri = regionImageUri;
		if (this.regionImageUri.indexOf("http://") == -1) {
			this.regionImageUri = "http://" + this.regionImageUri;
		}
		try {

			BufferedImage origImage = ImageIO
					.read(new URL(this.regionImageUri));
			// BufferedImage invert = negative(origImage);
		} catch (MalformedURLException e) {
			log.warn("Invalid region URI: {}", this.regionImageUri);
			e.printStackTrace();
		} catch (IOException e) {
			log.warn("Could not load region URI at {}.", this.regionImageUri);
			e.printStackTrace();
		}

	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// Ignore left-click.
		if (arg0.getButton() == MouseEvent.BUTTON1) {
			return;
		}

		// Toggle transparency on mouse clicks to either heat map
		if (arg0.getSource() instanceof SimpleHeatMap) {
			SimpleHeatMap map = (SimpleHeatMap) arg0.getSource();
			map.setEnableAlpha(!map.isEnableAlpha());
		} else if (arg0.getSource() instanceof VoronoiHeatMap) {
			VoronoiHeatMap map = (VoronoiHeatMap) arg0.getSource();
			map.setTransparency(!map.isTransparency());
		}

		// Toggle Anti-Aliasing on mouse clicks to any line charts
		else if (arg0.getSource() instanceof LineChart) {
			LineChart chart = (LineChart) arg0.getSource();
			chart.setAntiAlias(!chart.isAntiAlias());
		}
		// Toggle Anti-Aliasing on mouse clicks to any bar charts
		else if (arg0.getSource() instanceof BarChart) {
			BarChart chart = (BarChart) arg0.getSource();
			chart.setAntiAlias(!chart.isAntiAlias());
		}
		// Toggle Anti-Aliasing on mouse clicks to any scatterplots
		else if (arg0.getSource() instanceof ScatterPlotPanel) {
			ScatterPlotPanel plot = (ScatterPlotPanel) arg0.getSource();
			plot.setEnableAntiAliasing(!plot.isEnableAntiAliasing());
		}
		else if(arg0.getSource() instanceof SignalToDistanceMap){
			SignalToDistanceMap map = (SignalToDistanceMap)arg0.getSource();
			map.setTransparency(!map.isTransparency());
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public int getDesiredFps() {
		return desiredFps;
	}

	public void setDesiredFps(int desiredFps) {
		this.desiredFps = desiredFps;
		this.updateFps();

		if (this.updateTask != null) {
			this.updateTask.cancel();
			this.updateTask = new TimerTask() {

				@Override
				public void run() {
					TransmitterDetailsPanel.this.updateInfo();
					TransmitterDetailsPanel.this.repaintMembers();
				}
			};
			this.updateTimer.schedule(this.updateTask, 1000l / this.desiredFps,
					1000l / this.desiredFps);
		}
	}

	public int getMinFps() {
		return minFps;
	}

	public void setMinFps(int minFps) {
		this.minFps = minFps;
		this.updateFps();
	}

	@Override
	public void requestCompleted(ClientWorldModelInterface worldModel,
			AbstractRequestMessage message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dataResponseReceived(ClientWorldModelInterface worldModel,
			DataResponseMessage message) {
		if (message.getUri().equals("winlab")) {
			double width = -1f;
			double height = -1f;
			String mapURL = null;
			for (Attribute field : message.getAttributes()) {
				if ("width".equals(field.getAttributeName())) {
					width = (Double)DataConverter.decodeUri(field.getAttributeName(),field.getData());
				} else if ("height".equals(field.getAttributeName())) {
					height = (Double) DataConverter.decodeUri(field.getAttributeName(), field.getData());
				} else if ("map url".equals(field.getAttributeName())) {
					mapURL = (String) DataConverter.decodeUri(field.getAttributeName(),field.getData());
					continue;
				}
			}
			if (width > 0 && height > 0) {
				Rectangle2D bounds = new Rectangle2D.Float(0, 0, (float) width,
						(float) height);
			}
			this.setRegionImageUri(mapURL);

		} else if (message.getUri().startsWith(
				"pipsqueak.receiver")) {
			byte[] pipId = makeIdFromPipString(message.getUri());
			if (pipId == null) {
				log.debug("Null receiver id for {}", message
						.getUri());
				return;
			}
			double x = -1;
			double y = -1;

			for (Attribute field : message.getAttributes()) {
				if ("location.x".equals(field.getAttributeName())) {
					x = (Double) DataConverter.decodeUri(field.getAttributeName(), field.getData());
				} else if ("location.y".equals(field.getAttributeName())) {
					y = (Double) DataConverter.decodeUri(field.getAttributeName(), field.getData());
				}
			}
			if (x > 0 && y > 0) {
				HashableByteArray receiverHash = new HashableByteArray(pipId);
				this.receiverLocations.put(receiverHash, new Point2D.Float(
						(float) x, (float) y));
			}
		} else if (message.getUri().startsWith(
				"pipsqueak.transmitter")) {
			byte[] pipId = makeIdFromPipString(message.getUri());
			if (pipId == null) {
				log.debug("Null transmitter id for {}", message
						.getUri());
				return;
			}
			double x = -1;
			double y = -1;

			if (message.getAttributes() == null) {
				return;
			}

			for (Attribute field : message.getAttributes()) {
				if ("location.x".equals(field.getAttributeName())) {
					x = (Double) DataConverter.decodeUri(field.getAttributeName(), field.getData());
				} else if ("location.y".equals(field.getAttributeName())) {
					y = (Double) DataConverter.decodeUri(field.getAttributeName(), field.getData());
				}
			}
			if (x > 0 && y > 0) {
				this.transmitterLocations.put(new HashableByteArray(pipId),
						new Point2D.Float((float) x, (float) y));
			}
		}
	}

	@Override
	public void uriSearchResponseReceived(ClientWorldModelInterface worldModel,
			URISearchResponseMessage message) {
		if (message.getMatchingUris() == null) {
			return;
		}
		for (String uri : message.getMatchingUris()) {
			SnapshotRequestMessage request = new SnapshotRequestMessage();
			request.setBeginTimestamp(0l);
			request.setEndTimestamp(System.currentTimeMillis());
			request.setQueryURI(uri);
			request.setQueryAttributes(new String[] { "*" });

			this.worldServer.sendMessage(request);
		}
	}

	@Override
	public void attributeAliasesReceived(
			ClientWorldModelInterface clientWorldModelInterface,
			AttributeAliasMessage message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void originAliasesReceived(
			ClientWorldModelInterface clientWorldModelInterface,
			OriginAliasMessage message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void originPreferenceSent(ClientWorldModelInterface worldModel,
			OriginPreferenceMessage message) {
		// TODO Auto-generated method stub
		
	}

}

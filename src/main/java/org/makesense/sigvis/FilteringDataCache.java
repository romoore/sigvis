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

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.mina.util.ConcurrentHashSet;
import org.grailrtls.libcommon.SampleMessage;
import org.grailrtls.libcommon.util.HashableByteArray;
import org.makesense.sigvis.structs.ChartItem;
import org.makesense.sigvis.structs.SignalToDistanceItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilteringDataCache extends DataCache2 {
  private static final Logger log = LoggerFactory
      .getLogger(FilteringDataCache.class);

  protected final ConcurrentHashSet<String> allowedDevices = new ConcurrentHashSet<String>();

  public FilteringDataCache(ConnectionHandler handler){
    super(handler);
  }

  public FilteringDataCache(ConnectionHandler handler, final long timestamp) {
    super(handler,timestamp);
  }

  public void addAllowedDevice(String deviceId) {
    this.allowedDevices.add(deviceId);
  }

  public void removeAllowedDevice(String deviceId) {
    this.allowedDevices.remove(deviceId);
  }

  @Override
  public float getCurrentRssi(final String transmitter, final String receiver) {
    if (this.allowedDevices.size() == 0) {
      return super.getCurrentRssi(transmitter, receiver);
    }
    if (this.allowedDevices.contains(receiver)
        && this.allowedDevices.contains(transmitter)) {
      return super.getCurrentRssi(transmitter, receiver);
    }
    return Float.NaN;
  }

  @Override
  public float getCurrentVariance(final String transmitter,
      final String receiver) {
    if (this.allowedDevices.size() == 0) {
      return super.getCurrentVariance(transmitter, receiver);
    }
    if (this.allowedDevices.contains(receiver)
        && this.allowedDevices.contains(transmitter)) {
      return super.getCurrentVariance(transmitter, receiver);
    }
    return Float.NaN;
  }

  @Override
  public List<ChartItem<Float>> getRssiList(final String receiverId,
      final String transmitterId) {
    if (this.allowedDevices.size() == 0) {
      return super.getRssiList(receiverId, transmitterId);
    }
    if (this.allowedDevices.contains(receiverId)
        && this.allowedDevices.contains(transmitterId)) {
      return super.getRssiList(receiverId, transmitterId);
    }

    return null;
  }

  @Override
  public List<ChartItem<Float>> getVarianceList(final String receiverId,
      final String transmitterId) {
    if (this.allowedDevices.size() == 0) {
      return super.getVarianceList(receiverId, transmitterId);
    }
    if (this.allowedDevices.contains(receiverId)
        && this.allowedDevices.contains(transmitterId)) {
      return super.getVarianceList(receiverId, transmitterId);
    }
    return null;
  }

  @Override
  public List<SignalToDistanceItem> getSignalToDistance(final String receiverId) {
    if (this.allowedDevices.size() == 0) {
      return super.getSignalToDistance(receiverId);
    }
    if (this.allowedDevices.contains(receiverId)) {
      return super.getSignalToDistance(receiverId);
    }
    return null;
  }

  @Override
  public Point2D getDeviceLocation(final String deviceId) {
    if (this.allowedDevices.size() == 0) {
      return super.getDeviceLocation(deviceId);
    }
    if (this.allowedDevices.contains(deviceId)) {
      return super.getDeviceLocation(deviceId);
    }
    return null;
  }

  public List<String> getAllowedDevices() {
    LinkedList<String> returnedList = new LinkedList<String>();
    returnedList.addAll(this.allowedDevices);
    return returnedList;
  }

  @Override
  public List<String> getReceiverIds() {
    List<String> receivers = super.getReceiverIds();

    for (Iterator<String> i = receivers.iterator(); i.hasNext();) {
      String s = i.next();
      if (!this.allowedDevices.contains(s)) {
        i.remove();
      }
    }
    return receivers;
  }

  @Override
  public List<String> getFiduciaryTransmitterIds() {
    List<String> transmitters = super.getFiduciaryTransmitterIds();

    for (Iterator<String> i = transmitters.iterator(); i.hasNext();) {
      String s = i.next();
      if (!this.allowedDevices.contains(s)) {
        i.remove();
      }
    }
    return transmitters;
  }

  public void clearAllowedDevices() {

    this.allowedDevices.clear();
  }

  @Override
  public void clearAll() {
    super.clearAll();
    this.clearAllowedDevices();
  }

  protected void overlay(FilteringDataCache clone) {
    super.overlay(clone);
    clone.allowedDevices.addAll(this.allowedDevices);
  }

  @Override
  public FilteringDataCache clone() {
    FilteringDataCache returnedCache = new FilteringDataCache(new ConnectionHandler(),
        this.isClone ? this.getCreationTs() : System.currentTimeMillis());
    
    returnedCache.isClone = true;
    this.overlay(returnedCache);
    return returnedCache;
  }

  @Override
  protected synchronized void toStream(String filename, ObjectOutputStream out)
      throws IOException {

    List<String> allowed = new LinkedList<String>();
    allowed.addAll(this.allowedDevices);
    out.writeObject(allowed);
    super.toStream(filename, out);
  }

  protected synchronized void fromStream(String filename, ObjectInputStream in)
      throws Exception {

    List<String> allowed = (List<String>) in.readObject();

    super.fromStream(filename, in);
    this.allowedDevices.addAll(allowed);
    for (DataCache2Listener listener : this.listeners) {
      listener.transmitterAdded(null, true);
      listener.receiverAdded(null);
    }
  }
}

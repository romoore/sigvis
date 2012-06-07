package org.makesense.sigvis;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;

import javax.imageio.ImageIO;

import org.grailrtls.libworldmodel.client.ClientWorldConnection;
import org.grailrtls.libworldmodel.client.Response;
import org.grailrtls.libworldmodel.client.StepResponse;
import org.grailrtls.libworldmodel.client.WorldState;
import org.grailrtls.libworldmodel.client.protocol.messages.Attribute;
import org.grailrtls.libworldmodel.types.ByteArrayConverter;
import org.grailrtls.libworldmodel.types.DataConverter;
import org.grailrtls.libworldmodel.types.DoubleConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionHandler {

  private static final Logger log = LoggerFactory
      .getLogger(ConnectionHandler.class);

  private static final long JOIN_TIMEOUT = 1000;
  private ClientWorldConnection wmc;

  private RssiHandler rssiHandler;
  private VarianceHandler varianceHandler;
  private DataCache2 cache;

  private String clientHost;
  private int clientPort;
  private String region;

  public ConnectionHandler() {
    super();

  }

  public void setCache(final DataCache2 cache) {
    this.cache = cache;
  }

  public void setRegion(final String region) {
    this.region = region;

  }

  public void setClientConnection(String host, int port) {
    if (this.wmc != null) {
      this.disconnectAsClient();
    }else{
      this.wmc = new ClientWorldConnection();
    }
    
    this.clientHost = host;
    this.clientPort = port;
    this.wmc.setHost(host);
   
    this.wmc.setPort(port);

  }

  public boolean connectAsClient() {
    if (this.cache == null) {
      log.error("No cache set.  Unable to connect.");
      return false;
    }

    if (this.wmc == null) {
      log.error("World Model connection is null.  Can't connect.");
      return false;
    }

    if (this.region == null) {
      log.error("No region specified. Can't connect.");
      return false;
    }

    if (!this.wmc.connect()) {
      log.error("Unable to connect. See log for details.");
      return false;
    }
    log.info("Connecting to {}", this.wmc);
    this.getStarted();
    log.info("Connected to {}", this.wmc);
    return true;
  }

  public void disconnectAsClient() {

    if (this.rssiHandler != null) {
      this.rssiHandler.shutdown();
      try {
        this.rssiHandler.join(JOIN_TIMEOUT);
      } catch (InterruptedException ie) {
        log.warn("Couldn't join with RSSI handler thread.");
      }
      this.rssiHandler = null;
    }

    if (this.varianceHandler != null) {
      this.varianceHandler.shutdown();
      try {
        this.varianceHandler.join(JOIN_TIMEOUT);
      } catch (InterruptedException ie) {
        log.warn("Couldn't join with Variance handler thread.");
      }
      this.varianceHandler = null;
    }

    if (this.wmc != null) {
      this.wmc.disconnect();
      
    }
  }

  protected void getStarted() {

    if (this.cache == null) {
      log.error("No cache set.  Cannot start.");
      return;
    }

    // Set-up the initial data from the world model
    this.cache.setRegionUri(this.region);
    String[] matchingUris = this.wmc.searchURI("region\\." + this.region);
    this.retrieveRegionInfo(matchingUris);
    this.retrieveAnchors(this.region);

    // Retrieve transient data from world model
    this.startStreams();
  }

  /*
   * winlab.anchor.pipsqueak.receiver.667 winlab.anchor.pipsqueak.repeater.220
   * winlab.anchor.pipsqueak.transmitter.112
   */

  protected void retrieveRegionInfo(String[] matchingUris) {
    for (String uri : matchingUris) {
      Response res = this.wmc.getCurrentSnapshot(uri, "location\\..*",
          "image\\.url");
      double width = 0;
      double height = 0;
      String imageUrlString = null;
      try {
        WorldState state = res.get();
        for (String stateUri : state.getURIs()) {
          Collection<Attribute> attributes = state.getState(stateUri);
          for (Attribute attrib : attributes) {
            if ("location.maxx".equals(attrib.getAttributeName())) {
              width = ((Double) DataConverter.decodeUri(
                  attrib.getAttributeName(), attrib.getData())).doubleValue();
            } else if ("location.maxy".equals(attrib.getAttributeName())) {
              height = ((Double) DataConverter.decodeUri(
                  attrib.getAttributeName(), attrib.getData())).doubleValue();
            } else if ("image.url".equals(attrib.getAttributeName())) {
              imageUrlString = (String) DataConverter.decodeUri(
                  attrib.getAttributeName(), attrib.getData());
            }
          }
        }
      } catch (Exception e) {
        System.err.println("Couldn't retrieve dimension data for " + uri);

      }

      if (width != 0 && height != 0) {
        if (this.cache != null) {
          this.cache
              .setRegionBounds(new Rectangle2D.Double(0, 0, width, height));
          System.out.println("Set region bounds.");
        }
      }
      if (imageUrlString != null) {
        BufferedImage regionImage;
        try {

          if (!imageUrlString.startsWith("http://")) {
            imageUrlString = "http://" + imageUrlString;
          }
          if (this.cache != null) {
            this.cache.setRegionImageUrl(imageUrlString);
            URL imageUrl = new URL(imageUrlString);
            URLConnection conn = imageUrl.openConnection();
            conn.setConnectTimeout(5000);
            conn.connect();
            regionImage = ImageIO.read(conn.getInputStream());
            this.cache.setRegionImage(regionImage);
            System.out.println("Set image for " + uri + " as \"" + imageUrl
                + "\".");
          }

        } catch (MalformedURLException e) {
          log.warn("Malformed URL: {}", imageUrlString);
          e.printStackTrace();
        } catch (IOException e) {
          log.warn("Unable to load region image URL {} due to an exception.",
              imageUrlString, e);
          e.printStackTrace();
        }
      }
    }
  }

  protected void retrieveAnchors(final String regionName) {
    log.info("Retrieving anchor locations.");
    boolean success = false;
    do {
      try {
        Response res = this.wmc.getCurrentSnapshot(regionName + "\\.anchor.*",
            "location\\..*", "sensor.*");
        WorldState state = res.get();

        success = true;

        for (String uri : state.getURIs()) {

          String sensorString = null;
          BigInteger deviceId = null;
          double x = -1;
          double y = -1;
          Collection<Attribute> attribs = state.getState(uri);
          for (Attribute att : attribs) {
            if ("location.xoffset".equals(att.getAttributeName())) {
              x = ((Double) DataConverter.decodeUri(att.getAttributeName(),
                  att.getData())).doubleValue();
            } else if ("location.yoffset".equals(att.getAttributeName())) {
              y = ((Double) DataConverter.decodeUri(att.getAttributeName(),
                  att.getData())).doubleValue();
            } else if (att.getAttributeName().startsWith("sensor")) {
              if (att.getAttributeName().equals("sensor.mim")) {
                continue;
              }
              byte[] id = new byte[16];
              System.arraycopy(att.getData(), 1, id, 0, 16);
              sensorString = ByteArrayConverter.CONVERTER.asString(id);
              deviceId = new BigInteger(sensorString.substring(2), 16);
            }
          }

          if (x > 0 && y > 0 && deviceId != null) {
            if (uri.indexOf("wifi") != -1) {
              // sensorString = "2."+deviceId.toString(10);
              sensorString = deviceId.toString(10);
            } else if (uri.indexOf("pipsqueak") != -1) {
              // sensorString = "1." + deviceId.toString(10);
              sensorString = deviceId.toString(10);
            }

            // HashableByteArray deviceHash = new HashableByteArray(deviceId);
            Point2D location = new Point2D.Double(x, y);
            if (uri.contains("transmitter")) {
              this.cache.addFiduciaryTransmitter(uri);
            } else if (uri.contains("receiver")) {
              this.cache.addReceiver(uri);
            } else {
              return;
            }

            this.cache.mapSensorToUri(sensorString, uri);
            this.cache.setDeviceLocation(uri, location);
          }
        }

      } catch (Exception e) {
        log.error("Couldn't retrieve location data for anchors in "
            + this.region + ".", e);
        try {
          Thread.sleep(250);
        } catch (InterruptedException ie) {
          // Ignored
        }
      }
    } while (!success);

    log.info("Loaded anchors.");
  }

  protected void startStreams() {

    this.rssiHandler = new RssiHandler(this);
    this.rssiHandler.start();
    this.varianceHandler = new VarianceHandler(this);
    this.varianceHandler.start();
  }

  protected void noCacheListeners() {
    if (this.rssiHandler != null) {
      this.rssiHandler.shutdown();
      try {
        this.rssiHandler.join(JOIN_TIMEOUT);
      } catch (InterruptedException ie) {
        // Ignored
      }
    }
    if (this.varianceHandler != null) {
      this.varianceHandler.shutdown();
      try {

        this.varianceHandler.join(JOIN_TIMEOUT);
      } catch (InterruptedException ie) {

      }
    }

    if (this.wmc != null) {
      this.wmc.disconnect();
      log.info("Disconnected from world model.");
    }
  }

  private static final class RssiHandler extends Thread {

    private final ConnectionHandler handler;
    private boolean keepRunning = true;

    public RssiHandler(final ConnectionHandler handler) {
      this.handler = handler;
    }

    @Override
    public void run() {
      main: while (this.keepRunning) {
        log.info("Requesting average RSSI values.");
        
        if (this.handler.wmc == null) {
          log.info("RSSI Handler exiting.");

          break;
        }
        final StepResponse rssiResponse = this.handler.wmc.getStreamRequest(
            ".*", System.currentTimeMillis(), 0, "link average");

        WorldState state = null;
        while (!rssiResponse.isComplete() && !rssiResponse.isError()
            && this.keepRunning) {
          try {
            state = rssiResponse.next();

            if (state == null) {
              break;
            }
            for (String uri : state.getURIs()) {

              int txSensStart = uri.indexOf('.');
              int rxSensStart = uri.lastIndexOf('.');
              String txerSensor = uri.substring(txSensStart + 1, rxSensStart);
              String rxerSensor = uri.substring(rxSensStart + 1);
              Collection<Attribute> attribs = state.getState(uri);
              if (attribs == null || !attribs.iterator().hasNext()) {
                continue;
              }
              Attribute linkAvg = attribs.iterator().next();
              double value = DoubleConverter.CONVERTER
                  .decode(linkAvg.getData());
              if (this.handler.cache != null) {
                this.handler.cache.addRssi(rxerSensor, txerSensor,
                    (float) value, linkAvg.getCreationDate());
              }
            }
          } catch (Exception e) {
            log.error("Exception when retrieving RSSI value.", e);
            continue main;
          }
        }
        rssiResponse.cancel();
      }

    }

    public void shutdown() {
      this.keepRunning = false;
    }
  }

  private static final class VarianceHandler extends Thread {

    private final ConnectionHandler handler;
    private boolean keepRunning = true;

    public VarianceHandler(final ConnectionHandler handler) {
      this.handler = handler;
    }

    @Override
    public void run() {
      main: while (this.keepRunning) {
        log.info("Requesting RSSI variance values.");
        if (this.handler.wmc == null) {
          log.info("Variance Handler exiting.");

          break;
        }
        final StepResponse rssiResponse = this.handler.wmc.getStreamRequest(
            ".*", System.currentTimeMillis(), 0, "link variance");

        WorldState state = null;
        while (!rssiResponse.isComplete() && !rssiResponse.isError()
            && this.keepRunning) {
          try {
            state = rssiResponse.next();

            if (state == null) {
              break;
            }
            for (String uri : state.getURIs()) {

              int txSensStart = uri.indexOf('.');
              int rxSensStart = uri.lastIndexOf('.');
              String txerSensor = uri.substring(txSensStart + 1, rxSensStart);
              String rxerSensor = uri.substring(rxSensStart + 1);
              Collection<Attribute> attribs = state.getState(uri);
              if (attribs == null || !attribs.iterator().hasNext()) {
                continue;
              }
              Attribute linkAvg = attribs.iterator().next();
              double value = DoubleConverter.CONVERTER
                  .decode(linkAvg.getData());
              if (this.handler.cache != null) {
                this.handler.cache.addVariance(rxerSensor, txerSensor,
                    (float) value, linkAvg.getCreationDate());
              }
            }
          } catch (Exception e) {
            e.printStackTrace();
            continue main;
          }
        }
        rssiResponse.cancel();
      }
    }

    public void shutdown() {
      this.keepRunning = false;
    }
  }

  public String getClientHost() {
    return clientHost;
  }

  public int getClientPort() {
    return clientPort;
  }

  public String getRegion() {
    return region;
  }
}

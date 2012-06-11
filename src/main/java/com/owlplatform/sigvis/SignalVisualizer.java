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

package com.owlplatform.sigvis;

import java.awt.MediaTracker;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

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

public class SignalVisualizer {

  public static final String TITLE = "SigVis";

  public static final String VERSION = "1.0.0-BETA";
  public static final String ABOUT_HTML = "<h2 style=\"text-align: center;\">"
      + TITLE
      + " version "
      + VERSION
      + "</h2><p>Signal Visualization tools for the Make Sense Platform.</p>"
      + "<p style=\"font: smaller;\">Copyright &copy; 2012 Robert Moore and Rutgers University<br />"
      + "SigVis comes with ABSOLUTELY NO WARRANTY.<br />"
      + "This is free software, and you are welcome to redistribute it<br />"
      + "under certain conditions; see the included file LICENSE for details.</p>";

  public static final String ABOUT_TXT = TITLE
      + " version "
      + VERSION
      + "\n"
      + "Signal Visualization tools for the Make Sense Platform.\n\n"
      + "Copyright (C) 2012 Robert Moore and Rutgers University\n"
      + "SigVis comes with ABSOLUTELY NO WARRANTY.\n"
      + "This is free software, and you are welcome to redistribute it\n"
      + "under certain conditions; see the included file LICENSE for details.\n";

  public static final Logger log = LoggerFactory
      .getLogger(SignalVisualizer.class);

  public static void main(String[] args) {

    System.out.println(ABOUT_TXT);
    log.info(ABOUT_TXT);

    if (args.length == 3) {

      new SignalVisualizer(args[0], Integer.parseInt(args[1]), args[2]);
    } else {
      new SignalVisualizer();
    }

  }

  public SignalVisualizer() {
    this(null, -1, null);
  }

  public SignalVisualizer(final String wmHost, final int wmPort,
      final String regionName) {
    super();

    ConnectionHandler handler = new ConnectionHandler();
    FilteringDataCache cache = new FilteringDataCache(handler);
    if (wmHost != null) {
      handler.setClientConnection(wmHost, wmPort);
      handler.setRegion(regionName);

    }

    SimpleFrame initialFrame = new SimpleFrame("SigVis v1.0.0-BETA", cache);
    initialFrame.configureDisplay();
    if (wmHost != null) {
      if (handler.connectAsClient()) {
        handler.startup();
      } else {
        log.error("Connection failed.");
      }
    }

  }
}

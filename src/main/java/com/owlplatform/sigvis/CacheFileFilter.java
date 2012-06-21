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

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * A file filter for Sigvis cache files. The extension is ".svch".
 * @author Robert Moore
 *
 */
public class CacheFileFilter extends FileFilter {

  /**
   * Expected file extension.
   */
  public static final String EXTENSION = ".svch";
  /**
   * File type description.
   */
  private static final String DESCRIPTION = "SigVis Cache Files";
  
  @Override
  public boolean accept(File arg0) {
    if(arg0.isDirectory()){
      return true;
    }
    return arg0.getPath().indexOf(EXTENSION) > 0;
    
  }

  @Override
  public String getDescription() {
    return DESCRIPTION;
  }

}

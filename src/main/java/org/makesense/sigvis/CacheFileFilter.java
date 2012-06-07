package org.makesense.sigvis;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class CacheFileFilter extends FileFilter {

  public static final String EXTENSION = ".svch";
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

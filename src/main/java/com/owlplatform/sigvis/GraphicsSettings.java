package com.owlplatform.sigvis;

public class GraphicsSettings {

  private boolean useAA = false;
  private boolean useTransparency = false;
  private int desiredFps = 10;
  public boolean isUseAA() {
    return useAA;
  }
  public void setUseAA(boolean useAA) {
    this.useAA = useAA;
  }
  public boolean isUseTransparency() {
    return useTransparency;
  }
  public void setUseTransparency(boolean useTransparency) {
    this.useTransparency = useTransparency;
  }
  public int getDesiredFps() {
    return desiredFps;
  }
  public void setDesiredFps(int desiredFps) {
    this.desiredFps = desiredFps;
  }
  
}

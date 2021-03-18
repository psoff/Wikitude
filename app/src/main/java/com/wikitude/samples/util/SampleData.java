package com.wikitude.samples.util;

import java.io.Serializable;

public class SampleData implements Serializable {

  private final String name;
  private boolean isDeviceSupporting;
  private String isDeviceSupportingError;

  public SampleData(String name, boolean isDeviceSupporting, String isDeviceSupportingError) {
    this.name = name;
    this.isDeviceSupporting = isDeviceSupporting;
    this.isDeviceSupportingError = isDeviceSupportingError;
  }

  public String getName() {
    return name;
  }

  public boolean getIsDeviceSupporting() {
    return isDeviceSupporting;
  }

  public String getIsDeviceSupportingError() {
    return isDeviceSupportingError;
  }

}

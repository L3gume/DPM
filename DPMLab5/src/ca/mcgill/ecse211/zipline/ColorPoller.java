package ca.mcgill.ecse211.zipline;

import lejos.robotics.SampleProvider;

/**
 * This class is very simple: its only function is to continuously check the light level.
 * 
 * @author Justin Tremblay
 *
 */
public class ColorPoller extends Thread {
  // variables
  private SampleProvider sample1;
  private float[] lightData1;
  private LightLocalizer ll;

  private ZiplineController zc;

  public float lightl = 0.f;

  public enum l_mode {
    NONE, LOCALIZATION, ZIPLINING
  }
  private l_mode cur_mode = l_mode.NONE;

  /**
   * Constructor
   * 
   * @param sample1 a SampleProvider from which we fetch the samples.
   * @param lightData1 a float array to store the samples.
   * @param ll a LightLocalizer to which we will pass the data through a synchronized setter.
   */
  public ColorPoller(SampleProvider sample1, float[] lightData1) {
    this.sample1 = sample1;
    this.lightData1 = lightData1;
  }

  /**
   * Start the thread. Iterate forever, sending colour sensor readings to the classes that need them.
   */
  public void run() {
    // iterate forever
    while (true) {
      if (cur_mode == l_mode.LOCALIZATION) {
        sample1.fetchSample(lightData1, 0);
        if (lightData1[0] > 0.f) {
          ll.setLightLevel(lightData1[0]);
        }
      } else if (cur_mode == l_mode.ZIPLINING) {
        sample1.fetchSample(lightData1, 0);
        if (lightData1[0] > 0.f) {
          zc.setLightLevel(lightData1[0]);
        }
      }
      lightl = lightData1[0];
      try {
        Thread.sleep(20);
      } catch (Exception e) {
      } // Poor man's timed sampling
    }
  }

  /**
   * Helper methods.
   */
  
  /**
   * Set the light localizer to push data to.
   * @param ll light localizer
   */
  public void setLocalizer(LightLocalizer ll) {
    this.ll = ll;
  }

  /**
   * Set the zipline controller to push data to.
   * @param zc zipline controller
   */
  public void setZipController(ZiplineController zc) {
    this.zc = zc;
  }

  /**
   * Set the mode of the poller
   * @param mode the desired mode.
   */
  public void setMode(l_mode mode) {
    cur_mode = mode;
  }
}

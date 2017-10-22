package ca.mcgill.ecse211.zipline;

import lejos.robotics.SampleProvider;

/**
 * This class is very simple, its only function is to continuously check the light level.
 * 
 * @author Justin Tremblay
 *
 *         TODO: this class may need two color sensors, one for localizing and odometry correction
 *         and one for color detection
 */
public class ColorPoller extends Thread {
  private SampleProvider sample;
  private float[] lightData;
  private LightLocalizer ll;

  public enum l_mode {
    NONE, LOCALIZATION
  }

  private l_mode cur_mode = l_mode.NONE;

  /**
   * Constructor
   * 
   * @param sample a SampleProvider from which we fetch the samples.
   * @param lightData a float array to store the samples.
   * @param ll a LightLocalizer to which we will pass the data through a synchronized setter.
   */
  public ColorPoller(SampleProvider sample, float[] lightData) {
    this.sample = sample;
    this.lightData = lightData;
  }

  public void run() {
    while (true) {
      if (cur_mode == l_mode.LOCALIZATION) {
        sample.fetchSample(lightData, 0);
        if (lightData[0] > 0.f) {
          ll.setLightLevel(lightData[0]);
        }
        try {
          Thread.sleep(40);
        } catch (Exception e) {
        } // Poor man's timed sampling
      } else {
        // Nothing for now.
      }
    }
  }

  public void setLocalizer(LightLocalizer ll) {
    this.ll = ll;
  }
  
  public void setMode(l_mode localization) {
    cur_mode = localization;
  }
}

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
  private SampleProvider sample1;
  private float[] lightData1;
  private LightLocalizer ll;
  private OdometryCorrection oc;

  public enum l_mode {
    NONE, LOCALIZATION, CORRECTION
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

  public void run() {
    while (true) {
      if (cur_mode == l_mode.LOCALIZATION) {
        sample1.fetchSample(lightData1, 0);
        if (lightData1[0] > 0.f) {
          ll.setLightLevel(lightData1[0]);
        }
        try {
          Thread.sleep(20);
        } catch (Exception e) {
        } // Poor man's timed sampling
      } else if (cur_mode == l_mode.CORRECTION){
        // Nothing for now.
      }
    }
  }

  public void setLocalizer(LightLocalizer ll) {
    this.ll = ll;
  }
  
  public void setCorrection(OdometryCorrection oc) {
    this.oc = oc;
  }
  
  public void setMode(l_mode localization) {
    cur_mode = localization;
  }
}

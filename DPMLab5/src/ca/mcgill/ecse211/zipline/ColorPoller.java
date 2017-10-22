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
  private boolean done;

  private SampleProvider sample;
  private float[] lightData;
  private SensorData sd;

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
  public ColorPoller(SampleProvider sample, float[] lightData, SensorData sd) {
    this.done = false;
    this.sample = sample;
    this.lightData = lightData;
    this.sd = sd;
  }

  public void run() {
    while (!this.done) {
      // Stop polling data whenever the light level reference count in our
      // SensorData object has reached zero.
      if (this.sd.getLLRefs() > 0) {
        this.sample.fetchSample(this.lightData, 0);

        // * 100 to convert to cm.
        this.sd.lightLevelHandler(this.lightData[0] * 100.0f);
      }
      else {
        // Sleep indefinitely until this thread is interrupted, signaling that sensor
        // data may, once again, be needed.
        try {
          Thread.sleep(Long.MAX_VALUE);
        }
        catch (Exception e) {
          // ...
        }

        continue;
      }

      try {
        Thread.sleep(40);
      } catch (Exception e) {
        // ...
      }
    }
  }

  public void setMode(l_mode localization) {
    cur_mode = localization;
  }

  public void terminate() {
    this.done = true;
  }
}

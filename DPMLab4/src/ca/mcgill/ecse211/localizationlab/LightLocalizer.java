package ca.mcgill.ecse211.localizationlab;

import lejos.hardware.Button;
import lejos.hardware.Sound;

/**
 * 
 * @author Justin Tremblay
 *
 */
public class LightLocalizer extends Thread {
  private Driver driver;
  private Odometer odo;

  private final double SENSOR_OFFSET = 16.9; // The actual length won't give good results.
  private final float LIGHT_THRESHOLD = 0.37f;

  private int line_count = 0; // We will detect 4 lines in this lab
  private double[] angles = new double[4];

  private float light_level;

  public boolean done = false;

  public LightLocalizer(Driver driver, Odometer odo) {
    this.driver = driver;
    this.odo = odo;
  }

  public void run() {
    driver.rotate(360, true, true);
    localize();
  }

  private void localize() {
    // Start by finding all the lines
    sleepThread(1); // sleep the thread for a second to avoid false positives right off the bat.
    while (line_count != 4) {
      waitForLine();
      // The method returned, that means we found a line
      double theta = odo.getTheta();
      if (LocalizationLab.debug_mode) {
        System.out.println("Angle " + line_count + ": " + Math.toDegrees(theta));
      }
      
      angles[line_count++] = odo.getTheta(); // Record the angle at which we detected the line.
      
      sleepThread(0.5f); // wait for a second to avoid multiple detections of the same line.
    }

    driver.rotate(0, true, false);
    // We found all the lines, compute the position.
    computePosition();
  }

  private void computePosition() {
    /*
     * Here we know that we are always rotating in the same direction (counter-clockwise) so we know
     * that the first and third lines will be for the x position and the second and last will be for
     * the y position.
     * 
     * We also assume that both coordinates of the robot will always be negative.
     */
    
    double x_pos = -SENSOR_OFFSET * Math.cos((angles[2] - angles[0]) / 2);
    double y_pos = -SENSOR_OFFSET * Math.cos((angles[3] - angles[1]) / 2);
    
    // Both negative.
    if (x_pos > 0) {
      x_pos *= -1;
    }
    
    if (y_pos > 0) {
      y_pos *= -1;
    }
    
    odo.setX(x_pos);
    odo.setY(y_pos);
    
    done = true;
  }

  /**
   * This method stops the localizer until the light level becomes lower that the threshold level,
   * meaning we detected a line.
   */
  private void waitForLine() {
    while (getLightLevel() > LIGHT_THRESHOLD && getLightLevel() > 0.1f) {
    } ;
    Sound.setVolume(70);
    Sound.beep();
    return;
  }

  /*
   * Not really necessary, this is just to make the risingEdge and fallingEdge methods more
   * readable.
   */
  private void sleepThread(float seconds) {
    try {
      Thread.sleep((long) (seconds * 1000));
    } catch (Exception e) {
      // TODO: handle exception
    }
  }

  /*
   * Getters and Setters for the light_level, used by colorPoller
   */
  public synchronized float getLightLevel() {
    return light_level;
  }

  public synchronized void setLightLevel(float new_level) {
    light_level = new_level;
  }
}

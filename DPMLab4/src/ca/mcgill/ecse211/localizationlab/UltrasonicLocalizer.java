package ca.mcgill.ecse211.localizationlab;

import lejos.hardware.Button;
import lejos.hardware.Sound;

/**
 * 
 * @author Justin Tremblay
 */
public class UltrasonicLocalizer extends Thread {
  private Driver driver;
  private Odometer odo;

  // Enum for the localization mode chosen by the user.
  public enum Mode {
    FALLING_EDGE, RISING_EDGE, INVALID
  };
  private Mode mode = Mode.INVALID; // default value, should never stay that way.
  
  /*
   * Localization Constant(s)
   */
  private final float RISING_DIST_THRESHOLD = 30.f;
  private final float FALLING_DIST_THRESHOLD = 70.f;

  /*
   * Localization Variables
   */
  private float dist = -1.f;
  private float prev_dist = -1.f;

  private double theta1 = -1; // -1 is invalid since we are wrapping around from 359 to 0 and
  private double theta2 = -1; // vice-versa.

  // checked in main to ensure we don't skip steps.
  public boolean done = false;
  
  /**
   * 
   * @param mode enum value for selected localization mode.
   * @param driver driver object that handles moving the robot.
   * @param odo odometer.
   */
  public UltrasonicLocalizer(Mode mode, Driver driver, Odometer odo) {
    this.mode = mode;
    this.driver = driver;
    this.odo = odo;
  }

  /**
   * Run method. It is not in a loop, it only runs once.
   */
  public void run() {
    driver.rotate(360, true, true);
    switch (mode) {
      case FALLING_EDGE:
        fallingEdge();
        break;
      case RISING_EDGE:
        risingEdge();
        break;
      case INVALID:
        System.out.print("Invalid Ultrasonic Localizer mode, send help!");
        break;
    }
  }

  /*
   * The two next methods are the ultrasonic localization algorithms. They could be put into the
   * same method but it is a requirement to have them separated.
   */
  private void fallingEdge() {
    wait(mode);
    theta1 = odo.getTheta(); // Record the current theta.
    
    if (LocalizationLab.debug_mode) {
      System.out.println("theta1: " + theta1);
    }
    
    driver.rotate(-360, true, true);

    sleepThread(3); // Wait for a bit.

    wait(mode);
    driver.rotate(0, true, false);
    theta2 = odo.getTheta();
    
    if (LocalizationLab.debug_mode) {
      System.out.println("theta2: " + theta2);
    }
    
    computeOrientation();
  }

  private void risingEdge() {
    wait(mode);  
    theta1 = odo.getTheta(); // Record the current theta.
    
    if (LocalizationLab.debug_mode) {
      System.out.println("theta1: " + theta1);
    }
    
    // Rotate in the other direction.
    driver.rotate(-360, true, true);

    sleepThread(3); // Wait for a bit.

    wait(mode);
    driver.rotate(0, true, false);
    theta2 = odo.getTheta();
    
    if (LocalizationLab.debug_mode) {
      System.out.println("theta2: " + theta2);
    }
    
    computeOrientation();
  }
  
  /**
   * Computes the orientation of the robot using the recorded angles.
   */
  private void computeOrientation() {
    double new_theta = -1;
    switch (mode) {
      case FALLING_EDGE:
        new_theta = Math.toRadians(225.0) - ((theta1 + theta2) / 2);
        break;
      case RISING_EDGE:
        new_theta = Math.toRadians(45.0) - ((theta1 + theta2) / 2);
        break;
      case INVALID:
        System.out.print("Invalid Ultrasonic Localizer mode, send help!");
        break;
    }

    if (LocalizationLab.debug_mode) {
      System.out.println("current heading: " + Math.toDegrees(odo.getTheta()) + " error: " + Math.toDegrees(new_theta));
    }
    
    odo.setTheta(computeAngle(new_theta + odo.getTheta()));
    
    //System.out.print("Theta: " + odo.getTheta());
    // This can mean a long turn but hey, it's not required to do the smallest turn!
    Button.waitForAnyPress();
    driver.rotate(-odo.getTheta(), false, false);
    Button.waitForAnyPress();
    done = true;
  }

  /*
   * Utility methods, getters and setters.
   */
  
  /**
   * 
   * @param t_rad angle in radians
   * @return angle in radians, from 0 to 359.9999
   */
  private double computeAngle(double t_rad) {
    double t_deg = Math.toDegrees(t_rad);
    if (t_deg > 359.99999999 && t_deg >= 0) {
      t_deg = t_deg - 360;
    } else if (t_deg < 0) {
      t_deg = 360 + t_deg;
    }

    return Math.toRadians(t_deg);
  }

  /**
   * Used by the ultrasonic poller to pass the distance.
   * @param dist the distace read by the ultrasonic poller
   */
  public synchronized void setDist(float dist) {
    prev_dist = this.dist;
    this.dist = dist;
  }

  /**
   * returns the last distance read by the ultrasonic sensor.
   * @return distance (cm)
   */
  public synchronized float getDist() {
    return dist;
  }

  /**
   * returns the previous distance
   * @return previous distance. (cm)
   */
  public synchronized float getPrevDist() {
    return prev_dist;
  }

  /*
   * Not really necessary, this is just to make the risingEdge and fallingEdge methods more readable.
   */
  private void wait(Mode m) {
    Sound.setVolume(70);
    if (m == Mode.FALLING_EDGE) {
      while (getDist() > FALLING_DIST_THRESHOLD) {
      } ; // Wait until we capture a falling edge.
      Sound.beep();
      return;
    } else {
      while (getDist() < RISING_DIST_THRESHOLD) {
      } ; // Wait until we capture a rising edge.
      Sound.beep();
      return;
    }
  }

  /*
   * Not really necessary, this is just to make the risingEdge and fallingEdge methods more readable.
   */
  private void sleepThread(float seconds) {
    try {
      Thread.sleep((long) (seconds * 1000));
    } catch (Exception e) {
      // TODO: handle exception
    }
  }
}

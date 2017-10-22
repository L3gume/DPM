package ca.mcgill.ecse211.zipline;

import lejos.hardware.Button;
import lejos.hardware.Sound;

/**
 * 
 * @author Justin Tremblay
 */
public class UltrasonicLocalizer {
  private Driver driver;
  private Odometer odo;

  private Waypoint ref_pos;
  private int ref_angle;

  // Enum for the localization mode chosen by the user.
  public enum Mode {
    FALLING_EDGE, RISING_EDGE, INVALID
  };

  private Mode mode = Mode.RISING_EDGE; // default value, should never stay that way.

  /*
   * Localization Variables
   */
  private float dist = -1.f;
  private float prev_dist = -1.f;

  private double theta1 = -1; // -1 is invalid since we are wrapping around from 359 to 0 and
  private double theta2 = -1; // vice-versa.

  // checked in main to ensure we don't skip steps.
  public boolean done = false;
  public boolean localize = false;

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

  public void localize() {
    driver.rotate(360, true, true);
    risingEdge();
  }

  /*
   * The two next methods are the ultrasonic localization algorithms. They could be put into the
   * same method but it is a requirement to have them separated.
   */
  @SuppressWarnings("unused")
  private void fallingEdge() {
    wait(mode);
    theta1 = odo.getTheta(); // Record the current theta.

    if (ZipLineLab.debug_mode) {
      System.out.println("theta1: " + theta1);
    }

    driver.rotate(-360, true, true);

    sleepThread(3); // Wait for a bit.

    wait(mode);
    driver.rotate(0, true, false);
    theta2 = odo.getTheta();

    if (ZipLineLab.debug_mode) {
      System.out.println("theta2: " + theta2);
    }

    computeOrientation();
  }

  // This is left in the code just in case.
  @SuppressWarnings("unused")
  private void risingEdge() {
    wait(mode);
    theta1 = odo.getTheta(); // Record the current theta.

    if (ZipLineLab.debug_mode) {
      System.out.println("theta1: " + theta1);
    }

    // Rotate in the other direction.
    driver.rotate(-360, true, true);

    sleepThread(3); // Wait for a bit.

    wait(mode);
    driver.rotate(0, true, false);
    theta2 = odo.getTheta();

    if (ZipLineLab.debug_mode) {
      System.out.println("theta2: " + theta2);
    }

    computeOrientation();
  }

  /**
   * Computes the orientation of the robot using the recorded angles.
   */
  private void computeOrientation() {
    // compute the error with the reference angle (taken from the reference position).
    double theta_err = Math.toRadians(ref_angle) - ((theta1 + theta2) / 2);

    if (ZipLineLab.debug_mode) {
      System.out.println("current heading: " + Math.toDegrees(odo.getTheta()) + " error: "
          + Math.toDegrees(theta_err));
    }

    // Set the odometer's new orientation.
    odo.setTheta(computeAngle(theta_err + odo.getTheta()));

    done = true;
    localize = false;
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
   * 
   * @param dist the distace read by the ultrasonic poller
   */
  public synchronized void setDist(float dist) {
    prev_dist = this.dist;
    this.dist = dist;
  }

  /**
   * returns the last distance read by the ultrasonic sensor.
   * 
   * @return distance (cm)
   */
  public synchronized float getDist() {
    return dist;
  }

  /**
   * returns the previous distance
   * 
   * @return previous distance. (cm)
   */
  public synchronized float getPrevDist() {
    return prev_dist;
  }

  /*
   * Not really necessary, this is just to make the risingEdge and fallingEdge methods more
   * readable.
   */
  private void wait(Mode m) {
    Sound.setVolume(70);
    if (m == Mode.FALLING_EDGE) {
      while (getDist() > ZipLineLab.FALLING_DIST_THRESHOLD); // Wait until we capture a falling
                                                             // edge.
      Sound.beep();
      return;
    } else {
      while (getDist() < ZipLineLab.RISING_DIST_THRESHOLD); // Wait until we capture a rising edge.
      Sound.beep();
      return;
    }
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

  public void setRefPos(Waypoint ref_pos) {
    this.ref_pos = ref_pos;
    setRefAngle();
  }

  private void setRefAngle() {
    // TODO: these positions are only for lab 5, they will have to be changed (or removed) for the
    // final project since it's hard-coded crap.
    if (ref_pos.x == 1 && ref_pos.y == 1) {
      ref_angle = 45;
    } else if (ref_pos.x == 7 && ref_pos.y == 1) {
      ref_angle = 135;
    } else if (ref_pos.x == 7 && ref_pos.y == 7) {
      ref_angle = 225;
    } else if (ref_pos.x == 1 && ref_pos.y == 7) {
      ref_angle = 315;
    }
  }

  public synchronized void startLocalization() {
    done = false;
    localize = true;
  }
}

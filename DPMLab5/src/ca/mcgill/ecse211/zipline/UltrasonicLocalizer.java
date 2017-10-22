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
  private SensorData sd;

  private Object lock;

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

  /**
   * 
   * @param mode enum value for selected localization mode.
   * @param driver driver object that handles moving the robot.
   * @param odo odometer.
   */
  public UltrasonicLocalizer(Mode mode, Driver driver, Odometer odo, SensorData sd) {
    this.mode = mode;
    this.driver = driver;
    this.odo = odo;
    this.sd = sd;
  }

  /**
   * TODO: Add Javadoc description...
   */
  public void localize() {
    this.fallingEdge();
  }

  /**
   * TODO: Add Javadoc description...
   */
  private void edgeDetectionImpl(Mode mode) {
    boolean edgeDetected;

    this.driver.rotate(360, true, true);

    edgeDetected = false;

    // Detect edge #1.
    while (this.driver.isMoving()) {
      float d = this.sd.getUSDataLatest();

      // Update the current distance returned by our SensorData object.
      synchronized (this.lock) {
        this.prev_dist = this.dist;
        this.dist = d;
      }

      switch (mode) {
        case RISING_EDGE:
          if (d > ZipLineLab.RISING_DIST_THRESHOLD) {
            edgeDetected = true;
          }
          break;

        case FALLING_EDGE:
          if (d < ZipLineLab.FALLING_DIST_THRESHOLD) {
            edgeDetected = true;
          }
          break;

        // Error
        case INVALID:
          // This should be unreachable...
          System.out.println("error: edgeDetectionImpl(): `mode`: INVALID");
          System.exit(1);
      }

      // Stop rotating if we have already detected the edge.
      if (edgeDetected) {
        break;
      }

      try {
        Thread.sleep(30);
      }
      catch (Exception e) {
        // ...
      }
    }

    // Record the current theta.
    this.theta1 = this.odo.getTheta();

    if (ZipLineLab.debug_mode) {
      System.out.println("theta1: " + this.theta1);
    }

    this.driver.rotate(-360, true, true);

    try {
      // Sleep for a bit, so that we don't detect the same edge.
      Thread.sleep(1000);
    }
    catch (Exception e) {
      // ...
    }

    edgeDetected = false;

    // Detect edge #2.
    while (this.driver.isMoving()) {
      float d = this.sd.getUSDataLatest();

      // Update the current distance returned by our SensorData object.
      synchronized (this.lock) {
        this.prev_dist = this.dist;
        this.dist = d;
      }

      switch (mode) {
        case RISING_EDGE:
          if (d > ZipLineLab.RISING_DIST_THRESHOLD) {
            edgeDetected = true;
          }
          break;

        case FALLING_EDGE:
          if (d < ZipLineLab.FALLING_DIST_THRESHOLD) {
            edgeDetected = true;
          }
          break;

        // Error
        case INVALID:
          // This should be unreachable...
          System.out.println("error: edgeDetectionImpl(): `mode`: INVALID");
          System.exit(1);
      }

      try {
        Thread.sleep(30);
      }
      catch (Exception e) {
        // ...
      }
    }

    // Record the current theta.
    this.theta2 = this.odo.getTheta();

    // Stop rotation.
    this.driver.rotate(0, true, false);

    if (ZipLineLab.debug_mode) {
      System.out.println("theta2: " + this.theta2);
    }

    // Compute / update the current orientation.
    this.computeOrientation();

    this.sd.decrementUSRefs();
    this.done = true;
  }

  /*
   * The two next methods are the ultrasonic localization algorithms. They could be put into the
   * same method but it is a requirement to have them separated.
   */
  private void fallingEdge() {
    this.edgeDetectionImpl(Mode.FALLING_EDGE);
  }

  // This is left in the code just in case.
  @SuppressWarnings("unused")
  private void risingEdge() {
    this.edgeDetectionImpl(Mode.RISING_EDGE);
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
   * returns the last distance read by the ultrasonic sensor.
   * 
   * @return distance (cm)
   */
  public float getDist() {
    float d;
    synchronized (this.lock) {
      d = this.dist;
    }
    return d;
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
    sd.incrementUSRefs();
    done = false;
  }
}

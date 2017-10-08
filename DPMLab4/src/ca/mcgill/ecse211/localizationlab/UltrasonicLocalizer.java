package ca.mcgill.ecse211.localizationlab;
/**
 * 
 * @author Justin Tremblay
 * @author Xu Hai
 */
public class UltrasonicLocalizer extends Thread {
  private Driver driver;
  private Odometer odo;

  public enum Mode {
    FALLING_EDGE, RISING_EDGE, INVALID
  };
  private Mode mode = Mode.INVALID; // default value, should never stay that way.
  
  /*
   * Localization Constant(s)
   */
  private final float DIST_THRESHOLD = 50.f;

  /*
   * Localization Variables
   */
  private float dist = -1.f;
  private float prev_dist = -1.f;

  private double theta1 = -1; // -1 is invalid since we are wrapping around from 359 to 0 and
  private double theta2 = -1; // vice-versa.

  public UltrasonicLocalizer(Mode mode, Driver driver, Odometer odo) {
    this.mode = mode;
    this.driver = driver;
    this.odo = odo;
  }

  /**
   * Run method.
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
    driver.stop(); // We made it here, this means a falling edge has been detected.
    theta1 = computeAngle(odo.getTheta()); // Record the current theta.
    driver.rotate(-360, true, true);

    sleepThread(3); // Wait for a bit.

    wait(mode);
    driver.stop();
    theta2 = computeAngle(odo.getTheta());

    computeOrientation();
  }

  private void risingEdge() {
    wait(mode);
    driver.stop(); // We made it here, this means a falling edge has been detected.
    theta1 = computeAngle(odo.getTheta()); // Record the current theta.
    driver.rotate(-360, true, true);

    sleepThread(3); // Wait for a bit.

    wait(mode);
    driver.stop();
    theta2 = computeAngle(odo.getTheta());

    computeOrientation();
  }
  
  /**
   * Computes the orientation of the robot using the recorded angles.
   */
  private void computeOrientation() {
    double new_theta = -1;
    switch (mode) {
      case FALLING_EDGE:
        new_theta = 45.0 - ((theta1 + theta2) / 2);
        break;
      case RISING_EDGE:
        new_theta = 225.0 - ((theta1 + theta2) / 2);
        break;
      case INVALID:
        System.out.print("Invalid Ultrasonic Localizer mode, send help!");
        break;
    }

    odo.setTheta(computeAngle(new_theta + odo.getTheta()));
    driver.rotate(-odo.getTheta(), false, false);
  }

  /*
   * Utility methods, getters and setters.
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

  public synchronized float getDist() {
    return dist;
  }

  public synchronized float getPrevDist() {
    return prev_dist;
  }

  /*
   * Not really necessary, this is just to make the risingEdge and fallingEdge methods more readable.
   */
  private void wait(Mode m) {
    if (m == Mode.FALLING_EDGE) {
      while (getDist() > DIST_THRESHOLD) {
      } ; // Wait until we capture a falling edge.
      return;
    } else {
      while (getDist() < DIST_THRESHOLD) {
      } ; // Wait until we capture a rising edge.
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

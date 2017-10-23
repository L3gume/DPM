package ca.mcgill.ecse211.zipline;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class Odometer extends Thread {
  // robot position
  // Give volatile so that every access will be synchronized
  private volatile double x; // Position in X axis
  private volatile double y; // Position in Y axis
  private volatile double theta; // Heading
  private int leftMotorTachoCount;
  private int rightMotorTachoCount;
  private EV3LargeRegulatedMotor leftMotor;
  private EV3LargeRegulatedMotor rightMotor;

  private static final long ODOMETER_PERIOD = 25; /* odometer update period, in ms */
  private final double WHEEL_RAD;
  private final double WHEELBASE;
  private Object lock; /* lock object for mutual exclusion */

  // default constructor
  public Odometer(EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor, double wheel_rad, double wheel_base) {
    this.leftMotor = leftMotor;
    this.rightMotor = rightMotor;
    this.WHEEL_RAD = wheel_rad;
    this.WHEELBASE = wheel_base;
    this.x = 0.0;
    this.y = 0.0;
    this.theta = Math.toRadians(0.0);
    this.leftMotorTachoCount = 0;
    this.rightMotorTachoCount = 0;
    lock = new Object();

    leftMotor.resetTachoCount();
    rightMotor.resetTachoCount();
  }

  // run method (required for Thread)
  public void run() {
    long updateStart, updateEnd;

    while (true) {
      updateStart = System.currentTimeMillis();
      // TODO put (some of) your odometer code here

      int phi_l = leftMotor.getTachoCount() - getLeftMotorTachoCount(); // Variation of rotation of
                                                                        // right wheel, in degrees
      int phi_r = rightMotor.getTachoCount() - getRightMotorTachoCount(); // Variation of rotation
                                                                          // of right wheel, in
                                                                          // degrees

      // Set them for future use
      setLeftMotorTachoCount(leftMotor.getTachoCount());
      setRightMotorTachoCount(rightMotor.getTachoCount());

      double d_l = computeDisplacement(WHEEL_RAD, phi_l);
      double d_r = computeDisplacement(WHEEL_RAD, phi_r);

      // new theta
      double delta_theta = ((d_r - d_l) / WHEELBASE);

      double delta_dist = 0.5 * (d_l + d_r);
      // Compute the position variation
      
      double new_theta = Util.computeAngle(this.theta + delta_theta);
  
      double delta_x = delta_dist * Math.cos(new_theta);
      double delta_y = delta_dist * Math.sin(new_theta);
      
      synchronized (lock) {
        /**
         * Don't use the variables x, y, or theta anywhere but here! Only update the values of x, y,
         * and theta in this block. Do not perform complex math
         */

        // Update theta
        setTheta(new_theta);       

        // Update the position
        setX(getX() + delta_x);
        setY(getY() + delta_y);
      }

      // this ensures that the odometer only runs once every period
      updateEnd = System.currentTimeMillis();
      if (updateEnd - updateStart < ODOMETER_PERIOD) {
        try {
          Thread.sleep(ODOMETER_PERIOD - (updateEnd - updateStart));
        } catch (InterruptedException e) {
          // there is nothing to be done here because it is not
          // expected that the odometer will be interrupted by
          // another thread
        }
      }
    }
  }

  public void getPosition(double[] position, boolean[] update) {
    // ensure that the values don't change while the odometer is running
    synchronized (lock) {
      if (update[0])
        position[0] = x;
      if (update[1])
        position[1] = y;
      if (update[2])
        position[2] = Math.toDegrees(theta);
    }
  }

  public double getX() {
    double result;

    synchronized (lock) {
      result = x;
    }

    return result;
  }

  public double getY() {
    double result;

    synchronized (lock) {
      result = y;
    }

    return result;
  }

  public double getTheta() {
    double result;

    synchronized (lock) {
      result = theta;
    }

    return result;
  }

  // mutators
  public void setPosition(double[] position, boolean[] update) {
    // ensure that the values don't change while the odometer is running
    synchronized (lock) {
      if (update[0])
        x = position[0];
      if (update[1])
        y = position[1];
      if (update[2])
        theta = position[2];
    }
  }

  public void setX(double x) {
    synchronized (lock) {
      this.x = x;
    }
  }

  public void setY(double y) {
    synchronized (lock) {
      this.y = y;
    }
  }

  public void setTheta(double theta) {
    synchronized (lock) {
      this.theta = theta;
    }
  }

  /**
   * @return the leftMotorTachoCount
   */
  public int getLeftMotorTachoCount() {
    return leftMotorTachoCount;
  }

  /**
   * @param leftMotorTachoCount the leftMotorTachoCount to set
   */
  public void setLeftMotorTachoCount(int leftMotorTachoCount) {
    synchronized (lock) {
      this.leftMotorTachoCount = leftMotorTachoCount;
    }
  }

  /**
   * @return the rightMotorTachoCount
   */
  public int getRightMotorTachoCount() {
    return rightMotorTachoCount;
  }

  /**
   * @param rightMotorTachoCount the rightMotorTachoCount to set
   */
  public void setRightMotorTachoCount(int rightMotorTachoCount) {
    synchronized (lock) {
      this.rightMotorTachoCount = rightMotorTachoCount;
    }
  }

  private double computeDisplacement(double radius, double phi) {
    return (radius * Math.PI * phi) / 180;
  }
}

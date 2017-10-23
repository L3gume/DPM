package ca.mcgill.ecse211.zipline;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

// Taken from the last lab.

/**
 * 
 * Class that handles moving the robot when moving to a waypoint. Delegates control to the
 * PController when avoiding an obstacle.
 * 
 * @author Justin Tremblay
 *
 */
public class Driver {
  // The PController won't be necessary for this lab.
  // PController pCont;
  EV3LargeRegulatedMotor leftMotor;
  EV3LargeRegulatedMotor rightMotor;
  double leftRadius, rightRadius;
  double width;
  boolean rotating = false;
  boolean moving_forward = false;

  public Driver(EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor,
    double leftRadius, double rightRadius, double width) {
    this.leftMotor = leftMotor;
    this.rightMotor = rightMotor;
    this.leftRadius = leftRadius;
    this.rightRadius = rightRadius;
    this.width = width;
    // pCont = new PController(leftMotor, rightMotor, 30, 2);
    leftMotor.setAcceleration(3000);
    rightMotor.setAcceleration(3000);
  }

  /**
   * Rotate the robot the requested number of degrees.
   * 
   * @param angle - amount to rotate the robot
   * @param use_degrees - true for degrees, false for radians
   * @param ret - false to move on immediately, true to wait for motor to finish rotating before moving on to other code
   */
  public void rotate(double angle, boolean use_degrees, boolean ret) {
    leftMotor.setAcceleration(500);
    rightMotor.setAcceleration(500);
    leftMotor.setSpeed(ZipLineLab.ROTATE_SPEED);
    rightMotor.setSpeed(ZipLineLab.ROTATE_SPEED);
    if (!use_degrees) {
      angle = Math.toDegrees(angle);
    }

    if (angle < 0) {
      // turn right
      leftMotor.rotate(convertAngle(leftRadius, width, Math.abs(angle)), true);
      rightMotor.rotate(-convertAngle(rightRadius, width, Math.abs(angle)), false || ret);
    } else {
      // turn left
      leftMotor.rotate(-convertAngle(leftRadius, width, Math.abs(angle)), true);
      rightMotor.rotate(convertAngle(rightRadius, width, Math.abs(angle)), false || ret);
    }
  }

  /**
   * Move the robot forward the requested distance.
   * 
   * @param dist - the distance to move forward, in cm
   * @param ret - false to move on immediately, true to wait for motor to finish rotating before moving on to other code
   */
  public void moveForward(double dist, boolean ret) {
    leftMotor.setAcceleration(2000);
    rightMotor.setAcceleration(2000);
    leftMotor.setSpeed(ZipLineLab.FORWARD_SPEED);
    rightMotor.setSpeed(ZipLineLab.FORWARD_SPEED);
    leftMotor.rotate(convertDistance(leftRadius, dist), true);
    rightMotor.rotate(convertDistance(rightRadius, dist), false || ret);
  }

  public void infiniteMoveForward() {
    rightMotor.synchronizeWith(new EV3LargeRegulatedMotor[] {leftMotor});
    rightMotor.startSynchronization();
    rightMotor.setSpeed(ZipLineLab.FORWARD_SPEED);
    leftMotor.setSpeed(ZipLineLab.FORWARD_SPEED);
    rightMotor.forward();
    leftMotor.forward();
    rightMotor.endSynchronization();
  }
  
  public void stop() {
    rightMotor.synchronizeWith(new EV3LargeRegulatedMotor[] {leftMotor});
    rightMotor.startSynchronization();
    leftMotor.stop();
    rightMotor.stop();
    rightMotor.endSynchronization();
  }

  /**
   * Avoid an obstacle, staying at the specified distance from the obstacle.
   * 
   * @param dist - the distance to stay away from the obstacle
   */
  public void avoidObstacle(float dist) {
    // pCont.processUSData(dist);
  }

  /**
   * Helper methods
   */
  private static int convertDistance(double radius, double distance) {
    return (int) ((180.0 * distance) / (Math.PI * radius));
  }
  private static int convertAngle(double radius, double width, double angle) {
    return convertDistance(radius, Math.PI * width * angle / 360.0);
  }
  public boolean isRotating() {
    return rotating;
  }
  public boolean isGoingForward() {
    return moving_forward;
  }
}

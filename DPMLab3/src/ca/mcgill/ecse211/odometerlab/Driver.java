/*
 * SquareDriver.java
 */
package ca.mcgill.ecse211.odometerlab;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

// Taken from the last lab.

public class Driver {
  private static final int FORWARD_SPEED = 200;
  private static final int ROTATE_SPEED = 150;

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
  }
  
  public static void drive(EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor,
      double leftRadius, double rightRadius, double width) {
    // reset the motors
    for (EV3LargeRegulatedMotor motor : new EV3LargeRegulatedMotor[] {leftMotor, rightMotor}) {
      motor.stop();
      motor.setAcceleration(3000);
    }

    // wait 5 seconds
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      // there is nothing to be done here because it is not expected that
      // the odometer will be interrupted by another thread
    }

    /*for (int i = 0; i < 4; i++) {
      // drive forward two tiles
      leftMotor.setSpeed(FORWARD_SPEED);
      rightMotor.setSpeed(FORWARD_SPEED);

      leftMotor.rotate(convertDistance(leftRadius, 30), true);
      rightMotor.rotate(convertDistance(rightRadius, 30), false);

      // turn 90 degrees clockwise
      leftMotor.setSpeed(ROTATE_SPEED);
      rightMotor.setSpeed(ROTATE_SPEED);

      leftMotor.rotate(convertAngle(leftRadius, width, 90.0), true);
      rightMotor.rotate(-convertAngle(rightRadius, width, 90.0), false);
    }*/
  }
  
  
  
  public void rotate(double angle) {
    leftMotor.setSpeed(ROTATE_SPEED);
    rightMotor.setSpeed(ROTATE_SPEED);
    
    if (angle > 0) {
      // turn right
      //rotating = true;
      leftMotor.rotate(convertAngle(leftRadius, width, angle), true);
      rightMotor.rotate(-convertAngle(rightRadius, width, angle), true);
    } else {
      // turn left
      //rotating = true;
      leftMotor.rotate(-convertAngle(leftRadius, width, Math.abs(angle)), true);
      rightMotor.rotate(convertAngle(rightRadius, width, Math.abs(angle)), true);
    }
  }
  
  public void gotoPos(double dist) {
    leftMotor.setSpeed(FORWARD_SPEED);
    rightMotor.setSpeed(FORWARD_SPEED);

    leftMotor.rotate(convertDistance(leftRadius, dist), true);
    rightMotor.rotate(convertDistance(rightRadius, dist), true);
  }

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

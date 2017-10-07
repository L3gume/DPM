/*
 * SquareDriver.java
 */
package ca.mcgill.ecse211.localizationlab;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

// Taken from the last lab.

/**
 * Class that handles moving the robot when moving to a waypoint. Delegates control to the PController when avoiding an obstacle.
 * 
 * @author Justin Tremblay
 *
 */
public class Driver {
  private static final int FORWARD_SPEED = 200;
  private static final int ROTATE_SPEED = 75;

  // The PController won't be necessary for this lab.
  //PController pCont;
  
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
    
    //pCont = new PController(leftMotor, rightMotor, 30, 2);
    
    leftMotor.setAcceleration(3000);
    rightMotor.setAcceleration(3000);
  }
  
  public void rotate(double angle) {
    leftMotor.setAcceleration(1000);
    rightMotor.setAcceleration(1000);
    leftMotor.setSpeed(ROTATE_SPEED);
    rightMotor.setSpeed(ROTATE_SPEED);

    
    angle = Math.toDegrees(angle);
    
   if (angle < 0) {
      // turn right
      leftMotor.rotate(convertAngle(leftRadius, width, Math.abs(angle)), true);
      rightMotor.rotate(-convertAngle(rightRadius, width, Math.abs(angle)), true);
    } else {
      // turn left
     
      leftMotor.rotate(-convertAngle(leftRadius, width, Math.abs(angle)), true);
      rightMotor.rotate(convertAngle(rightRadius, width, Math.abs(angle)), true);
    }
  }
  
  public void moveTo(double dist) {
    leftMotor.setAcceleration(3000);
    rightMotor.setAcceleration(3000);
    leftMotor.setSpeed(FORWARD_SPEED);
    rightMotor.setSpeed(FORWARD_SPEED);
    
 

    leftMotor.rotate(convertDistance(leftRadius, dist), true);
    rightMotor.rotate(convertDistance(rightRadius, dist), true);
  }
  
  public void stop() {
    rightMotor.stop();
    leftMotor.stop();
  }
  
  public void avoidObstacle(float dist) {
    //pCont.processUSData(dist);
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

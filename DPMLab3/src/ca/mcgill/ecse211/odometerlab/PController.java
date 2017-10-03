package ca.mcgill.ecse211.odometerlab;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

// Code taken from Lab1 and slightly modified.
/**
 * Class that handles the movement of the robot while avoiding an obstacle. Slightly more hacky than
 * a proper PController.
 * 
 * @author Justin Tremblay
 *
 */
public class PController {
  // member constants
  private final int FILTER_COUNT = 5;
  private final int FILTER_DISTANCE = 70;
  private final int MOTOR_SPEED = 150;
  private final int RIGHT_SCALE = 2;
  private final double ERROR_SCALE = 1.7;
  private final int MAX_SPEED = 160;
  private final int ADJUST_COUNTER = 90;

  // passed member constants
  private final int bandCenter, bandwidth;
  private EV3LargeRegulatedMotor leftMotor, rightMotor;

  // member variables
  private volatile float distance;
  private int filterControl;
  private float distError = 0;
  private int rightTurnSpeedMult = 1;
  private int adjustCounter = 20;

  // Default Constructor
  public PController(EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor,
      int bandCenter, int bandwidth) {
    // Initialize Member Variables
    this.bandCenter = bandCenter;
    this.bandwidth = bandwidth;
    this.leftMotor = leftMotor;
    this.rightMotor = rightMotor;
    this.filterControl = 0;
  }

  /**
   * Take a decision depending on the distance read by the ultrasonic sensor.
   * 
   * @param sensorDistance the distance from the ultrasonic sensor to the obstacle.
   */
  public void processUSData(float sensorDistance) {
    // Filter used to delay when making big changes (ie sharp corners)
    if ((sensorDistance > FILTER_DISTANCE && this.filterControl < FILTER_COUNT)
        || sensorDistance < 0) {
      // bad value, do not set the sensorDistance var, however do increment the filter
      // value
      this.filterControl++;
      if (ObstacleAvoidanceLab.debug_mode) {
        System.out.println("[PController] filterCount: " + filterControl + "/" + FILTER_COUNT);
      }
      this.distance = bandCenter;
    } else if (sensorDistance >= FILTER_DISTANCE) {
      // set sensorDistance to FILTER_DISTANCE
      this.filterControl = 0;
      // this.distance = sensorDistance; //getAveragedReading(sensorDistance);
      this.distance = 70; // Just set it to our threshold
    } else if (sensorDistance > 0) {
      // sensorDistance went below FILTER_DISTANCE, therefore reset everything.
      this.filterControl = 0;
      this.distance = sensorDistance; // getAveragedReading(sensorDistance);
    }

    // If the distance is too high for too long, we're off track.
    if (distance >= 30) {
      if (adjustCounter++ > ADJUST_COUNTER) {
        leftAdjust();
        return;
      }
    } else {
      adjustCounter = 0;
    }

    // Calculate the distance Error from the bandCenter
    distError = bandCenter - distance;
    if (ObstacleAvoidanceLab.debug_mode) {
      System.out.println("[PController] Distance: " + distance);
      System.out.println("[PController] Dist Error: " + distError);
    }
    // Compute motor correction speeds (variableRate)
    float variableRate = (float) (ERROR_SCALE * Math.abs(distError));

    if (distance >= 0 && distance < 10) {
      backward();
      return;
    }

    // Travel straight
    if (Math.abs(distError) <= bandwidth) {
      forward();
    } else if (distError > 0) {

      // RIGHT_SCALE accounts for distError being disproportional from one side to the
      // other side of the bandCenter
      turnRight(variableRate);
    } else if (distError < 0) {

      turnLeft(variableRate);
    }
  }

  public float readUSDistance() {
    return this.distance;
  }

  private void turnRight(float variableRate) {
    variableRate *= 2;
    float leftSpeed = MOTOR_SPEED + (variableRate * RIGHT_SCALE);
    float rightSpeed = MOTOR_SPEED - (variableRate * RIGHT_SCALE);

    // We can't have the motors go over a maximum speed to prevent the robot from
    // going crazy.
    if (Math.abs(leftSpeed) > MAX_SPEED) {
      leftSpeed = (leftSpeed * rightTurnSpeedMult * MAX_SPEED) / Math.abs(leftSpeed);
    }
    if (Math.abs(rightSpeed) > MAX_SPEED) {
      rightSpeed = (rightSpeed * rightTurnSpeedMult * MAX_SPEED) / Math.abs(rightSpeed);
    }

    leftMotor.setSpeed(Math.abs(leftSpeed));
    rightMotor.setSpeed(Math.abs(rightSpeed));
    if (leftSpeed > 0) {
      leftMotor.forward();
    } else {
      leftMotor.backward();
    }
    if (rightSpeed > 0) {
      rightMotor.forward();
    } else {
      rightMotor.backward();
    }

  }

  private void turnLeft(float variableRate) {
    float leftSpeed = MOTOR_SPEED - variableRate;
    float rightSpeed = MOTOR_SPEED + variableRate;

    // We can't have the motors go over a maximum speed to prevent the robot from
    // going crazy.
    if (Math.abs(leftSpeed) > MAX_SPEED) {
      leftSpeed = leftSpeed * MAX_SPEED / Math.abs(leftSpeed);
    }
    if (Math.abs(rightSpeed) > MAX_SPEED) {
      rightSpeed = rightSpeed * MAX_SPEED / Math.abs(rightSpeed);
    }

    leftMotor.setSpeed(Math.abs(leftSpeed));
    rightMotor.setSpeed(Math.abs(rightSpeed));
    if (leftSpeed > 0) {
      leftMotor.forward();
    } else {
      // Hacky part: the controller isn't truly proportional when turning left but that's fine.
      leftMotor.setSpeed(120);
      leftMotor.forward();
    }
    if (rightSpeed > 0) {
      rightMotor.forward();
    } else {
      rightMotor.backward();
    }
  }

  private void forward() {
    leftMotor.setSpeed(MOTOR_SPEED);
    rightMotor.setSpeed(MOTOR_SPEED);
    leftMotor.forward();
    rightMotor.forward();
  }

  private void backward() {
    leftMotor.setSpeed(100);
    rightMotor.setSpeed(150);
    leftMotor.backward();
    rightMotor.backward();
  }

  private void leftAdjust() {
    // Actually using a proportional speed will almost never work in this case
    leftMotor.setSpeed(40);
    rightMotor.setSpeed(155);
    leftMotor.forward();
    rightMotor.forward();
  }

}

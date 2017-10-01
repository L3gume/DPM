package ca.mcgill.ecse211.odometerlab;

import lejos.hardware.motor.EV3LargeRegulatedMotor;


// Code taken from Lab1 and slightly modified.
public class PController /* implements UltrasonicController */ {
  private Object lock;
  // member constants
  private final int FILTER_COUNT = 1;
  private final int FILTER_DISTANCE = 70;
  private final int MOTOR_SPEED = 150;
  private final int RIGHT_SCALE = 2;
  private final double ERROR_SCALE = 1.7;
  private final int MAX_SPEED = 175;
  private final int ADJUST_COUNTER = 60;

  private final String TURN_RIGHT = "TURN_RIGHT";
  private final String TURN_LEFT = "TURN_LEFT";
  private final String NO_TURN = "NO_TURN";
  private final String BACKWARDS = "BACKWARDS";
  private final String ADJUST_LEFT = "ADJUST_LEFT";
  private String status;

  // passed member constants
  private final int bandCenter, bandwidth;
  private EV3LargeRegulatedMotor leftMotor, rightMotor;

  // member variables
  private volatile float distance;
  private int filterControl;
  private float distError = 0;
  private int rightTurnSpeedMult = 1;
  private int adjustCounter = 20;

  private final int ARRAY_LENGTH = 5;

  // Default Constructor
  public PController(EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor,
      int bandCenter, int bandwidth) {

    // Initialize Member Variables
    this.bandCenter = 30; // bandCenter;
    this.bandwidth = 2;// bandwidth;
    this.leftMotor = leftMotor;
    this.rightMotor = rightMotor;
    this.filterControl = 0;
    this.status = NO_TURN;
    lock = new Object();
  }

  public void processUSData(float sensorDistance) {
    // Filter used to delay when making big changes (ie sharp corners)
    // sensorDistance /= 1.3;
    synchronized (lock) {
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

      if (distance > 25) {
        if (adjustCounter++ > ADJUST_COUNTER) {
          leftAdjust();
          setStatus(ADJUST_LEFT);
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
        setStatus(BACKWARDS);
        return;
      }

      // Travel straight
      if (Math.abs(distError) <= bandwidth) {
        forward();
        setStatus(NO_TURN);
      } else if (distError > 0) {

        // RIGHT_SCALE accounts for distError being disproportional from one side to the
        // other side of the bandCenter
        turnRight(variableRate);
        setStatus(TURN_RIGHT);
      } else if (distError < 0) {

        turnLeft(variableRate);
        setStatus(TURN_LEFT);
      }
    }

    if (ObstacleAvoidanceLab.debug_mode) {
      System.out.println("[PController] Status: " + status);
    }
  }


  public float readUSDistance() {
    return this.distance;
  }

  public String getStatus() {
    return status;
  }

  private void setStatus(String s) {
    status = s;
  }

  private void turnRight(float variableRate) {
    variableRate *= 2;
    float leftSpeed = MOTOR_SPEED + (variableRate /** 4 */
        * RIGHT_SCALE);
    float rightSpeed = MOTOR_SPEED - (variableRate /** 4 */
        * RIGHT_SCALE);

    /*
     * if (leftSpeed > MAX_SPEED) { leftSpeed = MAX_SPEED + 100; } else if (leftSpeed < 0) {
     * leftSpeed = 50; }
     * 
     * if (rightSpeed > MAX_SPEED) { rightSpeed = MAX_SPEED; } else if (rightSpeed < 0) { rightSpeed
     * = 50; }
     */

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
      leftMotor.setSpeed(100);
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
    leftMotor.setSpeed(50);
    rightMotor.setSpeed(175);
    leftMotor.forward();
    rightMotor.forward();
  }

}

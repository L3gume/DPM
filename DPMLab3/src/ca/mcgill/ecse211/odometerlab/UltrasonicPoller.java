package ca.mcgill.ecse211.odometerlab;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;
import lejos.robotics.filter.MeanFilter;

// This is based on the code that was given for the first Lab (wallfollowing).
// This is meant to detect obstacles using the ultrasonic sensor mounted on a medium motor
// so we can scan for obstacles in multiple directions.

public class UltrasonicPoller extends Thread {
  // Ultrasonic sensor port.
  private static final Port usPort = LocalEV3.get().getPort("S2");

  private EV3MediumRegulatedMotor sensorMotor;
  private boolean motor_rotated = false;
  private SampleProvider us;
  private SampleProvider mean;
  // private UltrasonicController cont;
  private float[] usData;
  // private Navigator nav;
  private float distance = 0f;

  private Navigation nav;

  private Object lock;

  public UltrasonicPoller(EV3MediumRegulatedMotor sensorMotor) {
    @SuppressWarnings("resource") // Because we don't bother to close this resource
    SensorModes usSensor = new EV3UltrasonicSensor(usPort); // usSensor is the instance
    us = usSensor.getMode("Distance"); // usDistance provides samples from
                                       // this instance
    mean = new MeanFilter(us, us.sampleSize());
    usData = new float[mean.sampleSize()]; // usData is the buffer in which data are
    // returned
    this.sensorMotor = sensorMotor;
    sensorMotor.setSpeed(50);

    lock = new Object();
  }

  /*
   * Sensors now return floats using a uniform protocol. Need to convert US result to an integer
   * [0,255] (non-Javadoc)
   * 
   * @see java.lang.Thread#run()
   */
  public void run() {
    while (true) {
      mean.fetchSample(usData, 0); // acquire data
      distance = usData[0] * 100.0f; // extract from buffer, cast to int
      // cont.processUSData(distance); // now take action depending on value
      processData(distance);
      try {
        Thread.sleep(40);
      } catch (Exception e) {
      } // Poor man's timed sampling
    }
  }

  /**
   * processData
   * 
   * Continuously checks for the distance in front of the robot, a small distance means an obstacle
   * in front of us. If an obstacle is detected, we rotate the ultrasonic sensor 50 degress to the
   * left so that we can easily see the obstacle whle avoiding it.
   * 
   * 
   * @param distance: Distance in centimeters.
   *
   */
  private void processData(float distance) {
    if (distance < 15) {
      // That's an obstacle, we will do our thing.
      // Let the navigator know we have an obstacle in front of us and work with it to avoid the
      // obstacle.
      // System.out.println("[AVOIDING] Obstacle detected!!!!!");
      if (!nav.getObstacleDetected()) {
        nav.setObstacleDetected(true);
        setSensorPosition(true);
      }
    } else {
      if (!nav.getObstacleDetected()) {
        setSensorPosition(false);
      }
    }
  }

  /**
   * Handles rotating the ultrasonic sensor. Puts it in the same position as in the PController lab.
   */
  private void setSensorPosition(boolean set) {
    if (!motor_rotated && set) {
      sensorMotor.rotate(50, false);
      sensorMotor.stop();
      motor_rotated = true;
    } else if (motor_rotated && !set) {
      sensorMotor.rotate(-50, false);
      sensorMotor.stop();
      motor_rotated = false;
    }
  }

  public float getDistance() {
    // guarantee exclusive access to the variable since it is continuously being written to.
    synchronized (lock) {
      return distance;
    }
  }

  public void setNav(Navigation n) {
    nav = n;
  }
}

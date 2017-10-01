package ca.mcgill.ecse211.odometerlab;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;

// This is based on the code that was given for the first Lab (wallfollowing).
// This is meant to detect obstacles using the ultrasonic sensor mounted on a medium motor
// so we can scan for obstacles in multiple directions.

/**
 * Control of the wall follower is applied periodically by the UltrasonicPoller thread. The while
 * loop at the bottom executes in a loop. Assuming that the us.fetchSample, and cont.processUSData
 * methods operate in about 20mS, and that the thread sleeps for 50 mS at the end of each loop, then
 * one cycle through the loop is approximately 70 mS. This corresponds to a sampling rate of 1/70mS
 * or about 14 Hz.
 */
public class UltrasonicPoller extends Thread {
  private static final Port usPort = LocalEV3.get().getPort("S2");

  private EV3MediumRegulatedMotor sensorMotor;
  private double motor_orientation;
  private SampleProvider us;
  // private UltrasonicController cont;
  private float[] usData;
  // private Navigator nav;
  private volatile float distance = 0f;

  public UltrasonicPoller(EV3MediumRegulatedMotor sensorMotor) {
	  @SuppressWarnings("resource") // Because we don't bother to close this resource
      SensorModes usSensor = new EV3UltrasonicSensor(usPort); // usSensor is the instance
      us = usSensor.getMode("Distance"); // usDistance provides samples from
                                                                  // this instance
      usData = new float[us.sampleSize()]; // usData is the buffer in which data are
                                                              // returned
      this.sensorMotor = sensorMotor;
      sensorMotor.setSpeed(50);
      motor_orientation = 0.0;
	}

  /*
   * Sensors now return floats using a uniform protocol. Need to convert US result to an integer
   * [0,255] (non-Javadoc)
   * 
   * @see java.lang.Thread#run()
   */
  public void run() {
    while (true) {
      us.fetchSample(usData, 0); // acquire data
      distance = usData[0] * 100.0f; // extract from buffer, cast to int
      // cont.processUSData(distance); // now take action depending on value
      processData(distance);
      try {
        Thread.sleep(40);
      } catch (Exception e) {
      } // Poor man's timed sampling
    }
  }

  private void processData(float distance) {
    if (distance < 15) {
      // That's an obstacle, we will do our thing.
      // Let the navigator know we have an obstacle in front of us and work with it to avoid the
      // obstacle.
      Navigator.obstacle_detected = true;
      if (motor_orientation != 45.0) {
        sensorMotor.rotate(45, true);
        motor_orientation = 45.0;
      }
    } else if (distance > 150) {
      Navigator.obstacle_detected = false;
      if (motor_orientation != 0.0) {
        sensorMotor.rotate(-45, true);
        motor_orientation = 0;
      }
    }
  }

  public float getDistance() {
    return distance;
  }

}

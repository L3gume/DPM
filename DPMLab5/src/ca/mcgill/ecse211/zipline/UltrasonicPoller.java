package ca.mcgill.ecse211.zipline;

import lejos.robotics.SampleProvider;

// This is based on the code that was given for the first Lab (wallfollowing).
// This is meant to detect obstacles using the ultrasonic sensor mounted on a medium motor
// so we can scan for obstacles in multiple directions.

public class UltrasonicPoller extends Thread {
//  private Navigation nav; Not used in this lab. 

  private boolean done;

  private SensorData sd;
  private SampleProvider sample;
  private float[] usData;
  private float distance;
  
  public enum u_mode {
    NONE, LOCALIZATION, AVOIDANCE, SEARCH
  };
  
  private u_mode cur_mode = u_mode.NONE;
  
  public UltrasonicPoller(SampleProvider sample, float[] usData, SensorData sd) {
    this.done = false;
    this.sample = sample;
    this.usData = usData;
    this.sd = sd;
  }

  public void run() {
    float dist;

    while (!this.done) {
      // Stop polling data whenever the ultrasonic reference count in our
      // SensorData object has reached zero.
      if (this.sd.getUSRefs() > 0) {
        this.sample.fetchSample(this.usData, 0);

        // * 100 to convert to cm.
        dist = this.usData[0] * 100.0f;
        if (dist > 255.0f) {
            dist = 255.0f;
        }

        this.sd.ultrasonicHandler(dist);
        this.distance = dist;
      }
      else {
        // Sleep indefinitely until this thread is interrupted, signaling that sensor
        // data may, once again, be needed.
        try {
          Thread.sleep(Long.MAX_VALUE);
        }
        catch (Exception e) {
          // ...
        }

        continue;
      }

      try {
        Thread.sleep(40);
      } catch (Exception e) {
        // ...
      }
    }
  }

  // This method is not used in this Lab.
  /**
   * Handles rotating the ultrasonic sensor. Puts it in the same position as in the PController lab.
   */
  /*private void setSensorPosition(boolean set) {
    if (!motor_rotated && set) {
      sensorMotor.rotate(50, false);
      sensorMotor.stop();
      motor_rotated = true;
    } else if (motor_rotated && !set) {
      sensorMotor.rotate(-50, false);
      sensorMotor.stop();
      motor_rotated = false;
    }
  }*/
  
  public float getDistance() {
    return this.distance;
  }

  // Not used in this lab.
//  public void setNav(Navigation n) {
//    nav = n;
//  }
  
  public u_mode getMode() {
    return cur_mode;
  }
  
  public void setMode(u_mode new_mode) {
    cur_mode = new_mode;
  }
  
  public void terminate() {
    this.done = true;
  }
}

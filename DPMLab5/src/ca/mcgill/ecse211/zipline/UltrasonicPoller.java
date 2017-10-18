package ca.mcgill.ecse211.zipline;

import lejos.robotics.SampleProvider;

// This is based on the code that was given for the first Lab (wallfollowing).
// This is meant to detect obstacles using the ultrasonic sensor mounted on a medium motor
// so we can scan for obstacles in multiple directions.

public class UltrasonicPoller extends Thread {
//  private Navigation nav; Not used in this lab. 
  private UltrasonicLocalizer ul;
  private SampleProvider sample;
  private float[] usData;
  private float distance;
  
  public enum u_mode {
    NONE, LOCALIZATION, AVOIDANCE, SEARCH
  };
  
  private u_mode cur_mode = u_mode.NONE;
  
  public UltrasonicPoller(SampleProvider sample, float[] usData) {
    this.sample = sample;
    this.usData = usData;
  }

  public void run() {
    // Terminate whenever the ultrasonic localizer is done to spare system resources.
    while (!ul.done) {
        sample.fetchSample(usData, 0);
        // * 100 to convert to cm.
        ul.setDist(usData[0] * 100.f);
        distance = usData[0];
      try {
        Thread.sleep(30);
      } catch (Exception e) {
      } // Poor man's timed sampling
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
  
  public void setLocalizer(UltrasonicLocalizer ul) {
    this.ul = ul;
  }
}

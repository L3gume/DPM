package ca.mcgill.ecse211.zipline;

import lejos.hardware.Sound;

/**
 * 
 * @author Justin Tremblay
 *
 */

// TODO: Add angle correction.
public class LightLocalizer {
  private Driver driver;
  private Odometer odo;

  private Waypoint ref_pos; // The reference position to localize, used to be [0,0], now can be any
                            // corner of the area.
  private int ref_angle; // Reference angle to start the light localization. (points to the middle
                         // of the area)

  private int line_count = 0; // We will detect 4 lines in this lab
  private double[] angles = new double[4];

  private float light_level;
  
  private int x_pos_mult = 1;
  private int y_pos_mult = 1;

  public boolean done = false;

  public LightLocalizer(Driver driver, Odometer odo) {
    this.driver = driver;
    this.odo = odo;
  }

  /**
   * Where the magic happens. Get the heading (angle from 0 to 359.999) at the 4 lines before
   * computing the robot's position.
   */
  public void localize() {
    done = false;
    line_count = 0;
    if (ZipLineLab.debug_mode) {
      System.out.println("[LIGHTLOC] rotate to align: " + (ref_angle - odo.getTheta()));
    }

    if (!ref_pos.equals(ZipLineLab.START_POS)) {
      //System.out.println("[LIGHT] NOT START_POS, update ref angle");
      // we just want to change it if it isn't the starting position.
      ref_angle = updateRefAngle(Util.getDir(Math.toDegrees(odo.getTheta())));
    }

    driver.rotate(Math.toRadians(ref_angle) - odo.getTheta(), false, false); // align to ref_angle
                                                                             // // reference angle
    driver.rotate(360, true, true);

    // Start by finding all the lines
    while (line_count != 4) {
      waitForLine();
      // The method returned, that means we found a line
      double theta = odo.getTheta();
      if (ZipLineLab.debug_mode) {
        System.out.println("Angle " + line_count + ": " + Math.toDegrees(theta));
      }

      angles[line_count++] = odo.getTheta(); // Record the angle at which we detected the line.
      try {
        Thread.sleep(500);
      } catch (Exception e) {
        System.out.println("Can't pause thread");
        // TODO: handle exception
      }
    }

    driver.rotate(0, true, false);
    // We found all the lines, compute the position.
    computePosition();
  }

  /**
   * Computes the position of the robot using the angles found in the localize() method.
   */
  private void computePosition() {
    // Rotate array depending on which position the robot was initially facing, which changes the
    // order the lines were detected in.
    Util.rotateArray(angles, (int) (ref_angle / 90));

//    System.out.println("[LIGHT] ref_angle: " + ref_angle + " ref_pos: [" + ref_pos.x + ";" + ref_pos.y + "]");
    
    if (!ref_pos.equals(ZipLineLab.START_POS)) {
      // we just want to change it if it isn't the starting position.
      updatePosMultipliers();
//      System.out.println("[LIGHT] x_mult: " + x_pos_mult + " y_mult: " + y_pos_mult);
    }
    
    double x_pos = -ZipLineLab.SENSOR_OFFSET * Math.cos((angles[2] - angles[0]) / 2);
    double y_pos = -ZipLineLab.SENSOR_OFFSET * Math.cos((angles[3] - angles[1]) / 2);

    if (ref_pos.equals(ZipLineLab.START_POS)) {
      // Hard-coded bit, kinda crappy, but works and there's more important stuff to think about.
      if (ref_pos.y == 7) {
        // we are over the x axis
        if (y_pos < 0) {
          y_pos *= -1;
        }
      }
      if (ref_pos.x == 7) {
        // we are past the y axis
        if (x_pos < 0) {
          x_pos *= -1;
        }
      }
    } else {
      y_pos *= y_pos_mult;
      x_pos *= x_pos_mult;
    }

    x_pos = ref_pos.x * ZipLineLab.SQUARE_LENGTH + x_pos;
    y_pos = ref_pos.y * ZipLineLab.SQUARE_LENGTH + y_pos;

    odo.setX(x_pos);
    odo.setY(y_pos);

    /* Angle correction */
    correctAngle();

    // Notify the main method that we are done.
    done = true;
  }

  /**
   * This method stops the localizer until the light level becomes lower that the threshold level,
   * meaning we detected a line.
   */
  private void waitForLine() {
    while (getLightLevel() > ZipLineLab.LIGHT_THRESHOLD && getLightLevel() > 0.1f);
    Sound.setVolume(70);
    Sound.beep();
    return;
  }

  /*
   * Getters and Setters for the light_level, used by colorPoller
   */
  public synchronized float getLightLevel() {
    return light_level;
  }

  public synchronized void setLightLevel(float new_level) {
    light_level = new_level;
  }

  public void setRefPos(Waypoint ref_pos) {
    this.ref_pos = ref_pos;
    ref_angle = Util.findRefAngle(this.ref_pos);
  }

  private int updateRefAngle(Util.dir direction) {
    int ret = 45; // default value
    switch (direction) {
      case ZERO: // fallthrough
      case NINETY:
        ret = 45;
        break;
      case ONEEIGHTY:
      case TWOSEVENTY:
        ret = 225;
        break;
    }
    return ret;
  }
  
  private void updatePosMultipliers() {
    if (ref_angle == 45) {
      y_pos_mult = (angles[1] - angles[0]) > (angles[2] - angles[1]) ? 1 : -1;
      x_pos_mult = (angles[3] - angles[2]) > (angles[2] - angles[1]) ? 1 : -1;
    } else if (ref_angle == 225) {
      y_pos_mult = (angles[1] - angles[0]) > (angles[2] - angles[1]) ? -1 : 1;
      x_pos_mult = (angles[3] - angles[2]) > (angles[2] - angles[1]) ? -1 : 1;
    }
  }
  
  private void correctAngle() {
    double err_theta = Math.toRadians(79) + ((angles[2] - angles[0]) / 2) - (angles[2] - angles[0]);
    odo.setTheta(Util.computeAngle(odo.getTheta() + err_theta));
  }
}


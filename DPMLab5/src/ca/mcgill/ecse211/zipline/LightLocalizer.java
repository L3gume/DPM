package ca.mcgill.ecse211.zipline;

import lejos.hardware.Sound;

/**
 * 
 * @author Justin Tremblay
 *
 */

//TODO: Add angle correction.
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
    driver.rotate(Math.toRadians(ref_angle) - odo.getTheta(), false, false); // align to the
                                                                             // reference angle
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
    rotateArray(angles, (int) (ref_angle / 90));

    double x_pos = -ZipLineLab.SENSOR_OFFSET * Math.cos((angles[2] - angles[0]) / 2);
    double y_pos = -ZipLineLab.SENSOR_OFFSET * Math.cos((angles[3] - angles[1]) / 2);

    x_pos = ref_pos.x * ZipLineLab.SQUARE_LENGTH + x_pos;
    y_pos = ref_pos.y * ZipLineLab.SQUARE_LENGTH + y_pos;

    odo.setX(x_pos);
    odo.setY(y_pos);

    // Notify the main method that we are done.
    done = true;
  }

  private static void rotateArray(double[] angles2, int order) {
    if (angles2 == null || order < 0) {
      throw new IllegalArgumentException(
          "The array must be non-null and the order must be non-negative");
    }
    int offset = angles2.length - order % angles2.length;
    if (offset > 0) {
      double[] copy = angles2.clone();
      for (int i = 0; i < angles2.length; ++i) {
        int j = (i + offset) % angles2.length;
        angles2[i] = copy[j];
      }
    }
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
    setRefAngle();
  }

  private void setRefAngle() {
    // TODO: these positions are only for lab 5, they will have to be changed (or removed) for the
    // final project since it's hard-coded crap.
    if (ref_pos.x == 1 && ref_pos.y == 1) {
      ref_angle = 45;
    } else if (ref_pos.x == 7 && ref_pos.y == 1) {
      ref_angle = 135;
    } else if (ref_pos.x == 7 && ref_pos.y == 7) {
      ref_angle = 225;
    } else if (ref_pos.x == 1 && ref_pos.y == 7) {
      ref_angle = 315;
    } else {
      ref_angle = 45;
    }

    if (ZipLineLab.debug_mode) {
      System.out.println("[LOCALIZER] Reference position: [" + ref_pos.x + " ; " + ref_pos.y + "]");
      System.out.println("[LOCALIZER] Reference angle: " + ref_angle);
    }
  }
}


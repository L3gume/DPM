package ca.mcgill.ecse211.zipline;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

/**
 * Handles moving on robot on the zipline
 * 
 * @author Justin Tremblay
 *
 */
public class ZiplineController {

  private Odometer odo;
  private Driver dr;
  private EV3LargeRegulatedMotor zip_motor;

  // private Waypoint zipline_start_position;
  // private Waypoint zipline_end_position;

  // TODO: See if it is possible to use the color sensor at the back of the robot to determine if
  // the robot is touching the ground or not.

  public enum zip_state {
    IDLE, ALIGNING, MOVING, ZIPLINING, DONE
  };

  private zip_state cur_state = zip_state.IDLE;
  public boolean traverse = false;
  public boolean done = false;
  private float light_level = 0.f;

  private final int FILTER_MAX = 20;
  private int filter_count;

  double zip_vect[] = {1.0, 0.0};

  public ZiplineController(Odometer odo, Driver dr, EV3LargeRegulatedMotor zip_motor) {
    this.odo = odo;
    this.dr = dr;
    this.zip_motor = zip_motor;
  }

  /**
   * run() method.
   */
  public void process() {
    switch (cur_state) {
      case IDLE:
        cur_state = process_idle();
        break;
      case ALIGNING:
        cur_state = process_aligning();
        break;
      case MOVING:
        cur_state = process_moving();
        break;
      case ZIPLINING:
        cur_state = process_ziplining();
        break;
      case DONE:
        cur_state = process_done();
        break;
    }
  }

  public zip_state process_idle() {
    /*
     * We don't need to suspend the odometer since we will localize afterwards.
     */

    // Start the zipline motor
    zip_motor.setSpeed(ZipLineLab.ZIPLINE_TRAVERSAL_SPEED);
    zip_motor.backward();

    // double theta = odo.getTheta(); // get orientation in radians.
    double err_theta = Util.angleToPos(odo, ZipLineLab.ZIPLINE_END_POS);
    if (Math.abs(err_theta) > ZipLineLab.ZIPLINE_ORIENTATION_THRESHOLD) {
      return zip_state.ALIGNING;
    } else {
      return zip_state.MOVING;
    }
  }

  public zip_state process_aligning() {
    double err_theta = Util.angleToPos(odo, ZipLineLab.ZIPLINE_END_POS);
    if (Math.abs(err_theta) > ZipLineLab.ZIPLINE_ORIENTATION_THRESHOLD) {
      dr.rotate(err_theta, false, false);
      return zip_state.ALIGNING;
    } else {
      return zip_state.MOVING;
    }
  }

  public zip_state process_moving() {
    double err_theta = Util.angleToPos(odo, ZipLineLab.ZIPLINE_END_POS);

    if (getLightLevel() < ZipLineLab.FLOOR_LIGHT_READING
        && Math.abs(err_theta) > ZipLineLab.ZIPLINE_ORIENTATION_THRESHOLD) {
      dr.moveForward(ZipLineLab.SQUARE_LENGTH, false); // move away from the zipline
      if (getLightLevel() < ZipLineLab.FLOOR_LIGHT_READING) {
        dr.infiniteMoveForward();
        return zip_state.ZIPLINING;
      } else {
        return zip_state.MOVING;
      }
    }

    if (Math.abs(err_theta) > ZipLineLab.ZIPLINE_ORIENTATION_THRESHOLD) {
      return zip_state.ALIGNING;
      // return zip_state.ALIGNING;
    } else {
      dr.moveForward(ZipLineLab.SQUARE_LENGTH / 2, true); // move away from the zipline
      return zip_state.MOVING; // ensures that robot is in there at the time we move on
    }
  }


  public zip_state process_ziplining() {
    if (getLightLevel() > ZipLineLab.FLOOR_LIGHT_READING) {
      if (filter_count < FILTER_MAX) {
        filter_count++;
        return zip_state.ZIPLINING;
      } else {
        // we've arrived at the end of the zipline, and the wheels should be touching the ground
        // zip_motor.forward(); // move off the final bit of zipline
        dr.moveForward(ZipLineLab.SQUARE_LENGTH * 2, false); // move away from the zipline
        zip_motor.stop(); // stop the zipline motor
        dr.stop(); // stop the main motors
        filter_count = 0;
        return zip_state.DONE;
      }
    } else {
      return zip_state.ZIPLINING;
    }
  }

  public zip_state process_done() {
    done = true;
    return zip_state.IDLE;
  }

  public synchronized zip_state getCurrentState() {
    return cur_state;
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
}

package ca.mcgill.ecse211.zipline;

import lejos.hardware.Sound;

public class OdometryCorrection extends Thread {
  private static final long CORRECTION_PERIOD = 30;
  private Odometer odometer;
  private float light_level;
  long line_detected_time;
  private int line_count;

  private final boolean debug_mode = false;

  public enum dir {
    ZERO, NINETY, ONEEIGHTY, TWOSEVENTY
  };

  private dir cur_dir;

  private double[] prev_pos = new double[] {-1, -1, -1};

  private boolean correct = false;

  // constructor
  public OdometryCorrection(Odometer odometer/* , ColorPoller col */) {
    this.odometer = odometer;
    // this.colPoller = col;
  }

  // run method (required for Thread)
  public void run() {
    long correctionStart, correctionEnd;
    while (true) {

      correctionStart = System.currentTimeMillis();
      waitForLine();

      if (System.currentTimeMillis() - line_detected_time > 4000) {
        Sound.setVolume(70);
        if (correct) {
          Sound.beepSequenceUp();
        }
        double delta_x = 0, delta_y = 0;
        double cur_theta = odometer.getTheta();
        cur_dir = getDir(Math.toDegrees(cur_theta));
        switch (cur_dir) {
          case ZERO:
            delta_x = ZipLineLab.SQUARE_LENGTH;
            double angle_zero = computeAngle(90 - Math.toDegrees(cur_theta));
            delta_y = -1 * (delta_y * Math.sin(cur_theta)) / Math.sin(angle_zero);
            break;
          case NINETY:
            delta_y = ZipLineLab.SQUARE_LENGTH;
            double angle_ninety = computeAngle(90 - (Math.toDegrees(cur_theta) - 90));
            delta_x = -1 * (delta_x * Math.sin(cur_theta - Math.PI / 2)) / Math.sin(angle_ninety);
            break;
          case ONEEIGHTY:
            delta_x = -ZipLineLab.SQUARE_LENGTH;
            double angle_oneeighty = computeAngle(90 - (Math.toDegrees(cur_theta) - 180));
            delta_y =
                -1 * (delta_y * Math.sin(cur_theta - 2 * Math.PI)) / Math.sin(angle_oneeighty);
            break;
          case TWOSEVENTY:
            delta_y = -ZipLineLab.SQUARE_LENGTH;
            double angle_twoseventy = computeAngle(90 - (Math.toDegrees(cur_theta) - 270));
            delta_x = -1 * (delta_x * Math.sin(cur_theta - (3 * Math.PI) / 2))
                / Math.sin(angle_twoseventy);
            break;
        }

        if (correct && prev_pos[0] != -1) {
          setNewPos(prev_pos[0] + delta_x, prev_pos[1] + delta_y);
          updatePrevPos();
        } else if (correct && prev_pos[0] == -1) {
          updatePrevPos();
        } else if (!correct) {
          prev_pos[0] = -1;
        }

        // this ensure the odometry correction occurs only once every period
        correctionEnd = System.currentTimeMillis();
        if (correctionEnd - correctionStart < CORRECTION_PERIOD) {
          try {
            Thread.sleep(CORRECTION_PERIOD - (correctionEnd - correctionStart));
          } catch (InterruptedException e) {
            // there is nothing to be done here because it is not
            // expected that the odometry correction will be
            // interrupted by another thread
          }
        }
      } else if (System.currentTimeMillis() - line_detected_time > 9000) {
        // it's been too long, we missed lines.
        prev_pos[0] = -1;
      }
      line_detected_time = System.currentTimeMillis(); // Capture the last time we crossed a line.
    }
  }



  private void waitForLine() {
    while (getLightLevel() > ZipLineLab.LIGHT_THRESHOLD && getLightLevel() > 0.1f);
    return;
  }

  public synchronized void setLightLevel(float light_level) {
    this.light_level = light_level;
  }

  public synchronized float getLightLevel() {
    return light_level;
  }

  public int getLineCount() {
    return line_count;
  }

  private void updatePrevPos() {
    prev_pos[0] = odometer.getX();
    prev_pos[1] = odometer.getY();
    prev_pos[2] = odometer.getTheta();

    if (debug_mode) {
      System.out.println("Set Previous X: " + prev_pos[0]);
      System.out.println("Set Previous Y: " + prev_pos[1]);
      System.out.println("Set Previous Theta: " + Math.toDegrees(prev_pos[2]));
    }
  }

  private synchronized void setNewPos(double x, double y/* , double theta */) {
    odometer.setX(x);
    odometer.setY(y);
    // odometer.setTheta(theta);
  }

  private double computeAngle(double t_rad) {
    double t_deg = Math.toDegrees(t_rad);
    if (t_deg > 359.99999999 && t_deg >= 0) {
      t_deg = t_deg - 360;
    } else if (t_deg < 0) {
      t_deg = 360 + t_deg;
    }

    return Math.toRadians(t_deg);
  }

  public synchronized void setCorrecting(boolean arg) {
    correct = arg;
  }

  // Approximate the direction of the robot
  private dir getDir(double t_deg) {
    double error = 8.0;
    if (t_deg + error >= 0 && t_deg - error <= 0) {
      return dir.ZERO;
    } else if (t_deg + error >= 360 && t_deg - error <= 360) {
      return dir.ZERO;
    } else if (t_deg + error >= 90 && t_deg - error <= 90) {
      return dir.NINETY;
    } else if (t_deg + error >= 180 && t_deg - error <= 180) {
      return dir.ONEEIGHTY;
    } else if (t_deg + error >= 270 && t_deg - error <= 270) {
      return dir.TWOSEVENTY;
    }
    // That should not happen
    return dir.ZERO;
  }
}

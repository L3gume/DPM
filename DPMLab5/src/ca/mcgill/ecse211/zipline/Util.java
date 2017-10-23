package ca.mcgill.ecse211.zipline;

/**
 * Helper class holding various utility functions
 * 
 * @author Justin Tremblay
 *
 */
public class Util {
  
  public static enum dir {
    ZERO, NINETY, ONEEIGHTY, TWOSEVENTY
  };
  
  public static double computeAngle(double t_rad) {
    double t_deg = Math.toDegrees(t_rad);
    if (t_deg > 359.99999999 && t_deg >= 0) {
      t_deg = t_deg - 360;
    } else if (t_deg < 0) {
      t_deg = 360 + t_deg;
    }

    return Math.toRadians(t_deg);
  }

  // Approximate the direction of the robot
  public static dir getDir(double t_deg) {
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
    return t_deg - error <= 180 ? dir.ZERO : dir.ONEEIGHTY;
  }
  
  public static void rotateArray(double[] angles2, int order) {
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
  
  public static int findRefAngle(Waypoint ref_pos) {
    // TODO: these positions are only for lab 5, they will have to be changed (or removed) for the
    // final project since it's hard-coded crap.
    int ref_angle = 45;
    if (ref_pos.x == 1 && ref_pos.y == 1) {
      ref_angle = 45;
    } else if (ref_pos.x == 7 && ref_pos.y == 1) {
      ref_angle = 135;
    } else if (ref_pos.x == 7 && ref_pos.y == 7) {
      ref_angle = 225;
    } else if (ref_pos.x == 1 && ref_pos.y == 7) {
      ref_angle = 315;
    }
    return ref_angle;
  }
  
  public static double angleToPos(Odometer odo, Waypoint _pos) {
    double orientation_angle = odo.getTheta();
    double orientation_vect[] = new double[] {Math.cos(orientation_angle), Math.sin(orientation_angle)};
    double vect_to_pos[] = new double [] {_pos.x * ZipLineLab.SQUARE_LENGTH - odo.getX(), _pos.y * ZipLineLab.SQUARE_LENGTH - odo.getY()};
    double angle = Math.atan2(vect_to_pos[1] * orientation_vect[0] - vect_to_pos[0] * orientation_vect[1],
        orientation_vect[0] * vect_to_pos[0] + orientation_vect[1] * vect_to_pos[1]);

    return angle;
  }
}

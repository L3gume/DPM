package ca.mcgill.ecse211.odometerlab;

public class Navigator extends Thread {

  // Public volatile variable that will be changed by the ultrasonic poller to tell the navigator there is an obstacle.
  public static volatile boolean obstacle_detected = false;
  
  static final Waypoint path1[] = {new Waypoint(0, 2), new Waypoint(1, 1), new Waypoint(2, 2),
      new Waypoint(2, 1), new Waypoint(1, 0)};
  static final Waypoint path2[] = {new Waypoint(1, 1), new Waypoint(0, 2), new Waypoint(2, 2),
      new Waypoint(2, 1), new Waypoint(1, 0)};
  static final Waypoint path3[] = {new Waypoint(1, 0), new Waypoint(2, 1), new Waypoint(2, 2),
      new Waypoint(0, 2), new Waypoint(1, 1)};
  static final Waypoint path4[] = {new Waypoint(0, 1), new Waypoint(1, 2), new Waypoint(1, 0),
      new Waypoint(2, 1), new Waypoint(2, 2)};

  public static final double SQUARE_LENGTH = 30.48;

  public enum state {
    IDLE, COMPUTING, ROTATING, MOVING_STRAIGHT, MOVING_AVOIDING, REACHED_POINT
  }

  private state cur_state = state.IDLE;

  Odometer odo;
  OdometryCorrection corr;
  UltrasonicPoller uPoll;
  Driver driver;

  /*
   * Navigation constants
   */

  private final double ANGLE_THRESHOLD = Math.toRadians(5);
  private final double DISTANCE_THRESHOLD = 3;

  /*
   * Navigation variables
   */
  Waypoint[] path;
  Waypoint current_pos;
  Waypoint target_pos = null;
  int waypoint_progress = -1; // A counter to keep track of our progress (indexing the path array)
  double angle_to_target_pos;
  double dist_to_target_pos;
  double orientation_vect[] = {0.0, 1.0};
  double orientation_angle = 0.0;
  double min_dist = Double.MAX_VALUE;
  boolean done = false;

  public Navigator(Driver driver, Odometer odo, UltrasonicPoller uPoll) {
    this.driver = driver;
    current_pos = new Waypoint(0, 0);
    path = new Waypoint[5];
    this.odo = odo;
    this.uPoll = uPoll;
    
    
  }


  public void run() {
    while (true) {

      // To other stuff here
      updateOrientation();
      
      if (obstacle_detected) {
        // The ultrasonic poller has detected an obstacle.
        // Immediately abort current action and avoid the obstacle.
        cur_state = state.MOVING_AVOIDING;
      }

      switch (cur_state) {
        case IDLE:
          cur_state = process_idle();
          break;
        case COMPUTING:
          cur_state = process_computing();
          break;
        case ROTATING:
          cur_state = process_rotating();
          break;
        case MOVING_STRAIGHT:
          cur_state = process_movingstraight();
          break;
        case MOVING_AVOIDING:
          cur_state = process_movingavoiding();
          break;
        case REACHED_POINT:
          cur_state = process_reachedpoint();
          break;
        // There should never be a default case
        default:
          break;
      }

      System.out.println("Status: " + cur_state);

      if (done) {
        break; // break out of the loop and end the thread if we are done.
      }

      try {
        Thread.sleep(60);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  /*
   * Process methods,
   */

  // TODO: add thresholds to the checks since we won't ever be exactly at 0.0
  private state process_idle() {
    // Being idle means we just started, intialize stuff and get started.
    target_pos = getNextWaypoint(); // Get the next waypoint in the array, the first one in this
                                    // case.
    if (target_pos != null) {
      return state.COMPUTING;
    }
    return state.IDLE;
  }

  private state process_computing() {
    // Compute the distance and angle to the target position, if rotation is needed, set state to
    // rotating, if not: move.
    updateTargetInfo();
    System.out.println("Target position: " + target_pos.x + " " + target_pos.y);

    if (Math.abs(angle_to_target_pos) > 0) {
      return state.ROTATING;
    } else if (dist_to_target_pos > 0) {
      return state.MOVING_STRAIGHT;
    }

    return state.IDLE;
  }

  private state process_rotating() {
    updateTargetInfo();
    if (Math.abs(angle_to_target_pos) > ANGLE_THRESHOLD) {
      driver.rotate(angle_to_target_pos);
      return state.ROTATING;
    } else {
      // driver.stop();
      if (dist_to_target_pos > DISTANCE_THRESHOLD) {
        min_dist = Double.MAX_VALUE; // reset
        return state.MOVING_STRAIGHT;
      } else {
        return state.REACHED_POINT; // if angle AND distance are both small enough, we reached the
                                    // point.
      }
    }
  }

  private state process_movingstraight() {
    updateTargetInfo();
    if (Math.abs(angle_to_target_pos) > ANGLE_THRESHOLD) {
      return state.ROTATING; // We are a bit off, adjust.
    } else if (dist_to_target_pos < min_dist) {
      min_dist = dist_to_target_pos;
      if (dist_to_target_pos > DISTANCE_THRESHOLD) {
        driver.gotoPos(dist_to_target_pos);
        return state.MOVING_STRAIGHT;
      } else {
        // Find some way of confirming that we are at the right position
        return state.REACHED_POINT;
      }
      // }
    } else {
      // We missed the point, turn around and get there!
      return state.ROTATING;
    }
  }

  private state process_movingavoiding() {
    // NOT IMPLEMENTED YET
    // Synchronized access to the distance since it is volatile.
        
    float dist = uPoll.getDistance();
    if (dist > 20.f) {
      driver.avoidObstacle(dist);
    }
    
    return obstacle_detected ? state.MOVING_AVOIDING : state.ROTATING;
  }

  private state process_reachedpoint() {
    updateTargetInfo();
    if (dist_to_target_pos < DISTANCE_THRESHOLD) {
      min_dist = Double.MAX_VALUE; // reset
      target_pos = getNextWaypoint();
      return state.COMPUTING;
    } else {
      return state.MOVING_STRAIGHT; // Will have to improve this.
    }
  }

  /*
   * Math
   */

  private double angleToPos(double vect_to_pos[]) {
    double angle =
        Math.atan2(vect_to_pos[1] * orientation_vect[0] - vect_to_pos[0] * orientation_vect[1],
            orientation_vect[0] * vect_to_pos[0] + orientation_vect[1] * vect_to_pos[1]);
    // - Math.atan2(orientation_vect[1], orientation_vect[0]);
    /*if (Math.toDegrees(angle) > 180) {
      // convert to smaller value if the angle is over 180 degrees
      angle = Math.toRadians((Math.toDegrees(angle_to_target_pos) - 360));
    } else if (Math.toDegrees(angle) < -180) {
      angle = Math.toRadians((Math.toDegrees(angle_to_target_pos) + 360));
    }*/

    return angle;
  }

  /*
   * private double dotProd(double u[], double v[]) { return u[0] * v[0] + u[1] * v[1]; }
   */

  private double magnitude(double v[]) {
    return Math.sqrt(Math.pow(v[0], 2) + Math.pow(v[1], 2));
  }

  private void updateTargetInfo() {
    double x = odo.getX();
    double y = odo.getY();

    System.out.println("Current Position: " + x + ", " + y);
    double dist_x = target_pos.x * SQUARE_LENGTH - x;
    double dist_y = target_pos.y * SQUARE_LENGTH - y;

    double vect_to_target[] = {dist_x, dist_y};
    dist_to_target_pos = magnitude(vect_to_target);
    angle_to_target_pos = angleToPos(vect_to_target);

    System.out.println("Target Position: (" + target_pos.x + "; " + target_pos.y + ")");
    System.out.println("Distance to target: " + dist_to_target_pos);
    System.out.println("Vector to target: [" + vect_to_target[0] + ", " + vect_to_target[1] + "]");
    System.out.println("Angle to target: " + Math.toDegrees(angle_to_target_pos));
  }

  private void updateOrientation() {
    orientation_angle = odo.getTheta();
    orientation_vect[0] = Math.cos(orientation_angle);
    orientation_vect[1] = Math.sin(orientation_angle);

    System.out.println("Orientation angle: " + Math.toDegrees(orientation_angle));
    System.out
        .println("Orientation vector: [" + orientation_vect[0] + ", " + orientation_vect[1] + "]");
  }

  /*
   * Utility methods
   */

  // Taken from previous lab
  private static int convertDistance(double radius, double distance) {
    return (int) ((180.0 * distance) / (Math.PI * radius));
  }

  // Taken from previous lab
  @SuppressWarnings("unused")
  private static int convertAngle(double radius, double width, double angle) {
    return convertDistance(radius, Math.PI * width * angle / 360.0);
  }

  private Waypoint getNextWaypoint() {
    if (waypoint_progress + 1 >= path.length) {
      // That's a problem
      System.out.println("Error: getting out of bounds of the path array");
      done = true;
      return new Waypoint(0, 0);
    }
    return path[++waypoint_progress];
  }

  public Waypoint getTargetPos() {
    return target_pos;
  }

  public Waypoint getCurrentPos() {
    return current_pos;
  }

  public void setCurrentPos(Waypoint p) {
    this.current_pos = p;
  }

  public state getCurrentState() {
    return cur_state;
  }

  public void setPath(int i) {
    switch (i) {
      case 1:
        this.path = path1;
        break;
      case 2:
        this.path = path2;
        break;
      case 3:
        this.path = path3;
        break;
      case 4:
        this.path = path4;
        break;
      default:
        this.path = path1;
        break;
    }
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
}

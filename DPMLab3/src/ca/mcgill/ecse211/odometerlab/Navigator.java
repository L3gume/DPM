package ca.mcgill.ecse211.odometerlab;

public class Navigator extends Thread {

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
  
  private final double ANGLE_THRESHOLD = 5;
  private final double DISTANCE_THRESHOLD = 3;
  
  /*
   * Navigation variables
   */
  Waypoint[] path;
  Waypoint current_pos;
  Waypoint target_pos;
  int waypoint_progress = -1; // A counter to keep track of our progress (indexing the path array)
  double angle_to_target_pos;
  double dist_to_target_pos;
  double orientation_vect[] = {0.0, 1.0};
  double orientation_angle = 0.0;

  public Navigator(Driver driver) {
    this.driver = driver;
    current_pos = new Waypoint(0, 0);
  }


  public void run() {
    while (true) {


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
          break;
        case MOVING_AVOIDING:
          break;
        case REACHED_POINT:
          cur_state = process_computing();
          break;
        // There should never be a default case
        default:
          break;
      }
    }
  }

  /*
   * Process methods,
   */

  //TODO: add thresholds to the checks since we won't ever be exactly at 0.0
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
    
    //int delta_x = target_pos.x - current_pos.x;
    //int delta_y = target_pos.y - current_pos.y;

    double dist_x = target_pos.x * SQUARE_LENGTH - odo.getX();
    double dist_y = target_pos.y * SQUARE_LENGTH - odo.getY();
    
    double vect_to_target[] = {dist_x, dist_y};
    dist_to_target_pos = magnitude(vect_to_target);
    angle_to_target_pos = angleToPos(vect_to_target);
    if (Math.abs(angle_to_target_pos) > 0) {
      return state.ROTATING;
    } else if (dist_to_target_pos > 0) {
      return state.MOVING_STRAIGHT;
    }

    return state.IDLE;
  }

  private state process_rotating() {
    if (Math.abs(angle_to_target_pos) > ANGLE_THRESHOLD) {
      driver.rotate(angle_to_target_pos);
      return state.ROTATING;
    } else {
      if (dist_to_target_pos > DISTANCE_THRESHOLD) {
        return state.MOVING_STRAIGHT;
      }
    }
    return state.IDLE;
  }

  private state process_movingstraight() {
    return state.IDLE;
  }

  private state process_movingavoiding() {
    // NOT IMPLEMENTED YET
    return state.IDLE;
  }

  private state process_reachedpoint() {
    target_pos = getNextWaypoint();
    return state.COMPUTING;
  }

  /*
   * Math
   */

  private double angleToPos(double vect_to_pos[]) {
    // return Math.acos(dotProd(orientation_vect, vect_to_pos) / (magnitude(orientation_vect) *
    // magnitude(vect_to_pos)));
    double angle = Math.atan2(vect_to_pos[1], vect_to_pos[0])
        - Math.atan2(orientation_vect[1], orientation_vect[0]);
    if (Math.toDegrees(angle) > 180) {
      // convert to smaller value if the angle is over 180 degrees
      angle = -1 * Math.toRadians((360 - Math.toDegrees(angle_to_target_pos)));
    }
    return angle;
  }

  /*private double dotProd(double u[], double v[]) {
    return u[0] * v[0] + u[1] * v[1];
  }*/

  private double magnitude(double v[]) {
    return Math.sqrt(Math.pow(v[0], 2) + Math.pow(v[1], 2));
  }

  /*
   * Utility methods
   */

  // Taken from previous lab
  private static int convertDistance(double radius, double distance) {
    return (int) ((180.0 * distance) / (Math.PI * radius));
  }

  // Taken from previous lab
  private static int convertAngle(double radius, double width, double angle) {
    return convertDistance(radius, Math.PI * width * angle / 360.0);
  }

  private Waypoint getNextWaypoint() {
    if (waypoint_progress >= path.length || waypoint_progress < 0) {
      // That's a problem
      System.out.println("Error: getting out of bounds of the path array");
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
  
  public void setPath(Waypoint path[]) {
    this.path = path;
  }
}

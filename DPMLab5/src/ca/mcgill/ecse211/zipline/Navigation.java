package ca.mcgill.ecse211.zipline;

/*
 * READ THE README.md FILE!!!!! It contains info about the implementation of the Navigator
 */

/**
 * Class that handles navigating the robot.
 * 
 * @author Justin Tremblay
 *
 */
public class Navigation extends Thread {

  // The list of possible states for the navigator.
  public enum nav_state {
    IDLE, COMPUTING, ROTATING, MOVING, AVOIDING, REACHED_POINT, DONE
  }

  private nav_state cur_state = nav_state.IDLE; // The current state of the navigator, starts at IDLE.

  /*
   * References to other classes
   */
  private Odometer odo;
  private UltrasonicPoller uPoll;
  private Driver driver;

  /*
   * Navigation constants
   */

  private final double ANGLE_THRESHOLD = Math.toRadians(2); // If the angle to target position is
                                                            // lower than 2 degrees, then that's
                                                            // good enough.
  private final double DISTANCE_THRESHOLD = 2; // If the distance is below 2 cm, then that's good
                                               // enough.

  /*
   * Navigation variables
   */

  private boolean navigating; // This is here only because the lab outline asks for it. It's
                              // completely useless.

  Waypoint[] path; // The set of waypoints the robot will have to travel, initialized by the
                   // setPath() method.
  Waypoint target_pos = null; // Target waypoint
  int waypoint_progress = -1; // A counter to keep track of our progress (indexing the path array)
  double angle_to_target_pos; // Angle between the robot's direction and the target waypoint.
  double dist_to_target_pos; // Distance to target waypoint.
  double orientation_vect[] = {0.0, 1.0}; // we initially start with a theta of 90 degrees.
  double orientation_angle = 90.0; // we initially start with a theta of 90 degrees.
  double min_dist; // Used while moving, we constantly record the new lowest distance to the target
                   // point, when it starts going back up, we know we went past the waypoint.
  boolean done = false; // This will be set to true when we reach the last waypoint, making to
                        // program end the navigation thread.

  /*
   * Obstacle avoidance variables
   */
  private boolean obstacle_detected = false;
  private boolean obstacle_avoided = true;

  public Navigation(Driver driver, Odometer odo, UltrasonicPoller uPoll) {
    this.driver = driver;
    this.odo = odo;
    this.uPoll = uPoll;

//    uPoll.setNav(this); Not needed for this lab.

    min_dist = Double.MAX_VALUE;
  }

  /**
   * Run method. This is where the magic happens.
   * 
   * This is where we run our navigation algorithm, which is really just a simple state machine. We
   * also continuously look for obstacles and overwrite the curent state with the AVOIDING state if
   * needed.
   */
  public void run() {
    while (true) {
      if (navigating) {
        // To other stuff here
        updateOrientation();

        /*
         * The base of the state machine.
         * 
         * This is where, depending on the current state, we choose a process_x method to continue
         * our navigation. Each method has its specific conditions and outputs
         */
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
          case MOVING:
            cur_state = process_moving();
            break;
          case AVOIDING:
            cur_state = process_avoiding();
            break;
          case REACHED_POINT:
            cur_state = process_reachedpoint();
            break;
          // There should never be a default case
          default:
            break;
        }

        /**
         * Obstacle detectection
         * 
         * The obstacle_detected variable is modified by the ultrasonic poller when it detects low
         * distances. The Navigator then decides whether or not it is going to avoid the obstacle.
         */
        if (getObstacleDetected() && cur_state != nav_state.AVOIDING
            && Math.abs(angle_to_target_pos) < Math.toRadians(15)) {
          // The ultrasonic poller has detected an obstacle and it is in my way (angle to pos lower
          // than 15 degrees)
          // Immediately abort current action and avoid the obstacle by setting the state to
          // AVOIDING.
          cur_state = nav_state.AVOIDING;
        } else if (getObstacleDetected() && cur_state != nav_state.AVOIDING
            && Math.abs(angle_to_target_pos) > Math.toRadians(15)) {
          // An obstacle has been detected but it isn't in my way, ignore it and set obstacle
          // detected
          // back to false;
          setObstacleDetected(false);
        }
        if (ZipLineLab.debug_mode) {

          System.out.println("[NAVIGATION] Status: " + cur_state);
        }

        try {
          Thread.sleep(30);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Process methods
   * 
   * These methods are executed depending on the current state of the navigator. They process the
   * current state and return the new state, or the same if it's not done.
   */
  private nav_state process_idle() {
    // Being idle means we just started, intialize stuff and get started.
    target_pos = getNextWaypoint(); // Get the next waypoint in the array, the first one in this
                                    // case.
    if (target_pos != null) {
      if (ZipLineLab.debug_mode) {
        System.out.println("[NAVIGATION] Target Acquired: [" + target_pos.x + "; " + target_pos.y + "]");
      }
      return nav_state.COMPUTING;
    }
    // Fallthrough, go back to IDLE if we don't have a target position
    return nav_state.IDLE;
  }

  private nav_state process_computing() {
    // Compute the distance and angle to the target position, if rotation is needed, set state to
    // rotating, if not: move.
    updateTargetInfo();
    if (ZipLineLab.debug_mode) {
      System.out.println("[NAVIGATION] Target position: " + target_pos.x + " " + target_pos.y);
    }

    if (Math.abs(angle_to_target_pos) > 0) {
      return nav_state.ROTATING;
    } else if (dist_to_target_pos > 0) {
      return nav_state.MOVING;
    }

    // Fallthrough, shouldn't happen.
    return nav_state.IDLE;
  }

  private nav_state process_rotating() {
    updateTargetInfo();
    if (Math.abs(angle_to_target_pos) > ANGLE_THRESHOLD) {
      // As long as the angle to the target position is bigger than the threshold, keep rotating.
      driver.rotate(angle_to_target_pos, false, true);
      return nav_state.ROTATING;
    } else {
      // If our angle is smaller than the threshold, then we can move, start moving!
      if (dist_to_target_pos > DISTANCE_THRESHOLD) {
        min_dist = Double.MAX_VALUE; // reset
        return nav_state.MOVING;
      } else {
        // if angle AND distance are both small enough, we reached the point (happens rarely from
        // the ROTATING state).
        return nav_state.REACHED_POINT;
      }
    }
  }

  private nav_state process_moving() {
    updateTargetInfo();
    if (Math.abs(angle_to_target_pos) > ANGLE_THRESHOLD) {
      return nav_state.ROTATING; // We are a bit off, adjust.
    } else if (dist_to_target_pos < min_dist) {
      min_dist = dist_to_target_pos; // min_dist is continuously updated as long as the distance
                                     // gets smaller.
      if (dist_to_target_pos > DISTANCE_THRESHOLD) {
        driver.moveTo(dist_to_target_pos, true);
        return nav_state.MOVING;
      } else {
        // if angle AND distance are both small enough, we reached the point
        return nav_state.REACHED_POINT;
      }
    } else {
      // We missed the point (dist_to_target_pos > min_dist), turn around and get there!
      return nav_state.ROTATING;
    }
  }

  private nav_state process_avoiding() {
    updateTargetInfo();
    float dist = uPoll.getDistance(); // Get the distance to the obstacle so we can make a decision
    if (ZipLineLab.debug_mode) {
      System.out.println("[NAVIGATION][AVOIDING] Obstacle distance: " + dist);
    }
    // No mather what, call the avoidObstacle method, which uses the PController.
    driver.avoidObstacle(dist);

    // Determine if we did avoid the obstacle.
    obstacle_avoided =
        (Math.abs(angle_to_target_pos) < ANGLE_THRESHOLD && dist > 150) ? true : false;
    if (obstacle_avoided) {
      setObstacleDetected(false);
    }
    // Return the right state depending on whether or not we did avoid the obstacle.
    return getObstacleDetected() && !obstacle_avoided ? nav_state.AVOIDING : nav_state.ROTATING;
  }

  // Pretty self-explanatory
  private nav_state process_reachedpoint() {
    updateTargetInfo();
    if (dist_to_target_pos < DISTANCE_THRESHOLD) {
      min_dist = Double.MAX_VALUE; // reset
      driver.rotate(0, true, true);
      target_pos = getNextWaypoint();
      
      if (target_pos != null) {
        return nav_state.COMPUTING;
      } else {
        navigating = false;
        done = true;
        return nav_state.IDLE;
      }   
    } else {
      return nav_state.MOVING; // Will have to improve this.
    }
  }

  /*
   * Math
   */

  /**
   * Computes the angle between the robot and it's target position, from -180 to 180, so that the
   * robot always makes the smallest turn possible.
   * 
   * @param vect_to_pos, a vector from the robot's position to the target position.
   * @return the angle between the two vectors, in radians.
   */
  private double angleToPos(double vect_to_pos[]) {
    double angle =
        Math.atan2(vect_to_pos[1] * orientation_vect[0] - vect_to_pos[0] * orientation_vect[1],
            orientation_vect[0] * vect_to_pos[0] + orientation_vect[1] * vect_to_pos[1]);

    return angle;
  }

  /**
   * Computes the magnitude of a vector
   * 
   * @param v: vector
   * @return the magnitude of the vector.
   */
  private double magnitude(double v[]) {
    return Math.sqrt(Math.pow(v[0], 2) + Math.pow(v[1], 2));
  }

  /**
   * Update all the information regarding out target position: - Vector from robot's current
   * position to target (using odometer and known measurements). - The magnitude of that vector, the
   * distance to our target. - The angle to the traget position, using our orientation unit vector
   * and the vector we just computed.
   */
  private void updateTargetInfo() {
    double x = odo.getX();
    double y = odo.getY();

    double dist_x = target_pos.x * ZipLineLab.SQUARE_LENGTH - x;
    double dist_y = target_pos.y * ZipLineLab.SQUARE_LENGTH - y;

    double vect_to_target[] = {dist_x, dist_y};
    dist_to_target_pos = magnitude(vect_to_target);
    angle_to_target_pos = angleToPos(vect_to_target);

    if (ZipLineLab.debug_mode) {
      System.out.println("[NAVIGATION] Current Position: " + x + ", " + y);
      System.out.println("[NAVIGATION] Target Position: (" + target_pos.x + "; " + target_pos.y + ")");
      System.out.println("[NAVIGATION] Distance to target: " + dist_to_target_pos);
      System.out
          .println("[NAVIGATION] Vector to target: [" + vect_to_target[0] + ", " + vect_to_target[1] + "]");
      System.out.println("[NAVIGATION] Angle to target: " + Math.toDegrees(angle_to_target_pos));
    }
  }

  /**
   * Computes a unit vector that points in the robot's orientation, used for determining angles
   * between the robot and the target position at any given time.
   */
  private void updateOrientation() {
    orientation_angle = odo.getTheta();
    orientation_vect[0] = Math.cos(orientation_angle);
    orientation_vect[1] = Math.sin(orientation_angle);

    if (ZipLineLab.debug_mode) {
      System.out.println("[NAVIGATION] Orientation angle: " + Math.toDegrees(orientation_angle));
      System.out.println(
          "[NAVIGATION] Orientation vector: [" + orientation_vect[0] + ", " + orientation_vect[1] + "]");
    }
  }

  /*
   * Utility methods
   */

  /**
   * Gets the next waypoint in the path array.
   * 
   * @return the next waypoint in the path array.
   */
  private Waypoint getNextWaypoint() {
    if (path != null) {
      if (waypoint_progress + 1 >= path.length) {
        // That's a problem
        if (ZipLineLab.debug_mode) {
          System.out.println("Error: getting out of bounds of the path array");
        }
        // wait for new path.
        return null;
      }
    }
    return path[++waypoint_progress];
  }

  /**
   * Gets the target position, as a waypoint.
   * 
   * @return current target waypoint
   */
  public Waypoint getTargetPos() {
    return target_pos;
  }

  /**
   * Get the current state of the navigator.
   * 
   * @return current state.
   */
  public synchronized nav_state getCurrentState() {
    return cur_state;
  }

  /*
   * These two methods are meant to guarantee locked access to the obstacle_detected variable for
   * both the navigator and the ultrasonic poller
   */

  /**
   * Returns whether or not an obstacle has been detected,
   * 
   * @return boolean obstacle_detected
   */
  public synchronized boolean getObstacleDetected() {
    return obstacle_detected;
  }

  /**
   * Sets the value of the obstacle_detected variable.
   * 
   * @param arg
   */
  public synchronized void setObstacleDetected(boolean arg) {
    obstacle_detected = arg;
  }

  // In there only because the lab asks for it.
  public synchronized boolean isNavigating() {
    return navigating;
  }
  
  public synchronized void setNavigating(boolean arg) {
    this.navigating = arg;
  }

  public void setPath(Waypoint[] waypoints) {
    path = waypoints;
  }
}

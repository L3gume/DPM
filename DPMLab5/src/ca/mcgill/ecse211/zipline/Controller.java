package ca.mcgill.ecse211.zipline;

import ca.mcgill.ecse211.zipline.ColorPoller.l_mode;

/**
 * This is a state machine defining the robot's behaviour in . 
 * 
 * @author Justin Tremblay
 *
 */
public class Controller extends Thread {

  // external classes
  private Odometer odo;
  private Navigation nav;
  private Localizer loc;
  private ZiplineController zip;
  private ColorPoller cp;

  // list of states
  // the SEARCHING state won't be implemented in Lab 5
  public enum state {
    IDLE, LOCALIZING, NAVIGATING, SEARCHING, ZIPLINE
  };
  private state cur_state = state.IDLE;
  
  // booleans to update as you progress
  private boolean traversed_zipline = false;
  private boolean reached_first_corner = false;
  private boolean skipped_loc = false;
  private boolean reached_zipline = false;
  private boolean at_zipline = false;
  private boolean lab5_stop = false;

  /**
   * Constructor
   * 
   * @param odo Odometer
   * @param drv Driver
   * @param nav Navigator
   * @param loc Localizer
   * @param zip ZiplineController
   */
  public Controller(Odometer odo, ColorPoller cp, Navigation nav, Localizer loc,
      ZiplineController zip) {
    this.odo = odo;
    this.cp = cp;
    this.nav = nav;
    this.loc = loc;
    this.zip = zip;
        
    init();
  }

  /**
   * Starts all the threads.
   */
  private void init() {
    odo.start();
    // nav.start();
    loc.start();
  }

  /**
   * Runs each time the thread starts.
   */
  public void run() {
    if (ZipLineLab.debug_zipling) {
      cur_state = state.ZIPLINE;
    }
    while (true && !lab5_stop) {
      switch (cur_state) {
        case IDLE:
          cur_state = process_idle();
          break;
        case LOCALIZING:
          cur_state = process_localizing();
          break;
        case NAVIGATING:
          cur_state = process_navigating();
          break;
        case SEARCHING:
          cur_state = process_searching();
          break;
        case ZIPLINE:
          cur_state = process_zipline();
          break;
        default:
          break;
      }

      try {
        Thread.sleep(40);
      } catch (Exception e) {
        // ...
      }
    }
  }

  /*
   * Processing for idle state.
   */
  public state process_idle() {
    // For this lab, we don't need to check the goal we are given, just move to localization
    return state.LOCALIZING;
  }
  
  /**
   * Processing for localizing state. 
   * 
   * @return state - the state to go to next
   */
  public state process_localizing() {
    loc.startLocalization(
        traversed_zipline || reached_zipline || reached_first_corner ? true : false);
    
    while (!loc.done); // Wait for localization to complete
    // We don't really have a way of knowing if the localization was a success or not, let's just
    // assume it worked like in Lab 4.

    // reset the traversed_zipline boolean in case we ever need to traverse again.
    if (traversed_zipline) {
      traversed_zipline = false;
    }
    
    // navigate to the next required point
    if (!reached_first_corner && !reached_zipline) {
      if (ZipLineLab.START_POS.x != 1) {
        nav.setPath(new Waypoint[] {ZipLineLab.START_POS, new Waypoint(1, ZipLineLab.START_POS.y)});
      } else {
        reached_first_corner = true;
        skipped_loc = true;
      }
    }
    
    // when at the first corner, set path toward zipline
    if (reached_first_corner && !reached_zipline) {
      if (skipped_loc) {
        nav.setPath(new Waypoint[] {ZipLineLab.START_POS, ZipLineLab.ZIPLINE_START_POS});
      } else {
        nav.setPath(new Waypoint[] {ZipLineLab.ZIPLINE_START_POS});
      }
    } else if (reached_zipline && !at_zipline) {
      nav.setPath(new Waypoint[] {ZipLineLab.ZIPLINE_START_POS});
    }

    return state.NAVIGATING;
  }

  /**
   * Processing for navigating state.
   * 
   * @return state - the state to go to next
   */
  public state process_navigating() {
    if (ZipLineLab.debug_mode) {
      System.out.println("[CONTROLLER] Entering navigation");
    }
    // perform navigation processing in Navigation class
    nav.process();
    
    // we've reached the first corner, set the reference position
    if (nav.done && !reached_first_corner) {
      reached_first_corner = true;
      loc.setRefPos(new Waypoint(1, ZipLineLab.START_POS.y));
    } 
    // we've reached the zipline, set the reference position
    else if (nav.done && !reached_zipline) {
      // TODO: implement position check.
      reached_zipline = true;
      loc.setRefPos(ZipLineLab.ZIPLINE_START_POS);
    } 
    // we're ready for final zipline alignment
    else if (nav.done && reached_zipline) {
      at_zipline = true;
      reached_zipline = false;
    }
    
    // go to the next state as needed
    return nav.done ? at_zipline ? state.ZIPLINE : state.LOCALIZING : state.NAVIGATING;
  }
  
  /**
   * Processing for searching state.
   * 
   * @return state - the next state to move to
   */
  public state process_searching() {
    // Not implemented in this lab.
    return state.IDLE;
  }

  /**
   * Processing for zipline state.
   * 
   * @return state - the next state to move to
   */
  public state process_zipline() {
    // perform zipline processing in the ZiplineController class
    cp.setMode(l_mode.ZIPLINING);
	zip.process();
	
	// change the boolean when done
    if (zip.done) {
      traversed_zipline = true;
      lab5_stop = true;
    }

    // go to the next state as needed
    return zip.done ? state.IDLE : state.ZIPLINE;
  }

  /**
   * Update state.
   * 
   * @return state - current state
   */
  public synchronized state getCurrentState() {
    return cur_state;
  }

  /**
   * Returns the sub-state in the form of a string (since they're all different types)
   * 
   * @return substate
   */
  public synchronized String getCurSubState() {
    switch (cur_state) {
      case IDLE:
        return null;
      case LOCALIZING:
        return loc.getCurrentState().toString();
      case NAVIGATING:
        return nav.getCurrentState().toString();
      case SEARCHING:
        return null;
      case ZIPLINE:
        return zip.getCurrentState().toString();
    }

    // fallthrough
    return state.IDLE.toString();
  }

  public void setStartingPos(Waypoint start_pos) {
    loc.setRefPos(start_pos);
  }
}

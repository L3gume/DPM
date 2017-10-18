package ca.mcgill.ecse211.zipline;

public class Controller extends Thread {

  private Odometer odo;
  private Driver drv;
  private Navigation nav;
  private Localizer loc;
  private ZiplineController zip;

  // the SEARCHING state won't be implemented in this lab.
  public enum state {
    IDLE, LOCALIZING, NAVIGATING, SEARCHING, ZIPLINE
  };

  private state cur_state = state.IDLE;
  private boolean traversed_zipline = false;

  /**
   * Constructor
   * 
   * @param odo Odometer
   * @param drv Driver
   * @param nav Navigator
   * @param loc Localizer
   * @param zip ZiplineController
   */
  public Controller(Odometer odo, Driver drv, Navigation nav, Localizer loc, ZiplineController zip) {
    this.odo = odo;
    this.drv = drv;
    this.nav = nav;
    this.loc = loc;
    this.zip = zip;

    init();
  }

  // Might be better to start the threads when we need them.
  private void init() {
    odo.start();
    nav.start();
    loc.start();
  }

  /**
   * run() method.
   */
  public void run() {
    while (true) {
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
        Thread.sleep(20);
      } catch (Exception e) {
        // ...
      }
    }
  }

  /*
   * State processing methods.
   */

  public state process_idle() {
    // For this lab, we don't need to check the goal we are given, just move to localization
    return state.LOCALIZING;
  }

  public state process_localizing() {
    loc.startLocalization(traversed_zipline ? true : false);
    while (!loc.done); // Wait for localization to complete
    // We don't really have a way of knowing if the localization was a success or not, let's just
    // assume it worked like in lab 4.
    
    // reset the traversed_zipline boolean in case we ever need to traverse again.
    if (traversed_zipline) {
      traversed_zipline = false;
    }
    return state.NAVIGATING;
  }

  public state process_navigating() {
    // Need verification of the current path.
    nav.setNavigating(true);
    while (!nav.done); // Wait for navigation to complete.
    return state.IDLE;
  }

  public state process_searching() {
    // Not implemented in this lab.
    return state.IDLE;
  }

  public state process_zipline() {
    /*
     * Tricky part.
     */
    
    if (zip.done) {
      traversed_zipline = true;
    }
    
    return state.IDLE;
  }
  
  public synchronized state getCurrentState() {
    return cur_state;
  }
  
  /**
   * Returns the substate in the form of a string (since they're all different types
   * 
   * @return substate
   */
  public synchronized String getCurSubState() {  
    switch (cur_state) {
      case IDLE: return null;
      case LOCALIZING: return loc.getCurrentState().toString();
      case NAVIGATING: return nav.getCurrentState().toString();
      case SEARCHING: return null;
      case ZIPLINE: return null;
    }
    
    // fallthrough
    return state.IDLE.toString();
  }
}

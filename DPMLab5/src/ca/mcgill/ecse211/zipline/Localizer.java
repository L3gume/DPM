package ca.mcgill.ecse211.zipline;

import ca.mcgill.ecse211.zipline.ColorPoller.l_mode;
import ca.mcgill.ecse211.zipline.UltrasonicPoller.u_mode;
import lejos.hardware.Button;

/**
 * Handles the localization of the robot.
 * 
 * @author Justin Tremblay
 *
 */
public class Localizer extends Thread {
  /* References to other classes */
  private UltrasonicLocalizer ul;
  private LightLocalizer ll;
  private ColorPoller cp;
  private UltrasonicPoller up;
  private Driver dr;

  private boolean localizing = false; // Used to block the thread.
  private boolean skip_ultrasonic = false;
  public boolean done = false;
  
  private Waypoint ref_pos;

  public enum loc_state {
    IDLE, NOT_LOCALIZED, ULTRASONIC, LIGHT, DONE
  };

  private loc_state cur_state = loc_state.IDLE;

  /**
   * Constructor
   * 
   * @param ul UltrasonicLocalizer, performs rising edge localization
   * @param ll LightLocalization, works alongside the ultrasonic localizer to determine the robot's
   *        position
   * @param up UltrasonicPoller, Used by the ultrasonic localizer
   * @param cp ColorPoller, Used by the light localizer
   */
  public Localizer(UltrasonicLocalizer ul, LightLocalizer ll, UltrasonicPoller up, ColorPoller cp,
      Driver dr) {
    this.ul = ul;
    this.ll = ll;
    this.up = up;
    this.cp = cp;
    this.dr = dr;
    
    up.setLocalizer(ul);
    cp.setLocalizer(ll);
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
        case NOT_LOCALIZED:
          cur_state = process_notLocalized();
          break;
        case ULTRASONIC:
          cur_state = process_ultrasonic();
          break;
        case LIGHT:
          cur_state = process_light();
          break;
        case DONE:
          cur_state = process_done();
          break;
        default: // Should not happen.
          break;
      }
      
      /*
       * Space reserved for special cases, shouldn't be needed here.
       */
      
      try {
        Thread.sleep(40);
      } catch (Exception e) {
        // ...
      }
    }
  }

  /* State processing methods */
  
  /**
   * Checks for the value of localizing, returns IDLE if false.
   * 
   * @return new state
   */
  private loc_state process_idle() {
    return localizing ? loc_state.NOT_LOCALIZED : loc_state.IDLE;
  }

  /**
   * Checks for value of localizing again (just in case). returns IDLE if false.
   * 
   * @return new state
   */
  private loc_state process_notLocalized() {
    dr.rotate(360, true, true); // Start rotating
    
    // Fancy ternary nonsense!
    return localizing ? skip_ultrasonic ? loc_state.LIGHT : loc_state.ULTRASONIC : loc_state.IDLE;
  }

  /**
   * Sets up the ultrasonic poller and starts the ultrasonic localization
   * 
   * @return new state
   */
  private loc_state process_ultrasonic() {
    if (!localizing) {
      return loc_state.IDLE;
    }
    
    if (up.isAlive()) {
      up.setMode(u_mode.LOCALIZATION);
    } else {
      System.out.println("[LOCALIZER] UltrasonicPoller not running!");
      return loc_state.IDLE; // That's a big problem.
    }
    ul.localize();
    return loc_state.LIGHT; // Go directly to light localization.
  }

  /**
   * Sets up the color poller and starts the light localization
   * 
   * @return new state
   */
  private loc_state process_light() {
//    if (!localizing) {
//      return loc_state.IDLE;
//    }
    
    if (cp.isAlive()) {
      cp.setMode(l_mode.LOCALIZATION);
    } else {
      System.out.println("[LOCALIZER] ColorPoller not running!");
      return loc_state.IDLE; // That's a big problem.
    }
    ll.localize();
    return loc_state.DONE;
  }

  /**
   * Sets localizing to false to stop the thread from doing anything.
   * 
   * @return new state
   */
  private loc_state process_done() {
    localizing = false;
    done = true;
    
    // reset
    if (skip_ultrasonic) {
      skip_ultrasonic = false;
    }
    cp.setMode(l_mode.CORRECTION);
    return loc_state.IDLE;
  }

  /**
   * Getter for the state
   * 
   * @return current state of the localizer
   */
  public synchronized loc_state getCurrentState() {
    return cur_state;
  }

  /**
   * Starts the localization process.
   * 
   * @param skip_ultrasonic set to true s to skip ultrasonic localization.
   */
  public synchronized void startLocalization(boolean skip_ultrasonic) {
    this.skip_ultrasonic = skip_ultrasonic;
    localizing = true;
    done = false;
  }
  
  /**
   * probably won't ever be usefull.
   */
  public synchronized void abortLocalization() {
    localizing = false;
  }
  
  public void setRefPos(Waypoint ref_pos) {
    this.ref_pos = ref_pos;
    ul.setRefPos(this.ref_pos);
    ll.setRefPos(this.ref_pos);
  }
  
  public Waypoint getRefPos() {
    return ref_pos;
  }
}
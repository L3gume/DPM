package ca.mcgill.ecse211.localizationlab;

/**
 * 
 * @author Justin Tremblay
 *
 */
public class LightLocalizer extends Thread {
  private Driver driver;
  private Odometer odo;

  private final double GRID_LENGTH = 30.48;
  private final double SENSOR_OFFSET = 10.0;
  private final float LIGHT_THRESHOLD = 0.5f;

  private int line_count = 0; // We will detect 4 lines in this lab
  private double[] angles = new double[4];

  private float light_level;
  private float prev_light_level;

  public LightLocalizer(Driver driver, Odometer odo) {
    this.driver = driver;
    this.odo = odo;
  }

  public void run() {
    driver.rotate(360, true, true);
    localize();
  }

  private void localize() {
    // Start by finding all the lines
    while (line_count != 4) {
      waitForLine();
      // The method returned, that means we found a line
      angles[line_count++] = odo.getTheta(); // Record the angle at which we detected the line.
      sleepThread(1); // wait for a second to avoid multiple detections of the same line.
    }
    
    // We found all the lines, compute the position.
    computePosition();
  }

  private void computePosition() {
    
  }

  /**
   * This method locks the localizer until the light level becomes lower that the threshold level,
   * meaning we detected a line.
   */
  private void waitForLine() {
    while (getLightLevel() > LIGHT_THRESHOLD) {
    } ;
    return;
  }

  /*
   * Not really necessary, this is just to make the risingEdge and fallingEdge methods more readable.
   */
  private void sleepThread(float seconds) {
    try {
      Thread.sleep((long) (seconds * 1000));
    } catch (Exception e) {
      // TODO: handle exception
    }
  }
  
  /*
   * Getters and Setters for the light_level, used by colorPoller
   */
  public synchronized float getLightLevel() {
    return light_level;
  }

  public synchronized void setLightLevel(float new_level) {
    prev_light_level = light_level;
    light_level = new_level;
  }
}

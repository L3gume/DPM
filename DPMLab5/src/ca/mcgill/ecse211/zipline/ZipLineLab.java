package ca.mcgill.ecse211.zipline;

import ca.mcgill.ecse211.zipline.Driver;
import ca.mcgill.ecse211.zipline.Navigation;
import ca.mcgill.ecse211.zipline.Odometer;
import ca.mcgill.ecse211.zipline.Display;
import ca.mcgill.ecse211.zipline.UltrasonicLocalizer.Mode;
import ca.mcgill.ecse211.zipline.UltrasonicPoller;
import ca.mcgill.ecse211.zipline.Waypoint;
import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;
import lejos.robotics.filter.MeanFilter;
import lejos.robotics.filter.MedianFilter;

public class ZipLineLab {

  /*
   * Global Constants
   */
  public static final boolean debug_mode = false;
  public static final double SQUARE_LENGTH = 30.48; // The length of a square on the grid.

  /*
   * Odometry and Driver Constants
   */
  public static final double WHEEL_RADIUS = 2.1;
  public static final double TRACK = 11.75;
  public static final int FORWARD_SPEED = 175;
  public static final int ROTATE_SPEED = 75;
  
  /*
   * Navigation constants
   */

  public static final double ANGLE_THRESHOLD = Math.toRadians(2); // If the angle to target position is
                                                            // lower than 2 degrees, then that's
                                                            // good enough.
  public static final double DISTANCE_THRESHOLD = 2; // If the distance is below 2 cm, then that's good
                                               // enough.


  /*
   * Light Localization Constants
   */
  public static final double SENSOR_OFFSET = 19.85;
  public static final float LIGHT_THRESHOLD = 0.53f;
  public static Waypoint START_POS;

  /*
   * Ultrasonic Localization Constants
   */
  public static final float RISING_DIST_THRESHOLD = 40.f;
  public static final float FALLING_DIST_THRESHOLD = 50.f;

  /*
   * Zipline Controller Constants
   */
  public static Waypoint ZIPLINE_START_POS; // Is going to be inputed by the user.
  public static Waypoint ZIPLINE_END_POS; // Is going to be computed using the inputed zipline start
                                          // position.
  public static final double ZIPLINE_ORIENTATION = 0.0;
  public static final double ZIPLINE_ORIENTATION_THRESHOLD = Math.toRadians(2); 
  public static final double ZIPLINE_LENGTH = 100.0; // TODO: Temporary value for zipline length.
  public static final float ZIPLINE_TRAVERSAL_SPEED = 150.f;

  /*
   * Motors and Sensors
   */
  private static final EV3LargeRegulatedMotor leftMotor =
      new EV3LargeRegulatedMotor(LocalEV3.get().getPort("C"));
  private static final EV3LargeRegulatedMotor rightMotor =
      new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
  private static final EV3LargeRegulatedMotor zipMotor =
      new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));

  // Medium motor to which the US sensor is mounted, not used in this lab.
  @SuppressWarnings("unused")
  private static final EV3MediumRegulatedMotor sensorMotor =
      new EV3MediumRegulatedMotor(LocalEV3.get().getPort("B"));
  // Ultrasonic sensor port.
  private static final Port usPort = LocalEV3.get().getPort("S1");
  // Color sensor port.
  private static final Port colorPort = LocalEV3.get().getPort("S2");

  private static SampleProvider us;
  private static SampleProvider mean;
  private static float[] usData;

  // private static EV3ColorSensor colorSensor;
  private static SampleProvider cs;
  private static SampleProvider median;
  private static float[] colorData;

  private static Mode choice;

  public static void main(String[] args) {
    Waypoint coordsStart;
    Waypoint coordsZipLine;

    final TextLCD t = LocalEV3.get().getTextLCD();

    // Set up the ultrasonic sensor.
    @SuppressWarnings("resource") // Because we don't bother to close this resource
    SensorModes usSensor = new EV3UltrasonicSensor(usPort); // usSensor is the instance
    us = usSensor.getMode("Distance"); // usDistance provides samples from this instance
    mean = new MeanFilter(us, us.sampleSize());
    usData = new float[mean.sampleSize()]; // usData is the buffer in which data are stored

    // Set up the color sensor.
    @SuppressWarnings("resource")
    SensorModes colorSensor = new EV3ColorSensor(colorPort);
    cs = colorSensor.getMode("Red");
    median = new MedianFilter(cs, cs.sampleSize());
    colorData = new float[median.sampleSize()];

    // Display the main menu and receive the starting coordinates from the user.
    coordsStart = new Waypoint(ZipLineLab.getCoordinates(t, "Start", 0, 3));

    // Display the main menu and receive zip line coordinates from the user.
    coordsZipLine = new Waypoint(ZipLineLab.getCoordinates(t, "Zip Line", 0, 8));

    //
    // TODO: Create Controller and ZiplineController instances.
    //
    //
    //
    // Constructors (now):
    //
    // Controller(Odometer odo, Driver drv, Navigation nav, Localizer loc, ZiplineController zip)
    //
    //
    // Constructors (after):
    //
    // Controller(
    // Odometer odo, Driver drv, Navigation nav, Localizer loc, ZiplineController zip,
    // Waypoint coordsStart, Waypoint coordsZipLine
    // )
    // er

    Odometer odo = new Odometer(leftMotor, rightMotor, WHEEL_RADIUS, TRACK);
    Driver dr = new Driver(leftMotor, rightMotor, WHEEL_RADIUS, WHEEL_RADIUS, TRACK);
    UltrasonicLocalizer ul = new UltrasonicLocalizer(choice, dr, odo);
    UltrasonicPoller up = new UltrasonicPoller(mean, usData);
    LightLocalizer ll = new LightLocalizer(dr, odo);
    ColorPoller cp = new ColorPoller(median, colorData);
    Navigation nav = new Navigation(dr, odo, up);
    Localizer loc = new Localizer(ul, ll, up, cp, dr);
    ZiplineController zip = new ZiplineController(odo, dr, zipMotor);
    Controller cont = new Controller(odo, dr, nav, loc, zip);
    Display disp = new Display(odo, t, nav, ul, ll, cont);
    disp.start();

    ZIPLINE_START_POS = new Waypoint(1, 6);
    ZIPLINE_END_POS = new Waypoint(ZIPLINE_START_POS.x + 6, ZIPLINE_START_POS.y);
    START_POS = new Waypoint(1,1);
    cont.setStartingPos(START_POS);
    cont.start();

    while (Button.waitForAnyPress() != Button.ID_ESCAPE);
    System.exit(0);
  }

  /**
   * Display the main menu, querying the user for the X/Y-coordinates of the zip line.
   * 
   * @param t the EV3 LCD display to which the main menu should be output
   * @return the X/Y-coordinates of the zip line
   */
  static int[] getCoordinates(final TextLCD t, String title, int llim, int rlim) {
    boolean done = false;

    // Clear the display.
    t.clear();

    t.drawString(title, 0, 0);
    t.drawString("-----------------", 0, 1);
    t.drawString("                 ", 0, 2);

    int[] coords = new int[] {0, 0};

    int index = 0;

    while (!done) {
      int buttonChoice = -1;

      // Clear the current x/y-coordinate values.
      t.drawString("X:               ", 0, 3);
      t.drawString("Y:               ", 0, 4);

      // Print the current x/y-coordinate values.
      t.drawString(String.format("%2d", coords[0]), 3, 3);
      t.drawString(String.format("%2d", coords[1]), 3, 4);

      // Draw the indicator showing which coordinate value is currently selected.
      t.drawString("<--", 12, 3 + index);

      buttonChoice = Button.waitForAnyPress();

      switch (buttonChoice) {
        // Select the x-coordinate for modification.
        case Button.ID_UP:
          index = 0;

          break;

        // Select the y-coordinate for modification.
        case Button.ID_DOWN:
          index = 1;

          break;

        // Decrease the currently selected coordinate value.
        case Button.ID_LEFT:
          if (coords[index] > llim) {
            coords[index] -= 1;
          }

          break;

        // Increase the currently selected coordinate value.
        case Button.ID_RIGHT:
          if (coords[index] < rlim) {
            coords[index] += 1;
          }

          break;

        // Submit selected coordinates.
        case Button.ID_ENTER:
          done = true;

          break;

        // Ignore.
        default:
          break;
      }
    }

    return coords;
  }

}

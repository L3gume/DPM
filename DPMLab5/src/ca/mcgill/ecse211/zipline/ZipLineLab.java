package ca.mcgill.ecse211.zipline;

import ca.mcgill.ecse211.zipline.Driver;
import ca.mcgill.ecse211.zipline.Navigation;
import ca.mcgill.ecse211.zipline.Odometer;
import ca.mcgill.ecse211.zipline.Display;
import ca.mcgill.ecse211.zipline.UltrasonicLocalizer.Mode;
import ca.mcgill.ecse211.zipline.UltrasonicPoller;
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

  public static final boolean debug_mode = true;
  private static final EV3LargeRegulatedMotor leftMotor =
      new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
  private static final EV3LargeRegulatedMotor rightMotor =
      new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));
  // Medium motor to which the US sensor is mounted, not used in this lab.
  @SuppressWarnings("unused")
  private static final EV3MediumRegulatedMotor sensorMotor =
      new EV3MediumRegulatedMotor(LocalEV3.get().getPort("B"));
  // Ultrasonic sensor port.
  private static final Port usPort = LocalEV3.get().getPort("S2");
  // Color sensor port.
  private static final Port colorPort = LocalEV3.get().getPort("S1");

  private static SampleProvider us;
  private static SampleProvider mean;
  private static float[] usData;

  //private static EV3ColorSensor colorSensor;
  private static SampleProvider cs;
  private static SampleProvider median;
  private static float[] colorData;

  public static final double WHEEL_RADIUS = 2.1;
  public static final double TRACK = 9.545;

  private static Mode choice;

  public static void main(String[] args) {
    int[] coordinates;

    final TextLCD t = LocalEV3.get().getTextLCD();

    // Set up the ultrasonic sensor.
    @SuppressWarnings("resource") // Because we don't bother to close this resource
    SensorModes usSensor = new EV3UltrasonicSensor(usPort); // usSensor is the instance
    us = usSensor.getMode("Distance"); // usDistance provides samples from this instance
    mean = new MeanFilter(us, us.sampleSize());
    usData = new float[mean.sampleSize()]; // usData is the buffer in which data are
    
    // Set up the color sensor.
    @SuppressWarnings("resource")
    SensorModes colorSensor = new EV3ColorSensor(colorPort);  
    cs = colorSensor.getMode("Red");
    median = new MedianFilter(cs, cs.sampleSize());
    colorData = new float[median.sampleSize()];

    // Display the main menu and receive zip line coordinates from the user.
    coordinates = ZipLineLab.getZipLineCoordinates(t);

    Odometer odometer = new Odometer(leftMotor, rightMotor);
    Driver d = new Driver(leftMotor, rightMotor, WHEEL_RADIUS, WHEEL_RADIUS, TRACK);
    UltrasonicLocalizer ul = new UltrasonicLocalizer(choice, d, odometer);
    UltrasonicPoller u = new UltrasonicPoller(mean, usData, ul);
    LightLocalizer ll = new LightLocalizer(d, odometer);
    ColorPoller cp = new ColorPoller(median, colorData, ll);
    Navigation nav = new Navigation(d, odometer, u);
    Display odometryDisplay = new Display(odometer, t, nav, ul, ll);

    /*
     * Thread to detect early exits.
     */
    (new Thread() {
      public void run() {
        while (Button.waitForAnyPress() != Button.ID_ESCAPE);
        System.exit(0);
      }
    }).start();
    
    // "scripted" part for the action sequence. A state machine and goal queue would be better.
    odometer.start();
    odometryDisplay.start();
    u.start();
    ul.start();

    while (!ul.done);
    d.rotate(45, true, false);
    d.moveTo(5.0, false);
    cp.start();
    while (!cp.isAlive()); // Make sure the color poller thread is alive before starting the localization.
    ll.start();
    while (!ll.done);
    nav.start();
    nav.setNavigating(true);
    nav.setPath(new Waypoint[] {new Waypoint(0,0)});
    while (nav.isNavigating());
    d.rotate(-odometer.getTheta(), false, false);
    
    while (Button.waitForAnyPress() != Button.ID_ESCAPE);
    System.exit(0);
  }

  /**
   * Display the main menu, querying the user for the X/Y-coordinates of the zip line.
   * @param t the EV3 LCD display to which the main menu should be output
   * @return the X/Y-coordinates of the zip line
   */
  static int[] getZipLineCoordinates(final TextLCD t) {
    boolean done = false;

    // Clear the display.
    t.clear();

    t.drawString("Coordinates      ", 0, 0);
    t.drawString("-----------------", 0, 1);
    t.drawString("                 ", 0, 2);

    int[] coords = new int[] { 0, 0 };

    int index = 0;

    while (!done) {
      int buttonChoice = -1;

      // Clear the current x/y-coordinate values.
      t.drawString("X:               ", 0, 3);
      t.drawString("Y:               ", 0, 4);

      // Print the current x/y-coordinate values.
      t.drawString(String.format("%2d", coords[0]), 3, 3);
      t.drawString(String.format("%2d", coords[0]), 3, 4);

      // Draw the indicator showing which coordinate value is currently selected.
      t.drawString("<--", 12, 3 + index);

      buttonChoice = Button.waitForAnyPress();

      switch (buttonChoice) {
        // Select the x-coordinate for modification.
        case Button.ID_UP:
          if (index == 0) {
            break;
          }

          index = 1;

          break;

        // Select the y-coordinate for modification.
        case Button.ID_DOWN:
          if (index == 1) {
            break;
          }

          index = 0;

          break;

        // Decrease the currently selected coordinate value.
        case Button.ID_LEFT:
          if (coords[0] <=  0) {
            break;
          }

          coords[index] -= 1;

          break;

        // Increase the currently selected coordinate value.
        case Button.ID_RIGHT:
          if (coords[0] >= 12) {
            break;
          }

          coords[index] += 1;

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

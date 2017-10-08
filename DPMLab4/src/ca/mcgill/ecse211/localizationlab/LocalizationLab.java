package ca.mcgill.ecse211.localizationlab;

import ca.mcgill.ecse211.localizationlab.Driver;
import ca.mcgill.ecse211.localizationlab.Navigation;
import ca.mcgill.ecse211.localizationlab.Odometer;
import ca.mcgill.ecse211.localizationlab.Display;
import ca.mcgill.ecse211.localizationlab.UltrasonicLocalizer.Mode;
import ca.mcgill.ecse211.localizationlab.UltrasonicPoller;
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

public class LocalizationLab {

  public static final boolean debug_mode = false;
  private static final EV3LargeRegulatedMotor leftMotor =
      new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
  private static final EV3LargeRegulatedMotor rightMotor =
      new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));
  // Medium motor to which the US sensor is mounted, not used in this lab.
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
  public static final double TRACK = 9.8;

  private static Mode choice;

  public static void main(String[] args) {
    int buttonChoice = -1;

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
    
    // Set up the menu display.
    do {
      // clear the display
      t.clear();
      // ask the user whether the motors should drive in a square or float
      t.drawString("Left: Rising Edge", 0, 0);
      t.drawString("Right: Falling Edge", 0, 1);
      buttonChoice = Button.waitForAnyPress();
    } while (buttonChoice != Button.ID_LEFT && buttonChoice != Button.ID_RIGHT);

    switch (buttonChoice) {
      case Button.ID_LEFT:
        choice = Mode.RISING_EDGE;
        break;

      case Button.ID_RIGHT:
        choice = Mode.FALLING_EDGE;
        break;
      default:
        System.exit(0);
        break;
    }
    
    if (buttonChoice == Button.ID_LEFT || buttonChoice == Button.ID_RIGHT) {
      Odometer odometer = new Odometer(leftMotor, rightMotor);
      Driver d = new Driver(leftMotor, rightMotor, WHEEL_RADIUS, WHEEL_RADIUS, TRACK);
      UltrasonicLocalizer ul = new UltrasonicLocalizer(choice, d, odometer);
      UltrasonicPoller u = new UltrasonicPoller(mean, usData, ul);
      LightLocalizer ll = new LightLocalizer(d, odometer);
      ColorPoller cp = new ColorPoller(median, colorData, ll);
      Navigation nav = new Navigation(d, odometer, u);
      Display odometryDisplay = new Display(odometer, t, nav, ul, ll);
      
      (new Thread() {
        public void run() {
          while (Button.waitForAnyPress() != Button.ID_ESCAPE);
          System.exit(0);
        }
      }).start();
      
      odometer.start();
      odometryDisplay.start();
      u.start();
      ul.start();
      
      while (!ul.done);
      cp.start();
      while (!cp.isAlive()); // Make sure the color poller thread is alive before starting the localization.
      ll.start();
        
      while (!ll.done);
      nav.start();
      nav.setNavigating(true);
      nav.setPath(new Waypoint[] {new Waypoint(0,0)});
      
      while (nav.isNavigating());
      d.rotate(-odometer.getTheta(), false, false);
    }
    
    while (Button.waitForAnyPress() != Button.ID_ESCAPE);
    System.exit(0);
  }
}

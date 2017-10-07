package ca.mcgill.ecse211.localizationlab;

import ca.mcgill.ecse211.localizationlab.Driver;
import ca.mcgill.ecse211.localizationlab.Navigation;
import ca.mcgill.ecse211.localizationlab.Odometer;
import ca.mcgill.ecse211.localizationlab.OdometryDisplay;
import ca.mcgill.ecse211.localizationlab.UltrasonicPoller;
import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;

public class LocalizationLab {

  public static final boolean debug_mode = false;
  
  private static final EV3LargeRegulatedMotor leftMotor =
      new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));

  private static final EV3LargeRegulatedMotor rightMotor =
      new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));
  
  private static final EV3MediumRegulatedMotor sensorMotor = 
      new EV3MediumRegulatedMotor(LocalEV3.get().getPort("B"));

  public static final double WHEEL_RADIUS = 2.1;
  public static final double TRACK = 9.8;
  
  public static void main(String[] args) {
    int buttonChoice = -1;

    final TextLCD t = LocalEV3.get().getTextLCD();

    Odometer odometer = new Odometer(leftMotor, rightMotor);
    Driver d = new Driver(leftMotor, rightMotor, WHEEL_RADIUS, WHEEL_RADIUS, TRACK);
    UltrasonicPoller u = new UltrasonicPoller(sensorMotor);
    Navigation nav = new Navigation(d, odometer, u);
    OdometryDisplay odometryDisplay = new OdometryDisplay(odometer, t, nav);

    do {
      // clear the display
      t.clear();

      // ask the user whether the motors should drive in a square or float
      t.drawString("Left: Track 1", 0, 0);
      t.drawString("Right: Track 2", 0, 1);
      t.drawString("Up: Track 3", 0, 2);
      t.drawString("Down: Track 4", 0, 3);
      
      buttonChoice = Button.waitForAnyPress();
    } while (buttonChoice != Button.ID_LEFT && buttonChoice != Button.ID_RIGHT && buttonChoice != Button.ID_UP && buttonChoice != Button.ID_DOWN);
    
    if (buttonChoice != -1)  {
      switch (buttonChoice) {
        case Button.ID_LEFT: nav.setPath(1); break;
        case Button.ID_RIGHT: nav.setPath(2); break;
        case Button.ID_UP: nav.setPath(3); break;
        case Button.ID_DOWN: nav.setPath(4); break;
        case Button.ID_ESCAPE: System.exit(0); break;
      }
      odometer.start();
      odometryDisplay.start();
      nav.start();
      u.start();
    }

    while (Button.waitForAnyPress() != Button.ID_ESCAPE);
    System.exit(0);

  }

}

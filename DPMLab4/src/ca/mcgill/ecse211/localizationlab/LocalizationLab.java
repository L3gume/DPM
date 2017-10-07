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

			      t.drawString("Rising Edge", 0, 0);

			      t.drawString("Falling Edge", 0, 1);


			      buttonChoice = Button.waitForAnyPress();

			    } while (buttonChoice != Button.ID_LEFT && buttonChoice != Button.ID_RIGHT);

			
		   switch (buttonChoice) {

			case Button.ID_LEFT: // Bang-bang control selected

				usPoller = new UltrasonicPoller(usDistance, usData, bangbangController);


				break;

			case Button.ID_RIGHT: // Proportional control selected

				usPoller = new UltrasonicPoller(usDistance, usData, pController);

				break;


			}


			    

	}
}


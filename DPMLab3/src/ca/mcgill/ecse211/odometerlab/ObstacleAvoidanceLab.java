// Lab2.java

package ca.mcgill.ecse211.odometerlab;

import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.hardware.sensor.SensorMode;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;

public class ObstacleAvoidanceLab {

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
    OdometryCorrection odometryCorrection = new OdometryCorrection(odometer);
    Driver d = new Driver(leftMotor, rightMotor, WHEEL_RADIUS, WHEEL_RADIUS, TRACK);
    Navigator nav = new Navigator(d, odometer);
    OdometryDisplay odometryDisplay = new OdometryDisplay(odometer, t, odometryCorrection, nav);

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

    /*if (buttonChoice == Button.ID_LEFT) {

      leftMotor.forward();
      leftMotor.flt();
      rightMotor.forward();
      rightMotor.flt();

      odometer.start();
      odometryDisplay.start();

    } else {
      // clear the display
      t.clear();

      // ask the user whether the motors should drive in a square or float
      t.drawString("< Left | Right >", 0, 0);
      t.drawString("  No   | with   ", 0, 1);
      t.drawString(" corr- | corr-  ", 0, 2);
      t.drawString(" ection| ection ", 0, 3);
      t.drawString("       |        ", 0, 4);

      buttonChoice = Button.waitForAnyPress();

      odometer.start();
      odometryDisplay.start();

      if (buttonChoice == Button.ID_RIGHT) {
        odometryCorrection.start();
      }

      // spawn a new Thread to avoid SquareDriver.drive() from blocking
      (new Thread() {
        public void run() {
          SquareDriver.drive(leftMotor, rightMotor, WHEEL_RADIUS, WHEEL_RADIUS, TRACK);
        }
      }).start();
    }*/
    
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
      /*(new Thread() {
        public void run() {
          Driver.drive(leftMotor, rightMotor, WHEEL_RADIUS, WHEEL_RADIUS, TRACK);
        }
      }).start();*/
    }

    while (Button.waitForAnyPress() != Button.ID_ESCAPE);
    System.exit(0);
  }
}

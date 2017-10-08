package ca.mcgill.ecse211.localizationlab;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;
import lejos.robotics.filter.MedianFilter;

public class LightLocalizer {

	private static final int FORWARD_SPEED = 200;

	private static final int ROTATE_SPEED = 150;

	private double backwardDistance = 14.0;

	private double radius = 2.1;

	private float[] sample;

	private SampleProvider ambientLight;

	private SampleProvider median;

	private EV3ColorSensor sensor;
	
	public static final double TRACK = 15.75;

	public void drive(EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor) {

		Port port = LocalEV3.get().getPort("S1");

		sensor = new EV3ColorSensor(port);

		SensorModes colorSensor = sensor;

		ambientLight = colorSensor.getMode("Red");

		median = new MedianFilter(ambientLight, 5);

		sample = new float[median.sampleSize()];

		sensor.setFloodlight(false);

		median.fetchSample(sample, 0);

		float light_level = sample[0];

		assert (sample[0] > 0);

		// reset the motors

		for (EV3LargeRegulatedMotor motor : new EV3LargeRegulatedMotor[] { leftMotor, rightMotor }) {

			motor.stop();

			motor.setAcceleration(3000);

		}

		// wait 5 seconds

		try {

			Thread.sleep(2000);

		} catch (InterruptedException e) {

			// there is nothing to be done here because it is not expected that

			// the odometer will be interrupted by another thread

		}

		while (light_level < 0.5) {

			leftMotor.setSpeed(FORWARD_SPEED);

			rightMotor.setSpeed(FORWARD_SPEED);

			leftMotor.forward();

			rightMotor.forward();

		}

		// Backward

		leftMotor.rotate(-convertDistance(radius, backwardDistance), true);

		rightMotor.rotate(-convertDistance(radius, backwardDistance), true);

		// turn 90 degrees clockwise

		leftMotor.setSpeed(ROTATE_SPEED);

		rightMotor.setSpeed(ROTATE_SPEED);

		leftMotor.rotate(convertAngle(radius, TRACK, 90.0), true);

		rightMotor.rotate(-convertAngle(radius, TRACK, 90.0), false);

		/*
		 * 
		 * Now We need to let the robot move to (0, 0).
		 * 
		 */

	}

	private static int convertDistance(double radius, double distance) {

		return (int) ((180.0 * distance) / (Math.PI * radius));

	}

	private static int convertAngle(double radius, double width, double angle) {

		return convertDistance(radius, Math.PI * width * angle / 360.0);

	}

}

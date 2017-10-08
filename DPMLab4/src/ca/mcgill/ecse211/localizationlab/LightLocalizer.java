package ca.mcgill.ecse211.localizationlab;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;
import lejos.robotics.filter.MedianFilter;

public class LightLocalizer {

	private double gridDistance = 30.48;

	private Driver driver;

	private double backwardDistance = 14.0;

	private float[] sample;

	private SampleProvider ambientLight;

	private SampleProvider median;

	private EV3ColorSensor sensor;

	public LightLocalizer(Driver driver) {

		this.driver = driver;

	}

	public void drive() {

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

	

		while (light_level < 0.5) {

			driver.moveForward();

		}

		driver.stop();

		// Backward

		driver.moveTo(-backwardDistance - (gridDistance) / 2, true);

		// turn 90 degrees clockwise

		driver.rotate(90, true, true);

		driver.stop();

		/*
		 * 
		 * Now We need to let the robot move to (0, 0).
		 * 
		 */

		while (light_level < 0.5) {

			driver.moveForward();

		}

		driver.stop();

		// turn 90 degrees counterclockwise

		driver.rotate(-90, true, true);

		driver.stop();

		// Now move to the (0, 0).

		driver.moveTo((gridDistance) / 2, true);

		driver.stop();
		
		/*
		 * 
		 * Or we can make it more simple but the it may be not very precise
		 * 
		 * depending on whether we can put it precisely alone the 45 degree line.
		 * 
		 */

	/*	driver.rotate(45, true, true);
		
		while (light_level < 0.5) {

			driver.moveForward();

		}

		driver.stop();
		
		// Backward

		driver.moveTo(-backwardDistance, true);
		
		// Point to 0 degree.
		
		driver.rotate(-45, true, true);
	*/	

	}

}

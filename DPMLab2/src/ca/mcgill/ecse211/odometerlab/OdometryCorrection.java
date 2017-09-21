/*
 * OdometryCorrection.java
 */
package ca.mcgill.ecse211.odometerlab;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.SensorMode;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.Color;
import lejos.robotics.SampleProvider;
import lejos.robotics.filter.MeanFilter;
import lejos.robotics.filter.MedianFilter;

public class OdometryCorrection extends Thread {
	private static final long CORRECTION_PERIOD = 10;
	private Odometer odometer;
	private float[] sample;
	private float prev_light_level;
	private SampleProvider ambientLight;
	private SampleProvider average;
	private EV3ColorSensor sensor;
	long line_detected = 0;
	 

	private static final double GRID_LENGTH = 30.48; // in cm

	// constructor
	public OdometryCorrection(Odometer odometer/* , ColorPoller col */) {
		this.odometer = odometer;
		// this.colPoller = col;

		Port port = LocalEV3.get().getPort("S1");
		sensor = new EV3ColorSensor(port);
		SensorModes colorSensor = sensor;
		ambientLight = colorSensor.getMode("Ambient");
		average = new MedianFilter(ambientLight, 5);
		sample = new float[average.sampleSize()];
		sensor.setFloodlight(false);
		
		assert(ambientLight != null);
	}

	// run method (required for Thread)
	public void run() {
		long correctionStart, correctionEnd;

		while (true) {
			correctionStart = System.currentTimeMillis();
			average.fetchSample(sample, 0);
			float light_level = sample[0];
			
			assert(sample[0] > 0);
			
			if (prev_light_level - light_level > 0.01f && System.currentTimeMillis() - line_detected > 2000) {
				System.out.println("Line detected");
				line_detected = System.currentTimeMillis();
			}
			prev_light_level = light_level;
			

			// this ensure the odometry correction occurs only once every period
			correctionEnd = System.currentTimeMillis();
			if (correctionEnd - correctionStart < CORRECTION_PERIOD) {
				try {
					Thread.sleep(CORRECTION_PERIOD - (correctionEnd - correctionStart));
				} catch (InterruptedException e) {
					// there is nothing to be done here because it is not
					// expected that the odometry correction will be
					// interrupted by another thread
				}
			}
		}
	}
	
	public float getLightLevel() {
		return prev_light_level;
	}
}

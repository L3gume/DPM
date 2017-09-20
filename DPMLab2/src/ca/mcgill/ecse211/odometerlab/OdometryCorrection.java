/*
 * OdometryCorrection.java
 */
package ca.mcgill.ecse211.odometerlab;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.SensorMode;
import lejos.robotics.Color;

public class OdometryCorrection extends Thread {
	private static final long CORRECTION_PERIOD = 10;
	private Odometer odometer;
	// private ColorPoller colPoller;
	private Object lock;
	//private EV3ColorSensor colorSensor;

	private static final double GRID_LENGTH = 30.48; // in cm

	/*
	 * private static final Port lightPort = LocalEV3.get().getPort("S1"); private
	 * SensorMode colorRGBSensor; private float sample[];
	 */

	// constructor
	public OdometryCorrection(Odometer odometer/* , ColorPoller col */) {
		this.odometer = odometer;
		// this.colPoller = col;

		/*
		 * this.colorSensor = new EV3ColorSensor(lightPort); this.colorRGBSensor =
		 * colorSensor.getRGBMode(); int sampleSize = colorSensor.sampleSize();
		 * this.sample = new float[sampleSize];
		 */
	}

	// run method (required for Thread)
	public void run() {
		long correctionStart, correctionEnd;

		while (true) {
			correctionStart = System.currentTimeMillis();

			// TODO Place correction implementation here
			/* colorRGBSensor.fetchSample(sample, 0); */
			/*
			 * if (sample[0] < 50) {
			 * 
			 * System.out.println("Detected Something"); synchronized (lock) {
			 * odometer.setX(0.0); odometer.setY(0.0); odometer.setTheta(0.0); }
			 */

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
}

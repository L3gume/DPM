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

	// Create a counter to record
	private int line_count = 0;

	private static final double GRID_LENGTH = 30.48; // in cm
	private static final double VELOCITY = 7.33;

	private double[] prev_pos;

	// constructor
	public OdometryCorrection(Odometer odometer/* , ColorPoller col */) {
		this.odometer = odometer;
		// this.colPoller = col;
		Port port = LocalEV3.get().getPort("S1");
		sensor = new EV3ColorSensor(port);
		SensorModes colorSensor = sensor;
		ambientLight = colorSensor.getMode("Red");
		average = new MedianFilter(ambientLight, 5);
		sample = new float[average.sampleSize()];
		sensor.setFloodlight(false);
		prev_pos = new double[3];
		assert (ambientLight != null);
	}

	// run method (required for Thread)
	public void run() {
		long correctionStart, correctionEnd;
		while (true) {
			correctionStart = System.currentTimeMillis();
			average.fetchSample(sample, 0);
			float light_level = sample[0];
			assert (sample[0] > 0);

			// Changed the light level
			if (light_level < 0.5 && System.currentTimeMillis() - line_detected > 2000) {
				// System.out.println("Line detected");
				if (line_count++ > 0) {
					// Compute the distance travelled
					double distance_travelled = VELOCITY * ((System.currentTimeMillis() - line_detected) / 1000);

					double delta_x = distance_travelled * Math.sin(prev_pos[2]);
					double delta_y = distance_travelled * Math.cos(prev_pos[2]);

					// Now we know the distance we travelled, our Y (or X) variation, we only need
					// theta
					// depending on our previous know orientation, determine a new one with the distance travelled and the grid length.
					double new_theta = (Math.abs(Math.cos(prev_pos[2])) < 0.70710678)
							&& (Math.abs(Math.sin(prev_pos[2])) > 0.70710678)
									? Math.asin(GRID_LENGTH / distance_travelled)
									: Math.acos(GRID_LENGTH / distance_travelled);

					setNewPos(prev_pos[0] + delta_x, prev_pos[1] + delta_y, new_theta);
					// we're done, update the previous position
					updatePrevPos();
				} else {
					// It's the first time, just increment counter and update last pos
					updatePrevPos();
				}
				line_detected = System.currentTimeMillis(); // Capture the last time we crossed a line.
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
	
	public int getLineCount() {
		return line_count;
	}

	private void updatePrevPos() {
		prev_pos[0] = odometer.getX();
		prev_pos[1] = odometer.getY();
		prev_pos[2] = odometer.getTheta();
	}
	
	private void setNewPos(double x, double y, double theta) {
		odometer.setX(x);
		odometer.setY(y);
		odometer.setTheta(theta);
	}

}
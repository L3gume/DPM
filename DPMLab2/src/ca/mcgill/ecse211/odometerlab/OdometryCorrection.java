package ca.mcgill.ecse211.odometerlab;

import lejos.hardware.Sound;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;
import lejos.robotics.filter.MedianFilter;

public class OdometryCorrection extends Thread {
	private static final long CORRECTION_PERIOD = 10;
	private Odometer odometer;
	private float[] sample;
	private float prev_light_level;
	private SampleProvider ambientLight;
	private SampleProvider median;
	private EV3ColorSensor sensor;
	long line_detected_time;
	private int line_count;

	private static final double GRID_LENGTH = 30.48; // in cm
	private static final double VELOCITY = 7.33;
	private final boolean debug_mode = false;

	public enum dir {
		ZERO, NINETY, ONEEIGHTY, TWOSEVENTY
	};

	private dir cur_dir;

	private double[] prev_pos;

	// constructor
	public OdometryCorrection(Odometer odometer/* , ColorPoller col */) {
		this.odometer = odometer;
		// this.colPoller = col;
		Port port = LocalEV3.get().getPort("S1");
		sensor = new EV3ColorSensor(port);
		SensorModes colorSensor = sensor;
		ambientLight = colorSensor.getMode("Red");
		median = new MedianFilter(ambientLight, 5);
		sample = new float[median.sampleSize()];
		sensor.setFloodlight(false);
		prev_pos = new double[3];
		line_count = 0;
		line_detected_time = 0;
	}

	// run method (required for Thread)
	public void run() {
		long correctionStart, correctionEnd;
		while (true) {
			correctionStart = System.currentTimeMillis();
			median.fetchSample(sample, 0);
			float light_level = sample[0];
			assert (sample[0] > 0);

			// Changed the light level
			if (light_level > 0.1 && light_level < 0.5 && System.currentTimeMillis() - line_detected_time > 3000) {
				// System.out.println("Line detected");
				Sound.beep();
				if (line_count > 0 && line_count % 3 != 0) {

					// TODO: implement a direction enum

					double delta_x = 0, delta_y = 0, travelled_dist = 0;
					double cur_theta = (prev_pos[2] + odometer.getTheta()) / 2; // Average with past value to reduce
																				// random variations in the angle
					cur_dir = getDir(Math.toDegrees(cur_theta));
					switch (cur_dir) {
					case ZERO:
						delta_y = GRID_LENGTH;
						double angle_zero = computeAngle(90 - Math.toDegrees(cur_theta));
						delta_x = -1 * (delta_y * Math.sin(cur_theta)) / Math.sin(angle_zero);
						break;
					case NINETY:
						delta_x = GRID_LENGTH;
						double angle_ninety = computeAngle(90 - (Math.toDegrees(cur_theta) - 90));
						delta_y = -1 * (delta_x * Math.sin(cur_theta - Math.PI / 2)) / Math.sin(angle_ninety);
						break;
					case ONEEIGHTY:
						delta_y = -GRID_LENGTH;
						double angle_oneeighty = computeAngle(90 - (Math.toDegrees(cur_theta) - 180));
						delta_x = -1 * (delta_y * Math.sin(cur_theta - 2 * Math.PI)) / Math.sin(angle_oneeighty);
						break;
					case TWOSEVENTY:
						delta_x = -GRID_LENGTH;
						double angle_twoseventy = computeAngle(90 - (Math.toDegrees(cur_theta) - 270));
						delta_y = -1 * (delta_x * Math.sin(cur_theta - (3 * Math.PI) / 2)) / Math.sin(angle_twoseventy);
						break;
					}

					setNewPos(prev_pos[0] + delta_x, prev_pos[1] + delta_y);
				} else if (line_count == 0) {
					// first time we cross a line means origin in y
					odometer.setY(0.0);
				} else if (line_count == 3) {
					// When crossing the 4th line, origin in x
					odometer.setX(0.0);
				} else if (line_count == 8) {
					// reached y = 0 again
					odometer.setY(0.0);
				} else if (line_count == 11) {
					// reached x = 0 again
					odometer.setX(0.0);
				}
				updatePrevPos();
				line_detected_time = System.currentTimeMillis(); // Capture the last time we crossed a line.
				line_count++;
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

		if (debug_mode) {
			System.out.println("Set Previous X: " + prev_pos[0]);
			System.out.println("Set Previous Y: " + prev_pos[1]);
			System.out.println("Set Previous Theta: " + Math.toDegrees(prev_pos[2]));
		}
	}

	private synchronized void setNewPos(double x, double y/* , double theta */) {
		odometer.setX(x);
		odometer.setY(y);
		// odometer.setTheta(theta);
	}

	private double computeAngle(double t_rad) {
		double t_deg = Math.toDegrees(t_rad);
		if (t_deg > 359.99999999 && t_deg >= 0) {
			t_deg = t_deg - 360;
		} else if (t_deg < 0) {
			t_deg = 360 + t_deg;
		}

		return Math.toRadians(t_deg);
	}

	// Approximate the direction of the robot
	private dir getDir(double t_deg) {
		double error = 8.0;
		if (t_deg + error >= 0 && t_deg - error <= 0) {
			return dir.ZERO;
		} else if (t_deg + error >= 360 && t_deg - error <= 360) {
			return dir.ZERO;
		} else if (t_deg + error >= 90 && t_deg - error <= 90) {
			return dir.NINETY;
		} else if (t_deg + error >= 180 && t_deg - error <= 180) {
			return dir.ONEEIGHTY;
		} else if (t_deg + error >= 270 && t_deg - error <= 270) {
			return dir.TWOSEVENTY;
		}
		// That should not happen
		return dir.ZERO;
	}
}
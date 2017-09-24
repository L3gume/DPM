package ca.mcgill.ecse211.odometerlab;

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
				if (line_count > 0 && line_count % 3 != 0) {
					// Compute the distance travelled
					double delta_t = System.currentTimeMillis() - line_detected_time;
					double distance_travelled = VELOCITY * (delta_t / 1000);
					if (debug_mode)
						System.out.println("Distance travelled since last line: " + distance_travelled + " in "
								+ delta_t / 1000 + "s");

					// Knowing the distance travelled and the theta from the previous line, compute
					// the x and y variations
					double delta_x = distance_travelled * Math.sin(prev_pos[2]);
					double delta_y = distance_travelled * Math.cos(prev_pos[2]);

					// Now we know the distance we travelled, our Y (or X) variation, we only need
					// theta
					// depending on our previous know orientation, determine a new one with the
					// distance travelled and the grid length.

					/*double new_theta = 0.0;
					if (Math.abs(Math.cos(prev_pos[2])) >= 0.707106) {
						if (Math.cos(prev_pos[2]) >= 0) {
							delta_y = 30.48;
							delta_x = delta_y * Math.tan(odometer.getTheta());
							// new_theta = Math.atan(delta_x / delta_y);
						} else if (Math.cos(prev_pos[2]) < 0) {
							delta_y = -30.48;
							delta_x = delta_y * Math.tan(odometer.getTheta());
							// new_theta = Math.atan(delta_x / delta_y);
						}
					} else if (Math.abs(Math.sin(prev_pos[2])) >= 0.707106) {
						if (Math.sin(prev_pos[2]) >= 0) {
							delta_x = 30.48;
							delta_y = delta_x * Math.tan(odometer.getTheta());
							// new_theta = Math.atan(delta_y / delta_x);
						} else if (Math.sin(prev_pos[2]) < 0) {
							delta_x = -30.48;
							delta_y = delta_x * Math.tan(odometer.getTheta());
							// new_theta = Math.atan(delta_y / delta_x);
						}
					}*/

					// if (debug_mode) System.out.println("New theta: " +
					// Math.toDegrees(computeAngle(new_theta)));
					/* if (new_theta != 0.0) System.out.println("new theta = 0!"); */

					setNewPos(prev_pos[0] + delta_x, prev_pos[1] + delta_y/*,
							Double.isNaN(new_theta) ? prev_pos[2] : computeAngle(new_theta)*/);
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

	private synchronized void setNewPos(double x, double y/*, double theta*/) {
		odometer.setX(x);
		odometer.setY(y);
		//odometer.setTheta(theta);
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

}
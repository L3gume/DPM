package ca.mcgill.ecse211.wallfollowing;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class PController implements UltrasonicController {

	// member constants
	private final int FILTER_COUNT = 20;
	private final int FILTER_DISTANCE = 70;
	private final int MOTOR_SPEED = 170;
	private final int RIGHT_SCALE = 4;
	private final double ERROR_SCALE = 1.5;
	private final int MAX_SPEED = 250;

	private final String TURN_RIGHT = "TURN_RIGHT";
	private final String TURN_LEFT = "TURN_LEFT";
	private final String NO_TURN = "NO_TURN";
	private final String BACKWARDS = "BACKWARDS";
	private String status;

	// passed member constants
	private final int bandCenter, bandwidth;
	private EV3LargeRegulatedMotor leftMotor, rightMotor;

	// member variables
	private int distance;
	private int filterControl;
	private int distError = 0;

	private final int ARRAY_LENGTH = 5;
	private int pastValues[] = new int[ARRAY_LENGTH];

	// Default Constructor
	public PController(EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor, int bandCenter,
			int bandwidth) {

		// Initialize Member Variables
		this.bandCenter = bandCenter;
		this.bandwidth = bandwidth;
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
		this.filterControl = 0;
		this.status = NO_TURN;

		// Start rolling!
		leftMotor.setSpeed(MOTOR_SPEED);
		rightMotor.setSpeed(MOTOR_SPEED);
		leftMotor.forward();
		rightMotor.forward();
	}

	@Override
	public void processUSData(int sensorDistance) {
		// Filter used to delay when making big changes (ie sharp corners)
		sensorDistance /= 1.3;
		
		if ((sensorDistance >= FILTER_DISTANCE && this.filterControl < FILTER_COUNT) || sensorDistance < 0) {
			// bad value, do not set the sensorDistance var, however do increment the filter
			// value
			this.filterControl++;
		} else if (sensorDistance >= FILTER_DISTANCE) {
			// set sensorDistance to FILTER_DISTANCE
			this.filterControl = 0;
			this.distance = getAveragedReading(sensorDistance);
		} else if (sensorDistance > 0) {
			// sensorDistance went below FILTER_DISTANCE, therefore reset everything.
			this.filterControl = 0;
			this.distance = getAveragedReading(sensorDistance);
		}

		// Calculate the distance Error from the bandCenter
		distError = (bandCenter + 10) - this.distance;

		// Compute motor correction speeds (variableRate)
		int variableRate = (int)ERROR_SCALE * Math.abs(distError);

		if (distance >= 0 && distance < 5) {
			leftMotor.setSpeed(MOTOR_SPEED * 2);
			rightMotor.setSpeed(MOTOR_SPEED * 2);
			leftMotor.backward();
			rightMotor.backward();
			setStatus(BACKWARDS);
			return;
		}


		// Travel straight
		if (Math.abs(distError) <= bandwidth) {
			leftMotor.setSpeed(MOTOR_SPEED);
			rightMotor.setSpeed(MOTOR_SPEED);
			leftMotor.forward();
			rightMotor.forward();
			setStatus(NO_TURN);
		} else if (distError > 0) {

			// RIGHT_SCALE accounts for distError being disproportional from one side to the
			// other side of the bandCenter
			turnRight(variableRate);
			setStatus(TURN_RIGHT);
		} else if (distError < 0) {

			turnLeft(variableRate);
			setStatus(TURN_LEFT);
		}
	}

	@Override
	public int readUSDistance() {
		return this.distance;
	}

	@Override
	public String getStatus() {
		return status;
	}

	private void setStatus(String s) {
		status = s;
	}

	private void turnRight(int variableRate) {
		int leftSpeed = MOTOR_SPEED + (variableRate /** 4*/ * RIGHT_SCALE);
		int rightSpeed = MOTOR_SPEED - (variableRate /** 4*/ * RIGHT_SCALE);

		/*
		 * if (leftSpeed > MAX_SPEED) { leftSpeed = MAX_SPEED + 100; } else if
		 * (leftSpeed < 0) { leftSpeed = 50; }
		 * 
		 * if (rightSpeed > MAX_SPEED) { rightSpeed = MAX_SPEED; } else if (rightSpeed <
		 * 0) { rightSpeed = 50; }
		 */

		if (Math.abs(leftSpeed) > MAX_SPEED) {
			leftSpeed = leftSpeed * MAX_SPEED / Math.abs(leftSpeed);
		}
		if (Math.abs(rightSpeed) > MAX_SPEED) {
			rightSpeed = rightSpeed * MAX_SPEED / Math.abs(rightSpeed);
		}

		leftMotor.setSpeed(Math.abs(leftSpeed));
		rightMotor.setSpeed(Math.abs(rightSpeed));
		if (leftSpeed > 0) {
			leftMotor.forward();
		} else {
			leftMotor.backward();
		}
		if (rightSpeed > 0) {
			rightMotor.forward();
		} else {
			rightMotor.backward();
		}

	}

	public void turnLeft(int variableRate) {
		int leftSpeed = MOTOR_SPEED - variableRate;
		int rightSpeed = MOTOR_SPEED + variableRate;

		/*
		 * if (leftSpeed > MAX_SPEED) { leftSpeed = MAX_SPEED + 100; } else if
		 * (leftSpeed < 0) { leftSpeed = 50; }
		 * 
		 * if (rightSpeed > MAX_SPEED) { rightSpeed = MAX_SPEED; } else if (rightSpeed <
		 * 0) { rightSpeed = 50; }
		 */

		if (Math.abs(leftSpeed) > MAX_SPEED) {
			leftSpeed = leftSpeed * MAX_SPEED / Math.abs(leftSpeed);
		}
		if (Math.abs(rightSpeed) > MAX_SPEED) {
			rightSpeed = rightSpeed * MAX_SPEED / Math.abs(rightSpeed);
		}

		leftMotor.setSpeed(Math.abs(leftSpeed));
		rightMotor.setSpeed(Math.abs(rightSpeed));
		if (leftSpeed > 0) {
			leftMotor.forward();
		} else {
			leftMotor.setSpeed(60);
			leftMotor.forward();
		}
		if (rightSpeed > 0) {
			rightMotor.forward();
		} else {
			rightMotor.backward();
		}
	}

	public int getAveragedReading(int val) {
		int sum = 0, j = 0;
		for (int i = 0; i < pastValues.length - 1; i++) {
			if (pastValues[i] != 0) {
				j++;
			}
			pastValues[i + 1] = pastValues[i];
			sum += pastValues[i + 1];
		}

		pastValues[0] = val;

		if (j == 0) {
			j = 1;
		}

		int avg = (int) ((sum + val) / j);
		return Math.min(255, Math.abs(avg));
	}

}
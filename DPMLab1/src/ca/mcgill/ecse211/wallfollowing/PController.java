package ca.mcgill.ecse211.wallfollowing;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class PController implements UltrasonicController {

	// member constants
	private final int FILTER_COUNT = 10;
	private final int FILTER_DISTANCE = 70;
	private final int MOTOR_SPEED = 200;
	private final int RIGHT_SCALE = 4;
	private final double ERROR_SCALE = 1.7;
	private final int MAX_SPEED = 200;
	private final int ADJUST_COUNTER = 40;

	private final String TURN_RIGHT = "TURN_RIGHT";
	private final String TURN_LEFT = "TURN_LEFT";
	private final String NO_TURN = "NO_TURN";
	private final String BACKWARDS = "BACKWARDS";
	private final String ADJUST_LEFT = "ADJUST_LEFT";
	private String status;

	// passed member constants
	private final int bandCenter, bandwidth;
	private EV3LargeRegulatedMotor leftMotor, rightMotor;

	// member variables
	private int distance;
	private int filterControl;
	private int distError = 0;
	private int distanceAdjust = 0;
	private int rightCompensation = 4;
	private int rightTurnSpeedMult = 2;
	private int adjustCounter = 0;
	private boolean adjusted = false;

	private final int ARRAY_LENGTH = 5;
	private int pastValues[] = new int[ARRAY_LENGTH];

	// Default Constructor
	public PController(EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor, int bandCenter,
			int bandwidth) {

		// Initialize Member Variables
		this.bandCenter = bandCenter;
		this.bandwidth = 2;// bandwidth;
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
		//sensorDistance /= 1.5;

		if ((sensorDistance >= FILTER_DISTANCE && this.filterControl < FILTER_COUNT) || sensorDistance < 0) {
			// bad value, do not set the sensorDistance var, however do increment the filter
			// value
			this.filterControl++;
		} else if (sensorDistance >= FILTER_DISTANCE) {
			// set sensorDistance to FILTER_DISTANCE
			this.distance = 70;
			this.filterControl = 0;
			this.distance = /*sensorDistance;*/  getAveragedReading(sensorDistance);
		} else if (sensorDistance > 0) {
			// sensorDistance went below FILTER_DISTANCE, therefore reset everything.
			this.filterControl = 0;
			this.distance = /*sensorDistance;*/ getAveragedReading(sensorDistance);
		}

		if (distance > 30) {
			if (adjustCounter++ > ADJUST_COUNTER) {
				leftAdjust();
				setStatus(ADJUST_LEFT);
				return;
			}
		} else {
			adjustCounter = 0;
		}
		
		// Calculate the distance Error from the bandCenter
		distError = (bandCenter - distanceAdjust) - (this.distance);

		// Compute motor correction speeds (variableRate)
		float variableRate = (float) (ERROR_SCALE * Math.abs(distError));

		if (distance >= 0 && distance < 10) {
			backward();
			setStatus(BACKWARDS);
			return;
		}

		// Travel straight
		if (Math.abs(distError) <= bandwidth) {
			forward();
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

	private void turnRight(float variableRate) {
		variableRate *= 2;
		float leftSpeed = MOTOR_SPEED + (variableRate /** 4 */
				* RIGHT_SCALE);
		float rightSpeed = MOTOR_SPEED - (variableRate /** 4 */
				* RIGHT_SCALE);

		/*
		 * if (leftSpeed > MAX_SPEED) { leftSpeed = MAX_SPEED + 100; } else if
		 * (leftSpeed < 0) { leftSpeed = 50; }
		 * 
		 * if (rightSpeed > MAX_SPEED) { rightSpeed = MAX_SPEED; } else if (rightSpeed <
		 * 0) { rightSpeed = 50; }
		 */

		if (Math.abs(leftSpeed) > MAX_SPEED) {
			leftSpeed = (leftSpeed * rightTurnSpeedMult * MAX_SPEED) / Math.abs(leftSpeed);
		}
		if (Math.abs(rightSpeed) > MAX_SPEED) {
			rightSpeed = (rightSpeed * rightTurnSpeedMult * MAX_SPEED) / Math.abs(rightSpeed);
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

	private void turnLeft(float variableRate) {
		float leftSpeed = MOTOR_SPEED - variableRate;
		float rightSpeed = MOTOR_SPEED + variableRate;

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
			leftMotor.setSpeed(100);
			leftMotor.forward();
			
			//leftMotor.backward();
		}
		if (rightSpeed > 0) {
			rightMotor.forward();
		} else {
			rightMotor.backward();
		}
	}

	private void forward() {
		leftMotor.setSpeed(MOTOR_SPEED);
		rightMotor.setSpeed(MOTOR_SPEED);
		leftMotor.forward();
		rightMotor.forward();
	}

	private void backward() {
		leftMotor.setSpeed(MOTOR_SPEED * 2);
		rightMotor.setSpeed(MOTOR_SPEED * 4);
		leftMotor.backward();
		rightMotor.backward();
	}

	private void leftAdjust() {
		/*leftMotor.setSpeed(50);
		rightMotor.setSpeed(50);
		leftMotor.backward();
		rightMotor.forward();*/
		
		leftMotor.setSpeed(50);
		rightMotor.setSpeed(150);
		leftMotor.forward();
		rightMotor.forward();
	}

	public int getAveragedReading(int val) {
		int sum = 0, j = 0;
		for (int i = 0; i < pastValues.length - 1; i++) {
			if (pastValues[i] != 0) {
				j++;
				sum += pastValues[i];
			}
			pastValues[i + 1] = pastValues[i];
		}

		pastValues[0] = val;

		if (j == 0) {
			j = 1;
		}

		int avg = (int) ((sum + val) / j);
		return Math.min(255, Math.abs(avg));
	}

}
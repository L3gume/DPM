package ca.mcgill.ecse211.wallfollowing;

import javax.swing.plaf.ActionMapUIResource;

import lejos.hardware.motor.*;

@SuppressWarnings("unused")
public class BangBangController implements UltrasonicController {

	private final String TURN_RIGHT = "TURN_RIGHT";
	private final String QUICK_TURN_RIGHT = "QUICK_TURN_RIGHT";
	private final String TURN_LEFT = "TURN_LEFT";
	private final String QUICK_TURN_LEFT = "QUICK_TURN_LEFT"; // This might never be necessary
	private final String NO_TURN = "NO_TURN";
	private final String BACKWARDS = "BACKWARDS";

	private final int FILTER_DISTANCE = 70;
	private final int FILTER_COUNT = 10;
	
	private final int bandCenter;
	private final int bandwidth;
	private final int motorLow;
	private final int motorHigh;
	private int distance;
	
	private int filterCount = 0;
	
	private final int ARRAY_LENGTH = 7;
	private int pastValues[] = new int[ARRAY_LENGTH];

	private String status;

	public BangBangController(int bandCenter, int bandwidth, int motorLow, int motorHigh) {
		// Default Constructor
		this.bandCenter = bandCenter;
		this.bandwidth = bandwidth;
		this.motorLow = motorLow;
		this.motorHigh = motorHigh;
		this.status = NO_TURN;
		WallFollowingLab.leftMotor.setSpeed(motorHigh); // Start robot moving forward
		WallFollowingLab.rightMotor.setSpeed(motorHigh);
		WallFollowingLab.leftMotor.forward();
		WallFollowingLab.rightMotor.forward(); 
		
		for (int i = 0; i < pastValues.length; i++) {
			pastValues[i] = 0;
		}
	}

	@Override
	public void processUSData(int distance) {
		distance /= 1.3f;
		this.distance = distance;
		float actualDist = 0; //= (float)getAveragedReading(distance) / 1.3f;
		
		if ((distance >= FILTER_DISTANCE && this.filterCount < FILTER_COUNT) || distance < 0) {
			// bad value, do not set the sensorDistance var, however do increment the filter
			// value
			this.filterCount++;
		} else if (distance >= FILTER_DISTANCE) {
			// set sensorDistance to FILTER_DISTANCE
			this.filterCount = 0;
			actualDist = (float)getAveragedReading(distance);
		} else if (distance > 0) {
			// sensorDistance went below FILTER_DISTANCE, therefore reset everything.
			this.filterCount = 0;
			actualDist = getAveragedReading(distance);
		}
		
		if (actualDist <= 0) {
			return;
		}
		
		// TODO: process a movement based on the us distance passed in (BANG-BANG style)
		final int error = (int) actualDist - bandCenter;

		// TODO: implement some sort of correction function to smooth out the input.

		if (status.equals(TURN_RIGHT)) {
			if (actualDist < 15) {
				setStatus(QUICK_TURN_RIGHT);
				quickTurnRight();
				return;
			} else if (actualDist >= 15 && actualDist < 30) {
				setStatus(TURN_RIGHT);
				turnRight();
				return; // Keep turning right to avoid errors
			}
		}
		
		if (actualDist < 5) {
			setStatus(BACKWARDS);
			backwardsAdjust();
			return;
		}

		// System.out.println("Error:" + error + " Distance: " + distance);
		if (Math.abs(error) < bandwidth) {
			// We are on the right trajectory, just keep going
			setStatus(NO_TURN);
			setHighSpeed();
		} else if (error > 0 && error < 30) {
			// We are too far from the wall, turn left
			// We also have to make sure this is not one of those 10cm holes
			// That seems to not be an issue actually
			setStatus(TURN_LEFT);
			turnLeft();
		} else if (error >= 15){
			setStatus(QUICK_TURN_LEFT);
			quickTurnLeft();
		} else if (error < 0) {
			// We are too close to the wall, turn right
			// we have to make sure we turn fast enough if we encounter a wall right in
			// front of us.
			setStatus(TURN_RIGHT);
			turnRight();
		}

		filterCount = 0;
	}

	@Override
	public int readUSDistance() {
		return this.distance;
	}

	private void setHighSpeed() {
		WallFollowingLab.leftMotor.setSpeed(motorHigh); // Start robot moving forward
		WallFollowingLab.rightMotor.setSpeed(motorHigh);
		WallFollowingLab.leftMotor.forward();
		WallFollowingLab.rightMotor.forward();
	}

	private void turnLeft() {
		WallFollowingLab.leftMotor.setSpeed(motorLow); // Start robot moving forward
		WallFollowingLab.rightMotor.setSpeed(motorHigh);
		WallFollowingLab.leftMotor.forward();
		WallFollowingLab.rightMotor.forward();
	}

	private void quickTurnLeft() {
		WallFollowingLab.leftMotor.setSpeed(motorLow); // Start robot moving forward
		WallFollowingLab.rightMotor.setSpeed(motorHigh + 50);
		WallFollowingLab.leftMotor.forward();
		WallFollowingLab.rightMotor.forward();
	}

	private void turnRight() {
		WallFollowingLab.leftMotor.setSpeed(motorHigh); // Start robot moving forward
		WallFollowingLab.rightMotor.setSpeed(motorLow + 50);
		WallFollowingLab.leftMotor.forward();
		WallFollowingLab.rightMotor.forward();
	}

	private void quickTurnRight() {
		WallFollowingLab.leftMotor.setSpeed(motorHigh /** 2*/); // Start robot moving forward
		WallFollowingLab.leftMotor.forward();
		WallFollowingLab.rightMotor.setSpeed(motorHigh /** 2*/);
		WallFollowingLab.rightMotor.backward();
	}

	private void backwardsAdjust() {
		WallFollowingLab.leftMotor.setSpeed(motorHigh); // Start robot moving forward
		WallFollowingLab.rightMotor.setSpeed(motorHigh);
		WallFollowingLab.leftMotor.backward();
		WallFollowingLab.rightMotor.backward();
	}

	private void setStatus(String s) {
		status = s;
	}
	
	@Override
	public String getStatus() {
		return status;
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
		return Math.min(250, Math.abs(avg));
	}
	
}

package ca.mcgill.ecse211.wallfollowing;

import lejos.hardware.motor.*;

public class BangBangController implements UltrasonicController {

	private final String TURN_RIGHT = "TURN_RIGHT";
	private final String QUICK_TURN_RIGHT = "QUICK_TURN_RIGHT";
	private final String TURN_LEFT = "TURN_LEFT";
	private final String QUICK_TURN_LEFT = "QUICK_TURN_LEFT"; // This might never be necessary
	private final String NO_TURN = "NO_TURN";

	private final int bandCenter;
	private final int bandwidth;
	private final int motorLow;
	private final int motorHigh;
	private final int delta;
	private int distance;

	private String status;

	public BangBangController(int bandCenter, int bandwidth, int motorLow, int motorHigh) {
		// Default Constructor
		this.bandCenter = bandCenter;
		this.bandwidth = bandwidth;
		this.motorLow = motorLow;
		this.motorHigh = motorHigh;
		this.delta = 100;
		this.status = NO_TURN;
		WallFollowingLab.leftMotor.setSpeed(motorHigh); // Start robot moving forward
		WallFollowingLab.rightMotor.setSpeed(motorHigh);
		WallFollowingLab.leftMotor.forward();
		WallFollowingLab.rightMotor.forward();
	}

	@Override
	public void processUSData(int distance) {
		this.distance = distance;
		float actualDist = (float)distance / 1.6f;
		// TODO: process a movement based on the us distance passed in (BANG-BANG style)
		final int error = (int)actualDist - bandCenter;
		
		// TODO: implement some sort of correction function to smooth out the input.
		
		if (status.equals(TURN_RIGHT)) {
			if (actualDist < 10) {
				setStatus(QUICK_TURN_RIGHT);
				quickTurnRight();
				return;
			} else if (actualDist > 10 && actualDist < 50) {
				setStatus(TURN_RIGHT);
				turnRight();
				return; // Keep turning right to avoid errors
			}
		}

		// System.out.println("Error:" + error + " Distance: " + distance);
		if (Math.abs(error) < bandwidth) {
			// We are on the right trajectory, just keep going
			setStatus(NO_TURN);
			setHighSpeed();
		} else if (error > 0) {
			// We are too far from the wall, turn left
			// We also have to make sure this is not one of those 10cm holes
			// That seems to not be an issue actually
			setStatus(TURN_LEFT);
			turnLeft();
		} else if (error < 0) {
			// We are too close to the wall, turn right
			// we have to make sure we turn fast enough if we encounter a wall right in
			// front of us.
			setStatus(TURN_RIGHT);
			turnRight();
		}

		
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
		WallFollowingLab.leftMotor.setSpeed(motorHigh - delta); // Start robot moving forward
		WallFollowingLab.rightMotor.setSpeed(motorHigh + delta);
		WallFollowingLab.leftMotor.forward();
		WallFollowingLab.rightMotor.forward();
	}

	private void quickTurnLeft() {
		WallFollowingLab.leftMotor.setSpeed(motorHigh); // Start robot moving forward
		WallFollowingLab.rightMotor.setSpeed(motorHigh);
		WallFollowingLab.leftMotor.backward();
		WallFollowingLab.rightMotor.forward();
	}

	private void turnRight() {
		WallFollowingLab.leftMotor.setSpeed(motorHigh + delta); // Start robot moving forward
		WallFollowingLab.rightMotor.setSpeed(motorHigh - delta);
		WallFollowingLab.leftMotor.forward();
		WallFollowingLab.rightMotor.forward();
	}

	private void quickTurnRight() {
		WallFollowingLab.leftMotor.setSpeed(motorHigh); // Start robot moving forward
		WallFollowingLab.rightMotor.setSpeed(motorHigh);
		WallFollowingLab.leftMotor.forward();
		WallFollowingLab.rightMotor.backward();
	}

	private void setStatus(String s) {
		status = s;
	}

}

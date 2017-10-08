package ca.mcgill.ecse211.localizationlab;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class UltrasonicLocalizer {
	
	private EV3LargeRegulatedMotor leftMotor, rightMotor;

	private static final int ROTATE_SPEED = 150;
	
	private final double WHEEL_RAD = 2.1;

	private final double WHEELBASE = 15.75;

	private int choice;


	public UltrasonicLocalizer(int choice, EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor){

		this.choice = choice;
		
		this.leftMotor = leftMotor;
		
		this.rightMotor = rightMotor;
		
		// Start rolling!


		
	}
	
	
	public void fallingEdge(){
		
	}
	
	public void risingEdge(){
		
	}
	
}

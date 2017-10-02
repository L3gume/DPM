# README

The Navigation class is implemented slightly differently than how the Lab outline requiresit to be.

Instead of having a travelTo() method that calls the turnTo() method when required, this functionnality is separated into two separate methods: process_moving() and process_rotating() which respectively call the rotate() and moveTo() from a Driver object, which handles moving the robot without locking the navigation thread.

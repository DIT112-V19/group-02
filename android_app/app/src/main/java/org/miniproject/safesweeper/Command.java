package org.miniproject.safesweeper;

public class Command {

    //Car commands
    public static final String MOVE_FORWARD = "0";
    public static final String STAND_STILL = "4";
    public static final String MOVE_BACKWARD = "1";
    public static final String STEER_RIGHT = "3"; //consider changing these as well?
    public static final String STEER_LEFT = "2"; // ""
    public static final String SHARP_RIGHT = "a"; //left and right as letters for clarity and ease
    public static final String SHARP_LEFT = "b"; //while programming

    //Magic numbers
    public static final int SPEED_0 = 0;
    public static final int SPEED_1 = 25;
    public static final int SPEED_2 = 50;
    public static final int SPEED_3 = 75;
    public static final int SPEED_4 = 100;


    public static byte[] speed (int speedValue) {
        String command = "zero";
        //for now we only use one speed

        if (speedValue > SPEED_1){ //go forward when seekbar is above 25%
            command = MOVE_FORWARD;
        }else if (speedValue > SPEED_2){ // increase speed
            command = MOVE_FORWARD;
        }else if (speedValue > SPEED_3){ // increase speed
            command = MOVE_FORWARD;
        }else if (speedValue == SPEED_4){ //MAXIMUM SPEED INITIALIZED - Testing with 0
            command = STAND_STILL;
        }else if (speedValue == SPEED_0){ //stand still
            command = STAND_STILL;
        }else if (speedValue < SPEED_0){ //go backwards
            command = MOVE_BACKWARD;
        }

        return command.getBytes();
    }

    public static byte[] steer (int steerValue){
        String command;

        if (steerValue > 0 && steerValue < 50) { //go right
            command = STEER_RIGHT;
        }else if (steerValue > 50){ //sharp right turn
            command = SHARP_RIGHT;
        } else if (steerValue < 0 && steerValue > -50) { //go left
            command = STEER_LEFT;
        }else if (steerValue < -50 ){  //sharp left turn
            command = SHARP_LEFT;
        } else { //stop
            command = STAND_STILL;
        }

        return command.getBytes();
    }

}

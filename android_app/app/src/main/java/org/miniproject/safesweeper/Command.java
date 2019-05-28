package org.miniproject.safesweeper;

public class Command {

    //Car commands//
    public static final String MOVE_FSPEED1 = "a";
    public static final String MOVE_FSPEED2 = "b";
    public static final String MOVE_FSPEED3 = "d";
    public static final String MOVE_FSPEED4 = "e";
    public static final String STAND_STILL = "k";
    public static final String MOVE_BACKWARD = "f";
    public static final String STEER_RIGHT = "i"; //consider changing these as well?
    public static final String STEER_LEFT = "h"; // ""
    public static final String SHARP_RIGHT = "j"; //left and right as letters for clarity and ease
    public static final String SHARP_LEFT = "g"; //while programming

    //Magic numbers
    public static final int SPEED_0 = 0;
    public static final int SPEED_1 = 25;
    public static final int SPEED_2 = 50;
    public static final int SPEED_3 = 75;
    public static final int SPEED_4 = 100;


    public static byte[] speed (int speedValue) {
        String command = "zero";


        if (speedValue > SPEED_1 && speedValue < SPEED_2){ //go forward when seekbar is above 25%
            command = MOVE_FSPEED1;

        }else if (speedValue > SPEED_2 && speedValue < SPEED_3){ // increase speed
            command = MOVE_FSPEED2;

        }else if (speedValue > SPEED_3 && speedValue < SPEED_4){ // increase speed
            command = MOVE_FSPEED3;

        }else if (speedValue == SPEED_4){ //MAXIMUM SPEED INITIALIZED
            command = MOVE_FSPEED4;

        }else if (speedValue >= 0 && speedValue < SPEED_1){ //stand still
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
        }else if (steerValue == 50){ //sharp right turn
            command = SHARP_RIGHT;
        } else if (steerValue < 0 && steerValue > -50) { //go left
            command = STEER_LEFT;
        }else if (steerValue == -50 ){  //sharp left turn
            command = SHARP_LEFT;
        } else { //stop
            command = STAND_STILL;
        }

        return command.getBytes();
    }

}

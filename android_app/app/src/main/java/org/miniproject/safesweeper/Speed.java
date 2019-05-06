package org.miniproject.safesweeper;

public class Speed {

    public static byte[] speedCommand (int speedValue) {

        String command = "null";

        //for now we only use one speed

        if (speedValue > 25){ //go forward when seekbar is above 25%
            command = "0";

        }else if (speedValue > 50){ // increase speed
            command = "0";

        }else if (speedValue > 75){ // increase speed
            command = "0";

        }else if (speedValue == 100){ //MAXIMUM SPEED INITIALIZED - for now it will only stop
            command = "4";

        }else if (speedValue == 0){ //stand still
            command = "4";

        }else if (speedValue < 0){ //go backwards
            command = "1";

        }


        return command.getBytes();
    }

}

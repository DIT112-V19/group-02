package org.miniproject.safesweeper;

public class Steer {

    public static byte[] steerCommand (int steerValue){

        String command = "null";

        //for now we only have one option to steer

        if (steerValue > 0){ //go right
            command = "3";

        }else if (steerValue > 50){ //go HARD right
            command = "3";

        }else if (steerValue < 0){ //go left
            command = "2";

        }else if (steerValue < -50){ //go HARD left
            command = "2";

        }else if (steerValue == 0){ //stop
            command = "4";

        }

        return command.getBytes();
    }

}

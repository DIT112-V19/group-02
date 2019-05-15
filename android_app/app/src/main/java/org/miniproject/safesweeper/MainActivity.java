package org.miniproject.safesweeper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Button getLocationBtn;
    Button mineBtn;
    TextView textView1;
    SeekBar throttleBar;
    SeekBar steeringBar;
    TextView throttleText;
    TextView steeringText;
    TextView connectionTextView;
    TextView locationText;
    ToggleButton steeringToggle;

    public static final int THROTTLE_MIN = -30;
    public static final int THROTTLE_DEFAULT = 30;

    public static final int STEERING_MIN = -50;
    public static final int STEERING_DEFAULT = 50;

    //Car commands
    public static final String STAND_STILL = "0";
    public static final String AUTOMATIC_MODE = "7";
    public static final String MANUAL_MODE = "6";
    public static final String GET_LOCATION = "c";
    public static final String ACKNOWLEDGE_MINE = "m";

    //inputs from car
    public static final String LOCATION_REGEX = "c-?\\d+\\.\\d+\\s-?\\d+\\.\\d+/";
    public static final String MINE_REGEX = "m";
    public static final String LAT_LNG_SEPARATOR = "\\s";
    public static final String END_OF_INPUT = "/";

    public static final String DOUBLE_WITH_DECIMALS_REGEX = "\\d+\\.\\d+";

    private int speedValue;
    private int steerValue;
    private String command;
    private byte[] commandTest;

    private String macAddress;
    BluetoothAdapter mBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private OutputStream outputStream;
    private InputStream inputStream;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        macAddress = getIntent().getStringExtra("MAC");

        textView1 = (TextView) findViewById(R.id.textView1);
        throttleBar = (SeekBar) findViewById(R.id.throttleBar);
        steeringBar = (SeekBar) findViewById(R.id.steeringBar);
        throttleText = (TextView) findViewById(R.id.throttleText);
        steeringText = (TextView) findViewById(R.id.steeringText);
        connectionTextView = (TextView) findViewById(R.id.connectionTextView);
        locationText = (TextView) findViewById(R.id.locationText);
        steeringToggle = (ToggleButton) findViewById(R.id.steeringToggle);
        getLocationBtn = (Button) findViewById(R.id.getLocationBtn);
        mineBtn = (Button) findViewById(R.id.mineBtn);

        //Start task to connect with cars bluetooth asynchronously
        new ConnectBT().execute();

    }


    private class ConnectBT extends AsyncTask<Void, Void, Void>
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute() {
            connectionTextView.setText("Connecting.. Please wait!");
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try {
                if (btSocket == null || !isBtConnected) {
                    mBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice mBluetoothDevice = mBluetooth.getRemoteDevice(macAddress);//connects to the device's address and checks if it's available
                    btSocket = mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            } catch (IOException e) {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            setConnectionTextView("");
            //connectionTextView.setText("");

            if (!ConnectSuccess) {
                Toast.makeText(MainActivity.this, "Could not connect..", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(MainActivity.this, BluetoothActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Connected!", Toast.LENGTH_SHORT).show();
                isBtConnected = true;

                try {
                    outputStream = btSocket.getOutputStream();
                    inputStream = btSocket.getInputStream();

                } catch (IOException exc) {
                    Log.e("IOException: ", exc.getMessage());
                }

                getLocationBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        writeToCar(GET_LOCATION);
                    }
                });

                throttleBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { //Speed controls
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        steeringBar.getProgress();
                        speedValue = THROTTLE_MIN + progress;
                        throttleText.setText(speedValue + "");
                        commandTest = Command.speed(speedValue);
                        writeToCarTest(commandTest);
                    }


                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        throttleBar.setProgress(THROTTLE_DEFAULT);
                        speedValue = 0; //Reset speed
                        command = STAND_STILL;
                        writeToCar(command);
                    }
                });

                //toggle switch to go between manual steering and automatic steering, starts with manual
                steeringToggle.setOnClickListener(new View.OnClickListener() { //Toggle between automatic and manual via different commands
                    @Override
                    public void onClick(View v) {
                        if (steeringToggle.isChecked()) {
                            textView1.setText("Automatic");
                            command = AUTOMATIC_MODE;
                        } else {
                            textView1.setText("Manual");
                            command = MANUAL_MODE;
                        }
                        writeToCar(command);
                    }
                });


                steeringBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()

                { //Steering controls
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        steerValue = STEERING_MIN + progress;
                        steeringText.setText(steerValue + "");

                        commandTest = Command.steer(steerValue);
                        writeToCarTest(commandTest);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        steeringBar.setProgress(STEERING_DEFAULT);
                        steerValue = 0; //Reset
                    }
                });

                getLocationBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        writeToCar(GET_LOCATION);
                    }
                });

                mineBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        writeToCar(ACKNOWLEDGE_MINE);
                        connectionTextView.setText("");
                        locationText.setText("");
                    }
                });

                //start a thread which will constantly check for inputs from the car
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            byte[] buffer = new byte[256];
                            int bytes;
                            String readMessage;

                            try {
                                if (inputStream.available() != 0) {
                                    bytes = inputStream.read(buffer);
                                    readMessage = new String(buffer, 0, bytes);
                                    handleInput(readMessage);
                                }
                            } catch (IOException exc) {
                                Log.e("IOException: ", exc.getMessage());
                            }
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException ie) {
                                // do nothing
                            }
                            continue;
                        }
                    }
                }).start();
            }
        }
    }

    private void writeToCar(String command) {
        try {
            outputStream.write(command.getBytes());
        } catch (IOException exc) {
            Log.e("IOException: ", exc.getMessage());
        }
    }

    private void writeToCarTest(byte[] commandTest) {
        try {
            outputStream.write(commandTest);
        } catch (IOException exc) {
            Log.e("IOException: ", exc.getMessage());
        }
    }

    private void handleInput(String input) {
        if(input.matches(MINE_REGEX)) {
            showMineDetected();
        }

        else if(input.matches(LOCATION_REGEX)){
            int indexOfSpace = input.indexOf(LAT_LNG_SEPARATOR);
            int lastIndex = input.indexOf(END_OF_INPUT);

            String locationStr  = input.substring(1, lastIndex);
            locationStr = extractLocation(locationStr);
            showLocation(locationStr);

            /* if we need the coordinates as double
            String latitudeStr = input.substring(1, indexOfSpace);
            String longitudeStr = input.substring(indexOfSpace + 1, lastIndex);
            double lat = convertToDouble(latitudeStr);
            double lng = convertToDouble(longitudeStr);
            */
        }
    }

    private double convertToDouble(String str){
        if(str.matches(DOUBLE_WITH_DECIMALS_REGEX)){
            return Double.parseDouble(str);
        }else{
            return 0.0;
        }
    }

    private void showLocation(String coordinates) {
        final String str = coordinates;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                locationText.setText(str);
            }
        });
    }

    private void showMineDetected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionTextView.setText("MINE DETECTED!");
            }
        });    }

    public void setConnectionTextView(String msg) {
        final String str = msg;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionTextView.setText(str);
            }
        });
    }

    public String extractLocation(String locationStr){
        String firstText = locationStr.substring(0,locationStr.indexOf(" "));
        String secondText = locationStr.substring(locationStr.indexOf(" ") + 1);

        firstText = convertLocation(firstText);
        secondText = convertLocation(secondText);

        return firstText + ", " + secondText;
    }

    public String convertLocation(String text){ //Eyuell
        String direction;
        if(text.charAt(0) == '-'){
            if(text.indexOf(".") == 3){
                direction = "S";
            } else {
                direction = "W";
            }
        } else {
            if(text.indexOf(".") == 2){
                direction = "N";
            } else {
                direction = "E";
            }
        }

        if(text.charAt(0) == '-')
            text = text.substring(1);

        double numFormat = Double.parseDouble(text);
        int degree = (int) numFormat;
        numFormat = ((numFormat - degree)*100.0);
        int minute = (int) numFormat;
        numFormat = ((numFormat - minute)*100.0);

        numFormat = numFormat * 10;
        numFormat = Math.round(numFormat);
        numFormat = numFormat / 10.0;

        return degree + "° " + minute + "′ " + numFormat + "″ " + direction;
    }
}

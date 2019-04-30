package org.miniproject.safesweeper;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.OutputStream;
import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Button btn1;
    Button getLocationBtn;
    TextView textView1;
    SeekBar throttleBar;
    SeekBar steeringBar;
    TextView throttleText;
    TextView steeringText;
    TextView connectionTextView;
    ToggleButton steeringToggle;

    public static final int THROTTLE_MIN = -30;
    public static final int THROTTLE_DEFAULT = 30;

    public static final int STEERING_MIN = -50;
    public static final int STEERING_DEFAULT = 50;

    //Car commands
    public static final String MOVE_FORWARD = "0";
    public static final String STAND_STILL = "4";
    public static final String MOVE_BACKWARD = "1";
    public static final String AUTOMATIC_MODE = "5";
    public static final String MANUAL_MODE = "6";
    public static final String STEER_RIGHT = "3";
    public static final String STEER_LEFT = "2";

    private int speedValue;
    private int steerValue;
    private String command;

    private String macAddress;
    BluetoothAdapter mBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private OutputStream outputStream;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        macAddress = getIntent().getStringExtra("MAC");

        btn1 = (Button) findViewById(R.id.btn1);
        textView1 = (TextView) findViewById(R.id.textView1);
        throttleBar = (SeekBar) findViewById(R.id.throttleBar);
        steeringBar = (SeekBar) findViewById(R.id.steeringBar);
        throttleText = (TextView) findViewById(R.id.throttleText);
        steeringText = (TextView) findViewById(R.id.steeringText);
        connectionTextView = (TextView) findViewById(R.id.connectionTextView);
        steeringToggle = (ToggleButton) findViewById(R.id.steeringToggle);
        //TODO initialize getLocationBtn and the "mine notification" element.

        new ConnectBT().execute();

    }


    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            connectionTextView.setText("Connecting.. Please wait!");
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    mBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice mBluetoothDevice = mBluetooth.getRemoteDevice(macAddress);//connects to the device's address and checks if it's available
                    btSocket = mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            connectionTextView.setText("");

            if (!ConnectSuccess)
            {
                Toast.makeText(MainActivity.this, "Could not connect..", Toast.LENGTH_LONG).show();
              //  Intent intent = new Intent(MainActivity.this, BluetoothActivity.class);
               // startActivity(intent);
            }
            else
            {
                Toast.makeText(MainActivity.this, "Connected!", Toast.LENGTH_SHORT).show();
                isBtConnected = true;

                try {
                    outputStream = btSocket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                throttleBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { //Speed controls
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        steeringBar.getProgress();
                        speedValue = THROTTLE_MIN + progress;
                        throttleText.setText(speedValue + "");

                        if (speedValue > 25){ //go forward when seekbar is above 25%
                            command = MOVE_FORWARD;
                            try {
                                outputStream.write(command.getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if (speedValue == 0){ //stand still
                            command = STAND_STILL;
                            try {
                                outputStream.write(command.getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else if (speedValue < 0){ //go backwards
                            command = MOVE_BACKWARD;
                            try {
                                outputStream.write(command.getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }


                    }


                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        throttleBar.setProgress(THROTTLE_DEFAULT);
                        speedValue = 0; //Reset speed
                    }
                });

                //toggle switch to go between manual steering and automatic steering, starts with manual
                steeringToggle.setOnClickListener(new View.OnClickListener() { //Toggle between automatic and manual via different commands
                    @Override
                    public void onClick(View v) {
                        if(steeringToggle.isChecked()){
                            textView1.setText("Automatic");
                            //auto();
                            command = AUTOMATIC_MODE;
                            try {
                                outputStream.write(command.getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                                textView1.setText("Something went wrong");
                            }

                        }else
                            textView1.setText("Manual");
                        command = MANUAL_MODE;
                        try {
                            outputStream.write(command.getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                });

                steeringBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { //Steering controls
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        steerValue = STEERING_MIN + progress;
                        throttleText.setText(steerValue + "");

                        if (steerValue > 0){ //go right
                            command = STEER_RIGHT;
                            try {
                                outputStream.write(command.getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else if (steerValue < 0){ //go left
                            command = STEER_LEFT;
                            try {
                                outputStream.write(command.getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else if (steerValue == 0){ //stop
                            command = STAND_STILL;
                            try {
                                outputStream.write(command.getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
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

            }
        }
    }
}

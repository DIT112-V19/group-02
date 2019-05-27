package org.miniproject.safesweeper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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
    public static final String LAT_LNG_SEPARATOR = " ";
    public static final String END_OF_INPUT = "/";

    public static final String DOUBLE_WITH_DECIMALS_REGEX = "-?\\d+\\.\\d+";

    public static final String COORDINATE_REGEX = "-?\\d+\\.\\d+";

    String lat1Text = "";
    String lat2Text = "";
    String lon1Text = "";
    String lon2Text = "";

    private int speedValue;
    private int steerValue;
    private String command;
    private byte[] commandTest;

    private ServerInfo serverInfo;
    private ServerConnection conn;
    private ArrayList<Mine> mines;
    private boolean mineIsDetected = false;


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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

        serverInfo = new ServerInfo();
        conn = new ServerConnection(serverInfo);
        mines = new ArrayList<Mine>();

        //Start task to connect with cars bluetooth asynchronously
        new ConnectBT().execute();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) { //menu created
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {   //when items of menu are selected
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();  //get the id of item selected

        switch (id){
            case R.id.action_addBoundary:   //id add date is selected
                openAddBoundaryActivity();
                break;
            default:    //R.id.action_settings or other
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute() {
            connectionTextView.setText("Connecting...");
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

    private boolean writeToCar(String command) {
        try {
            outputStream.write(command.getBytes());
            return true;
        } catch (IOException exc) {
            Log.e("IOException: ", exc.getMessage());
            return false;
        }
    }

    private boolean writeToCarTest(byte[] commandTest) {
        try {
            outputStream.write(commandTest);
            return true;
        } catch (IOException exc) {
            Log.e("IOException: ", exc.getMessage());
            return false;
        }
    }

    private int handleInput(String input) {

        if(input.matches(MINE_REGEX)) {
            showMineDetected();
            return 2;
        }

        else if(input.matches(LOCATION_REGEX)){
            int indexOfSpace = input.indexOf(LAT_LNG_SEPARATOR);
            int lastIndex = input.indexOf(END_OF_INPUT);

            String locationStr  = input.substring(1, lastIndex);

            String latitudeStr = input.substring(1, indexOfSpace);
            String longitudeStr = input.substring(indexOfSpace + 1, lastIndex);
            double lat = convertToDouble(latitudeStr);
            double lng = convertToDouble(longitudeStr);

            if(mineIsDetected) {
                new addMinesToDb().execute(lat, lng);
                mineIsDetected = false;
            }


            locationStr = extractLocation(locationStr);
            showLocation(locationStr);
            return 3;



        } else if(input.equals("x")){
            String locationInfo = "\n" + "\n" + "Fetching from satellite (Try Again)    ";
            showLocation(locationInfo);

        } else if (input.equals("y")){
            String locationInfo = "\n" + "\n" + "Inactive GPS-Module             ";
            showLocation(locationInfo);
        }
        return 0;
    }


    public class addMinesToDb extends AsyncTask<Double, Void, Mine> {

        @Override
        protected Mine doInBackground(Double... doubles) {
            double lat = doubles[0];
            double lng = doubles[1];
            Mine mine = new Mine(lat,lng);
            String location = conn.addMine(lat,lng);

            return mine;
        }

        @Override
        protected void onPostExecute(Mine mine) {
            mines.add(mine);
            Toast.makeText(getApplicationContext(),""+mine.getLat() + mine.getLng(),Toast.LENGTH_SHORT).show();
            //add marker on map here.

        }
    }

    public double convertToDouble(String str){
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
                locationText.setText("");   //in case a coordinate was displaying, to clear it
                connectionTextView.setText("MINE DETECTED!");
                mineIsDetected = true;
            }
        });


    }

    public void setConnectionTextView(String msg) {
        final String str = msg;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionTextView.setText(str);
            }
        });
    }

    //Preparing text to be displayed on location view
    public String extractLocation(String locationStr){
        String firstText = locationStr.substring(0,locationStr.indexOf(LAT_LNG_SEPARATOR));
        String secondText = locationStr.substring(locationStr.indexOf(LAT_LNG_SEPARATOR) + 1);
        String result = "\n" + "             Mine Location       " +"\n";   //not to conflict when shown together with 'detected' text

        firstText = convertLocation(firstText);
        secondText = convertLocation(secondText);

        return result + firstText + ", " + secondText + "      ";
    }

    //to display the location in DMS (degree, minute, second) format
    public String convertLocation(String text){
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


    public void openAddBoundaryActivity(){
        final AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        View mView = getLayoutInflater().inflate(R.layout.boundary_input, null);
        mBuilder.setCancelable(true);

        final TextInputEditText lat1Input = (TextInputEditText) mView.findViewById(R.id.lat1InputTxt);
        final TextInputEditText lat2Input = (TextInputEditText) mView.findViewById(R.id.lat2InputTxt);
        final TextInputEditText lon1Input = (TextInputEditText) mView.findViewById(R.id.lon1InputTxt);
        final TextInputEditText lon2Input = (TextInputEditText) mView.findViewById(R.id.lon2InputTxt);

        Button cancelButton = (Button) mView.findViewById(R.id.cancelButton);
        Button applyButton = (Button) mView.findViewById(R.id.insertButton);

        mBuilder.setView(mView);
        final AlertDialog dialog = mBuilder.create();
        dialog.show();

        applyButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                boolean notComplete = false;
                String lat1 = lat1Input.getText().toString().trim();
                String lat2 = lat2Input.getText().toString().trim();
                String lon1 = lon1Input.getText().toString().trim();
                String lon2 = lon2Input.getText().toString().trim();

                lat1 = limitDigit(lat1);
                lat2 = limitDigit(lat2);
                lon1 = limitDigit(lon1);
                lon2 = limitDigit(lon2);

                if(lat1.isEmpty()){
                    lat1Input.setError("Field is empty");
                    notComplete = true;
                } else if (!lat1.matches(COORDINATE_REGEX)){
                    lat1Input.setError("use format of 00.0000000");
                    notComplete = true;
                } else if (latitudeDigit(lat1)){
                    lat1Input.setError("latitude should be two digits max before decimal");
                    notComplete = true;
                }

                if(lat2.isEmpty()){
                    lat2Input.setError("Field is empty");
                    notComplete = true;
                } else if (!lat2.matches(COORDINATE_REGEX)){
                    lat2Input.setError("use format of 00.0000000");
                    notComplete = true;
                } else if (latitudeDigit(lat2)){
                    lat2Input.setError("latitude should be two digits max before decimal");
                    notComplete = true;
                }

                if(lon1.isEmpty()){
                    lon1Input.setError("Field is empty");
                    notComplete = true;
                } else if (!lon1.matches(COORDINATE_REGEX)){
                    lon1Input.setError("use format of 000.0000000");
                    notComplete = true;
                } else if (longitudeDigit(lon1)){
                    lon1Input.setError("latitude should be three digits max before decimal");
                    notComplete = true;
                }

                if(lon2.isEmpty()){
                    lon2Input.setError("Field is empty");
                    notComplete = true;
                } else if (!lon2.matches(COORDINATE_REGEX)){
                    lon2Input.setError("use format of 000.0000000");
                    notComplete = true;
                } else if (longitudeDigit(lon2)){
                    lon2Input.setError("latitude should be three digits max before decimal");
                    notComplete = true;
                }

                if(!notComplete){
                    lat1Text = lat1;
                    lat2Text = lat2;
                    lon1Text = lon1;
                    lon2Text = lon2;

                    dialog.dismiss();
                }
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                dialog.cancel();
            }
        });
    }

    private String limitDigit(String input){
        int last = input.length()-1;
        int decimal = input.indexOf(".");

        if((last - decimal) > 7)
            return input.substring(0,(decimal + 7));
        else
            return input;
    }

    private boolean longitudeDigit(String lat){
        if(lat.charAt(0) == '-')
            lat = lat.substring(1);

        if(lat.indexOf(".") > 3)
            return true;
        else
            return false;
    }

    private boolean latitudeDigit(String lat){
        if(lat.charAt(0) == '-')
            lat = lat.substring(1);

        if(lat.indexOf(".") > 2)
            return true;
        else
            return false;
    }
}
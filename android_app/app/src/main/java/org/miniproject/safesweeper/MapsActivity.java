package org.miniproject.safesweeper;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, PopupMenu.OnMenuItemClickListener{

    private GoogleMap mMap;
    private ServerConnection mines;
    private ServerInfo serverInfo;
    private ServerConnection conn;
    private String address;
    private boolean connected = true;
    private BluetoothAdapter BA;
    private double lowLat = 0.0, highLat = 0.0, leftLong = 0.0, rightLong = 0.0;


    Button menuBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        address = getIntent().getStringExtra("MAC");
        extractBoundary();
        menuBtn = (Button) findViewById(R.id.menuBtn);
        BA = BluetoothAdapter.getDefaultAdapter();

        serverInfo = new ServerInfo();
        conn = new ServerConnection(serverInfo);


        menuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(MapsActivity.this, v);
                popup.setOnMenuItemClickListener(MapsActivity.this);
                popup.inflate(R.menu.popup_menu);
                popup.show();
            }
        });
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        new loadMines().execute();
    }
    public class loadMines extends AsyncTask<Void, Void, ArrayList<Mine>> {

        @Override
        protected ArrayList<Mine> doInBackground(Void... voids) {

            ArrayList<Mine> mine = conn.getMines();
            return mine;
        }

        @Override
        protected void onPostExecute(ArrayList<Mine> mine) {
            for (Mine currentMine : mine) {
                LatLng toMarker = new LatLng(currentMine.getLat(), currentMine.getLng());
                mMap.addMarker(new MarkerOptions().position(toMarker).title("Mine"));


            }

            if (lowLat != highLat) {    //this means there is a boundary set

                // Add a thin red line1
                Polyline line1 = mMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(lowLat, leftLong), new LatLng(lowLat, rightLong))
                        .width(10)
                        .color(Color.RED));

                // Add a thin red line2
                Polyline line2 = mMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(lowLat, leftLong), new LatLng(highLat, leftLong))
                        .width(10)
                        .color(Color.RED));

                // Add a thin red line3
                Polyline line3 = mMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(highLat, rightLong), new LatLng(highLat, leftLong))
                        .width(10)
                        .color(Color.RED));

                // Add a thin red line4
                Polyline line4 = mMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(highLat, rightLong), new LatLng(lowLat, rightLong))
                        .width(10)
                        .color(Color.RED));
            }

        }
    }

    public void extractBoundary(){
        String text = address.substring(address.indexOf(" ")).trim();

        if(text.length() > 0){
            lowLat = Double.parseDouble(text.substring(0, text.indexOf(" ")));
            text = text.substring(text.indexOf(" ") + 1);

            highLat = Double.parseDouble(text.substring(0, text.indexOf(" ")));
            text = text.substring(text.indexOf(" ") + 1);

            leftLong = Double.parseDouble(text.substring(0, text.indexOf(" ")));

            rightLong = Double.parseDouble(text.substring(text.indexOf(" ") + 1));
        }
    }

    public boolean onMenuItemClick(MenuItem item) {
        Toast.makeText(this, "Selected Item: " +item.getTitle(), Toast.LENGTH_SHORT).show();
        switch (item.getItemId()) {
            case R.id.map_item:
                Toast.makeText(this,"You are already on this page!", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.control_item:
                Intent intentC = new Intent(this, MainActivity.class);
                address = address.substring(0, address.indexOf(" ")).trim();   //incase boundary was sent together
                intentC.putExtra("MAC", address);
                //intentC.putExtra("MAP", "YES");
                startActivity(intentC);
                return true;
            case R.id.bluetooth_item:
                Intent intentB = new Intent(this, BluetoothActivity.class);
                startActivity(intentB);
                return true;
            default:
                return false;
        }
    }
}
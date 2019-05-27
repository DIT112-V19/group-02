package org.miniproject.safesweeper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class BluetoothActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener{

    private Set<BluetoothDevice> pairedDevices;
    private BluetoothAdapter BA;

    ListView lv;
    Button btnOn, btnOff, btnRefresh, menuBtn;
    static String address;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        BA = BluetoothAdapter.getDefaultAdapter();

        btnOn = (Button) findViewById(R.id.btnOn);
        btnOff = (Button) findViewById(R.id.btnOff);
        btnRefresh = (Button) findViewById(R.id.btnRefresh);
        lv = (ListView) findViewById(R.id.listView);
        menuBtn = (Button) findViewById(R.id.menuBtn);

        menuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(BluetoothActivity.this, v);
                popup.setOnMenuItemClickListener(BluetoothActivity.this);
                popup.inflate(R.menu.popup_menu);
                popup.show();
            }
        });

    }

    public void on(View v) {
        if (!BA.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
        }
    }

    public void off(View v) {
        BA.disable();
        Toast.makeText(getApplicationContext(), "Turned off", Toast.LENGTH_LONG).show();
    }

    public void refresh(View v) {
        pairedDevices = BA.getBondedDevices();

        ArrayList list = new ArrayList();
        for (BluetoothDevice bt : pairedDevices){
            list.add(bt.getName() + "\n" + bt.getAddress());
        }

        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);

        lv.setAdapter(adapter);
        lv.setOnItemClickListener(myListClickListener);
    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // Get the device MAC address, the last 17 chars in the View
            String info = ((TextView) view).getText().toString();
            address = info.substring(info.length() - 17);
            // Make an intent to start next activity.
            Intent i = new Intent(BluetoothActivity.this, MainActivity.class);
            i.putExtra("MAC", address);
            startActivity(i);
        }
    };

    public boolean onMenuItemClick(MenuItem item) {
        Toast.makeText(this, "Selected Item: " +item.getTitle(), Toast.LENGTH_SHORT).show();
        switch (item.getItemId()) {
            /*
            case R.id.home_item:
                Intent intentH = new Intent(this, HomeActivity.class);
                startActivity(intentH);
                return true;*/
            case R.id.map_item:
                //Intent intent = new Intent(this, HomeActivity.class);
                return true;
            case R.id.control_item:
                if (address != null) {
                    Intent intentC = new Intent(this, MainActivity.class);

                    startActivity(intentC);
                } else
                {
                    Toast.makeText(this,"Please select a bluetooth connection first!", Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.bluetooth_item:
                Toast.makeText(this,"You are already on this page!", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return false;
        }
    }
}

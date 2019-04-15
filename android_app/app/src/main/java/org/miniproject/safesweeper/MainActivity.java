package org.miniproject.safesweeper;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    Button btn1;
    TextView textView1;
    SeekBar throttleBar;
    SeekBar steeringBar;
    TextView throttleText;
    TextView steeringText;

    public static final int THROTTLE_MIN = -30;
    public static final int THROTTLE_DEFAULT = 30;

    public static final int STEERING_MIN = -50;
    public static final int STEERING_DEFAULT = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn1 = (Button) findViewById(R.id.btn1);
        textView1 = (TextView) findViewById(R.id.textView1);
        throttleBar = (SeekBar) findViewById(R.id.throttleBar);
        steeringBar = (SeekBar) findViewById(R.id.steeringBar);
        throttleText = (TextView) findViewById(R.id.throttleText);
        steeringText = (TextView) findViewById(R.id.steeringText);

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView1.setText("It works");
            }
        });

        throttleBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                throttleText.setText((THROTTLE_MIN + progress) + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                throttleBar.setProgress(THROTTLE_DEFAULT);
            }
        });

        steeringBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                steeringText.setText((STEERING_MIN + progress) + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                steeringBar.setProgress(STEERING_DEFAULT);
            }
        });


    }
}

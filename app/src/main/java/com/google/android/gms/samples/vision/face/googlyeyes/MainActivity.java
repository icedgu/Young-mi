package com.google.android.gms.samples.vision.face.googlyeyes;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private Button btnTrack, btnBle;
    private long pressedTime=0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        btnTrack = (Button) findViewById(R.id.btnStartTrk);
        btnBle = (Button) findViewById(R.id.btnStartBle);

        btnTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent faceTrackIntent = new Intent(MainActivity.this, EyesActivity.class);
                startActivity(faceTrackIntent);
            }
        });

        btnBle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bleIntent = new Intent(MainActivity.this, LinkingActivity.class);
                startActivity(bleIntent);
            }
        });

    }

    @Override
    public void onBackPressed() {
        if (pressedTime == 0) {
            Toast.makeText(MainActivity.this, " 한 번 더 누르면 종료됩니다.", Toast.LENGTH_LONG).show();
            pressedTime = System.currentTimeMillis();
        } else {
            int seconds = (int) (System.currentTimeMillis() - pressedTime);

            if (seconds > 2000) {
                Toast.makeText(MainActivity.this, " 한 번 더 누르면 종료됩니다.", Toast.LENGTH_LONG).show();
                pressedTime = 0;
            } else {
                super.onBackPressed();  //finish(); // app 종료 시키기
            }
        }
    }
}

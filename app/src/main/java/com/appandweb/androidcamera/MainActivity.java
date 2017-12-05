package com.appandweb.androidcamera;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchCamera();
            }
        });

        findViewById(R.id.btn_legacy_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchLegacyCamera();
            }

        });
    }

    protected void launchCamera() {
        Intent intent = new Intent(this, CustomCameraActivity.class);
        startActivity(intent);
    }

    protected void launchLegacyCamera() {
        Intent intent = new Intent(this, LegacyCameraActivity.class);
        startActivity(intent);
    }
}

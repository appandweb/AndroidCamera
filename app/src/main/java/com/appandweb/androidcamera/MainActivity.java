package com.appandweb.androidcamera;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;

import static com.appandweb.androidcamera.EditPictureActivity.EXTRA_FORCE_FIXED_ASPECT_RATIO;
import static com.appandweb.androidcamera.EditPictureActivity.EXTRA_PICTURE_FILE;

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

        findViewById(R.id.btn_original_edit_picture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchOriginalEditPicture();
            }
        });

        findViewById(R.id.btn_edit_picture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchEditPicture();
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

    protected void launchOriginalEditPicture() {
        File f = new File(this.getExternalFilesDir(null), "pic.jpg");
        Intent intent = new Intent(this, EditPictureActivity.class);
        intent.putExtra(EXTRA_PICTURE_FILE, f.getAbsolutePath());
        intent.putExtra(EXTRA_FORCE_FIXED_ASPECT_RATIO, false);
        startActivity(intent);
    }

    protected void launchEditPicture() {
        File f = new File(this.getExternalFilesDir(null), "pic.jpg");
        CropImage.activity(Uri.parse(f.getAbsolutePath()))
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this);
    }
}

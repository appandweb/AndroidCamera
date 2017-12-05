package com.appandweb.androidcamera;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import java.io.File;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;

public class CustomCameraActivity extends AppCompatActivity implements CustomCameraFragment.Callbacks {
    public static final String EXTRA_PICTURE_FILE = "pictureFile";
    public static final String EXTRA_CAMERA = "camera";

    public static final int FRONT_CAMERA = CAMERA_FACING_FRONT;
    public static final int REAR_CAMERA = CAMERA_FACING_BACK;

    String picturePath = "";
    AlertDialog permissionsDialog;

    public int getLayoutId() {
        return R.layout.activity_custom_camera;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getLayoutId());

        int camera = getCameraFromExtras(getIntent());
        createCameraFragment(camera);
    }

    private int getCameraFromExtras(Intent intent) {
        if (intent != null) {
            return intent.getIntExtra(EXTRA_CAMERA, REAR_CAMERA);
        }

        return REAR_CAMERA;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (permissionsDialog != null) {
            permissionsDialog.cancel();
        }
    }

    private void createCameraFragment(int camera) {
        Bundle args = new Bundle();
        args.putInt(EXTRA_CAMERA, camera);
        Fragment f = Fragment.instantiate(this, CustomCameraFragment.class.getName(), args);
        if (f instanceof CustomCameraFragment) {
            ((CustomCameraFragment) f).setListener(this);
        }
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .replace(R.id.custom_camera_root, f)
                .commit();
    }

    @Override
    public void onPictureTaken(File pictureFile) {
        picturePath = pictureFile.getAbsolutePath();

        Intent data = new Intent();
        data.putExtra(EXTRA_PICTURE_FILE, picturePath);

        setResult(Activity.RESULT_OK, data);
        finish();
    }

    @Override
    public void onPictureError(Exception e) {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }
}
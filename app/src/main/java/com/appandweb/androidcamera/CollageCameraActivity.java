package com.appandweb.androidcamera;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class CollageCameraActivity extends CustomCameraActivity implements CameraListener {
    @Override
    protected Fragment createCameraFragment(int camera) {
        Bundle args = new Bundle();
        args.putInt(EXTRA_CAMERA, camera);
        Fragment f = Fragment.instantiate(this, CollageCameraFragment.class.getName(), args);
        if (f instanceof CollageCameraFragment) {
            ((CollageCameraFragment) f).setListener(this);
        }
        return f;
    }
}

package com.appandweb.androidcamera;

import java.io.File;

public interface CameraListener {
    void onPictureTaken(File pictureFile);

    void onPictureError(Exception e);
}
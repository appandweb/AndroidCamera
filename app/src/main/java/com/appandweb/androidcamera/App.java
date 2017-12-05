package com.appandweb.androidcamera;

import android.app.Application;

import java.io.File;

public class App extends Application {
    public static final String IMAGES_DIR = "images";

    public File getPicturesDir() {
        File f = getExternalFilesDir(IMAGES_DIR);
        return f;
    }
}

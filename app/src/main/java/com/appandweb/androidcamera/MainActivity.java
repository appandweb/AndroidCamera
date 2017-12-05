package com.appandweb.androidcamera;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

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

        findViewById(R.id.btn_collage_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchCollageCamera();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri uri = result.getUri();
                File src = new File(uri.getPath());
                File dst = new File(this.getExternalFilesDir(null), "cropped.jpg");
                copyFile(src, dst);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception e = result.getError();
                e.printStackTrace();
            }
        }
    }

    public void copyFile(File src, File dst) {
        try {
            if (src.exists()) {
                FileChannel srcChannel = new FileInputStream(src).getChannel();
                FileChannel dstChannel = new FileOutputStream(dst).getChannel();
                dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                srcChannel.close();
                dstChannel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        CropImage.activity(Uri.fromFile(f))
                .start(this);
    }

    protected void launchCollageCamera() {

    }
}

package com.appandweb.androidcamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.edmodo.cropper.CropImageView;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class EditPictureActivity extends AppCompatActivity {
    public static final String EXTRA_PICTURE_FILE = "pictureFile";
    public static final String EXTRA_FORCE_FIXED_ASPECT_RATIO = "forceFixedAspectRatio";

    protected static final long ACCEPT_BUTTON_DELAY = 3500;

    CropImageView cropImageView;
    ProgressBar progressBar;
    ImageButton btnAccept;
    ImageButton btnCancel;
    ImageButton btnRotate;

    private Target defaultTarget = new Target() {
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            cropImageView.setImageBitmap(bitmap);
            btnAccept.setVisibility(View.VISIBLE);
        }

        public void onBitmapFailed(Drawable errorDrawable) {
            Log.d(getClass().getName(), String.format("Err", 1));
            Toast.makeText(EditPictureActivity.this, "Bitmap load failed", Toast.LENGTH_SHORT).show();
        }

        public void onPrepareLoad(Drawable placeHolderDrawable) {
        }
    };

    public int getLayoutId() {
        return R.layout.activity_edit_picture;
    }

    String picturePath = "";
    boolean forceFixedAspectRatio = true;

    //region Activity lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getLayoutId());

        picturePath = getPicturePathFromExtras();
        forceFixedAspectRatio = getForceAspectRationParameterFromExtras();

        attachViewListeners();

        loadPicture(picturePath);
        configureCropImageView();
        displayAcceptButtonAfterAFewSeconds();

        // workAroundRotateImage90DegreesInSamsungDevices(); // Fixed in new samsungs
    }

    private void attachViewListeners() {
        cropImageView = (CropImageView)
                findViewById(R.id.picture_edit_img);

        progressBar = (ProgressBar)
                findViewById(R.id.picture_edit_pbr_loading);

        btnAccept = (ImageButton)
                findViewById(R.id.picture_edit_btn_accept);

        btnCancel = (ImageButton)
                findViewById(R.id.picture_edit_btn_cancel);

        btnRotate = (ImageButton)
                findViewById(R.id.picture_edit_btn_rotate);

        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAcceptClick(v);
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleCancelClick(v);
            }
        });

        btnRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleRotateClick(v);
            }
        });
    }

    private void displayAcceptButtonAfterAFewSeconds() {
        btnAccept.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (btnAccept != null && btnAccept.getVisibility() != View.VISIBLE) {
                    btnAccept.setVisibility(View.VISIBLE);
                }
            }
        }, ACCEPT_BUTTON_DELAY);
    }

    //endregion

    //region Business logic
    private void configureCropImageView() {
        cropImageView.setImageResource(R.mipmap.ic_loading_centered);
        cropImageView.setFixedAspectRatio(forceFixedAspectRatio);
        cropImageView.setGuidelines(1);
    }

    private void loadPicture(String path) {
        final int w = getScreenWidth(this);
        final int h = w;
        final File f = new File(path);

        btnRotate.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (f.exists()) {
                    Picasso.with(EditPictureActivity.this)
                            .load(f)
                            .resize(w, h)
                            .centerInside()
                            .error(android.R.drawable.ic_menu_close_clear_cancel)
                            .into(defaultTarget);
                } else {
                    Toast.makeText(EditPictureActivity.this, String.format("File not found: %s", f.getAbsolutePath()), Toast.LENGTH_SHORT).show();
                }

            }
        }, 1500);
    }

    private int getScreenWidth(Context ctx) {
        int w = 0;
        if (ctx instanceof Activity) {
            DisplayMetrics displaymetrics = new DisplayMetrics();
            ((Activity) ctx).getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            w = displaymetrics.widthPixels;
        }
        return w;
    }

    private String getPicturePathFromExtras() {
        if (getIntent() != null && getIntent().hasExtra(EXTRA_PICTURE_FILE))
            return getIntent().getStringExtra(EXTRA_PICTURE_FILE);

        return "";
    }

    private boolean getForceAspectRationParameterFromExtras() {
        if (getIntent() != null)
            return getIntent().getBooleanExtra(EXTRA_FORCE_FIXED_ASPECT_RATIO, true);

        return true;
    }

    protected void handleCancelClick(View v) {
        setResult(RESULT_CANCELED);
        finish();
    }

    protected void handleAcceptClick(View v) {
        showLoading();
        Bitmap bmp = cropImageView.getCroppedImage();
        saveBitmapToFile(bmp, picturePath);

        Intent data = new Intent();
        data.putExtra(EXTRA_PICTURE_FILE, picturePath);
        setResult(RESULT_OK, data);
        finish();
    }

    protected void handleRotateClick(View v) {
        cropImageView.rotateImage(90);
    }

    protected void workAroundRotateImage90DegreesInSamsungDevices() {
        if (Build.MANUFACTURER.equalsIgnoreCase("samsung")) {
            cropImageView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    cropImageView.rotateImage(90);

                }
            }, ACCEPT_BUTTON_DELAY + 100);
        }
    }

    public static void saveBitmapToFile(Bitmap bmp, String absolutePath) {
        bitmapToFile(getBytesFromBitmap(bmp), absolutePath);
    }

    private static void bitmapToFile(byte[] data, String absolutePath) {
        if (absolutePath == null) {
            return;
        }
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(absolutePath);
            fos.write(data, 0, data.length);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    private static byte[] getBytesFromBitmap(Bitmap bmp) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
        return bos.toByteArray();
    }

    //endregion

    //region View methods

    public void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        hideButtons();
    }

    public void hideLoading() {
        progressBar.setVisibility(View.INVISIBLE);
        showButtons();
    }

    public void showButtons() {
        showButtons(View.VISIBLE);
    }

    public void hideButtons() {
        showButtons(View.INVISIBLE);
    }

    public void showButtons(int visibility) {
        btnAccept.setVisibility(visibility);
        btnCancel.setVisibility(visibility);
        btnRotate.setVisibility(visibility);
    }

    //endregion
}

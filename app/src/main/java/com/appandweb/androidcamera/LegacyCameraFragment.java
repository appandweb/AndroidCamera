package com.appandweb.androidcamera;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
import static com.appandweb.androidcamera.CustomCameraActivity.EXTRA_CAMERA;

public class LegacyCameraFragment extends Fragment {
    // Native camera.
    private Camera camera;

    // View to display the camera output.
    private CameraPreview cameraPreview;

    // Reference to the containing view.
    private View cameraView;

    private View cameraSwap;
    private static int cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;

    /**
     * Default empty constructor.
     */
    public LegacyCameraFragment() {
        super();
    }

    /**
     * OnCreateView fragment override
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(getLayoutId(), container, false);

        cameraId = getCameraFromArguments(getArguments());
        // Create our Preview view and set it as the content of our activity.
        boolean open = safeCameraOpenInView(view);

        if (!open) {
            Log.d("CameraGuide", "AbsError, Camera failed to open");
            return view;
        }

        // Trap the capture button.
        View captureButton = (View) view.findViewById(R.id.custom_camera_btn_capture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        camera.takePicture(null, rawCallback, picture);
                    }
                }
        );
        cameraSwap = (View) view.findViewById(R.id.custom_camera_btn_swap_camera);
        cameraSwap.setVisibility(Camera.getNumberOfCameras() > 1 ? View.VISIBLE : View.GONE);
        cameraSwap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doCameraSwap();
            }
        });
        return view;
    }

    private void doCameraSwap() {

        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        if (cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            cameraId = CAMERA_FACING_BACK;
        } else {
            cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }

        camera = Camera.open(cameraId);
        setCameraDisplayOrientation(getActivity(), cameraId, camera);
        try {
            camera.setPreviewDisplay(cameraPreview.getHolder());
        } catch (IOException e) {
            e.printStackTrace();
        }
        camera.startPreview();
    }

    private void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private int getCameraFromArguments(Bundle arguments) {
        if (arguments != null) {
            return arguments.getInt(EXTRA_CAMERA, CAMERA_FACING_BACK);
        }

        return CAMERA_FACING_BACK;
    }

    protected int getLayoutId() {
        return R.layout.fragment_legacy_camera;
    }

    /**
     * Recommended "safe" way to open the camera.
     *
     * @param view
     * @return
     */
    private boolean safeCameraOpenInView(View view) {
        boolean qOpened = false;
        releaseCameraAndPreview();
        camera = getCameraInstance();
        cameraView = view;
        qOpened = (camera != null);

        if (qOpened) {
            cameraPreview = new CameraPreview(getActivity().getBaseContext(), camera, view);
            FrameLayout preview = (FrameLayout) view.findViewById(R.id.custom_camera_preview);
            preview.addView(cameraPreview);
            cameraPreview.startCameraPreview();
        }
        return qOpened;
    }

    /**
     * Safe method for getting a camera instance.
     *
     * @return
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            if (Camera.getNumberOfCameras() > 1) {
                c = Camera.open(cameraId);
            } else {
                c = Camera.open(); // attempt to get a Camera instance
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return c; // returns null if camera is unavailable
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseCameraAndPreview();
    }

    /**
     * Clear any existing preview / camera.
     */
    private void releaseCameraAndPreview() {

        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        if (cameraPreview != null) {
            cameraPreview.destroyDrawingCache();
            cameraPreview.camera = null;
        }
    }

    /**
     * Surface on which the camera projects it's capture results. This is derived both from Google's docs and the
     * excellent StackOverflow answer provided below.
     * <p/>
     * Reference / Credit: http://stackoverflow.com/questions/7942378/android-camera-will-not-work-startpreview-fails
     */
    class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

        // SurfaceHolder
        private SurfaceHolder holder;

        // Our Camera.
        private Camera camera;

        // Parent Context.
        private Context context;

        // Camera Sizing (For rotation, orientation changes)
        private Camera.Size previewSize;

        // List of supported preview sizes
        private List<Camera.Size> supportedPreviewSizes;

        // Flash modes supported by this camera
        private List<String> supportedFlashModes;

        // View holding this camera.
        private View cameraView;

        public CameraPreview(Context context, Camera camera, View cameraView) {
            super(context);

            // Capture the context
            this.cameraView = cameraView;
            this.context = context;
            setCamera(camera);

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            holder = getHolder();
            holder.addCallback(this);
            holder.setKeepScreenOn(true);
            // deprecated setting, but required on Android versions prior to 3.0
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        /**
         * Begin the preview of the camera input.
         */
        public void startCameraPreview() {
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Extract supported preview and flash modes from the camera.
         *
         * @param camera
         */
        private void setCamera(Camera camera) {
            // Source: http://stackoverflow.com/questions/7942378/android-camera-will-not-work-startpreview-fails
            this.camera = camera;
            supportedPreviewSizes = this.camera.getParameters().getSupportedPreviewSizes();
            supportedFlashModes = this.camera.getParameters().getSupportedFlashModes();

            // Set the camera to Auto Flash mode.
            if (supportedFlashModes != null && supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                Camera.Parameters parameters = this.camera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                this.camera.setParameters(parameters);
            }

            requestLayout();
        }

        /**
         * The Surface has been created, now tell the camera where to draw the preview.
         *
         * @param holder
         */
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                if (camera != null)
                    camera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Dispose of the camera preview.
         *
         * @param holder
         */
        public void surfaceDestroyed(SurfaceHolder holder) {
            releaseCameraAndPreview();
        }

        /**
         * React to surface changed events
         *
         * @param holder
         * @param format
         * @param w
         * @param h
         */
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (this.holder.getSurface() == null) {
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                Camera.Parameters parameters = camera.getParameters();

                // Set the auto-focus mode to "continuous"
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

                // Preview size must exist.
                if (previewSize != null) {
                    Camera.Size previewSize = this.previewSize;
                    parameters.setPreviewSize(previewSize.width, previewSize.height);
                }

                camera.setParameters(parameters);
                camera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Calculate the measurements of the layout
         *
         * @param widthMeasureSpec
         * @param heightMeasureSpec
         */
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            // Source: http://stackoverflow.com/questions/7942378/android-camera-will-not-work-startpreview-fails
            final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
            final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
            setMeasuredDimension(width, height);

            if (supportedPreviewSizes != null) {
                previewSize = getOptimalPreviewSize(supportedPreviewSizes, width, height);
            }
        }

        /**
         * Update the layout based on rotation and orientation changes.
         *
         * @param changed
         * @param left
         * @param top
         * @param right
         * @param bottom
         */
        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            // Source: http://stackoverflow.com/questions/7942378/android-camera-will-not-work-startpreview-fails
            if (changed) {
                final int width = right - left;
                final int height = bottom - top;

                int previewWidth = width;
                int previewHeight = height;

                if (previewSize != null) {
                    Display display = ((WindowManager)
                            context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

                    switch (display.getRotation()) {
                        case Surface.ROTATION_0:
                            previewWidth = previewSize.height;
                            previewHeight = previewSize.width;
                            camera.setDisplayOrientation(90);
                            break;
                        case Surface.ROTATION_90:
                            previewWidth = previewSize.width;
                            previewHeight = previewSize.height;
                            break;
                        case Surface.ROTATION_180:
                            previewWidth = previewSize.height;
                            previewHeight = previewSize.width;
                            break;
                        case Surface.ROTATION_270:
                            previewWidth = previewSize.width;
                            previewHeight = previewSize.height;
                            camera.setDisplayOrientation(180);
                            break;
                    }
                }

                final int scaledChildHeight = previewHeight * width / previewWidth;
                cameraView.layout(0, height - scaledChildHeight, width, height);
            }
        }

        /**
         * @param sizes
         * @param width
         * @param height
         * @return
         */
        private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int width, int height) {
            // Source: http://stackoverflow.com/questions/7942378/android-camera-will-not-work-startpreview-fails
            Camera.Size optimalSize = null;

            final double aspectTolerance = 0.1;
            double targetRatio = (double) height / width;

            // Try to find a size match which suits the whole screen minus the menu on the left.
            for (Camera.Size size : sizes) {

                if (size.height != width)
                    continue;
                double ratio = (double) size.width / size.height;
                if (ratio <= targetRatio + aspectTolerance && ratio >= targetRatio - aspectTolerance) {
                    optimalSize = size;
                }
            }

            // If we cannot find the one that matches the aspect ratio, assign a default size
            if (optimalSize == null && sizes != null && sizes.size() > 0) {
                optimalSize = sizes.get(sizes.size() - 1);
            }

            return optimalSize;
        }
    }

    /**
     * Picture Callback for handling a picture capture and saving it out to a file.
     */
    private Camera.PictureCallback picture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile();
            if (pictureFile == null) {
                Toast.makeText(getActivity(), "Image retrieval failed.", Toast.LENGTH_SHORT)
                        .show();
                return;
            }

            try {
                Log.i(getClass().getName(), "Data Size: " + data.length);
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();

                // Restart the camera preview.
                safeCameraOpenInView(cameraView);
                listener.onPictureTaken(pictureFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                listener.onPictureError(e);
            } catch (IOException e) {
                e.printStackTrace();
                listener.onPictureError(e);
            }
        }
    };

    /**
     * Used to return the camera File output.
     *
     * @return
     */
    private File getOutputMediaFile() {

        File picturesDir = ((App) getActivity().getApplication()).getPicturesDir();
        if (!picturesDir.exists()) {
            if (!picturesDir.mkdirs()) {
                Log.d("Camera Guide", "Required media storage does not exist");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        mediaFile = new File(picturesDir.getPath()
                + File.separator
                + String.format("profilePicture_%s.jpg", timeStamp));

        //DialogHelper.showDialog( "Success!","Your picture has been saved!",getActivity());

        return mediaFile;
    }

    //region Fragment Callbacks

    CameraListener listener = new EmptyListener();

    public void setListener(CameraListener listener) {
        if (listener != null)
            this.listener = listener;
    }

    private class EmptyListener implements CameraListener {
        public void onPictureTaken(File pictureFile) {
            /* Empty */
        }

        public void onPictureError(Exception e) {
            /* Empty */
        }
    }

    //endregion

    //region Raw Picture Callback
    Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.i(getClass().getName(), "RAW Data Size: " + (data != null ? data.length : 0));
        }
    };
    //endregion
}

package com.applex.camera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCapture.OutputFileOptions;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    public static final int REQ_CODE = 1012;
    public static final int RESULT_ACTIVITY_CODE = 1013;
    Executor executor = Executors.newSingleThreadExecutor();
    PreviewView mPreviewView;
    String qr1, qr2;

    ImageCapture imageCapture;
    CameraControl cameraControl;
    CameraInfo cameraInfo;
    TextView zoomTv;
    boolean isFlashOn;
    ImageView capture;

    View box;
    boolean qrCodeChecked = false;
    ImageView machineImage;
    int machineId;

    Bitmap roiBitmap, croppedImage;
    String name;

    @Override
    @androidx.camera.core.ExperimentalGetImage
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        checkPermissions();

//        module = Module.load(ResultActivity.getPath("jit_model_320_EP_500.pt", MainActivity.this));

        mPreviewView = findViewById(R.id.viewFinder);
        zoomTv = findViewById(R.id.tv_zoom);
        capture = findViewById(R.id.btn_capture);
//        box = findViewById(R.id.box_main);

//        machineId = getIntent().getIntExtra(getString(R.string.machine_id), R.id.rb_machine_2);
//        machineImage = findViewById(R.id.box_machine);
//        resetMachineImage();

        MyGestureListener gestureListener = new MyGestureListener();
        ScaleGestureDetector detector = new ScaleGestureDetector(this, gestureListener);

        View frameLayout = findViewById(R.id.frame_layout);
        frameLayout.setOnTouchListener((view, motionEvent) -> detector.onTouchEvent(motionEvent));

        ImageView flashBtn = findViewById(R.id.btn_flash);

        flashBtn.setOnClickListener(view -> {
            isFlashOn = !isFlashOn;
            if(isFlashOn)
                imageCapture.setFlashMode(ImageCapture.FLASH_MODE_ON);
            else
                imageCapture.setFlashMode(ImageCapture.FLASH_MODE_OFF);

            flashBtn.setImageDrawable(isFlashOn ? ContextCompat.getDrawable(this, R.drawable.flashlight) : ContextCompat.getDrawable(this, R.drawable.flashlight_off));
        });

        capture.setOnClickListener(view -> {
            File f1 =  new File(Environment.getExternalStorageDirectory() + "/AppCam" );
            f1.mkdirs();
            if(f1.exists()) {
                f1.mkdir();
            }

            long ts = Calendar.getInstance().getTimeInMillis();
            File file = new File(f1, ts+".jpg");

            OutputFileOptions outputFileOptions = new OutputFileOptions.Builder(file)
                    .build();
            imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
//                    Toast.makeText(CameraActivity.this, "Saved", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Toast.makeText(CameraActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();

                }
            });
        });


        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        frameLayout.setLayoutParams(new LinearLayout.LayoutParams(width, (int) (width * (4.7 / 3f))));
//        frameLayout.setLayoutParams(new LinearLayout.LayoutParams(width, height));

//        org.opencv.core.Rect rect = ResultActivity.getRect(machineId, width);
//        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(rect.width, rect.height);
//        params.gravity = Gravity.CENTER_HORIZONTAL;
//        params.setMargins(0, rect.y, 0, 0);
//        box.setLayoutParams(params);
    }

    @androidx.camera.core.ExperimentalGetImage
    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();


        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .build();


        imageCapture = new ImageCapture.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).build();

        preview.setSurfaceProvider(mPreviewView.createSurfaceProvider());

        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, imageCapture);
        cameraControl = camera.getCameraControl();
        cameraInfo = camera.getCameraInfo();



        zoomTv.setText(String.format(Locale.getDefault(), "%d%%", (int) cameraInfo.getZoomState().getValue().getLinearZoom() * 100));
    }

    @androidx.camera.core.ExperimentalGetImage
    void checkPermissions() {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_CODE);
        } else {
            startCamera();
        }
    }

    @Override
    @androidx.camera.core.ExperimentalGetImage
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CODE) {
            // Checking whether user granted the permission or not.
            if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Showing the toast message
                startCamera();
            } else {
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // In the SimpleOnGestureListener subclass you should override
    // onDown and any other gesture that you want to detect.
    class MyGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float linearZoom = cameraInfo.getZoomState().getValue().getLinearZoom();
            float scale = linearZoom + (detector.getScaleFactor() - 1) / 10;
            cameraControl.setLinearZoom(scale);
            zoomTv.setText(String.format(Locale.getDefault(), "%d%%", (int) (linearZoom * 100)));
            return super.onScale(detector);
        }
    }
}
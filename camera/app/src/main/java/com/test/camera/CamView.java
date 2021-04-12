package com.test.camera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;

public class CamView extends AppCompatActivity {
    private static final int CAMERA_FACING = Camera.CameraInfo.CAMERA_FACING_BACK;

    private SurfaceView surfaceView;
    private CameraPreview mCameraPreview;

    final String TAG = getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera);

        surfaceView = findViewById(R.id.camera_preview_main);

        startCamera();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mCameraPreview.takePicture();
            }
        },500);
    }

    void startCamera(){
        mCameraPreview = new CameraPreview(this,this,CAMERA_FACING,surfaceView);
    }

}

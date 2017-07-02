package com.codespurt.imagefromfrontcamera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, TextureView.SurfaceTextureListener {

    private final int REQUEST_CAMERA = 9001;
    private final int PERMISSION_CAMERA = 1001;

    private Camera mCamera;

    private Button takePhoto;
    private TextureView mTextureView;
    private ImageView image;

    private boolean captureImage = false;
    private boolean exitApp = true;

    private String TAG = "Front Camera";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        takePhoto = (Button) findViewById(R.id.btn_take_photo);
        mTextureView = (TextureView) findViewById(R.id.img_selected);
        image = (ImageView) findViewById(R.id.image);
    }

    @Override
    protected void onResume() {
        super.onResume();
        takePhoto.setOnClickListener(this);
        mTextureView.setSurfaceTextureListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        takePhoto.setOnClickListener(null);
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_take_photo:
                captureImage = true;
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (exitApp) {
            super.onBackPressed();
        } else {
            image.setVisibility(View.GONE);
            mTextureView.setVisibility(View.VISIBLE);
            exitApp = true;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        if (!checkCameraPermission()) {
            return;
        }

        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCamera = Camera.open(i);
                break;
            }
        }

        if (mCamera == null) {
            Toast.makeText(this, "No front facing camera", Toast.LENGTH_SHORT).show();
            finish();
        }

        // This doesn 't work
        // setCameraDisplayOrientation(info, mCamera);

        // make portrait
        mCamera.setDisplayOrientation(90);
        // create a matrix to invert the x-plane
        Matrix matrix = new Matrix();
        matrix.setScale(-1, 1);
        // move it back to in view otherwise it'll be off to the left.
        matrix.postTranslate(width, 0);
        mTextureView.setTransform(matrix);

        try {
            // Tell the camera to write onto our textureView mTextureView
            mCamera.setPreviewTexture(surfaceTexture);
            mCamera.startPreview();
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        if (captureImage) {
            // This is where you get the image to check for barcode

            // Read the image from the SurfaceTexture
            Bitmap barcodeBmp = mTextureView.getBitmap();

            // Get pixel array
            int width = barcodeBmp.getWidth();
            int height = barcodeBmp.getHeight();
            int[] pixels = new int[barcodeBmp.getHeight() * barcodeBmp.getWidth()];
            barcodeBmp.getPixels(pixels, 0, width, 0, 0, width, height);

            mTextureView.setVisibility(View.GONE);
            image.setVisibility(View.VISIBLE);

            image.setImageBitmap(flipBitmapHorizontally(barcodeBmp));

            captureImage = false;
            exitApp = false;

            // If using zbar barcode processing library

            // Create a barcode image
//            Image barcode = new Image(width, height, "RGB4");
//            barcode.setData(pixels);
//            int result = mScanner.scanImage(barcode.convert("Y800"));
        }
    }

    private Bitmap flipBitmapHorizontally(Bitmap bmp) {
        Bitmap bInput = bmp;
        Matrix matrix = new Matrix();
        matrix.preScale(-1.0f, 1.0f);
        return Bitmap.createBitmap(bInput, 0, 0, bInput.getWidth(), bInput.getHeight(), matrix, true);
    }

    private Bitmap flipBitmapVertically(Bitmap bmp) {
        Bitmap bInput = bmp;
        Matrix matrix = new Matrix();
        matrix.preScale(1.0f, -1.0f);
        return Bitmap.createBitmap(bInput, 0, 0, bInput.getWidth(), bInput.getHeight(), matrix, true);
    }

    @SuppressWarnings("deprecation")
    private void setCameraDisplayOrientation(Camera.CameraInfo info, android.hardware.Camera camera) {
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
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
        result = (info.orientation + degrees) % 360;
        result = (360 - result) % 360;  // compensate the mirror

        camera.setDisplayOrientation(result);
    }

    private boolean checkCameraPermission() {
        // check camera permission
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // request permission
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CAMERA:

                    break;
            }
        }
    }
}